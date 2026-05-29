package com.locomotora.demo.routine;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoutineTemplateController {
    private static final Set<String> SUPPORTED_SPLITS = Set.of(
            "FULL_BODY",
            "UPPER_LOWER",
            "PUSH_PULL_LEGS",
            "PPL_UPPER_LOWER",
            "BRO_SPLIT"
    );
    private static final Set<String> ALLOWED_DIFFICULTIES = Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED");
    private static final String GOAL_HYPERTROPHY = "HYPERTROPHY";

    private final JdbcTemplate jdbcTemplate;

    public RoutineTemplateController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/routines/templates")
    public List<RoutineController.RoutineResponse> templates(@RequestParam(required = false) String goal) {
        String normalizedGoal = normalizeGoal(goal);
        List<UUID> ids = normalizedGoal == null
                ? jdbcTemplate.query(
                        """
                        SELECT id
                        FROM routines
                        WHERE user_id IS NULL AND source = 'TEMPLATE' AND is_active = true
                        ORDER BY days_per_week, difficulty, title
                        """,
                        (rs, rowNum) -> rs.getObject("id", UUID.class)
                )
                : jdbcTemplate.query(
                        """
                        SELECT id
                        FROM routines
                        WHERE user_id IS NULL AND source = 'TEMPLATE' AND is_active = true AND upper(objective) = ?
                        ORDER BY days_per_week, difficulty, title
                        """,
                        (rs, rowNum) -> rs.getObject("id", UUID.class),
                        normalizedGoal
                );
        return ids.stream().map(this::getTemplateResponse).toList();
    }

    @GetMapping("/routines/templates/{id}")
    public RoutineController.RoutineResponse getTemplate(@PathVariable UUID id) {
        return getTemplateResponse(id);
    }

    @GetMapping("/routines/recommendation")
    public RoutineController.RoutineResponse recommendation() {
        UUID userId = CurrentUser.id();
        UUID templateId = findRecommendedTemplateId(loadRecommendationProfile(userId));
        return getTemplateResponse(templateId);
    }

    @PostMapping("/routines/templates/{templateId}/use")
    @Transactional
    public RoutineController.RoutineResponse useTemplate(@PathVariable UUID templateId) {
        UUID userId = CurrentUser.id();
        UUID clonedId = cloneTemplateRoutine(templateId, userId);
        return getUserRoutineResponse(clonedId, userId);
    }

    @PostMapping("/routines/recommendation/use")
    @Transactional
    public RoutineController.RoutineResponse useRecommendation() {
        UUID userId = CurrentUser.id();
        UUID templateId = findRecommendedTemplateId(loadRecommendationProfile(userId));
        UUID clonedId = cloneTemplateRoutine(templateId, userId);
        return getUserRoutineResponse(clonedId, userId);
    }

    private RoutineController.RoutineResponse getTemplateResponse(UUID routineId) {
        boolean categoriesAvailable = routineCategoriesAvailable();
        TemplateHeader header = jdbcTemplate.query(
                categoriesAvailable
                        ? """
                        SELECT r.id, r.title, r.description, r.objective, r.difficulty,
                               r.estimated_duration_minutes, r.days_per_week,
                               r.category_id, rc.name AS category_name,
                               r.source, r.template_key, r.routine_split
                        FROM routines r
                        LEFT JOIN routine_categories rc ON rc.id = r.category_id
                        WHERE r.id = ? AND r.user_id IS NULL AND r.source = 'TEMPLATE' AND r.is_active = true
                        """
                        : """
                        SELECT r.id, r.title, r.description, r.objective, r.difficulty,
                               r.estimated_duration_minutes, r.days_per_week,
                               NULL::uuid AS category_id, NULL::varchar AS category_name,
                               r.source, r.template_key, r.routine_split
                        FROM routines r
                        WHERE r.id = ? AND r.user_id IS NULL AND r.source = 'TEMPLATE' AND r.is_active = true
                        """,
                this::mapHeader,
                routineId
        ).stream().findFirst().orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Routine template not found"));
        return buildRoutineResponse(header);
    }

    private RoutineController.RoutineResponse getUserRoutineResponse(UUID routineId, UUID userId) {
        boolean categoriesAvailable = routineCategoriesAvailable();
        TemplateHeader header = jdbcTemplate.query(
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
        ).stream().findFirst().orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Routine not found"));
        return buildRoutineResponse(header);
    }

    private RoutineController.RoutineResponse buildRoutineResponse(TemplateHeader header) {
        List<RoutineController.RoutineDayResponse> days = jdbcTemplate.query(
                """
                SELECT id, day_index, title, focus
                FROM routine_days
                WHERE routine_id = ?
                ORDER BY day_index
                """,
                this::mapDayHeader,
                header.id()
        ).stream().map(day -> new RoutineController.RoutineDayResponse(
                day.id().toString(),
                day.dayIndex(),
                day.title(),
                day.focus(),
                exercisesForDay(day.id())
        )).toList();

        List<RoutineController.ExerciseResponse> legacyExercises = days.isEmpty() ? List.of() : days.get(0).exercises();
        return new RoutineController.RoutineResponse(
                header.id().toString(),
                header.title(),
                header.description(),
                header.objective(),
                header.difficulty(),
                header.estimatedDurationMinutes(),
                header.daysPerWeek(),
                header.categoryId() == null ? null : header.categoryId().toString(),
                header.categoryName(),
                header.routineSplit(),
                header.source(),
                "TEMPLATE".equalsIgnoreCase(header.source()) && header.templateKey() != null,
                header.templateKey(),
                days,
                legacyExercises
        );
    }

    private List<RoutineController.ExerciseResponse> exercisesForDay(UUID dayId) {
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

    private UUID cloneTemplateRoutine(UUID templateId, UUID userId) {
        TemplateHeader template = jdbcTemplate.query(
                """
                SELECT id, title, description, objective, difficulty, estimated_duration_minutes, days_per_week,
                       category_id, NULL::varchar AS category_name, source, template_key, routine_split
                FROM routines
                WHERE id = ? AND user_id IS NULL AND source = 'TEMPLATE' AND is_active = true
                """,
                this::mapHeader,
                templateId
        ).stream().findFirst().orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Routine template not found"));

        UUID clonedId = jdbcTemplate.queryForObject(
                """
                INSERT INTO routines (user_id, title, description, objective, source, difficulty,
                                      estimated_duration_minutes, days_per_week, is_active, category_id, routine_split)
                VALUES (?, ?, ?, ?, 'TEMPLATE', ?, ?, ?, true, ?, ?)
                RETURNING id
                """,
                UUID.class,
                userId,
                template.title(),
                template.description(),
                template.objective(),
                template.difficulty(),
                template.estimatedDurationMinutes(),
                template.daysPerWeek(),
                template.categoryId(),
                template.routineSplit()
        );

        List<DayHeader> sourceDays = jdbcTemplate.query(
                """
                SELECT id, day_index, title, focus
                FROM routine_days
                WHERE routine_id = ?
                ORDER BY day_index
                """,
                this::mapDayHeader,
                templateId
        );
        for (DayHeader sourceDay : sourceDays) {
            UUID clonedDayId = jdbcTemplate.queryForObject(
                    "INSERT INTO routine_days (routine_id, day_index, title, focus) VALUES (?, ?, ?, ?) RETURNING id",
                    UUID.class,
                    clonedId,
                    sourceDay.dayIndex(),
                    sourceDay.title(),
                    sourceDay.focus()
            );
            jdbcTemplate.update(
                    """
                    INSERT INTO routine_exercises
                        (routine_day_id, exercise_id, position, sets, reps_min, reps_max, target_reps,
                         target_weight_kg, rest_seconds, tempo, notes)
                    SELECT ?, exercise_id, position, sets, reps_min, reps_max, target_reps,
                           target_weight_kg, rest_seconds, tempo, notes
                    FROM routine_exercises
                    WHERE routine_day_id = ?
                    ORDER BY position
                    """,
                    clonedDayId,
                    sourceDay.id()
            );
        }

        return clonedId;
    }

    private RecommendationProfile loadRecommendationProfile(UUID userId) {
        jdbcTemplate.update(
                """
                INSERT INTO user_profiles (user_id)
                VALUES (?)
                ON CONFLICT (user_id) DO NOTHING
                """,
                userId
        );

        return jdbcTemplate.query(
                """
                SELECT up.experience_level,
                       up.training_days_per_week,
                       up.session_duration_minutes,
                       up.medical_notes,
                       (
                           SELECT ug.goal_type
                           FROM user_goals ug
                           WHERE ug.user_id = up.user_id
                           ORDER BY ug.priority, ug.created_at DESC
                           LIMIT 1
                       ) AS goal_type,
                       (
                           SELECT pref.preferred_training_style
                           FROM user_preferences pref
                           WHERE pref.user_id = up.user_id
                           ORDER BY pref.created_at DESC
                           LIMIT 1
                       ) AS preferred_training_style,
                       (
                           SELECT count(*)
                           FROM user_limitations ul
                           WHERE ul.user_id = up.user_id
                       ) AS limitation_count
                FROM user_profiles up
                WHERE up.user_id = ?
                """,
                (rs, rowNum) -> new RecommendationProfile(
                        normalizeGoalWithFallback(rs.getString("goal_type")),
                        normalizeDifficultyOrDefault(rs.getString("experience_level")),
                        normalizePositiveOrDefault(asInteger(rs.getObject("training_days_per_week")), 3),
                        normalizePositiveOrDefault(asInteger(rs.getObject("session_duration_minutes")), 60),
                        nullableTrimmed(rs.getString("preferred_training_style")),
                        nullableTrimmed(rs.getString("medical_notes")),
                        rs.getInt("limitation_count")
                ),
                userId
        ).stream().findFirst().orElse(new RecommendationProfile(GOAL_HYPERTROPHY, "BEGINNER", 3, 60, null, null, 0));
    }

    private UUID findRecommendedTemplateId(RecommendationProfile profile) {
        List<TemplateCandidate> candidates = jdbcTemplate.query(
                """
                SELECT id, objective, difficulty, days_per_week, estimated_duration_minutes, routine_split
                FROM routines
                WHERE user_id IS NULL AND source = 'TEMPLATE' AND is_active = true
                """,
                this::mapTemplateCandidate
        );
        if (candidates.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No routine templates available");
        }

        List<String> splitPriority = new ArrayList<>();
        String preferredSplit = normalizeSplitPreference(profile.preferredTrainingStyle());
        if (preferredSplit != null && preferredSplitAllowed(preferredSplit, profile.experienceLevel())) {
            splitPriority.add(preferredSplit);
        }

        String derivedSplit = deriveRecommendedSplit(profile.trainingDaysPerWeek(), profile.experienceLevel());
        if (!splitPriority.contains(derivedSplit)) {
            splitPriority.add(derivedSplit);
        }

        List<String> compatibleDifficulties = compatibleDifficulties(profile.experienceLevel());
        String objective = normalizeGoalWithFallback(profile.goal());
        for (String split : splitPriority) {
            UUID match = pickBestTemplate(
                    candidates,
                    objective,
                    split,
                    compatibleDifficulties,
                    profile.trainingDaysPerWeek(),
                    profile.sessionDurationMinutes()
            );
            if (match != null) {
                return match;
            }
        }

        UUID fallback = pickBestTemplate(
                candidates,
                objective,
                null,
                compatibleDifficulties,
                profile.trainingDaysPerWeek(),
                profile.sessionDurationMinutes()
        );
        if (fallback != null) {
            return fallback;
        }

        throw new ApiException(HttpStatus.NOT_FOUND, "No compatible routine template found");
    }

    private UUID pickBestTemplate(
            List<TemplateCandidate> candidates,
            String objective,
            String split,
            List<String> compatibleDifficulties,
            int trainingDaysPerWeek,
            int sessionDurationMinutes
    ) {
        return candidates.stream()
                .filter(candidate -> candidate.objective() != null)
                .filter(candidate -> objective.equalsIgnoreCase(candidate.objective()))
                .filter(candidate -> split == null || split.equalsIgnoreCase(candidate.routineSplit()))
                .filter(candidate -> compatibleDifficulties.contains(normalizeDifficultyOrDefault(candidate.difficulty())))
                .min(Comparator
                        .comparingInt((TemplateCandidate candidate) -> difficultyRank(compatibleDifficulties, candidate.difficulty()))
                        .thenComparingInt(candidate -> Math.abs((candidate.daysPerWeek() == null ? trainingDaysPerWeek : candidate.daysPerWeek()) - trainingDaysPerWeek))
                        .thenComparingInt(candidate -> Math.abs((candidate.estimatedDurationMinutes() == null ? sessionDurationMinutes : candidate.estimatedDurationMinutes()) - sessionDurationMinutes))
                        .thenComparing(candidate -> candidate.routineSplit() == null ? "" : candidate.routineSplit()))
                .map(TemplateCandidate::id)
                .orElse(null);
    }

    private boolean preferredSplitAllowed(String preferredSplit, String experienceLevel) {
        if ("BRO_SPLIT".equals(preferredSplit) && "BEGINNER".equals(experienceLevel)) {
            return false;
        }
        return SUPPORTED_SPLITS.contains(preferredSplit);
    }

    private int difficultyRank(List<String> compatibleDifficulties, String difficulty) {
        String normalized = normalizeDifficultyOrDefault(difficulty);
        int index = compatibleDifficulties.indexOf(normalized);
        return index >= 0 ? index : compatibleDifficulties.size() + 1;
    }

    private List<String> compatibleDifficulties(String experienceLevel) {
        return switch (normalizeDifficultyOrDefault(experienceLevel)) {
            case "ADVANCED" -> List.of("ADVANCED", "INTERMEDIATE");
            case "INTERMEDIATE" -> List.of("INTERMEDIATE", "BEGINNER");
            default -> List.of("BEGINNER");
        };
    }

    private String deriveRecommendedSplit(int trainingDaysPerWeek, String experienceLevel) {
        String normalizedDifficulty = normalizeDifficultyOrDefault(experienceLevel);
        if (trainingDaysPerWeek <= 2) {
            return "FULL_BODY";
        }
        if (trainingDaysPerWeek == 3) {
            return "BEGINNER".equals(normalizedDifficulty) ? "FULL_BODY" : "PUSH_PULL_LEGS";
        }
        if (trainingDaysPerWeek == 4) {
            return "UPPER_LOWER";
        }
        if (trainingDaysPerWeek == 5) {
            return "PPL_UPPER_LOWER";
        }
        return "PUSH_PULL_LEGS";
    }

    private String normalizeSplitPreference(String preferredTrainingStyle) {
        String normalized = normalizeSplit(preferredTrainingStyle);
        if (normalized != null) {
            return normalized;
        }
        if (preferredTrainingStyle == null) {
            return null;
        }
        String compact = preferredTrainingStyle.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return switch (compact) {
            case "PPL" -> "PUSH_PULL_LEGS";
            case "BRO", "BODY_PART_SPLIT" -> "BRO_SPLIT";
            default -> null;
        };
    }

    private boolean routineCategoriesAvailable() {
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

    private String normalizeDifficultyOrDefault(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return "BEGINNER";
        }
        String normalized = difficulty.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_DIFFICULTIES.contains(normalized)) {
            return "BEGINNER";
        }
        return normalized;
    }

    private String normalizeGoal(String goal) {
        String normalized = nullableTrimmed(goal);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeGoalWithFallback(String goal) {
        String normalized = normalizeGoal(goal);
        if (normalized == null) {
            return GOAL_HYPERTROPHY;
        }
        return switch (normalized) {
            case GOAL_HYPERTROPHY, "BUILD_MUSCLE", "GAIN_MUSCLE", "HIPERTROFIA" -> GOAL_HYPERTROPHY;
            default -> GOAL_HYPERTROPHY;
        };
    }

    private String normalizeSplit(String split) {
        String normalized = nullableTrimmed(split);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return SUPPORTED_SPLITS.contains(normalized) ? normalized : null;
    }

    private int normalizePositiveOrDefault(Integer value, int fallback) {
        return value == null || value < 1 ? fallback : value;
    }

    private Integer asInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private String nullableTrimmed(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private TemplateHeader mapHeader(ResultSet rs, int rowNum) throws SQLException {
        return new TemplateHeader(
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

    private DayHeader mapDayHeader(ResultSet rs, int rowNum) throws SQLException {
        return new DayHeader(
                rs.getObject("id", UUID.class),
                rs.getInt("day_index"),
                rs.getString("title"),
                rs.getString("focus")
        );
    }

    private RoutineController.ExerciseResponse mapExercise(ResultSet rs, int rowNum) throws SQLException {
        Integer targetReps = (Integer) rs.getObject("target_reps");
        Integer repsMin = (Integer) rs.getObject("reps_min");
        Integer repsMax = (Integer) rs.getObject("reps_max");
        int reps = targetReps != null ? targetReps : (repsMax != null ? repsMax : (repsMin != null ? repsMin : 0));
        return new RoutineController.ExerciseResponse(
                rs.getObject("id", UUID.class).toString(),
                rs.getObject("exercise_id", UUID.class).toString(),
                rs.getString("name"),
                rs.getInt("sets"),
                reps,
                repsMin,
                repsMax,
                rs.getInt("rest_seconds"),
                rs.getString("primary_muscle_group"),
                rs.getString("notes")
        );
    }

    private TemplateCandidate mapTemplateCandidate(ResultSet rs, int rowNum) throws SQLException {
        return new TemplateCandidate(
                rs.getObject("id", UUID.class),
                rs.getString("objective"),
                rs.getString("difficulty"),
                asInteger(rs.getObject("days_per_week")),
                asInteger(rs.getObject("estimated_duration_minutes")),
                rs.getString("routine_split")
        );
    }

    private record TemplateHeader(
            UUID id,
            String title,
            String description,
            String objective,
            String difficulty,
            Integer estimatedDurationMinutes,
            Integer daysPerWeek,
            UUID categoryId,
            String categoryName,
            String source,
            String templateKey,
            String routineSplit
    ) {
    }

    private record DayHeader(UUID id, int dayIndex, String title, String focus) {
    }

    private record RecommendationProfile(
            String goal,
            String experienceLevel,
            int trainingDaysPerWeek,
            int sessionDurationMinutes,
            String preferredTrainingStyle,
            String medicalNotes,
            int limitationCount
    ) {
    }

    private record TemplateCandidate(
            UUID id,
            String objective,
            String difficulty,
            Integer daysPerWeek,
            Integer estimatedDurationMinutes,
            String routineSplit
    ) {
    }
}
