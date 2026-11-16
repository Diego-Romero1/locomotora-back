package com.locomotora.demo.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.locomotora.demo.ai.config.AiProperties;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Cliente de GroqCloud ({@code POST /chat/completions}, compatible con OpenAI). Sin SDK: solo
 * {@link RestClient}. La clave sale de GROQ_API_KEY y nunca llega a la app ni a los logs.
 *
 * <p>Reintenta una vez ante 429/498 (si la espera pedida es corta) y ante 500/502/503. Los logs
 * guardan modelo, estado, latencia y el id de Groq, nunca el contenido de los mensajes.
 */
@Component
public class GroqModelClient implements AiModelClient {
    static final long MAX_RETRY_WAIT_SECONDS = 5;
    /** Un intento y hasta dos reintentos por limite de tokens por minuto; uno solo por errores 5xx. */
    static final int MAX_ATTEMPTS = 3;
    private static final Logger log = LoggerFactory.getLogger(GroqModelClient.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern THINK_BLOCK = Pattern.compile("(?s)<think>.*?</think>");

    /** Para no esperar de verdad en los tests. */
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final RestClient restClient;
    private final String apiKey;
    private final Sleeper sleeper;

    @Autowired
    public GroqModelClient(AiProperties properties, RestClient.Builder builder) {
        this(properties, builder.requestFactory(requestFactory(properties)).baseUrl(properties.baseUrl()).build(),
                duration -> Thread.sleep(duration.toMillis()));
    }

    GroqModelClient(AiProperties properties, RestClient restClient, Sleeper sleeper) {
        this.restClient = restClient;
        this.apiKey = properties.apiKey();
        this.sleeper = sleeper;
    }

    private static JdkClientHttpRequestFactory requestFactory(AiProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        return factory;
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    @Override
    public Completion complete(Request request) {
        if (!isConfigured()) {
            throw new AiUnavailableException("GROQ_API_KEY is not configured");
        }
        String body = body(request).toString();
        AiUnavailableException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            long started = System.nanoTime();
            Raw raw;
            try {
                raw = send(body);
            } catch (ResourceAccessException timeout) {
                log.warn("Groq request failed for model {}: {}", request.model(), timeout.getMessage());
                throw new AiUnavailableException("Groq request failed", timeout);
            }
            long millis = (System.nanoTime() - started) / 1_000_000;
            if (raw.status() >= 200 && raw.status() < 300) {
                Completion completion = parse(raw.body(), request.model());
                log.info("Groq ok model={} status={} ms={} in={} out={} stop={} tpmLeft={}", completion.model(), raw.status(), millis,
                        completion.inputTokens(), completion.outputTokens(), completion.stopReason(), raw.remainingTokens());
                return completion;
            }
            last = error(raw);
            log.warn("Groq error model={} status={} ms={} type={} code={} limit={} retryAfter={} tpmLeft={} attempt={}", request.model(),
                    raw.status(), millis, errorField(raw.body(), "type"), errorField(raw.body(), "code"), limitKind(raw.body()),
                    raw.retryAfter(), raw.remainingTokens(), attempt);
            if (last.status() == 413) {
                log.warn("Groq: la peticion supera el limite por minuto de la cuenta; baja LOCOMOTORA_AI_*_MAX_TOKENS o mejora el plan");
            }
            Duration wait = retryWait(last, attempt);
            if (attempt == MAX_ATTEMPTS || wait == null) {
                break;
            }
            try {
                sleeper.sleep(wait);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw last;
    }

    /** {@code remainingTokens}: tokens que le quedan a la cuenta en este minuto (cabecera de Groq). */
    private record Raw(int status, String retryAfter, String remainingTokens, String body) {
    }

    private Raw send(String body) {
        return restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(body)
                .exchange((req, res) -> new Raw(
                        res.getStatusCode().value(),
                        res.getHeaders().getFirst("retry-after"),
                        res.getHeaders().getFirst("x-ratelimit-remaining-tokens"),
                        readBody(res.getBody())));
    }

    private static String readBody(java.io.InputStream stream) throws IOException {
        return stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Espera antes del reintento, o null si no conviene reintentar. */
    static Duration retryWait(AiUnavailableException error, int attempt) {
        if (error.rateLimited()) {
            long seconds = error.retryAfterSeconds() == null ? 2 : error.retryAfterSeconds();
            return seconds <= MAX_RETRY_WAIT_SECONDS ? Duration.ofSeconds(Math.max(1, seconds)) : null;
        }
        int status = error.status();
        return attempt == 1 && (status == 500 || status == 502 || status == 503) ? Duration.ofSeconds(1) : null;
    }

    // ------------------------------------------------------------------ peticion

    static ObjectNode body(Request request) {
        ObjectNode body = JSON.createObjectNode();
        body.put("model", request.model());
        ArrayNode messages = body.putArray("messages");
        if (request.system() != null && !request.system().isBlank()) {
            messages.addObject().put("role", "system").put("content", request.system());
        }
        for (Turn turn : request.turns()) {
            ObjectNode message = messages.addObject();
            switch (turn.role()) {
                case USER -> message.put("role", "user").put("content", turn.text());
                case ASSISTANT -> {
                    message.put("role", "assistant");
                    if (turn.text() == null || turn.text().isEmpty()) {
                        message.putNull("content");
                    } else {
                        message.put("content", turn.text());
                    }
                    if (!turn.toolCalls().isEmpty()) {
                        ArrayNode calls = message.putArray("tool_calls");
                        for (ToolCall call : turn.toolCalls()) {
                            ObjectNode node = calls.addObject().put("id", call.id()).put("type", "function");
                            node.putObject("function").put("name", call.name()).put("arguments", call.arguments());
                        }
                    }
                }
                case TOOL -> message.put("role", "tool")
                        .put("tool_call_id", turn.toolCallId())
                        .put("name", turn.toolName())
                        .put("content", turn.text());
            }
        }
        body.put("max_completion_tokens", request.maxTokens());

        // Muestreo recomendado por Qwen: modo directo (sin razonamiento) o modo con razonamiento.
        boolean thinking = request.reasoningEffort() != null && !request.reasoningEffort().isBlank()
                && !"none".equals(request.reasoningEffort());
        body.put("temperature", thinking ? 1.0 : 0.7);
        body.put("top_p", thinking ? 0.95 : 0.8);
        if (request.reasoningEffort() != null && !request.reasoningEffort().isBlank()) {
            body.put("reasoning_effort", request.reasoningEffort());
            // Con herramientas o JSON solo se admite parsed o hidden; hidden deja solo la respuesta.
            body.put("reasoning_format", "hidden");
        }

        if (request.jsonSchema() != null) {
            ObjectNode format = body.putObject("response_format").put("type", "json_schema");
            ObjectNode schema = format.putObject("json_schema");
            schema.put("name", request.schemaName() == null ? "response" : request.schemaName());
            schema.put("strict", request.strictJson());
            schema.set("schema", JSON.valueToTree(request.jsonSchema()));
        } else if (!request.tools().isEmpty()) {
            ArrayNode tools = body.putArray("tools");
            for (ToolSpec tool : request.tools()) {
                ObjectNode function = tools.addObject().put("type", "function").putObject("function");
                function.put("name", tool.name()).put("description", tool.description());
                function.set("parameters", JSON.valueToTree(tool.parameters()));
            }
            body.put("tool_choice", request.toolChoice() == ToolChoice.NONE ? "none" : "auto");
            body.put("parallel_tool_calls", true);
        }
        return body;
    }

    // ------------------------------------------------------------------ respuesta

    static Completion parse(String raw, String requestedModel) {
        JsonNode root;
        try {
            root = JSON.readTree(raw);
        } catch (IOException invalid) {
            throw new AiUnavailableException("Groq returned an unreadable response", 502, null);
        }
        JsonNode choice = root.path("choices").path(0);
        JsonNode message = choice.path("message");
        List<ToolCall> calls = new ArrayList<>();
        for (JsonNode call : message.path("tool_calls")) {
            calls.add(new ToolCall(
                    call.path("id").asText(""),
                    call.path("function").path("name").asText(""),
                    call.path("function").path("arguments").asText("{}")));
        }
        String content = message.path("content").isTextual() ? message.path("content").asText() : "";
        content = THINK_BLOCK.matcher(content).replaceAll("").trim();
        boolean refused = message.path("refusal").isTextual() && !message.path("refusal").asText().isBlank();
        StopReason stop = switch (choice.path("finish_reason").asText("")) {
            case "stop" -> StopReason.END;
            case "length" -> StopReason.MAX_TOKENS;
            case "tool_calls" -> StopReason.TOOL_CALLS;
            case "content_filter" -> StopReason.REFUSAL;
            default -> calls.isEmpty() ? StopReason.OTHER : StopReason.TOOL_CALLS;
        };
        if (refused) {
            stop = StopReason.REFUSAL;
        }
        if (!calls.isEmpty() && stop == StopReason.END) {
            stop = StopReason.TOOL_CALLS;
        }
        JsonNode usage = root.path("usage");
        return new Completion(
                content,
                root.path("model").asText(requestedModel),
                stop,
                calls,
                usage.path("prompt_tokens").asLong(0),
                usage.path("completion_tokens").asLong(0)
        );
    }

    /** Limite de Groq que se paso, segun el mensaje del 429: TPM, OTPM, ITPM, RPM, RPD o TPD. */
    private static final Pattern LIMIT = Pattern.compile("\\((TPM|OTPM|ITPM|RPM|RPD|TPD|ASH|ASD)\\)");

    static String limitKind(String body) {
        Matcher matcher = LIMIT.matcher(errorField(body, "message"));
        return matcher.find() ? matcher.group(1) : "-";
    }

    private static AiUnavailableException error(Raw raw) {
        // "Request too large": la peticion sola pasa el limite por minuto (p. ej. max_completion_tokens > OTPM).
        // Reintentar no sirve; se informa como 413 para no confundirlo con un 429 pasajero.
        if (raw.status() == 429 && errorField(raw.body(), "message").startsWith("Request too large")) {
            return new AiUnavailableException("Groq request too large for the account limits", 413, null);
        }
        Long retryAfter = null;
        if (raw.retryAfter() != null) {
            try {
                retryAfter = (long) Math.ceil(Double.parseDouble(raw.retryAfter().trim()));
            } catch (NumberFormatException ignored) {
                retryAfter = null;
            }
        }
        return new AiUnavailableException("Groq returned " + raw.status(), raw.status(), retryAfter);
    }

    /** Solo campos cortos del error (type, code): el mensaje puede traer texto generado. */
    private static String errorField(String body, String field) {
        try {
            return JSON.readTree(body).path("error").path(field).asText("-");
        } catch (IOException | RuntimeException unreadable) {
            return "-";
        }
    }
}
