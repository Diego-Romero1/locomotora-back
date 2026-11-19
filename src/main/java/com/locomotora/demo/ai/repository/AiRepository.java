package com.locomotora.demo.ai.repository;

import com.locomotora.demo.ai.dto.ConversationResponse;
import com.locomotora.demo.ai.dto.MessageResponse;
import com.locomotora.demo.ai.dto.RecommendationResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AiRepository {
    private final JdbcTemplate jdbcTemplate;

    public AiRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ConversationResponse> findConversations(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT id, title, created_at, updated_at
                FROM ai_conversations
                WHERE user_id = ?
                ORDER BY updated_at DESC
                LIMIT 50
                """,
                (rs, rowNum) -> new ConversationResponse(
                        rs.getObject("id", UUID.class).toString(),
                        rs.getString("title"),
                        rs.getTimestamp("created_at").toInstant().toString(),
                        rs.getTimestamp("updated_at").toInstant().toString()
                ),
                userId
        );
    }

    /** Borra la conversacion (y sus mensajes, en cascada) si es del usuario. */
    public boolean deleteConversation(UUID conversationId, UUID userId) {
        return jdbcTemplate.update("DELETE FROM ai_conversations WHERE id = ? AND user_id = ?", conversationId, userId) > 0;
    }

    public List<MessageResponse> findMessages(UUID conversationId, UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT m.id, m.role, m.content, m.model, m.created_at
                FROM ai_messages m
                JOIN ai_conversations c ON c.id = m.conversation_id
                WHERE c.id = ? AND c.user_id = ?
                ORDER BY m.created_at
                """,
                this::mapMessage,
                conversationId,
                userId
        );
    }

    public UUID createConversation(UUID userId, String title, String contextSnapshot) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO ai_conversations (user_id, title, context_snapshot)
                VALUES (?, ?, CAST(? AS jsonb))
                RETURNING id
                """,
                UUID.class,
                userId,
                title,
                contextSnapshot
        );
    }

    public void createUserMessage(UUID conversationId, String message) {
        jdbcTemplate.update(
                "INSERT INTO ai_messages (conversation_id, role, content) VALUES (?, 'USER', ?)",
                conversationId,
                message
        );
    }

    /** @param metadata JSON con tokens, origen (CLAUDE/LOCAL) y modelo pedido. */
    public void createAssistantMessage(UUID conversationId, String answer, String model, String metadata) {
        jdbcTemplate.update(
                """
                INSERT INTO ai_messages (conversation_id, role, content, model, metadata)
                VALUES (?, 'ASSISTANT', ?, ?, CAST(? AS jsonb))
                """,
                conversationId,
                answer,
                model,
                metadata
        );
    }

    /** Llamada a una herramienta del agente: nombre, argumentos y resultado (recortado) en metadata. */
    public void createToolMessage(UUID conversationId, String content, String metadata) {
        jdbcTemplate.update(
                """
                INSERT INTO ai_messages (conversation_id, role, content, metadata)
                VALUES (?, 'TOOL', ?, CAST(? AS jsonb))
                """,
                conversationId,
                content,
                metadata
        );
    }

    /** Tokens de toda la app en las ultimas 24 horas (la clave de Groq es una sola). */
    public long sumTokensLastDay() {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(sum(input_tokens + output_tokens), 0) FROM ai_usage WHERE created_at > now() - interval '1 day'",
                Long.class
        );
        return total == null ? 0 : total;
    }

    /** Tokens del ultimo dia de una tarea del sistema (sin usuario), como el llenado del catalogo. */
    public long sumSystemTokensLastDay(String feature) {
        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(sum(input_tokens + output_tokens), 0) FROM ai_usage
                WHERE user_id IS NULL AND feature = ? AND created_at > now() - interval '1 day'
                """,
                Long.class,
                feature
        );
        return total == null ? 0 : total;
    }

    public boolean conversationBelongsTo(UUID conversationId, UUID userId) {
        Boolean owned = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM ai_conversations WHERE id = ? AND user_id = ?)",
                Boolean.class,
                conversationId,
                userId
        );
        return Boolean.TRUE.equals(owned);
    }

    /** Ultimos mensajes de la conversacion, en orden cronologico. */
    public List<MessageResponse> findRecentMessages(UUID conversationId, int limit) {
        return jdbcTemplate.query(
                """
                SELECT * FROM (
                    SELECT id, role, content, model, created_at
                    FROM ai_messages
                    WHERE conversation_id = ? AND role IN ('USER', 'ASSISTANT')
                    ORDER BY created_at DESC
                    LIMIT ?
                ) recent
                ORDER BY created_at
                """,
                this::mapMessage,
                conversationId,
                limit
        );
    }

    public record ModelChoice(String coachModel, String routineModel) {
    }

    public ModelChoice findModelChoice(UUID userId) {
        return jdbcTemplate.query(
                "SELECT coach_model, routine_model FROM user_ai_settings WHERE user_id = ?",
                rs -> rs.next()
                        ? new ModelChoice(rs.getString("coach_model"), rs.getString("routine_model"))
                        : new ModelChoice(null, null),
                userId
        );
    }

    public void saveModelChoice(UUID userId, String coachModel, String routineModel) {
        jdbcTemplate.update(
                """
                INSERT INTO user_ai_settings (user_id, coach_model, routine_model)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE
                SET coach_model = EXCLUDED.coach_model,
                    routine_model = EXCLUDED.routine_model,
                    updated_at = now()
                """,
                userId,
                coachModel,
                routineModel
        );
    }

    /** Llamadas al modelo de esa tarea en las ultimas 24 horas. */
    public int countUsageLastDay(UUID userId, String feature) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM ai_usage
                WHERE user_id = ? AND feature = ? AND created_at > now() - interval '1 day'
                """,
                Integer.class,
                userId,
                feature
        );
        return count == null ? 0 : count;
    }

    public void recordUsage(UUID userId, String feature, String model, long inputTokens, long outputTokens) {
        jdbcTemplate.update(
                "INSERT INTO ai_usage (user_id, feature, model, input_tokens, output_tokens) VALUES (?, ?, ?, ?, ?)",
                userId,
                feature,
                model,
                (int) Math.min(Integer.MAX_VALUE, inputTokens),
                (int) Math.min(Integer.MAX_VALUE, outputTokens)
        );
    }

    public void touchConversation(UUID conversationId) {
        jdbcTemplate.update("UPDATE ai_conversations SET updated_at = now() WHERE id = ?", conversationId);
    }

    public List<RecommendationResponse> findRecommendations(UUID userId) {
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
                userId
        );
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
}
