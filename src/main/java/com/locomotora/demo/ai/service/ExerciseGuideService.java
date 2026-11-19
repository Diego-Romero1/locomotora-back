package com.locomotora.demo.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.locomotora.demo.ai.client.AiModelClient;
import com.locomotora.demo.ai.client.AiModelClient.Completion;
import com.locomotora.demo.ai.client.AiModelClient.StopReason;
import com.locomotora.demo.ai.client.AiModelClient.Turn;
import com.locomotora.demo.ai.client.AiUnavailableException;
import com.locomotora.demo.ai.config.AiModelCatalog.Feature;
import com.locomotora.demo.ai.config.AiProperties;
import com.locomotora.demo.ai.dto.ExerciseGuideResponse;
import com.locomotora.demo.ai.repository.ExerciseGuideRepository;
import com.locomotora.demo.ai.tools.AiToolRegistry;
import com.locomotora.demo.ai.youtube.YouTubeClient;
import com.locomotora.demo.ai.youtube.YouTubeClient.Video;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import com.locomotora.demo.routine.repository.RoutineRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * "Detalles con video" de un ejercicio: consejos de tecnica generados con IA y un video corto de YouTube
 * que la IA confirma que muestra exactamente ese ejercicio (mismo equipo y variante). Es contenido general
 * del ejercicio, asi que se genera una vez y se guarda: el primer usuario que lo pide gasta un mensaje del
 * coach; los demas lo leen guardado.
 *
 * <p>La IA solo ve el titulo, el canal, la duracion y el comienzo de la descripcion de cada video (no el
 * video en si): por eso exige confianza alta y, si duda, no muestra ninguno.
 */
@Service
public class ExerciseGuideService {
    /** Si no se encontro video, se vuelve a buscar pasado este tiempo (YouTube suma videos). */
    static final Duration RETRY_NOT_FOUND = Duration.ofDays(30);
    static final int CANDIDATES = 8;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AiModelClient client;
    private final AiSettingsService settings;
    private final AiProperties properties;
    private final YouTubeClient youtube;
    private final RoutineRepository routineRepository;
    private final ExerciseGuideRepository guideRepository;

    public ExerciseGuideService(
            AiModelClient client,
            AiSettingsService settings,
            AiProperties properties,
            YouTubeClient youtube,
            RoutineRepository routineRepository,
            ExerciseGuideRepository guideRepository
    ) {
        this.client = client;
        this.settings = settings;
        this.properties = properties;
        this.youtube = youtube;
        this.routineRepository = routineRepository;
        this.guideRepository = guideRepository;
    }

