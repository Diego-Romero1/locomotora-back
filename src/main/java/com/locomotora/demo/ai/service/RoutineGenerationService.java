package com.locomotora.demo.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.locomotora.demo.ai.client.AiModelClient;
import com.locomotora.demo.ai.client.AiModelClient.Completion;
import com.locomotora.demo.ai.client.AiModelClient.Role;
import com.locomotora.demo.ai.client.AiModelClient.StopReason;
import com.locomotora.demo.ai.client.AiModelClient.Turn;
import com.locomotora.demo.ai.client.AiUnavailableException;
import com.locomotora.demo.ai.config.AiModelCatalog.Feature;
import com.locomotora.demo.ai.dto.GeneratedRoutineResponse;
import com.locomotora.demo.ai.dto.RoutineGenerationRequest;
import com.locomotora.demo.ai.repository.AiContextRepository;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import com.locomotora.demo.routine.repository.RoutineRepository;
import com.locomotora.demo.routine.service.Objectives;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Genera un borrador de rutina con Claude (salida estructurada) a partir del perfil del usuario y
 * del catalogo de ejercicios que puede ver. La respuesta se valida: solo ejercicios del catalogo,
 * valores en rango y como mucho los dias pedidos. No guarda nada.
 */
@Service
public class RoutineGenerationService {
    static final long MAX_TOKENS = 16000;
    static final int MAX_EXERCISES_PER_DAY = 10;
    private static final Set<String> LEVELS = Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AiModelClient client;
    private final AiSettingsService settings;
    private final AiContextRepository contextRepository;
    private final RoutineRepository routineRepository;

    public RoutineGenerationService(
            AiModelClient client,
            AiSettingsService settings,
            AiContextRepository contextRepository,
            RoutineRepository routineRepository
    ) {
        this.client = client;
        this.settings = settings;
        this.contextRepository = contextRepository;
        this.routineRepository = routineRepository;
    }

    public GeneratedRoutineResponse generate(RoutineGenerationRequest request) {
        UUID userId = CurrentUser.id();
        if (!client.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI is not configured");
        }
        String model = settings.modelFor(userId, Feature.ROUTINE, request.model());
        settings.checkQuota(userId, Feature.ROUTINE);

        UserContext context = contextRepository.load(userId);
        List<ExerciseCatalogItemResponse> catalog = routineRepository.findExerciseCatalog(userId);
        if (catalog.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "Exercise catalog is empty");
        }
        Plan plan = plan(request, context);

