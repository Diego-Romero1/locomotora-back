package com.locomotora.demo.ai.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.locomotora.demo.ai.client.AiModelClient.Completion;
import com.locomotora.demo.ai.client.AiModelClient.Request;
import com.locomotora.demo.ai.client.AiModelClient.StopReason;
import com.locomotora.demo.ai.client.AiModelClient.ToolCall;
import com.locomotora.demo.ai.client.AiModelClient.ToolChoice;
import com.locomotora.demo.ai.client.AiModelClient.ToolSpec;
import com.locomotora.demo.ai.client.AiModelClient.Turn;
import com.locomotora.demo.ai.config.AiProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Contrato HTTP con GroqCloud (chat completions compatible con OpenAI), sin red. */
class GroqModelClientTest {
    private static final String BASE = "https://api.groq.test/openai/v1";
    private static final String URL = BASE + "/chat/completions";
    private static final String QWEN = AiProperties.DEFAULT_MODEL;

    private MockRestServiceServer server;
    private GroqModelClient client;
    private final List<Duration> sleeps = new ArrayList<>();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GroqModelClient(AiProperties.withKey("gsk-test", BASE), builder.build(), sleeps::add);
    }

    private static String reply(String content, String finish) {
        return """
                {"id":"chatcmpl-1","object":"chat.completion","model":"qwen/qwen3.8-27b",
                 "choices":[{"index":0,"message":{"role":"assistant","content":%s},"finish_reason":"%s"}],
                 "usage":{"prompt_tokens":120,"completion_tokens":30,"total_tokens":150},
                 "x_groq":{"id":"req_1"}}
                """.formatted(content == null ? "null" : "\"" + content + "\"", finish);
    }

    @Test
    void sendsAPlainChatWithQwenSamplingAndReadsUsage() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer gsk-test"))
                .andExpect(jsonPath("$.model").value(QWEN))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("Hola"))
                .andExpect(jsonPath("$.max_completion_tokens").value(1024))
                .andExpect(jsonPath("$.temperature").value(0.7))
                .andExpect(jsonPath("$.top_p").value(0.8))
                .andExpect(jsonPath("$.reasoning_effort").value("none"))
                .andExpect(jsonPath("$.reasoning_format").value("hidden"))
                .andExpect(jsonPath("$.tools").doesNotExist())
                .andExpect(jsonPath("$.response_format").doesNotExist())
                .andRespond(withSuccess(reply("<think>pienso</think>Hola, ¿en que te ayudo?", "stop"), MediaType.APPLICATION_JSON));

        Completion completion = client.complete(Request.text(QWEN, "Eres un coach", List.of(Turn.user("Hola")), 1024, "none"));

        assertEquals("Hola, ¿en que te ayudo?", completion.text());
        assertEquals(StopReason.END, completion.stopReason());
        assertEquals(120, completion.inputTokens());
        assertEquals(30, completion.outputTokens());
        server.verify();
    }

    @Test
    void reasoningModeUsesThinkingSampling() {
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.reasoning_effort").value("low"))
                .andExpect(jsonPath("$.temperature").value(1.0))
                .andExpect(jsonPath("$.top_p").value(0.95))
                .andRespond(withSuccess(reply("ok", "stop"), MediaType.APPLICATION_JSON));
        client.complete(Request.text(QWEN, "s", List.of(Turn.user("x")), 100, "low"));
        server.verify();
    }

    @Test
    void sendsToolsAndToolResultsAndParsesToolCalls() {
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.tools[0].type").value("function"))
                .andExpect(jsonPath("$.tools[0].function.name").value("search_exercises"))
                .andExpect(jsonPath("$.tools[0].function.parameters.type").value("object"))
                .andExpect(jsonPath("$.tool_choice").value("auto"))
                .andExpect(jsonPath("$.messages[2].role").value("assistant"))
                .andExpect(jsonPath("$.messages[2].content").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.messages[2].tool_calls[0].id").value("call_a"))
                .andExpect(jsonPath("$.messages[2].tool_calls[0].function.arguments").value("{\"muscle\":\"Pecho\"}"))
                .andExpect(jsonPath("$.messages[3].role").value("tool"))
                .andExpect(jsonPath("$.messages[3].tool_call_id").value("call_a"))
                .andExpect(jsonPath("$.messages[3].name").value("search_exercises"))
                .andRespond(withSuccess("""
                        {"model":"qwen/qwen3.8-27b","choices":[{"message":{"role":"assistant","content":null,
                          "tool_calls":[{"id":"call_b","type":"function","function":{"name":"list_my_routines","arguments":"{}"}}]},
                          "finish_reason":"tool_calls"}],"usage":{"prompt_tokens":300,"completion_tokens":12}}
                        """, MediaType.APPLICATION_JSON));

        Completion completion = client.complete(new Request(QWEN, "s",
                List.of(Turn.user("¿Que hago?"),
                        Turn.assistantToolCalls("", List.of(new ToolCall("call_a", "search_exercises", "{\"muscle\":\"Pecho\"}"))),
                        Turn.toolResult("call_a", "search_exercises", "{\"count\":0}")),
                1024, "none", null, null, false,
                List.of(new ToolSpec("search_exercises", "Busca", Map.of("type", "object", "properties", Map.of()))),
                ToolChoice.AUTO));

        assertEquals(StopReason.TOOL_CALLS, completion.stopReason());
        assertEquals(List.of(new ToolCall("call_b", "list_my_routines", "{}")), completion.toolCalls());
        assertEquals("", completion.text());
        server.verify();
    }

    @Test
    void structuredOutputIsStrictAndNeverMixedWithTools() {
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andExpect(jsonPath("$.response_format.json_schema.name").value("routine"))
                .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
                .andExpect(jsonPath("$.response_format.json_schema.schema.additionalProperties").value(false))
                .andExpect(jsonPath("$.tools").doesNotExist())
                .andExpect(jsonPath("$.tool_choice").doesNotExist())
                .andRespond(withSuccess(reply("{}", "length"), MediaType.APPLICATION_JSON));

        Completion completion = client.complete(new Request(QWEN, "s", List.of(Turn.user("x")), 4096, "none", "routine",
                Map.of("type", "object", "additionalProperties", false, "properties", Map.of(), "required", List.of()), true,
                List.of(new ToolSpec("t", "d", Map.of("type", "object"))), ToolChoice.AUTO));

        assertEquals(StopReason.MAX_TOKENS, completion.stopReason());
        server.verify();
    }

    @Test
    void shortRateLimitIsRetriedOnce() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .header("retry-after", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"message\":\"Rate limit reached\",\"type\":\"tokens\"}}"));
        server.expect(requestTo(URL)).andRespond(withSuccess(reply("ok", "stop"), MediaType.APPLICATION_JSON));

        assertEquals("ok", client.complete(Request.text(QWEN, "s", List.of(Turn.user("x")), 10, "none")).text());
        assertEquals(List.of(Duration.ofSeconds(2)), sleeps);
        server.verify();
    }

    @Test
    void rateLimitIsRetriedUpToTwice() {
        for (int index = 0; index < 2; index++) {
            server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("retry-after", "3")
                    .body("{\"error\":{\"message\":\"Rate limit reached\",\"type\":\"tokens\",\"code\":\"rate_limit_exceeded\"}}"));
        }
        server.expect(requestTo(URL)).andRespond(withSuccess(reply("ok", "stop"), MediaType.APPLICATION_JSON));

        assertEquals("ok", client.complete(Request.text(QWEN, "s", List.of(Turn.user("x")), 10, "none")).text());
        assertEquals(List.of(Duration.ofSeconds(3), Duration.ofSeconds(3)), sleeps);
        server.verify();
    }

    @Test
    void aRequestTooLargeForTheAccountIsNotRetried() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"message\":\"Request too large for model `qwen/qwen3.8-27b` on output tokens per minute (OTPM): "
                        + "Limit 1000, Requested 1024.\",\"type\":\"tokens\",\"code\":\"rate_limit_exceeded\"}}"));

        AiUnavailableException error = assertThrows(AiUnavailableException.class,
                () -> client.complete(Request.text(QWEN, "s", List.of(Turn.user("x")), 1024, "none")));

        assertEquals(413, error.status());
        assertFalse(error.rateLimited());
        assertTrue(sleeps.isEmpty());
        assertEquals("OTPM", GroqModelClient.limitKind("{\"error\":{\"message\":\"on output tokens per minute (OTPM): Limit 1000\"}}"));
        server.verify();
    }

    @Test
    void longRateLimitIsNotRetriedAndKeepsTheWait() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("retry-after", "30")
                .body("{\"error\":{\"message\":\"Rate limit reached\",\"type\":\"tokens\"}}"));

        AiUnavailableException error = assertThrows(AiUnavailableException.class,
                () -> client.complete(Request.text(QWEN, "s", List.of(Turn.user("x")), 10, "none")));

        assertTrue(error.rateLimited());
        assertEquals(30L, error.retryAfterSeconds());
        assertTrue(sleeps.isEmpty());
        server.verify();
    }

    @Test
    void serverErrorsAreRetriedOnceAndBadRequestsAreNot() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        AiUnavailableException unavailable = assertThrows(AiUnavailableException.class,
                () -> client.complete(Request.text(QWEN, "s", List.of(Turn.user("x")), 10, "none")));
        assertEquals(503, unavailable.status());
        assertEquals(1, sleeps.size());
        server.verify();

        server.reset();
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .body("{\"error\":{\"message\":\"json_validate_failed\",\"type\":\"invalid_request_error\"}}"));
        AiUnavailableException rejected = assertThrows(AiUnavailableException.class,
                () -> client.complete(Request.text(QWEN, "s", List.of(Turn.user("x")), 10, "none")));
        assertTrue(rejected.rejected());
        assertFalse(rejected.rateLimited());
        server.verify();
    }

    @Test
    void withoutKeyNothingIsSent() {
        GroqModelClient noKey = new GroqModelClient(AiProperties.withKey("", BASE), RestClient.builder().build(), sleeps::add);
        assertFalse(noKey.isConfigured());
        assertThrows(AiUnavailableException.class, () -> noKey.complete(Request.text(QWEN, "s", List.of(Turn.user("x")), 10, "none")));
    }
}
