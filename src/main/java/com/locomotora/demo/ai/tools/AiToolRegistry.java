package com.locomotora.demo.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.locomotora.demo.ai.client.AiModelClient.ToolSpec;
import com.locomotora.demo.ai.repository.AiContextRepository;
import com.locomotora.demo.ai.service.UserContext;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import com.locomotora.demo.routine.dto.HistoryDtos.ExerciseHistory;
import com.locomotora.demo.routine.dto.RoutineResponse;
import com.locomotora.demo.routine.repository.RoutineRepository;
import com.locomotora.demo.routine.service.RoutineReadService;
import com.locomotora.demo.routine.service.WorkoutHistoryService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Herramientas que el agente puede pedir. Las ejecuta el backend, siempre para el usuario
 * autenticado y solo con lectura: el modelo nunca accede a PostgreSQL, nunca ve SQL ni datos de
 * otros usuarios. Los argumentos se validan y los resultados se recortan para gastar pocos tokens.
 */
@Component
public class AiToolRegistry {
    public static final int MAX_RESULT_CHARS = 2500;
    static final List<String> MUSCLES = List.of(
            "Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps", "Cuádriceps", "Isquiotibiales", "Glúteos", "Pantorrillas", "Abdomen");
    static final List<String> EQUIPMENT = List.of(
            "BODYWEIGHT", "DUMBBELL", "BARBELL", "FREE_WEIGHT", "MACHINE", "CABLE", "SMITH_MACHINE");
    private static final Logger log = LoggerFactory.getLogger(AiToolRegistry.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RoutineRepository routineRepository;
    private final RoutineReadService routineReadService;
    private final WorkoutHistoryService workoutHistoryService;
    private final AiContextRepository contextRepository;

    public AiToolRegistry(
            RoutineRepository routineRepository,
            RoutineReadService routineReadService,
            WorkoutHistoryService workoutHistoryService,
            AiContextRepository contextRepository
    ) {
        this.routineRepository = routineRepository;
        this.routineReadService = routineReadService;
        this.workoutHistoryService = workoutHistoryService;
        this.contextRepository = contextRepository;
    }

    /** Resultado de una herramienta: el JSON que ve el modelo y si fue un error. */
    public record Result(String content, boolean error) {
    }

    public List<ToolSpec> specs() {
        return List.of(
                new ToolSpec("search_exercises",
                        "Busca ejercicios del catalogo del usuario por grupo muscular, equipo o nombre. "
                                + "Usala para recomendar ejercicios concretos o alternativas.",
                        object(Map.of(
                                "muscle", Map.of("type", "string", "enum", MUSCLES),
                                "equipment", Map.of("type", "string", "enum", EQUIPMENT),
                                "query", Map.of("type", "string", "description", "Parte del nombre, por ejemplo 'press'"),
                                // Texto y no booleano: Qwen escribe los booleanos como True y Groq rechaza la llamada.
                                "equipment_scope", Map.of("type", "string", "enum", List.of("mine", "all"),
                                        "description", "mine = solo con el equipo del usuario (por defecto); all = todo el catalogo")),
                                List.of())),
                new ToolSpec("list_my_routines",
                        "Lista las rutinas activas del usuario con sus dias.",
                        object(Map.of(), List.of())),
                new ToolSpec("get_routine",
                        "Devuelve una rutina del usuario con sus ejercicios, series, repeticiones y descansos.",
                        object(Map.of("routine_id", Map.of("type", "string", "description", "id de list_my_routines")),
                                List.of("routine_id"))),
                new ToolSpec("get_recent_training",
                        "Sesiones completadas recientes: fecha, dia, series, volumen y esfuerzo percibido.",
                        object(Map.of("days", Map.of("type", "integer", "description", "Cuantos dias hacia atras, de 1 a 90")),
                                List.of())),
                new ToolSpec("get_exercise_progress",
                        "Progreso del usuario en un ejercicio: mejores series por sesion y records.",
                        object(Map.of("exercise_name", Map.of("type", "string")), List.of("exercise_name")))
        );
    }

    /** Ejecuta una herramienta. Nunca lanza: un error vuelve como resultado para que el modelo lo sepa. */
    public Result execute(UUID userId, String name, String rawArguments) {
        JsonNode args;
        try {
            args = rawArguments == null || rawArguments.isBlank() ? JSON.createObjectNode() : JSON.readTree(rawArguments);
            if (!args.isObject()) {
                return error("invalid_arguments");
            }
        } catch (Exception invalid) {
            return error("invalid_arguments");
        }
        try {
            Object value = switch (name == null ? "" : name) {
                case "search_exercises" -> searchExercises(userId, args);
                case "list_my_routines" -> listRoutines();
                case "get_routine" -> getRoutine(args);
                case "get_recent_training" -> recentTraining(userId, args);
                case "get_exercise_progress" -> exerciseProgress(userId, args);
                default -> null;
            };
            if (value == null) {
                return error("unknown_tool");
            }
            return new Result(truncate(JSON.writeValueAsString(value)), false);
        } catch (ToolError toolError) {
            return error(toolError.getMessage());
        } catch (ApiException apiError) {
            return error(apiError.status().value() == 404 ? "not_found" : "invalid_arguments");
        } catch (Exception unexpected) {
            log.warn("AI tool {} failed: {}", name, unexpected.getClass().getSimpleName());
            return error("tool_failed");
        }
    }

    // ------------------------------------------------------------------ herramientas

    private Object searchExercises(UUID userId, JsonNode args) {
        String muscle = text(args, "muscle", 40);
        String equipment = text(args, "equipment", 40);
        String query = text(args, "query", 60);
        boolean onlyMine = !"all".equalsIgnoreCase(text(args, "equipment_scope", 10))
                && (!args.has("only_my_equipment") || args.path("only_my_equipment").asBoolean(true));
        Set<String> allowed = onlyMine ? ExerciseCatalogFilter.allowedEquipment(contextRepository.equipment(userId)) : null;

        List<Map<String, Object>> found = new ArrayList<>();
        for (ExerciseCatalogItemResponse exercise : routineRepository.findExerciseCatalog(userId)) {
            if (muscle != null && !ExerciseCatalogFilter.normalize(exercise.primaryMuscleGroup()).equals(ExerciseCatalogFilter.normalize(muscle))) {
                continue;
            }
            if (equipment != null && !equipment.equalsIgnoreCase(exercise.equipment())) {
                continue;
            }
            if (query != null && !ExerciseCatalogFilter.normalize(exercise.name()).contains(ExerciseCatalogFilter.normalize(query))) {
                continue;
            }
            if (!ExerciseCatalogFilter.fits(exercise, allowed)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", exercise.name());
            item.put("muscle", exercise.primaryMuscleGroup());
            item.put("equipment", exercise.equipment());
            found.add(item);
            if (found.size() == 15) {
                break;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", found.size());
        result.put("exercises", found);
        return result;
    }

    private Object listRoutines() {
        List<Map<String, Object>> routines = new ArrayList<>();
        for (RoutineResponse routine : routineReadService.routines(null)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("routine_id", routine.id());
            item.put("title", routine.title());
            item.put("objective", routine.objective());
            item.put("days", routine.days().stream().map(day -> day.title() == null ? "Dia " + day.dayIndex() : day.title()).toList());
            routines.add(item);
            if (routines.size() == 10) {
                break;
            }
        }
        return Map.of("routines", routines);
    }

    private Object getRoutine(JsonNode args) {
        UUID id;
        try {
            id = UUID.fromString(text(args, "routine_id", 40));
        } catch (RuntimeException invalid) {
            throw new ToolError("invalid_arguments");
        }
        RoutineResponse routine = routineReadService.getRoutine(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", routine.title());
        result.put("objective", routine.objective());
        result.put("difficulty", routine.difficulty());
        List<Map<String, Object>> days = new ArrayList<>();
        routine.days().forEach(day -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", day.title() == null ? "Dia " + day.dayIndex() : day.title());
            item.put("exercises", day.exercises().stream().map(exercise -> {
                Integer min = exercise.repsMin() == null ? exercise.reps() : exercise.repsMin();
                Integer max = exercise.repsMax() == null ? exercise.reps() : exercise.repsMax();
                return exercise.name() + " " + exercise.sets() + "x" + (min.equals(max) ? min : min + "-" + max)
                        + " descanso " + exercise.restSeconds() + "s";
            }).toList());
            days.add(item);
        });
        result.put("days", days);
        return result;
    }

    private Object recentTraining(UUID userId, JsonNode args) {
        int days = args.has("days") ? args.path("days").asInt(28) : 28;
        days = Math.max(1, Math.min(90, days));
        List<Map<String, Object>> sessions = new ArrayList<>();
        for (UserContext.Session session : contextRepository.recentSessions(userId, days, 10)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", session.day().toString());
            item.put("title", session.title());
            item.put("sets", session.completedSets());
            item.put("volume_kg", Math.round(session.volumeKg()));
            item.put("rpe", session.perceivedExertion());
            sessions.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", days);
        result.put("sessions", sessions);
        return result;
    }

    private Object exerciseProgress(UUID userId, JsonNode args) {
        String name = text(args, "exercise_name", 80);
        if (name == null) {
            throw new ToolError("invalid_arguments");
        }
        String wanted = ExerciseCatalogFilter.normalize(name);
        List<ExerciseCatalogItemResponse> catalog = routineRepository.findExerciseCatalog(userId);
        ExerciseCatalogItemResponse match = catalog.stream()
                .filter(exercise -> ExerciseCatalogFilter.normalize(exercise.name()).equals(wanted))
                .findFirst()
                .or(() -> catalog.stream().filter(exercise -> ExerciseCatalogFilter.normalize(exercise.name()).contains(wanted)).findFirst())
                .orElseThrow(() -> new ToolError("not_found"));
        ExerciseHistory history = workoutHistoryService.exerciseHistory(UUID.fromString(match.id()), 8);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("exercise", history.name());
        result.put("sessions", history.points().stream().map(point -> point.completedAt().substring(0, 10) + ": "
                + kg(point.bestWeightKg()) + " kg x " + point.bestReps() + " (fuerza estimada " + kg(point.estimatedOneRepMaxKg()) + " kg)").toList());
        if (history.records() != null && history.records().heaviest() != null) {
            result.put("heaviest", kg(history.records().heaviest().weightKg()) + " kg x " + history.records().heaviest().reps());
        }
        if (history.records() != null && history.records().bestEstimatedOneRepMax() != null) {
            result.put("best_estimated_1rm_kg", kg(history.records().bestEstimatedOneRepMax().value()));
        }
        return result;
    }

    // ------------------------------------------------------------------ utilidades

    private static final class ToolError extends RuntimeException {
        ToolError(String code) {
            super(code);
        }
    }

    private static Result error(String code) {
        return new Result("{\"error\":\"" + code + "\"}", true);
    }

    private static String text(JsonNode args, String field, int maxLength) {
        JsonNode value = args.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            return null;
        }
        String text = value.asText().trim();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private static String kg(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    static String truncate(String json) {
        return json.length() <= MAX_RESULT_CHARS ? json : json.substring(0, MAX_RESULT_CHARS) + "…(recortado)";
    }

    private static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }
}
