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

    public void createAssistantMessage(UUID conversationId, String answer) {
        jdbcTemplate.update(
                "INSERT INTO ai_messages (conversation_id, role, content, model) VALUES (?, 'ASSISTANT', ?, 'local-context-coach-v1')",
                conversationId,
                answer
        );
    }

    public void touchConversation(UUID conversationId) {
        jdbcTemplate.update("UPDATE ai_conversations SET updated_at = now() WHERE id = ?", conversationId);
    }

    public UUID createRecommendation(UUID userId, UUID conversationId, String answer) {
        return jdbcTemplate.queryForObject(
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

    public int countCompletedWorkouts(UUID userId) {
        Integer workouts = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM workout_sessions WHERE user_id = ? AND status = 'COMPLETED'",
                Integer.class,
                userId
        );
        return workouts == null ? 0 : workouts;
    }

    public String findPrimaryActiveGoal(UUID userId) {
        return jdbcTemplate.query(
                "SELECT goal_type FROM user_goals WHERE user_id = ? AND status = 'ACTIVE' ORDER BY priority LIMIT 1",
                (rs, rowNum) -> rs.getString("goal_type"),
                userId
        ).stream().findFirst().orElse("general_fitness");
    }

    public String findLatestActiveRoutineTitle(UUID userId) {
        return jdbcTemplate.query(
                "SELECT title FROM routines WHERE user_id = ? AND is_active = true ORDER BY updated_at DESC LIMIT 1",
                (rs, rowNum) -> rs.getString("title"),
                userId
        ).stream().findFirst().orElse("tu rutina actual");
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
