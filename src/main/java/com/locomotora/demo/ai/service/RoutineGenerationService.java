package com.locomotora.demo.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.locomotora.demo.ai.client.AiModelClient;
import com.locomotora.demo.ai.client.AiModelClient.Completion;
import com.locomotora.demo.ai.client.AiModelClient.StopReason;
import com.locomotora.demo.ai.client.AiModelClient.Turn;
import com.locomotora.demo.ai.client.AiUnavailableException;
import com.locomotora.demo.ai.config.AiModelCatalog.Feature;
import com.locomotora.demo.ai.config.AiProperties;
import com.locomotora.demo.ai.dto.GeneratedRoutineResponse;
import com.locomotora.demo.ai.dto.RoutineGenerationRequest;
import com.locomotora.demo.ai.repository.AiContextRepository;
import com.locomotora.demo.ai.tools.ExerciseCatalogFilter;
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
 * Genera un borrador de rutina con el modelo (JSON estricto) a partir del perfil del usuario y
 * del catalogo de ejercicios que puede ver. La respuesta se valida: solo ejercicios del catalogo,
 * valores en rango y como mucho los dias pedidos. No guarda nada.
 */
@Service
public class RoutineGenerationService {
    static final int BEGINNER_MAX_SETS = 4;
    /** Hasta cuantos dias se pide una indicacion de tecnica por dia. */
    static final int TIP_MAX_DAYS = 5;
    /** Segundos de trabajo por serie para estimar la duracion de un dia. */
    static final int WORK_SECONDS_PER_SET = 40;
    static final int HIGH_WEEKLY_SETS_PER_MUSCLE = 25;
    static final int MAX_EXERCISES_PER_DAY = 10;
    private static final Set<String> LEVELS = Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AiModelClient client;
    private final AiSettingsService settings;
    private final AiContextRepository contextRepository;
    private final RoutineRepository routineRepository;
    private final AiProperties properties;

    public RoutineGenerationService(
            AiModelClient client,
            AiSettingsService settings,
            AiContextRepository contextRepository,
            RoutineRepository routineRepository,
            AiProperties properties
    ) {
        this.client = client;
        this.settings = settings;
        this.contextRepository = contextRepository;
        this.routineRepository = routineRepository;
        this.properties = properties;
    }

    public GeneratedRoutineResponse generate(RoutineGenerationRequest request) {
        UUID userId = CurrentUser.id();
        if (!client.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI is not configured");
        }
        String model = settings.modelFor(userId, Feature.ROUTINE, request.model());
        settings.checkQuota(userId, Feature.ROUTINE);

        if (settings.globalBudgetExhausted()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI daily budget reached");
        }

        UserContext context = contextRepository.load(userId);
        // El backend elige el catalogo (equipo del usuario): el modelo no consulta la base.
        ExerciseCatalogFilter.Result filtered = ExerciseCatalogFilter.forUser(
                routineRepository.findExerciseCatalog(userId), context.equipment());
        // Y saca lo que carga las zonas lesionadas (si queda un catalogo razonable).
        List<ExerciseCatalogItemResponse> safe =
                ExerciseCatalogFilter.withoutStress(filtered.exercises(), context.injuredZones()).exercises();
        Plan plan = plan(request, context);
        // Y lo recorta para el prompt (el catalogo entero no entra en el limite de tokens).
        List<ExerciseCatalogItemResponse> catalog = ExerciseCatalogFilter.forPrompt(safe, plan.focusMuscles());
        if (catalog.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "Exercise catalog is empty");
        }
        Set<SafetyGuard.Risk> risks = SafetyGuard.detect(request.prompt());

