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
import com.locomotora.demo.ai.repository.AiRepository;
import com.locomotora.demo.ai.repository.ExerciseGuideRepository;
import com.locomotora.demo.ai.repository.ExerciseGuideRepository.Stored;
import com.locomotora.demo.ai.repository.ExerciseGuideRepository.StoredVideo;
import com.locomotora.demo.ai.repository.YouTubeQuotaRepository;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * "Detalles con video" de un ejercicio: consejos de tecnica generados con IA y un video corto de YouTube que
 * muestra exactamente ese ejercicio, el mismo para todos los usuarios.
 *
 * <p>Los videos del catalogo los busca un proceso diario ({@link CatalogVideoJob}) por adelantado, con 2 o 3
 * alternativas guardadas; nadie espera ni gasta al abrir un ejercicio. Al pedido solo se busca para ejercicios
 * propios del usuario y cuando un video deja de cargar y no quedan alternativas.
 *
 * <p>La IA ve solo titulo, canal, duracion y comienzo de la descripcion de cada video (no el video): por eso
 * antes se aplican reglas fijas ({@link VideoRules}) y solo vale una eleccion con confianza alta.
 */
@Service
public class ExerciseGuideService {
    /** Si no se encontro video, se vuelve a buscar pasado este tiempo (YouTube suma videos). */
    static final Duration RETRY_NOT_FOUND = Duration.ofDays(30);
    static final int CANDIDATES = 8;
    /** Video principal mas alternativas que se guardan por ejercicio. */
    static final int KEEP = 3;
    static final String SYSTEM_FEATURE = "CATALOG";
    static final String USER_SEARCH_FEATURE = "VIDEO_SEARCH";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AiModelClient client;
    private final AiSettingsService settings;
    private final AiProperties properties;
    private final AiRepository aiRepository;
    private final YouTubeClient youtube;
    private final YouTubeQuotaRepository quota;
    private final RoutineRepository routineRepository;
    private final ExerciseGuideRepository guideRepository;
    private final int dailySearchBudget;
    private final int userDailyReplacements;

    public ExerciseGuideService(
            AiModelClient client,
            AiSettingsService settings,
            AiProperties properties,
            AiRepository aiRepository,
            YouTubeClient youtube,
            YouTubeQuotaRepository quota,
            RoutineRepository routineRepository,
            ExerciseGuideRepository guideRepository,
            @Value("${locomotora.youtube.daily-search-budget:95}") int dailySearchBudget,
            @Value("${locomotora.youtube.user-daily-replacements:5}") int userDailyReplacements
    ) {
        this.client = client;
        this.settings = settings;
        this.properties = properties;
        this.aiRepository = aiRepository;
        this.youtube = youtube;
        this.quota = quota;
        this.routineRepository = routineRepository;
        this.guideRepository = guideRepository;
        this.dailySearchBudget = dailySearchBudget;
        this.userDailyReplacements = userDailyReplacements;
    }

    // ------------------------------------------------------------------ pedido del usuario

