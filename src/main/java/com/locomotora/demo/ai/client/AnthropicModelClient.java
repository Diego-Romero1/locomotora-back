package com.locomotora.demo.ai.client;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.beta.messages.BetaContentBlock;
import com.anthropic.models.beta.messages.BetaFallbacksParam;
import com.anthropic.models.beta.messages.BetaJsonOutputFormat;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.locomotora.demo.ai.config.AiProperties;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Cliente de Claude (API de Anthropic). Usa el endpoint beta de mensajes para poder pedir
 * modelos de respaldo en el servidor cuando el modelo elegido los necesita.
 */
@Component
public class AnthropicModelClient implements AiModelClient {
    static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";
    private static final Logger log = LoggerFactory.getLogger(AnthropicModelClient.class);

    private final AnthropicClient client;

    public AnthropicModelClient(AiProperties properties) {
        if (!properties.hasApiKey()) {
            this.client = null;
            return;
        }
        AnthropicOkHttpClient.Builder builder = AnthropicOkHttpClient.builder()
                .apiKey(properties.apiKey())
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .maxRetries(2);
        if (!properties.baseUrl().isEmpty()) {
            builder.baseUrl(properties.baseUrl());
        }
        this.client = builder.build();
    }

    @Override
    public boolean isConfigured() {
        return client != null;
    }

    @Override
    public Completion complete(Request request) {
        if (client == null) {
            throw new AiUnavailableException("ANTHROPIC_API_KEY is not configured");
        }
        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(request.model())
                .maxTokens(request.maxTokens())
                .system(request.system());
        for (Turn turn : request.turns()) {
            if (turn.role() == Role.USER) {
                params.addUserMessage(turn.text());
            } else {
                params.addAssistantMessage(turn.text());
            }
        }

        BetaOutputConfig.Builder output = BetaOutputConfig.builder().effort(effort(request.effort()));
        if (request.jsonSchema() != null) {
            BetaJsonOutputFormat.Schema.Builder schema = BetaJsonOutputFormat.Schema.builder();
            request.jsonSchema().forEach((key, value) -> schema.putAdditionalProperty(key, JsonValue.from(value)));
            output.format(BetaJsonOutputFormat.builder().schema(schema.build()).build());
        }
        params.outputConfig(output.build());

        if (request.fallbacks()) {
            params.addBeta(FALLBACK_BETA).fallbacks(BetaFallbacksParam.ofDefault());
        }

        BetaMessage message;
        try {
            message = client.beta().messages().create(params.build());
        } catch (AnthropicException exception) {
            log.warn("Claude request failed for model {}: {}", request.model(), exception.getMessage());
            throw new AiUnavailableException("Claude request failed", exception);
        }

        String text = message.content().stream()
                .map(BetaContentBlock::text)
                .flatMap(java.util.Optional::stream)
                .map(block -> block.text())
                .collect(Collectors.joining());
        return new Completion(
                text,
                message.model().asString(),
                stopReason(message),
                message.usage().inputTokens(),
                message.usage().outputTokens()
        );
    }

    private static StopReason stopReason(BetaMessage message) {
        BetaStopReason reason = message.stopReason().orElse(null);
        if (BetaStopReason.END_TURN.equals(reason)) {
            return StopReason.END;
        }
        if (BetaStopReason.MAX_TOKENS.equals(reason)) {
            return StopReason.MAX_TOKENS;
        }
        if (BetaStopReason.REFUSAL.equals(reason)) {
            return StopReason.REFUSAL;
        }
        return StopReason.OTHER;
    }

    private static BetaOutputConfig.Effort effort(String value) {
        Map<String, BetaOutputConfig.Effort> efforts = Map.of(
                "low", BetaOutputConfig.Effort.LOW,
                "medium", BetaOutputConfig.Effort.MEDIUM,
                "high", BetaOutputConfig.Effort.HIGH);
        return efforts.getOrDefault(value == null ? "" : value, BetaOutputConfig.Effort.MEDIUM);
    }

    @PreDestroy
    void close() {
        if (client != null) {
            client.close();
        }
    }
}