    public ExerciseGuideResponse guide(UUID exerciseId) {
        UUID userId = CurrentUser.id();
        ExerciseCatalogItemResponse exercise = routineRepository.findVisibleExercise(exerciseId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exercise not found"));
        ExerciseGuideRepository.Stored stored = guideRepository.find(exerciseId).orElse(null);

        boolean aiAvailable = client.isConfigured();
        ExerciseGuideResponse.Tips tips = stored == null ? null : parseStoredTips(stored.tipsJson());
        boolean needTips = aiAvailable && tips == null;
        boolean needVideo = aiAvailable && youtube.isConfigured() && videoIsDue(stored, Instant.now());

        long inputTokens = 0;
        long outputTokens = 0;
        String model = null;
        String videoStatus = videoStatus(stored);
        ExerciseGuideResponse.Video video = storedVideo(stored);

        if (needTips || needVideo) {
            // Un pedido que llama al modelo cuenta como un mensaje del coach.
            settings.checkQuota(userId, Feature.COACH);
            if (settings.globalBudgetExhausted()) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI daily budget reached");
            }
            model = settings.modelFor(userId, Feature.COACH, null);
        }
        try {
            if (needTips) {
                Completion completion = call(model, TIPS_SYSTEM, tipsMessage(exercise), "exercise_tips", tipsSchema(), 600);
                inputTokens += completion.inputTokens();
                outputTokens += completion.outputTokens();
                tips = parseTips(completion.text());
                if (tips == null) {
                    throw new ApiException(HttpStatus.BAD_GATEWAY, "AI returned invalid tips");
                }
                guideRepository.saveTips(exerciseId, JSON.valueToTree(tips).toString(), completion.model());
            }
            if (needVideo) {
                VideoSearch search = findVideo(model, exercise);
                inputTokens += search.inputTokens();
                outputTokens += search.outputTokens();
                if (search.video() != null) {
                    Video found = search.video();
                    guideRepository.saveVideo(exerciseId, "FOUND", found.id(), found.title(), found.channel(), found.durationSeconds());
                    video = toResponse(found.id(), found.title(), found.channel(), found.durationSeconds());
                    videoStatus = "FOUND";
                } else if (search.searched()) {
                    guideRepository.saveVideo(exerciseId, "NOT_FOUND", null, null, null, null);
                    video = null;
                    videoStatus = "NOT_FOUND";
                } else {
                    videoStatus = video != null ? "FOUND" : "UNAVAILABLE";
                }
            }
        } catch (AiUnavailableException unavailable) {
            if (tips == null) {
                if (unavailable.rateLimited()) {
                    throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI provider is busy, try again in a minute");
                }
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI service unavailable");
            }
            // Los consejos ya estan; el video se reintenta en la proxima apertura.
            videoStatus = video != null ? "FOUND" : "UNAVAILABLE";
        } finally {
            if (model != null && inputTokens + outputTokens > 0) {
                settings.recordUsage(userId, Feature.COACH, model, inputTokens, outputTokens);
            }
        }

        return new ExerciseGuideResponse(
                exercise.id(),
                exercise.name(),
                tips,
                video,
                video != null ? "FOUND" : videoStatus,
                searchUrl(exercise),
                aiAvailable
        );
    }

    // ------------------------------------------------------------------ video

    /** searched: se pudo buscar (con o sin resultado); false si YouTube fallo. */
    record VideoSearch(Video video, boolean searched, long inputTokens, long outputTokens) {
    }

    /** Primero en español; si ninguno es exactamente el ejercicio, en inglés con el nombre original. */
    VideoSearch findVideo(String model, ExerciseCatalogItemResponse exercise) {
        List<String[]> queries = new ArrayList<>();
        queries.add(new String[] {safeName(exercise) + " técnica correcta", "es"});
        if (exercise.nameEn() != null && !exercise.nameEn().isBlank()) {
            queries.add(new String[] {exercise.nameEn() + " proper form", "en"});
        }
        long inputTokens = 0;
        long outputTokens = 0;
        try {
            for (String[] query : queries) {
                List<Video> candidates = youtube.searchShort(query[0], query[1], CANDIDATES);
                if (candidates.isEmpty()) {
                    continue;
                }
                Completion completion = call(model, JUDGE_SYSTEM, judgeMessage(exercise, candidates), "video_choice", judgeSchema(), 120);
                inputTokens += completion.inputTokens();
                outputTokens += completion.outputTokens();
                Video chosen = chosen(completion.text(), candidates);
                if (chosen != null) {
                    return new VideoSearch(chosen, true, inputTokens, outputTokens);
                }
            }
            return new VideoSearch(null, true, inputTokens, outputTokens);
        } catch (YouTubeClient.YouTubeUnavailableException failure) {
            return new VideoSearch(null, false, inputTokens, outputTokens);
        }
    }

    /** Solo acepta una eleccion con confianza alta dentro de la lista; si no, ninguno. */
    static Video chosen(String text, List<Video> candidates) {
        JsonNode root = readJson(text);
        if (root == null) {
            return null;
        }
        int choice = root.path("choice").asInt(0);
        String confidence = root.path("confidence").asText("");
        if (!"high".equals(confidence) || choice < 1 || choice > candidates.size()) {
            return null;
        }
        return candidates.get(choice - 1);
    }

    static boolean videoIsDue(ExerciseGuideRepository.Stored stored, Instant now) {
        if (stored == null || "PENDING".equals(stored.videoStatus())) {
            return true;
        }
        return "NOT_FOUND".equals(stored.videoStatus())
                && (stored.videoCheckedAt() == null || stored.videoCheckedAt().plus(RETRY_NOT_FOUND).isBefore(now));
    }