    public ExerciseGuideResponse guide(UUID exerciseId) {
        UUID userId = CurrentUser.id();
        ExerciseCatalogItemResponse exercise = visible(exerciseId, userId);
        Stored stored = guideRepository.find(exerciseId).orElse(null);

        boolean aiAvailable = client.isConfigured();
        ExerciseGuideResponse.Tips tips = stored == null ? null : parseStoredTips(stored.tipsJson());
        boolean needTips = aiAvailable && tips == null;
        // Los del catalogo los llena el proceso diario; al pedido solo se busca para ejercicios propios.
        boolean needVideo = exercise.custom() && aiAvailable && youtube.isConfigured() && videoIsDue(stored, Instant.now())
                && searchesLeft() >= 1;

        if (!needTips && !needVideo) {
            return response(exercise, tips, stored, aiAvailable);
        }
        settings.checkQuota(userId, Feature.COACH);
        if (settings.globalBudgetExhausted()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI daily budget reached");
        }
        String model = settings.modelFor(userId, Feature.COACH, null);
        Usage usage = new Usage();
        try {
            if (needTips) {
                tips = generateTips(model, exercise, usage);
                if (tips == null) {
                    throw new ApiException(HttpStatus.BAD_GATEWAY, "AI returned invalid tips");
                }
                guideRepository.saveTips(exerciseId, JSON.valueToTree(tips).toString(), model);
            }
            if (needVideo) {
                Search search = findVideos(model, exercise, failed(stored), usage);
                stored = saveSearch(exerciseId, stored, search, "ON_DEMAND");
            }
        } catch (AiUnavailableException unavailable) {
            if (tips == null) {
                if (unavailable.rateLimited()) {
                    throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI provider is busy, try again in a minute");
                }
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI service unavailable");
            }
        } finally {
            if (usage.total() > 0) {
                settings.recordUsage(userId, Feature.COACH, model, usage.input, usage.output);
            }
        }
        return response(exercise, tips, guideRepository.find(exerciseId).orElse(stored), aiAvailable);
    }

    /**
     * El video no cargo en el telefono del usuario. Se confirma con YouTube (1 unidad) para que un problema de
     * red de una persona no cambie el video de todos; si de verdad no esta, pasa a la siguiente alternativa
     * (sin costo) y solo si no quedan busca otro, con un limite diario por usuario.
     */
    public ExerciseGuideResponse reportUnavailable(UUID exerciseId, String videoId) {
        UUID userId = CurrentUser.id();
        ExerciseCatalogItemResponse exercise = visible(exerciseId, userId);
        Stored stored = guideRepository.find(exerciseId).orElse(null);
        boolean aiAvailable = client.isConfigured();
        ExerciseGuideResponse.Tips tips = stored == null ? null : parseStoredTips(stored.tipsJson());
        if (stored == null || stored.videoId() == null || !stored.videoId().equals(videoId)) {
            // Otro usuario ya lo reemplazo (o nunca fue este): se devuelve el actual.
            return response(exercise, tips, stored, aiAvailable).withNotice("REPLACED");
        }
        if (youtube.isConfigured()) {
            try {
                if (youtube.available(List.of(videoId)).containsKey(videoId)) {
                    return response(exercise, tips, stored, aiAvailable).withNotice("STILL_AVAILABLE");
                }
            } catch (YouTubeClient.YouTubeUnavailableException failure) {
                // No se pudo confirmar: se toma la alternativa igual (es gratis y no empeora nada).
            }
        }
        Set<String> failed = new LinkedHashSet<>(stored.failedIds());
        failed.add(videoId);
        List<StoredVideo> alternatives = stored.alternatives().stream().filter(video -> !failed.contains(video.id())).toList();
        if (!alternatives.isEmpty()) {
            guideRepository.saveVideo(exerciseId, "FOUND", alternatives.get(0), alternatives.subList(1, alternatives.size()),
                    List.copyOf(failed), "REPLACEMENT");
            return response(exercise, tips, guideRepository.find(exerciseId).orElse(null), aiAvailable).withNotice("REPLACED");
        }
        // Sin alternativas: se saca el video caido y, si se puede, se busca otro.
        guideRepository.saveVideo(exerciseId, "PENDING", null, List.of(), List.copyOf(failed), stored.videoSource());
        boolean canSearch = aiAvailable && youtube.isConfigured() && searchesLeft() >= 1;
        if (!canSearch) {
            return response(exercise, tips, guideRepository.find(exerciseId).orElse(null), aiAvailable).withNotice("NO_OTHER");
        }
        if (aiRepository.countUsageLastDay(userId, USER_SEARCH_FEATURE) >= userDailyReplacements || settings.globalBudgetExhausted()) {
            return response(exercise, tips, guideRepository.find(exerciseId).orElse(null), aiAvailable).withNotice("LIMIT");
        }
        String model = properties.coachDefaultModel();
        Usage usage = new Usage();
        try {
            Search search = findVideos(model, exercise, failed, usage);
            Stored saved = saveSearch(exerciseId, guideRepository.find(exerciseId).orElse(null), search, "REPLACEMENT");
            String notice = saved != null && "FOUND".equals(saved.videoStatus()) ? "REPLACED" : "NO_OTHER";
            return response(exercise, tips, saved, aiAvailable).withNotice(notice);
        } catch (AiUnavailableException unavailable) {
            return response(exercise, tips, guideRepository.find(exerciseId).orElse(null), aiAvailable).withNotice("NO_OTHER");
        } finally {
            // Se registra aunque no gaste tokens: cuenta para el limite diario de busquedas del usuario.
            aiRepository.recordUsage(userId, USER_SEARCH_FEATURE, model, usage.input, usage.output);
        }
    }

