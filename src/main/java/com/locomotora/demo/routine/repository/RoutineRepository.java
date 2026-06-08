package com.locomotora.demo.routine.repository;

import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import com.locomotora.demo.routine.dto.ExerciseResponse;
import com.locomotora.demo.routine.dto.RoutineCategoryResponse;
import com.locomotora.demo.routine.model.RoutineDayHeader;
import com.locomotora.demo.routine.model.RoutineHeader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RoutineRepository {
    private final JdbcTemplate jdbcTemplate;

    public RoutineRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UUID> findLatestActiveRoutineId(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT id FROM routines
                WHERE user_id = ? AND is_active = true
                ORDER BY updated_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                userId
        ).stream().findFirst();
    }

    public List<RoutineHeader> findActiveRoutines(UUID userId, boolean categoriesAvailable) {
        return jdbcTemplate.query(
                categoriesAvailable
                        ? """
                        SELECT r.id, r.title, r.description, r.objective, r.difficulty,
                               r.estimated_duration_minutes, r.days_per_week,
                               r.category_id, rc.name AS category_name,
                               r.source, r.template_key, r.routine_split
                        FROM routines r
                        LEFT JOIN routine_categories rc ON rc.id = r.category_id
                        WHERE r.user_id = ? AND r.is_active = true
                        ORDER BY r.updated_at DESC, r.title
                        """
                        : """
                        SELECT r.id, r.title, r.description, r.objective, r.difficulty,
                               r.estimated_duration_minutes, r.days_per_week,
                               NULL::uuid AS category_id, NULL::varchar AS category_name,
                               r.source, r.template_key, r.routine_split
                        FROM routines r
                        WHERE r.user_id = ? AND r.is_active = true
                        ORDER BY r.updated_at DESC, r.title
                        """,
                this::mapHeader,
                userId
        );
    }

    public List<RoutineHeader> findActiveRoutinesByCategory(UUID userId, UUID categoryId) {
        return jdbcTemplate.query(
                """
                SELECT r.id, r.title, r.description, r.objective, r.difficulty,
                       r.estimated_duration_minutes, r.days_per_week,
                       r.category_id, rc.name AS category_name,
                       r.source, r.template_key, r.routine_split
                FROM routines r
                LEFT JOIN routine_categories rc ON rc.id = r.category_id
                WHERE r.user_id = ?
                  AND r.is_active = true
                  AND r.category_id = ?
                ORDER BY r.updated_at DESC, r.title
                """,
                this::mapHeader,
                userId,
                categoryId
        );
    }

    public Optional<RoutineHeader> findActiveRoutine(UUID routineId, UUID userId, boolean categoriesAvailable) {
        return jdbcTemplate.query(
                categoriesAvailable
                        ? """
                        SELECT r.id, r.title, r.description, r.objective, r.difficulty,
                            r.estimated_duration_minutes, r.days_per_week,
                            r.category_id, rc.name AS category_name,
                            r.source, r.template_key, r.routine_split
                        FROM routines r
                        LEFT JOIN routine_categories rc ON rc.id = r.category_id
                        WHERE r.id = ? AND r.user_id = ? AND r.is_active = true
                        """
                        : """
                        SELECT r.id, r.title, r.description, r.objective, r.difficulty,
                            r.estimated_duration_minutes, r.days_per_week,
                            NULL::uuid AS category_id, NULL::varchar AS category_name,
                            r.source, r.template_key, r.routine_split
                        FROM routines r
                        WHERE r.id = ? AND r.user_id = ? AND r.is_active = true
                        """,
                this::mapHeader,
                routineId,
                userId
        ).stream().findFirst();
    }

    public List<RoutineCategoryResponse> findCategories() {
        return jdbcTemplate.query(
                """
                SELECT id, name, description, icon, color
                FROM routine_categories
                ORDER BY created_at, name
                """,
                (rs, rowNum) -> new RoutineCategoryResponse(
                        rs.getObject("id", UUID.class).toString(),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("icon"),
                        rs.getString("color")
                )
        );
    }

    public List<RoutineDayHeader> findDays(UUID routineId) {
        return jdbcTemplate.query(
                """
                SELECT id, day_index, title, focus
                FROM routine_days
                WHERE routine_id = ?
                ORDER BY day_index
                """,
                this::mapDayHeader,
                routineId
        );
    }

    public List<ExerciseResponse> findExercisesForDay(UUID dayId) {
        return jdbcTemplate.query(
                """
                SELECT re.id, re.exercise_id, e.name, e.primary_muscle_group,
                       re.sets, re.target_reps, re.reps_min, re.reps_max, re.rest_seconds, re.notes
                FROM routine_exercises re
                JOIN exercises e ON e.id = re.exercise_id
                WHERE re.routine_day_id = ?
                ORDER BY re.position
                """,
                this::mapExercise,
                dayId
        );
    }

    public List<ExerciseCatalogItemResponse> findExerciseCatalog() {
        return jdbcTemplate.query(
                """
                SELECT id, name, category, primary_muscle_group, equipment, difficulty, description, instructions
                FROM exercises
                WHERE is_active = true
                ORDER BY COALESCE(primary_muscle_group, 'ZZZ'), name
                """,
                this::mapExerciseCatalogItem
        );
    }

    public boolean routineCategoriesAvailable() {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM information_schema.tables
                WHERE table_schema = current_schema() AND table_name = 'routine_categories'
                """,
                Integer.class
        );
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = 'routines' AND column_name = 'category_id'
                """,
                Integer.class
        );
        return tableCount != null && tableCount > 0 && columnCount != null && columnCount > 0;
    }

    public boolean categoryExists(UUID categoryId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM routine_categories WHERE id = ?",
                Integer.class,
                categoryId
        );
        return count != null && count > 0;
    }

    private RoutineHeader mapHeader(ResultSet rs, int rowNum) throws SQLException {
        return new RoutineHeader(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("objective"),
                rs.getString("difficulty"),
                asInteger(rs.getObject("estimated_duration_minutes")),
                asInteger(rs.getObject("days_per_week")),
                rs.getObject("category_id", UUID.class),
                rs.getString("category_name"),
                rs.getString("source"),
                rs.getString("template_key"),
                rs.getString("routine_split")
        );
    }

    private RoutineDayHeader mapDayHeader(ResultSet rs, int rowNum) throws SQLException {
        return new RoutineDayHeader(
                rs.getObject("id", UUID.class),
                rs.getInt("day_index"),
                rs.getString("title"),
                rs.getString("focus")
        );
    }

    private ExerciseResponse mapExercise(ResultSet rs, int rowNum) throws SQLException {
        Integer targetReps = (Integer) rs.getObject("target_reps");
        Integer repsMin = (Integer) rs.getObject("reps_min");
        Integer repsMax = (Integer) rs.getObject("reps_max");
        return new ExerciseResponse(
                rs.getObject("id", UUID.class).toString(),
                rs.getObject("exercise_id", UUID.class).toString(),
                rs.getString("name"),
                rs.getInt("sets"),
                targetReps != null ? targetReps : (repsMax != null ? repsMax : (repsMin != null ? repsMin : 0)),
                repsMin,
                repsMax,
                rs.getInt("rest_seconds"),
                rs.getString("primary_muscle_group"),
                rs.getString("notes")
        );
    }

    private ExerciseCatalogItemResponse mapExerciseCatalogItem(ResultSet rs, int rowNum) throws SQLException {
        return new ExerciseCatalogItemResponse(
                rs.getObject("id", UUID.class).toString(),
                rs.getString("name"),
                rs.getString("category"),
                rs.getString("primary_muscle_group"),
                rs.getString("equipment"),
                rs.getString("difficulty"),
                rs.getString("description"),
                rs.getString("instructions")
        );
    }

    private Integer asInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }
}