    private static String videoStatus(ExerciseGuideRepository.Stored stored) {
        if (stored == null || "PENDING".equals(stored.videoStatus())) {
            return "UNAVAILABLE";
        }
        return stored.videoStatus();
    }

    private static ExerciseGuideResponse.Video storedVideo(ExerciseGuideRepository.Stored stored) {
        if (stored == null || !"FOUND".equals(stored.videoStatus()) || stored.videoId() == null) {
            return null;
        }
        return toResponse(stored.videoId(), stored.videoTitle(), stored.videoChannel(),
                stored.videoDurationSeconds() == null ? 0 : stored.videoDurationSeconds());
    }

    static ExerciseGuideResponse.Video toResponse(String id, String title, String channel, int seconds) {
        return new ExerciseGuideResponse.Video(id, title, channel, seconds,
                "https://www.youtube.com/watch?v=" + id, "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg");
    }

    static String searchUrl(ExerciseCatalogItemResponse exercise) {
        return "https://www.youtube.com/results?search_query="
                + URLEncoder.encode(safeName(exercise) + " técnica", StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ modelo

    private Completion call(String model, String system, String message, String schemaName, Map<String, Object> schema, long maxTokens) {
        Completion completion = client.complete(new AiModelClient.Request(
                model,
                system,
                List.of(Turn.user(message)),
                maxTokens,
                properties.reasoningEffortFor(Feature.COACH),
                schemaName,
                schema,
                settings.strictJson(model),
                List.of(),
                AiModelClient.ToolChoice.NONE
        ));
        if (completion.stopReason() == StopReason.REFUSAL || completion.stopReason() == StopReason.MAX_TOKENS) {
            throw new AiUnavailableException("AI response unusable: " + completion.stopReason());
        }
        return completion;
    }

    static final String TIPS_SYSTEM = """
            Eres un entrenador personal certificado. Explicas la técnica de un ejercicio de gimnasio en español \
            neutro, tuteando, con frases cortas y concretas que se puedan leer entre series. Usas palabras simples \
            de gimnasio (agarre, omóplatos, zona lumbar, codos, rodillas), no tecnicismos de anatomía. Cada error \
            común dice qué pasa y cómo corregirlo. No das consejos médicos ni diagnósticos. Respondes solo con el \
            JSON pedido.""";

    static final String JUDGE_SYSTEM = """
            Eres un entrenador personal que revisa videos de YouTube para una app de gimnasio. Ves solo el \
            título, el canal, la duración y el comienzo de la descripción de cada video. Eliges un video solo si \
            deja claro que muestra EXACTAMENTE el ejercicio pedido: mismo equipo (barra, mancuernas, máquina, \
            polea...) y misma variante (plano o inclinado, agarre, una o dos manos, de pie o sentado). Un \
            ejercicio parecido no sirve. Prefieres los que enseñan la técnica y, si son igual de buenos, los que \
            están en español. Si dudas, eliges 0 (ninguno). Respondes solo con el JSON pedido.""";

    static String tipsMessage(ExerciseCatalogItemResponse exercise) {
        StringBuilder text = new StringBuilder("Ejercicio: ").append(safeName(exercise)).append('\n');
        appendFacts(text, exercise);
        text.append("""

                Da: un resumen de una frase de qué trabaja y cómo se ejecuta; de 3 a 5 claves de técnica (máximo \
                90 caracteres cada una); de 2 a 4 errores comunes con cómo evitarlos (máximo 90 caracteres); y \
                cómo respirar en una frase.""");
        return text.toString();
    }

    static String judgeMessage(ExerciseCatalogItemResponse exercise, List<Video> candidates) {
        StringBuilder text = new StringBuilder("Ejercicio: ").append(safeName(exercise)).append('\n');
        appendFacts(text, exercise);
        text.append("\nVideos candidatos (número | título | canal | duración | descripción):\n");
        for (int index = 0; index < candidates.size(); index++) {
            Video video = candidates.get(index);
            text.append(index + 1).append(" | ").append(oneLine(video.title()))
                    .append(" | ").append(oneLine(video.channel()))
                    .append(" | ").append(video.durationSeconds()).append(" s")
                    .append(" | ").append(oneLine(video.description())).append('\n');
        }
        text.append("\n¿Qué número muestra exactamente este ejercicio? 0 si ninguno. Confianza alta solo si el título lo deja claro.");
        return text.toString();
    }

    private static void appendFacts(StringBuilder text, ExerciseCatalogItemResponse exercise) {
        if (exercise.nameEn() != null && !exercise.nameEn().isBlank()) {
            text.append("Nombre en inglés: ").append(exercise.nameEn()).append('\n');
        }
        if (exercise.equipment() != null) {
            text.append("Equipo: ").append(AiToolRegistry.equipmentLabel(exercise.equipment())).append('\n');
        }
        if (exercise.primaryMuscleGroup() != null) {
            text.append("Músculo principal: ").append(exercise.primaryMuscleGroup());
            if (!exercise.secondaryMuscles().isEmpty()) {
                text.append(" (secundarios: ").append(String.join(", ", exercise.secondaryMuscles())).append(')');
            }
            text.append('\n');
        }
        if (exercise.instructions() != null && !exercise.instructions().isBlank()) {
            String steps = exercise.instructions().replace('\n', ' ');
            text.append("Cómo se hace: ").append(steps.length() > 400 ? steps.substring(0, 400) : steps).append('\n');
        }
    }

    /** Nombre sin datos personales (un ejercicio propio puede tener cualquier texto). */
    private static String safeName(ExerciseCatalogItemResponse exercise) {
        return PersonalDataFilter.redact(exercise.name().trim());
    }

    private static String oneLine(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").replace("|", "/").trim();
    }

    static Map<String, Object> tipsSchema() {
        Map<String, Object> line = Map.of("type", "string");
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("summary", "cues", "mistakes", "breathing"),
                "properties", Map.of(
                        "summary", line,
                        "cues", Map.of("type", "array", "minItems", 3, "maxItems", 5, "items", line),
                        "mistakes", Map.of("type", "array", "minItems", 2, "maxItems", 4, "items", line),
                        "breathing", line));
    }

    static Map<String, Object> judgeSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("choice", "confidence"),
                "properties", Map.of(
                        "choice", Map.of("type", "integer", "description", "Número del video, 0 si ninguno"),
                        "confidence", Map.of("type", "string", "enum", List.of("high", "medium", "low"))));
    }

    // ------------------------------------------------------------------ consejos

    /** Consejos validados: textos recortados, sin vacios; null si no alcanza (menos de 2 claves). */
    static ExerciseGuideResponse.Tips parseTips(String text) {
        JsonNode root = readJson(text);
        if (root == null) {
            return null;
        }
        List<String> cues = lines(root.path("cues"), 5, 140);
        List<String> mistakes = lines(root.path("mistakes"), 4, 140);
        String summary = clip(root.path("summary").asText(""), 240);
        String breathing = clip(root.path("breathing").asText(""), 200);
        if (cues.size() < 2 || summary.isEmpty()) {
            return null;
        }
        return new ExerciseGuideResponse.Tips(summary, cues, mistakes, breathing.isEmpty() ? null : breathing);
    }

    private static ExerciseGuideResponse.Tips parseStoredTips(String json) {
        return json == null ? null : parseTips(json);
    }

    private static List<String> lines(JsonNode array, int max, int maxLength) {
        List<String> out = new ArrayList<>();
        if (array.isArray()) {
            for (JsonNode item : array) {
                String value = clip(item.asText(""), maxLength);
                if (!value.isEmpty() && out.size() < max) {
                    out.add(value);
                }
            }
        }
        return out;
    }

    private static String clip(String value, int max) {
        String text = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return text.length() <= max ? text : text.substring(0, max - 1).trim() + "…";
    }

    /** El JSON de la respuesta; tolera texto alrededor (modelos en modo de mejor esfuerzo). */
    static JsonNode readJson(String text) {
        if (text == null) {
            return null;
        }
        try {
            JsonNode node = JSON.readTree(text.trim());
            if (node != null && node.isObject()) {
                return node;
            }
        } catch (Exception ignored) {
            // sigue con el recorte
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            JsonNode node = JSON.readTree(text.substring(start, end + 1));
            return node instanceof ObjectNode ? node : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