    // ------------------------------------------------------------------ proceso diario

    /** Busquedas de YouTube que quedan hoy dentro del presupuesto del backend. */
    public int searchesLeft() {
        return Math.max(0, dailySearchBudget - quota.searchesToday());
    }

    /**
     * Llena un ejercicio del catalogo (lo llama el proceso diario): consejos si faltan y video con alternativas.
     * Devuelve los tokens gastados (se registran como uso del sistema, sin usuario).
     */
    public long fill(UUID exerciseId) {
        ExerciseCatalogItemResponse exercise = routineRepository.findVisibleExercise(exerciseId, null).orElse(null);
        if (exercise == null) {
            return 0;
        }
        Stored stored = guideRepository.find(exerciseId).orElse(null);
        String model = properties.coachDefaultModel();
        Usage usage = new Usage();
        try {
            if (stored == null || parseStoredTips(stored.tipsJson()) == null) {
                ExerciseGuideResponse.Tips tips = generateTips(model, exercise, usage);
                if (tips != null) {
                    guideRepository.saveTips(exerciseId, JSON.valueToTree(tips).toString(), model);
                }
            }
            if (videoIsDue(stored, Instant.now())) {
                Search search = findVideos(model, exercise, failed(stored), usage);
                saveSearch(exerciseId, guideRepository.find(exerciseId).orElse(stored), search, "FILL");
            }
        } finally {
            if (usage.total() > 0) {
                aiRepository.recordUsage(null, SYSTEM_FEATURE, model, usage.input, usage.output);
            }
        }
        return usage.total();
    }

    /**
     * Refresco: confirma que los videos guardados siguen disponibles y actualiza titulo, canal y duracion (la
     * politica de YouTube pide refrescar sus datos cada 30 dias). Si el principal ya no esta, pasa a la siguiente
     * alternativa; si no queda ninguna, el ejercicio vuelve a la cola del proceso diario.
     */
    public int refresh(List<UUID> exerciseIds) {
        List<Map.Entry<UUID, Stored>> rows = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (UUID exerciseId : exerciseIds) {
            Stored stored = guideRepository.find(exerciseId).orElse(null);
            if (stored == null || stored.videoId() == null) {
                continue;
            }
            rows.add(Map.entry(exerciseId, stored));
            ids.add(stored.videoId());
            stored.alternatives().forEach(video -> ids.add(video.id()));
        }
        if (ids.isEmpty()) {
            return 0;
        }
        Map<String, Video> available = youtube.available(ids.stream().distinct().toList());
        for (Map.Entry<UUID, Stored> row : rows) {
            Stored stored = row.getValue();
            Set<String> failed = new LinkedHashSet<>(stored.failedIds());
            List<StoredVideo> alive = new ArrayList<>();
            for (StoredVideo video : withPrimary(stored)) {
                Video fresh = available.get(video.id());
                if (fresh == null) {
                    failed.add(video.id());
                } else {
                    alive.add(stored(fresh));
                }
            }
            if (alive.isEmpty()) {
                guideRepository.saveVideo(row.getKey(), "PENDING", null, List.of(), List.copyOf(failed), stored.videoSource());
            } else if (!alive.get(0).id().equals(stored.videoId())) {
                guideRepository.saveVideo(row.getKey(), "FOUND", alive.get(0), alive.subList(1, alive.size()),
                        List.copyOf(failed), "REPLACEMENT");
            } else {
                guideRepository.saveRefresh(row.getKey(), alive.get(0), alive.subList(1, alive.size()), List.copyOf(failed));
            }
        }
        return rows.size();
    }