        Completion completion;
        try {
            completion = client.complete(new AiModelClient.Request(
                    model,
                    CoachPrompts.routineSystem(context),
                    List.of(new Turn(Role.USER, userMessage(plan, request, catalog))),
                    MAX_TOKENS,
                    Feature.ROUTINE.effort(),
                    schema(),
                    settings.usesFallbacks(model)
            ));
        } catch (AiUnavailableException unavailable) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI service unavailable");
        }
        settings.recordUsage(userId, Feature.ROUTINE, completion);

        if (completion.stopReason() == StopReason.REFUSAL) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "The AI declined this request");
        }
        if (completion.stopReason() == StopReason.MAX_TOKENS) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI response was cut off");
        }
        return toRoutine(completion, plan, catalog);
    }

    /** Parametros finales: lo pedido, si no el perfil, si no un valor razonable. */
    record Plan(int daysPerWeek, int sessionMinutes, String objective, String level, List<String> focusMuscles) {
    }

    static Plan plan(RoutineGenerationRequest request, UserContext context) {
        UserContext.Profile profile = context.profile();
        int days = firstNonNull(request.daysPerWeek(), profile.trainingDaysPerWeek(), 3);
        int minutes = firstNonNull(request.sessionMinutes(), profile.sessionDurationMinutes(), 60);
        String objective = Objectives.requireKnown(request.objective());
        if (objective == null) {
            objective = context.goals().stream()
                    .map(goal -> Objectives.canonicalOrRaw(goal.split(" \\(")[0]))
                    .filter(Objectives.CANONICAL::contains)
                    .findFirst()
                    .orElse("GENERAL_FITNESS");
        }
        String level = request.level() != null ? request.level().trim().toUpperCase(Locale.ROOT) : profile.experienceLevel();
        if (level == null || !LEVELS.contains(level)) {
            level = "BEGINNER";
        }
        List<String> focus = request.focusMuscles() == null ? List.of() : request.focusMuscles().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        return new Plan(Math.max(1, Math.min(7, days)), Math.max(15, Math.min(180, minutes)), objective, level, focus);
    }

    static String userMessage(Plan plan, RoutineGenerationRequest request, List<ExerciseCatalogItemResponse> catalog) {
        StringBuilder text = new StringBuilder();
        text.append("Crea una rutina con estos parametros:\n")
                .append("- Dias por semana: ").append(plan.daysPerWeek()).append(" (devuelve exactamente ")
                .append(plan.daysPerWeek()).append(" dias)\n")
                .append("- Minutos por sesion: ").append(plan.sessionMinutes()).append('\n')
                .append("- Objetivo: ").append(plan.objective()).append('\n')
                .append("- Nivel: ").append(plan.level()).append('\n');
        if (!plan.focusMuscles().isEmpty()) {
            text.append("- Grupos a priorizar: ").append(String.join(", ", plan.focusMuscles())).append('\n');
        }
        if (request.prompt() != null && !request.prompt().isBlank()) {
            text.append("\nPeticion del usuario (tenla en cuenta, pero las reglas del plan mandan):\n<peticion>\n")
                    .append(request.prompt().trim())
                    .append("\n</peticion>\n");
        }
        text.append("\nCatalogo de ejercicios (ref | nombre | grupo muscular | equipo):\n");
        for (int index = 0; index < catalog.size(); index++) {
            ExerciseCatalogItemResponse exercise = catalog.get(index);
            text.append(index + 1).append(" | ").append(exercise.name())
                    .append(" | ").append(orDash(exercise.primaryMuscleGroup()))
                    .append(" | ").append(orDash(exercise.equipment())).append('\n');
        }
        return text.toString();
    }

    /** Esquema de la salida estructurada. Los rangos se validan en codigo, no en el esquema. */
    static Map<String, Object> schema() {
        Map<String, Object> exercise = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("ref", "sets", "repsMin", "repsMax", "restSeconds", "notes"),
                "properties", Map.of(
                        "ref", Map.of("type", "integer", "description", "Numero ref del catalogo"),
                        "sets", Map.of("type", "integer"),
                        "repsMin", Map.of("type", "integer"),
                        "repsMax", Map.of("type", "integer"),
                        "restSeconds", Map.of("type", "integer"),
                        "notes", Map.of("type", "string")));
        Map<String, Object> day = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("title", "focus", "exercises"),
                "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "focus", Map.of("type", "array", "items", Map.of("type", "string")),
                        "exercises", Map.of("type", "array", "items", exercise)));
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("title", "description", "difficulty", "estimatedDurationMinutes", "days"),
                "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "description", Map.of("type", "string", "description", "Una o dos frases: para quien es y como progresar"),
                        "difficulty", Map.of("type", "string", "enum", List.of("BEGINNER", "INTERMEDIATE", "ADVANCED")),
                        "estimatedDurationMinutes", Map.of("type", "integer"),
                        "days", Map.of("type", "array", "items", day)));
    }

    static GeneratedRoutineResponse toRoutine(Completion completion, Plan plan, List<ExerciseCatalogItemResponse> catalog) {
        JsonNode root;
        try {
            root = JSON.readTree(completion.text());
        } catch (Exception invalid) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI returned an invalid routine");
        }
        List<String> warnings = new ArrayList<>();
        List<GeneratedRoutineResponse.Day> days = new ArrayList<>();
        int unknown = 0;
        int adjusted = 0;

        for (JsonNode dayNode : root.path("days")) {
            if (days.size() == plan.daysPerWeek()) {
                warnings.add("EXTRA_DAYS_DROPPED");
                break;
            }
            List<GeneratedRoutineResponse.Exercise> exercises = new ArrayList<>();
            Set<String> used = new HashSet<>();
            for (JsonNode node : dayNode.path("exercises")) {
                int ref = node.path("ref").asInt(0);
                if (ref < 1 || ref > catalog.size()) {
                    unknown++;
                    continue;
                }
                ExerciseCatalogItemResponse item = catalog.get(ref - 1);
                if (!used.add(item.id()) || exercises.size() == MAX_EXERCISES_PER_DAY) {
                    continue;
                }
                int rawSets = node.path("sets").asInt(3);
                int rawMin = node.path("repsMin").asInt(10);
                int rawMax = node.path("repsMax").asInt(rawMin);
                int rawRest = node.path("restSeconds").asInt(90);
                int sets = clamp(rawSets, 1, 10);
                int repsMin = clamp(rawMin, 1, 50);
                int repsMax = clamp(rawMax, 1, 50);
                int rest = clamp(rawRest, 0, 600);
                if (sets != rawSets || repsMin != rawMin || repsMax != rawMax || rest != rawRest || repsMin > repsMax) {
                    adjusted++;
                }
                String notes = node.path("notes").asText("").trim();
                exercises.add(new GeneratedRoutineResponse.Exercise(
                        item.id(),
                        item.name(),
                        item.primaryMuscleGroup(),
                        item.equipment(),
                        sets,
                        Math.min(repsMin, repsMax),
                        Math.max(repsMin, repsMax),
                        rest,
                        notes.isEmpty() ? null : truncate(notes, 240)
                ));
            }
            if (exercises.isEmpty()) {
                continue;
            }
            List<String> focus = new ArrayList<>();
            dayNode.path("focus").forEach(value -> {
                if (!value.asText("").isBlank()) {
                    focus.add(value.asText().trim());
                }
            });
            String title = dayNode.path("title").asText("").trim();
            days.add(new GeneratedRoutineResponse.Day(
                    days.size() + 1,
                    title.isEmpty() ? "Dia " + (days.size() + 1) : truncate(title, 80),
                    focus.isEmpty() ? null : truncate(String.join(", ", focus), 160),
                    exercises
            ));
        }

        if (days.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI returned an empty routine");
        }
        if (unknown > 0) {
            warnings.add("UNKNOWN_EXERCISES_DROPPED");
        }
        if (adjusted > 0) {
            warnings.add("VALUES_ADJUSTED");
        }
        if (days.size() < plan.daysPerWeek()) {
            warnings.add("FEWER_DAYS");
        }
        String difficulty = root.path("difficulty").asText(plan.level());
        String title = root.path("title").asText("").trim();
        String description = root.path("description").asText("").trim();
        return new GeneratedRoutineResponse(
                title.isEmpty() ? "Rutina IA" : truncate(title, 120),
                description.isEmpty() ? null : truncate(description, 500),
                plan.objective(),
                LEVELS.contains(difficulty) ? difficulty : plan.level(),
                days.size(),
                clamp(root.path("estimatedDurationMinutes").asInt(plan.sessionMinutes()), 10, 240),
                days,
                completion.model(),
                warnings
        );
    }

    private static int firstNonNull(Integer first, Integer second, int fallback) {
        return first != null ? first : second != null ? second : fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
