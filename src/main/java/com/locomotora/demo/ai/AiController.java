package com.locomotora.demo.ai;

import com.locomotora.demo.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {
    private final JdbcTemplate jdbcTemplate;

    public AiController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/ai/conversations")
    public List<ConversationResponse> conversations() {
        return jdbcTemplate.query(
                """
                SELECT id, title, created_at, updated_at
                FROM ai_conversations
                WHERE user_id = ?
                ORDER BY updated_at DESC
                """,
                (rs, rowNum) -> new ConversationResponse(
                        rs.getObject("id", UUID.class).toString(),
                        rs.getString("title"),
                        rs.getTimestamp("created_at").toInstant().toString(),
                        rs.getTimestamp("updated_at").toInstant().toString()
                ),
                CurrentUser.id()
        );
    }

    @GetMapping("/ai/conversations/{id}/messages")
    public List<MessageResponse> messages(@PathVariable UUID id) {
        return jdbcTemplate.query(
                """
                SELECT m.id, m.role, m.content, m.model, m.created_at
                FROM ai_messages m
                JOIN ai_conversations c ON c.id = m.conversation_id
                WHERE c.id = ? AND c.user_id = ?
                ORDER BY m.created_at
                """,
                this::mapMessage,
                id,
                CurrentUser.id()
        );
    }

    @PostMapping("/ai/chat")
    @Transactional
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        UUID userId = CurrentUser.id();
        UUID conversationId = request.conversationId() == null
                ? createConversation(userId, request.message())
                : UUID.fromString(request.conversationId());

        jdbcTemplate.update(
                "INSERT INTO ai_messages (conversation_id, role, content) VALUES (?, 'USER', ?)",
                conversationId,
                request.message()
        );

        String answer = buildCoachAnswer(userId, request.message());
        jdbcTemplate.update(
                "INSERT INTO ai_messages (conversation_id, role, content, model) VALUES (?, 'ASSISTANT', ?, 'local-context-coach-v1')",
                conversationId,
                answer
        );
        jdbcTemplate.update("UPDATE ai_conversations SET updated_at = now() WHERE id = ?", conversationId);

        UUID recommendationId = jdbcTemplate.queryForObject(
                """
                INSERT INTO ai_recommendations (user_id, conversation_id, type, title, content)
                VALUES (?, ?, 'GENERAL', 'AI Coach recommendation', ?)
                RETURNING id
                """,
                UUID.class,
                userId,
                conversationId,
                answer
        );

        return new ChatResponse(conversationId.toString(), answer, recommendationId.toString());
    }

    @GetMapping("/ai/recommendations")
    public List<RecommendationResponse> recommendations() {
        return jdbcTemplate.query(
                """
                SELECT id, type, title, content, status, created_at
                FROM ai_recommendations
                WHERE user_id = ?
                ORDER BY created_at DESC
                LIMIT 50
                """,
                (rs, rowNum) -> new RecommendationResponse(
                        rs.getObject("id", UUID.class).toString(),
                        rs.getString("type"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant().toString()
                ),
                CurrentUser.id()
        );
    }

    private UUID createConversation(UUID userId, String message) {
        String title = message.length() > 60 ? message.substring(0, 60) : message;
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO ai_conversations (user_id, title, context_snapshot)
                VALUES (?, ?, CAST(? AS jsonb))
                RETURNING id
                """,
                UUID.class,
                userId,
                title,
                contextSnapshot(userId)
        );
    }

    private String buildCoachAnswer(UUID userId, String message) {
        Integer workouts = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM workout_sessions WHERE user_id = ? AND status = 'COMPLETED'",
                Integer.class,
                userId
        );
        String goal = jdbcTemplate.query(
                "SELECT goal_type FROM user_goals WHERE user_id = ? AND status = 'ACTIVE' ORDER BY priority LIMIT 1",
                (rs, rowNum) -> rs.getString("goal_type"),
                userId
        ).stream().findFirst().orElse("general_fitness");
        String routine = jdbcTemplate.query(
                "SELECT title FROM routines WHERE user_id = ? AND is_active = true ORDER BY updated_at DESC LIMIT 1",
                (rs, rowNum) -> rs.getString("title"),
                userId
        ).stream().findFirst().orElse("tu rutina actual");

        String lower = message.toLowerCase();
        if (lower.contains("nutri") || lower.contains("comida") || lower.contains("prote")) {
            return "Para tu objetivo " + goal + ", prioriza proteina en cada comida, hidratos alrededor del entrenamiento y registra tus comidas en Nutricion. Con " + workouts + " entrenamientos completados, el siguiente paso es sostener consistencia y ajustar calorias segun tus metricas.";
        }
        if (lower.contains("rutina") || lower.contains("entren")) {
            return "Tu foco actual es " + goal + ". Usa " + routine + " como base, completa las sesiones y registra pesos/reps para progresar. Si una semana se siente demasiado pesada, reduce volumen antes de cambiar el objetivo.";
        }
        if (lower.contains("peso") || lower.contains("metrica") || lower.contains("progreso")) {
            return "Mira tendencia, no solo una medicion aislada. Registra peso y medidas 1 o 2 veces por semana, y compara contra entrenamientos completados. Hoy tengo registrados " + workouts + " entrenamientos completados.";
        }
        return "Puedo ayudarte con rutinas, nutricion y progreso usando tu perfil, objetivos, metricas y entrenamientos. Para darte una recomendacion mas fina, dime tu objetivo principal, dias disponibles y cualquier limitacion fisica.";
    }

    private String contextSnapshot(UUID userId) {
        Integer workouts = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM workout_sessions WHERE user_id = ? AND status = 'COMPLETED'",
                Integer.class,
                userId
        );
        return "{\"completedWorkouts\":" + (workouts == null ? 0 : workouts) + "}";
    }

    private MessageResponse mapMessage(ResultSet rs, int rowNum) throws SQLException {
        return new MessageResponse(
                rs.getObject("id", UUID.class).toString(),
                rs.getString("role"),
                rs.getString("content"),
                rs.getString("model"),
                rs.getTimestamp("created_at").toInstant().toString()
        );
    }

    public record ChatRequest(String conversationId, @NotBlank String message) {
    }

    public record ChatResponse(String conversationId, String answer, String recommendationId) {
    }

    public record ConversationResponse(String id, String title, String createdAt, String updatedAt) {
    }

    public record MessageResponse(String id, String role, String content, String model, String createdAt) {
    }

    public record RecommendationResponse(String id, String type, String title, String content, String status, String createdAt) {
    }
}