    // ------------------------------------------------------------------ busqueda

    /** videos: elegidos en orden (vacio si ninguno); searched: false si YouTube fallo y hay que reintentar. */
    record Search(List<Video> videos, boolean searched) {
    }

    /** Tokens gastados en un pedido (puede haber varias llamadas al modelo). */
    static final class Usage {
        long input;
        long output;

        void add(Completion completion) {
            input += completion.inputTokens();
            output += completion.outputTokens();
        }

        long total() {
            return input + output;
        }
    }

    /**
     * Primero en español; si ningun video sirve, en ingles con el nombre original. Las reglas fijas y los videos
     * que ya fallaron se descartan antes de preguntarle a la IA, asi no gasta tokens en ellos.
     */
    Search findVideos(String model, ExerciseCatalogItemResponse exercise, Set<String> excluded, Usage usage) {
        List<String[]> queries = new ArrayList<>();
        queries.add(new String[] {safeName(exercise) + " técnica correcta", "es"});
        if (exercise.nameEn() != null && !exercise.nameEn().isBlank()) {
            queries.add(new String[] {exercise.nameEn() + " proper form", "en"});
        }
        try {
            for (String[] query : queries) {
                if (searchesLeft() < 1) {
                    return new Search(List.of(), false);
                }
                List<Video> candidates = youtube.searchShort(query[0], query[1], CANDIDATES).stream()
                        .filter(video -> !excluded.contains(video.id()))
                        .filter(video -> VideoRules.acceptable(exercise, video))
                        .toList();
                if (candidates.isEmpty()) {
                    continue;
                }
                Completion completion = call(model, JUDGE_SYSTEM, judgeMessage(exercise, candidates), "video_choice", judgeSchema(), 200);
                usage.add(completion);
                List<Video> chosen = chosen(completion.text(), candidates);
                if (!chosen.isEmpty()) {
                    return new Search(chosen, true);
                }
            }
            return new Search(List.of(), true);
        } catch (YouTubeClient.YouTubeUnavailableException failure) {
            return new Search(List.of(), false);
        }
    }

    /** Guarda el resultado de una busqueda; si YouTube fallo, deja todo como estaba. */
    private Stored saveSearch(UUID exerciseId, Stored stored, Search search, String source) {
        List<String> failed = stored == null ? List.of() : stored.failedIds();
        if (!search.videos().isEmpty()) {
            List<StoredVideo> videos = search.videos().stream().map(ExerciseGuideService::stored).toList();
            guideRepository.saveVideo(exerciseId, "FOUND", videos.get(0), videos.subList(1, videos.size()), failed, source);
        } else if (search.searched()) {
            guideRepository.saveVideo(exerciseId, "NOT_FOUND", null, List.of(), failed, source);
        } else {
            return stored;
        }
        return guideRepository.find(exerciseId).orElse(stored);
    }

    /** Solo elecciones con confianza alta, dentro de la lista y sin repetir, en el orden de la IA (hasta 3). */
    static List<Video> chosen(String text, List<Video> candidates) {
        JsonNode root = readJson(text);
        List<Video> picked = new ArrayList<>();
        if (root == null) {
            return picked;
        }
        for (JsonNode match : root.path("matches")) {
            int number = match.path("n").asInt(0);
            if (!"high".equals(match.path("confidence").asText("")) || number < 1 || number > candidates.size()) {
                continue;
            }
            Video video = candidates.get(number - 1);
            if (!picked.contains(video)) {
                picked.add(video);
            }
            if (picked.size() == KEEP) {
                break;
            }
        }
        return picked;
    }

