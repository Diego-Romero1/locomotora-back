package com.locomotora.demo.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Prueba de integracion de la Fase 5 (Groq) con Postgres real (Testcontainers, migraciones V1–V12)
 * y un servidor falso de GroqCloud: la app completa, por HTTP, de punta a punta.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiGroqIntegrationTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String QWEN = "qwen/qwen3.8-27b";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    static final FakeGroq GROQ = FakeGroq.start();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("locomotora.ai.api-key", () -> "gsk-integration-test");
        registry.add("locomotora.ai.base-url", () -> GROQ.baseUrl());
        registry.add("locomotora.ai.daily-token-budget", () -> "100000");
    }

    @Autowired
    private TestRestTemplate http;
    @Autowired
    private JdbcTemplate jdbc;

    @AfterAll
    static void stopGroq() {
        GROQ.server.stop(0);
    }

    @BeforeEach
    void resetGroq() {
        GROQ.reset();
    }

    // ------------------------------------------------------------------ pruebas

    @Test
    void modelsShowOnlyQwenWithoutChoice() {
        String token = register();
        JsonNode models = get("/ai/models", token);
        assertTrue(models.path("configured").asBoolean());
        assertFalse(models.path("selectable").asBoolean());
        assertEquals(1, models.path("models").size());
        assertEquals(QWEN, models.path("coach").path("selected").asText());
    }

    @Test
    void chatToolsReadOnlyTheUsersOwnData() {
        String ana = register();
        JsonNode routine = post("/routines", ana, routineBody("Rutina de Ana", firstExercise()));
        String anaRoutineId = routine.path("id").asText();

        GROQ.enqueue(200, toolCall("list_my_routines", "{}"));
        GROQ.enqueue(200, text("Tu rutina es Rutina de Ana."));
        JsonNode reply = post("/ai/chat", ana, Map.of("message", "¿Que rutina tengo? mi mail es ana@mail.com"));

        assertEquals("AI", reply.path("source").asText());
        assertEquals("Tu rutina es Rutina de Ana.", reply.path("answer").asText());
        assertEquals(2, GROQ.received.size());
        JsonNode first = GROQ.received.get(0);
        assertEquals(QWEN, first.path("model").asText());
        assertEquals(5, first.path("tools").size());
        assertFalse(first.toString().contains("ana@mail.com"), "personal data must not leave the backend");
        JsonNode toolMessage = GROQ.received.get(1).path("messages").get(3);
        assertEquals("tool", toolMessage.path("role").asText());
        assertTrue(toolMessage.path("content").asText().contains("Rutina de Ana"));

        UUID conversation = UUID.fromString(reply.path("conversationId").asText());
        assertEquals(1, count("SELECT count(*) FROM ai_messages WHERE conversation_id = ? AND role = 'TOOL'", conversation));
        assertEquals(3, count("SELECT count(*) FROM ai_messages WHERE conversation_id = ?", conversation));
        Map<String, Object> usage = jdbc.queryForMap(
                "SELECT count(*) AS calls, sum(input_tokens) AS input FROM ai_usage a JOIN ai_conversations c ON c.user_id = a.user_id WHERE c.id = ?",
                conversation);
        assertEquals(1L, ((Number) usage.get("calls")).longValue(), "one usage row per chat message");
        assertEquals(240L, ((Number) usage.get("input")).longValue());

        // Otro usuario no puede leer la rutina de Ana aunque el modelo pida su id.
        String bruno = register();
        GROQ.reset();
        GROQ.enqueue(200, toolCall("get_routine", "{\"routine_id\":\"" + anaRoutineId + "\"}"));
        GROQ.enqueue(200, text("No encontre esa rutina."));
        post("/ai/chat", bruno, Map.of("message", "Muestrame la rutina " + anaRoutineId));
        String toolResult = GROQ.received.get(1).path("messages").get(3).path("content").asText();
        assertEquals("{\"error\":\"not_found\"}", toolResult);
    }

    @Test
    void generatedRoutineIsValidatedAgainstTheCatalogAndSavedThroughTheNormalRules() {
        String token = register();
        GROQ.enqueue(200, text("""
                {"title":"Full body IA","description":"Dos dias para empezar.","difficulty":"BEGINNER","estimatedDurationMinutes":45,
                 "days":[{"title":"Dia A","focus":["Pecho"],"exercises":[
                   {"ref":1,"sets":3,"repsMin":8,"repsMax":12,"restSeconds":90,"notes":"Controla la bajada"},
                   {"ref":9999,"sets":3,"repsMin":8,"repsMax":12,"restSeconds":90,"notes":""}]},
                  {"title":"Dia B","focus":[],"exercises":[{"ref":2,"sets":3,"repsMin":10,"repsMax":10,"restSeconds":60,"notes":""}]}]}
                """.replace("\n", "").replace("\"", "\\\"")));

        JsonNode draft = post("/routines/generate", token, Map.of("daysPerWeek", 2, "sessionMinutes", 45, "level", "BEGINNER"));

        JsonNode request = GROQ.received.get(0);
        assertEquals("json_schema", request.path("response_format").path("type").asText());
        assertTrue(request.path("response_format").path("json_schema").path("strict").asBoolean());
        assertTrue(request.path("tools").isMissingNode(), "structured output never goes with tools");
        assertTrue(request.path("messages").get(1).path("content").asText().contains("1 | "));

        assertEquals(2, draft.path("days").size());
        assertTrue(draft.path("warnings").toString().contains("UNKNOWN_EXERCISES_DROPPED"));
        String exerciseId = draft.path("days").get(0).path("exercises").get(0).path("exerciseId").asText();
        assertEquals(1, count("SELECT count(*) FROM exercises WHERE id = ?", UUID.fromString(exerciseId)));
        assertEquals(0, count("SELECT count(*) FROM routines r JOIN users u ON u.id = r.user_id WHERE r.title = 'Full body IA'", null),
                "generating never saves");

        // Guardar pasa por POST /routines y sus validaciones, con origen AI.
        Map<String, Object> exercise = Map.of(
                "exerciseId", exerciseId,
                "name", draft.path("days").get(0).path("exercises").get(0).path("name").asText(),
                "sets", 3, "repsMin", 8, "repsMax", 12, "restSeconds", 90);
        Map<String, Object> body = new java.util.HashMap<>(routineBody("Full body IA", exercise));
        body.put("source", "AI");
        JsonNode saved = post("/routines", token, body);
        assertEquals("AI", saved.path("source").asText());
    }

    @Test
    void providerRateLimitFallsBackToTheLocalCoach() {
        String token = register();
        GROQ.enqueue(429, "{\"error\":{\"message\":\"Rate limit reached\",\"type\":\"tokens\"}}", "retry-after", "60");

        JsonNode reply = post("/ai/chat", token, Map.of("message", "¿Cuanta proteina necesito?"));

        assertEquals("LOCAL", reply.path("source").asText());
        assertEquals(1, GROQ.received.size(), "a long retry-after is not retried");
    }

    @Test
    void whenTheAppBudgetIsSpentNothingIsSentToGroq() {
        String token = register();
        UUID userId = UUID.fromString(jdbc.queryForObject("SELECT id::text FROM users ORDER BY created_at DESC LIMIT 1", String.class));
        jdbc.update("INSERT INTO ai_usage (user_id, feature, model, input_tokens, output_tokens) VALUES (?, 'ROUTINE', ?, 100000, 0)",
                userId, QWEN);
        try {
            JsonNode reply = post("/ai/chat", token, Map.of("message", "Hola"));
            assertEquals("LOCAL", reply.path("source").asText());
            assertTrue(GROQ.received.isEmpty());

            ResponseEntity<String> generate = exchange(HttpMethod.POST, "/routines/generate", token, Map.of());
            assertEquals(429, generate.getStatusCode().value());
        } finally {
            jdbc.update("DELETE FROM ai_usage WHERE input_tokens = 100000");
        }
    }

    @Test
    void aiEndpointsRequireAuthentication() {
        assertTrue(http.getForEntity("/ai/models", String.class).getStatusCode().is4xxClientError());
    }

    // ------------------------------------------------------------------ ayudas

    private Map<String, Object> firstExercise() {
        Map<String, Object> exercise = jdbc.queryForMap(
                "SELECT id::text AS id, name FROM exercises WHERE created_by_user_id IS NULL AND is_active ORDER BY name LIMIT 1");
        return Map.of("exerciseId", exercise.get("id"), "name", exercise.get("name"),
                "sets", 3, "repsMin", 8, "repsMax", 12, "restSeconds", 90);
    }

    private static Map<String, Object> routineBody(String title, Map<String, Object> exercise) {
        return Map.of(
                "title", title,
                "objective", "BUILD_MUSCLE",
                "difficulty", "BEGINNER",
                "daysPerWeek", 1,
                "estimatedDurationMinutes", 45,
                "days", List.of(Map.of("dayIndex", 1, "title", "Dia A", "exercises", List.of(exercise))));
    }

    private String register() {
        String email = "it-" + UUID.randomUUID() + "@test.local";
        JsonNode auth = read(http.postForEntity("/auth/register",
                Map.of("name", "Prueba", "email", email, "password", "Password123!"), String.class));
        return auth.path("token").asText();
    }

    private JsonNode get(String path, String token) {
        return read(exchange(HttpMethod.GET, path, token, null));
    }

    private JsonNode post(String path, String token, Object body) {
        ResponseEntity<String> response = exchange(HttpMethod.POST, path, token, body);
        assertTrue(response.getStatusCode().is2xxSuccessful(), path + " -> " + response.getStatusCode() + " " + response.getBody());
        return read(response);
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private static JsonNode read(ResponseEntity<String> response) {
        try {
            return JSON.readTree(response.getBody());
        } catch (IOException invalid) {
            throw new AssertionError("not JSON: " + response.getBody(), invalid);
        }
    }

    private int count(String sql, UUID id) {
        Integer value = id == null ? jdbc.queryForObject(sql, Integer.class) : jdbc.queryForObject(sql, Integer.class, id);
        return value == null ? 0 : value;
    }

    private static String text(String content) {
        return "{\"model\":\"" + QWEN + "\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\""
                + content + "\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":140,\"completion_tokens\":25}}";
    }

    private static String toolCall(String name, String arguments) {
        return "{\"model\":\"" + QWEN + "\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":null,"
                + "\"tool_calls\":[{\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"" + name + "\",\"arguments\":"
                + JSON.valueToTree(arguments) + "}}]},\"finish_reason\":\"tool_calls\"}],"
                + "\"usage\":{\"prompt_tokens\":100,\"completion_tokens\":10}}";
    }

    /** Servidor HTTP minimo que responde lo encolado y guarda cada peticion. */
    static final class FakeGroq {
        final HttpServer server;
        final List<JsonNode> received = Collections.synchronizedList(new ArrayList<>());
        private final Deque<String[]> responses = new ArrayDeque<>();

        private FakeGroq(HttpServer server) {
            this.server = server;
        }

        static FakeGroq start() {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                FakeGroq fake = new FakeGroq(server);
                server.createContext("/openai/v1/chat/completions", exchange -> {
                    byte[] request = exchange.getRequestBody().readAllBytes();
                    fake.received.add(JSON.readTree(request));
                    String[] next;
                    synchronized (fake.responses) {
                        next = fake.responses.isEmpty()
                                ? new String[] {"500", "{\"error\":{\"message\":\"nothing queued\",\"type\":\"test\"}}"}
                                : fake.responses.poll();
                    }
                    for (int index = 2; index + 1 < next.length; index += 2) {
                        exchange.getResponseHeaders().add(next[index], next[index + 1]);
                    }
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    byte[] body = next[1].getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(Integer.parseInt(next[0]), body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
                server.start();
                return fake;
            } catch (IOException failed) {
                throw new IllegalStateException(failed);
            }
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/openai/v1";
        }

        void enqueue(int status, String body, String... headers) {
            String[] entry = new String[2 + headers.length];
            entry[0] = String.valueOf(status);
            entry[1] = body;
            System.arraycopy(headers, 0, entry, 2, headers.length);
            synchronized (responses) {
                responses.add(entry);
            }
        }

        void reset() {
            received.clear();
            synchronized (responses) {
                responses.clear();
            }
        }
    }
}