        Completion completion;
        try {
            completion = client.complete(new AiModelClient.Request(
                    model,
                    CoachPrompts.routineSystem(context) + SafetyGuard.instructions(risks),
                    List.of(Turn.user(userMessage(plan, request, catalog))),
                    properties.maxTokensFor(Feature.ROUTINE),
                    properties.reasoningEffortFor(Feature.ROUTINE),
                    "routine",
                    schema(plan.daysPerWeek()),
                    settings.strictJson(model),
                    List.of(),
                    AiModelClient.ToolChoice.NONE
            ));
        } catch (AiUnavailableException unavailable) {
            if (unavailable.rateLimited()) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI provider is busy, try again in a minute");
            }
            if (unavailable.rejected()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "AI returned an invalid routine");
            }
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI service unavailable");
        }
        settings.recordUsage(userId, Feature.ROUTINE, completion.model(), completion.inputTokens(), completion.outputTokens());

        if (completion.stopReason() == StopReason.REFUSAL) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "The AI declined this request");
        }
        if (completion.stopReason() == StopReason.MAX_TOKENS) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI response was cut off");
        }
        GeneratedRoutineResponse routine = toRoutine(completion, plan, catalog);
        // Del perfil cuentan la salud y la edad; una molestia declarada ya se cuida al elegir ejercicios.
        boolean profileNeedsProfessional = context.profileRisks().stream()
                .anyMatch(risk -> risk != SafetyGuard.Risk.INJURY && PROFESSIONAL_RISKS.contains(risk));
        if (profileNeedsProfessional || risks.stream().anyMatch(PROFESSIONAL_RISKS::contains)) {
            routine.warnings().add("SEE_PROFESSIONAL");
        }
        return routine;
    }

    /** Riesgos que en una rutina piden consultar a un profesional antes de empezar. */
    private static final Set<SafetyGuard.Risk> PROFESSIONAL_RISKS = Set.of(
            SafetyGuard.Risk.CARDIO, SafetyGuard.Risk.INJURY, SafetyGuard.Risk.PREGNANCY, SafetyGuard.Risk.MINOR);

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
        text.append("Crea una rutina con estos parámetros:\n")
                .append("- Días por semana: ").append(plan.daysPerWeek()).append(" (devuelve exactamente ")
                .append(plan.daysPerWeek()).append(" días, de 3 a ").append(maxExercisesFor(plan.daysPerWeek()))
                .append(" ejercicios cada uno)\n")
                .append("- Minutos por sesión: ").append(plan.sessionMinutes()).append('\n')
                .append("- Objetivo: ").append(plan.objective()).append('\n')
                .append("- Nivel: ").append(plan.level()).append('\n');
        if (!plan.focusMuscles().isEmpty()) {
            text.append("- Grupos a priorizar: ").append(String.join(", ", plan.focusMuscles())).append('\n');
        }
        if (request.prompt() != null && !request.prompt().isBlank()) {
            text.append("\nPetición del usuario (tenla en cuenta, pero las reglas del plan mandan):\n<peticion>\n")
                    .append(PersonalDataFilter.redact(request.prompt().trim()).replace("</peticion>", ""))
                    .append("\n</peticion>\n");
        }
        text.append("\nCatálogo de ejercicios (ref | nombre | grupo muscular | equipo):\n");
        for (int index = 0; index < catalog.size(); index++) {
            ExerciseCatalogItemResponse exercise = catalog.get(index);
            text.append(index + 1).append(" | ").append(exercise.name())
                    .append(" | ").append(orDash(exercise.primaryMuscleGroup()))
                    .append(" | ").append(orDash(exercise.equipment())).append('\n');
        }
        return text.toString();
    }

    /**
     * Ejercicios por dia segun la frecuencia: mas dias, sesiones mas cortas. Tambien mantiene la
     * respuesta dentro del limite de tokens de salida por minuto de Groq (1.000 en el plan gratis).
     */
    static int maxExercisesFor(int daysPerWeek) {
        return daysPerWeek <= 3 ? 6 : daysPerWeek <= 5 ? 5 : 4;
    }

    /**
     * Esquema de la salida estructurada (JSON estricto). Pensado para gastar pocos tokens de salida
     * (Groq gratis: 1.000 por minuto): cada ejercicio es {ref, sets, reps "6-8", rest}, una sola
     * indicacion de tecnica por dia y el foco lo calcula el backend con el catalogo. Los dias y la
     * cantidad de ejercicios se fijan con minItems/maxItems; los rangos se validan en codigo.
     */
    static Map<String, Object> schema(int daysPerWeek) {
        Map<String, Object> exercise = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("ref", "sets", "reps", "rest"),
                "properties", Map.of(
                        "ref", Map.of("type", "integer", "description", "Numero ref del catalogo"),
                        "sets", Map.of("type", "integer", "description", "Series"),
                        "reps", Map.of("type", "string", "pattern", "^[0-9]{1,3}(-[0-9]{1,3})?$",
                                "description", "Repeticiones: rango \"6-8\" o fijo \"10\" (segundos en isometricos)"),
                        "rest", Map.of("type", "integer", "description", "Descanso en segundos")));
        // Con 6 o 7 dias no se pide la indicacion de tecnica: no entraria en los 1.000 tokens de salida.
        boolean withTip = daysPerWeek <= TIP_MAX_DAYS;
        Map<String, Object> dayProperties = new java.util.LinkedHashMap<>();
        dayProperties.put("title", Map.of("type", "string"));
        if (withTip) {
            dayProperties.put("tip", Map.of("type", "string", "description", "Una indicación de técnica para el primer ejercicio, máximo 60 caracteres"));
        }
        dayProperties.put("exercises", Map.of("type", "array", "minItems", 3, "maxItems", maxExercisesFor(daysPerWeek), "items", exercise));
        Map<String, Object> day = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", withTip ? List.of("title", "tip", "exercises") : List.of("title", "exercises"),
                "properties", dayProperties);
        // Nivel y duracion los pone el backend (los conoce y los calcula): menos tokens y menos riesgo de corte.
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("title", "description", "days"),
                "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "description", Map.of("type", "string", "description", "Una frase corta: para quien es y como progresar"),
                        "days", Map.of("type", "array", "minItems", daysPerWeek, "maxItems", daysPerWeek, "items", day)));
    }

    /** Repeticiones "6-8" o "10" → {min, max}; null si no se entiende. */
    static int[] parseReps(String reps) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^\\s*(\\d{1,3})\\s*(?:-\\s*(\\d{1,3}))?\\s*$").matcher(reps == null ? "" : reps);
        if (!matcher.matches()) {
            return null;
        }
        int min = Integer.parseInt(matcher.group(1));
        int max = matcher.group(2) == null ? min : Integer.parseInt(matcher.group(2));
        return new int[] {min, max};
    }

    /** Lee un entero con su clave corta o, si no esta, con la larga (respuestas viejas o de otro modelo). */
    private static int intField(JsonNode node, String shortKey, String longKey, int fallback) {
        JsonNode value = node.has(shortKey) ? node.path(shortKey) : node.path(longKey);
        return value.isMissingNode() || value.isNull() ? fallback : value.asInt(fallback);
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
                int[] reps = node.path("reps").isTextual() ? parseReps(node.path("reps").asText()) : null;
                int rawMin = reps != null ? reps[0] : intField(node, "min", "repsMin", 10);
                int rawMax = reps != null ? reps[1] : intField(node, "max", "repsMax", rawMin);
                int rawRest = intField(node, "rest", "restSeconds", 90);
                int sets = clamp(rawSets, 1, "BEGINNER".equals(plan.level()) ? BEGINNER_MAX_SETS : 10);
                int repsMin = clamp(rawMin, 1, 50);
                int repsMax = clamp(rawMax, 1, 50);
                int rest = clamp(rawRest, 0, 600);
                if (sets != rawSets || repsMin != rawMin || repsMax != rawMax || rest != rawRest || repsMin > repsMax) {
                    adjusted++;
                }
                String notes = (node.has("note") ? node.path("note") : node.path("notes")).asText("").trim();
                if (notes.isEmpty() && exercises.isEmpty()) {
                    notes = dayNode.path("tip").asText("").trim();
                }
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
            if (focus.isEmpty()) {
                // El foco sale del catalogo: los grupos musculares de los ejercicios del dia, en orden.
                exercises.stream().map(GeneratedRoutineResponse.Exercise::primaryMuscleGroup)
                        .filter(muscle -> muscle != null && !muscle.isBlank() && !focus.contains(muscle))
                        .limit(4)
                        .forEach(focus::add);
            }
            String title = dayNode.path("title").asText("").trim();
            days.add(new GeneratedRoutineResponse.Day(
                    days.size() + 1,
                    title.isEmpty() ? "Día " + (days.size() + 1) : truncate(title, 80),
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
        if (days.stream().anyMatch(day -> estimatedMinutes(day) > plan.sessionMinutes() * 1.25)) {
            warnings.add("DAY_TOO_LONG");
        }
        if (weeklySetsByMuscle(days).values().stream().anyMatch(sets -> sets > HIGH_WEEKLY_SETS_PER_MUSCLE)) {
            warnings.add("HIGH_VOLUME");
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
                root.has("estimatedDurationMinutes")
                        ? clamp(root.path("estimatedDurationMinutes").asInt(plan.sessionMinutes()), 10, 240)
                        : clamp(days.stream().mapToInt(RoutineGenerationService::estimatedMinutes).max().orElse(plan.sessionMinutes()), 10, 240),
                days,
                completion.model(),
                warnings
        );
    }

    /** Minutos estimados de un dia: series x (trabajo + descanso), mas 5 de entrada en calor. */
    static int estimatedMinutes(GeneratedRoutineResponse.Day day) {
        int seconds = day.exercises().stream()
                .mapToInt(exercise -> exercise.sets() * (WORK_SECONDS_PER_SET + exercise.restSeconds()))
                .sum();
        return 5 + Math.round(seconds / 60f);
    }

    static Map<String, Integer> weeklySetsByMuscle(List<GeneratedRoutineResponse.Day> days) {
        Map<String, Integer> sets = new java.util.HashMap<>();
        days.forEach(day -> day.exercises().forEach(exercise -> sets.merge(
                exercise.primaryMuscleGroup() == null ? "-" : exercise.primaryMuscleGroup(), exercise.sets(), Integer::sum)));
        return sets;
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