    static boolean videoIsDue(Stored stored, Instant now) {
        if (stored == null || "PENDING".equals(stored.videoStatus())) {
            return true;
        }
        return "NOT_FOUND".equals(stored.videoStatus())
                && (stored.videoCheckedAt() == null || stored.videoCheckedAt().plus(RETRY_NOT_FOUND).isBefore(now));
    }

    private static Set<String> failed(Stored stored) {
        return stored == null ? Set.of() : new LinkedHashSet<>(stored.failedIds());
    }

    private static List<StoredVideo> withPrimary(Stored stored) {
        List<StoredVideo> all = new ArrayList<>();
        all.add(stored.primary());
        all.addAll(stored.alternatives());
        return all;
    }

    private static StoredVideo stored(Video video) {
        return new StoredVideo(video.id(), video.title(), video.channel(), video.durationSeconds());
    }

    // ------------------------------------------------------------------ respuesta

    private ExerciseCatalogItemResponse visible(UUID exerciseId, UUID userId) {
        return routineRepository.findVisibleExercise(exerciseId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exercise not found"));
    }

    private ExerciseGuideResponse response(ExerciseCatalogItemResponse exercise, ExerciseGuideResponse.Tips tips,
            Stored stored, boolean aiAvailable) {
        ExerciseGuideResponse.Video video = null;
        String status;
        if (stored != null && "FOUND".equals(stored.videoStatus()) && stored.videoId() != null) {
            video = toResponse(stored.videoId(), stored.videoTitle(), stored.videoChannel(),
                    stored.videoDurationSeconds() == null ? 0 : stored.videoDurationSeconds());
            status = "FOUND";
        } else if (stored != null && "NOT_FOUND".equals(stored.videoStatus())) {
            status = "NOT_FOUND";
        } else if (!exercise.custom() && youtube.isConfigured()) {
            // En la cola del proceso diario.
            status = "PENDING";
        } else {
            status = "UNAVAILABLE";
        }
        return new ExerciseGuideResponse(exercise.id(), exercise.name(), tips, video, status, searchUrl(exercise), aiAvailable);
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

    private ExerciseGuideResponse.Tips generateTips(String model, ExerciseCatalogItemResponse exercise, Usage usage) {
        Completion completion = call(model, TIPS_SYSTEM, tipsMessage(exercise), "exercise_tips", tipsSchema(), 600);
        usage.add(completion);
        return parseTips(completion.text());
    }

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
            título, el canal, la duración y el comienzo de la descripción de cada video. Un video sirve solo si \
            deja claro que muestra EXACTAMENTE el ejercicio pedido: mismo equipo (barra, mancuernas, máquina, \
            polea...) y misma variante (plano o inclinado, agarre, una o dos manos, de pie o sentado). Un \
            ejercicio parecido no sirve, y tampoco las recopilaciones de varios ejercicios ni las rutinas. Devuelves \
            hasta 3 videos que sirvan, del mejor al peor: primero los que enseñan la técnica y, si son igual de \
            buenos, los que están en español. Confianza alta solo si el título lo deja claro. Si ninguno sirve, la \
            lista va vacía. Respondes solo con el JSON pedido.""";

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
        text.append("\n¿Qué videos muestran exactamente este ejercicio? Hasta 3, del mejor al peor; lista vacía si ninguno.");
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
        Map<String, Object> match = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("n", "confidence"),
                "properties", Map.of(
                        "n", Map.of("type", "integer", "description", "Número del video"),
                        "confidence", Map.of("type", "string", "enum", List.of("high", "medium", "low"))));
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("matches"),
                "properties", Map.of("matches", Map.of("type", "array", "maxItems", KEEP, "items", match)));
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
