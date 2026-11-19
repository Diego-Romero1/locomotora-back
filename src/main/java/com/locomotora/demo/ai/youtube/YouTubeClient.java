package com.locomotora.demo.ai.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Busqueda de videos con la YouTube Data API v3. Solo videos publicos, embebibles y cortos. La clave sale
 * de YOUTUBE_API_KEY y nunca llega a la app. Cuota: cada busqueda cuesta 100 unidades y cada consulta de
 * detalles 1 (10.000 por dia en el plan gratis); por eso el resultado se guarda por ejercicio.
 */
@Component
public class YouTubeClient {
    public static final String BASE_URL = "https://www.googleapis.com/youtube/v3";
    /** Mas corto que esto no alcanza para ver una repeticion completa; mas largo ya no es "corto". */
    public static final int MIN_SECONDS = 15;
    public static final int MAX_SECONDS = 300;
    private static final Pattern ISO_DURATION = Pattern.compile("^PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?$");
    private static final Logger log = LoggerFactory.getLogger(YouTubeClient.class);

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public YouTubeClient(@Value("${locomotora.youtube.api-key:}") String apiKey, RestClient.Builder builder) {
        this(apiKey, builder.requestFactory(requestFactory()).baseUrl(BASE_URL).build());
    }

    YouTubeClient(String apiKey, RestClient restClient) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.restClient = restClient;
    }

    private static JdkClientHttpRequestFactory requestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    /** Video candidato con lo necesario para elegirlo y mostrarlo. */
    public record Video(String id, String title, String channel, String description, int durationSeconds, long views) {
    }

    /** Falla de la API (cuota, red, clave invalida): el servicio lo trata como "sin video por ahora". */
    public static class YouTubeUnavailableException extends RuntimeException {
        public YouTubeUnavailableException(String message) {
            super(message);
        }
    }

    /**
     * Busca videos cortos y embebibles para la consulta y devuelve los que duran entre MIN_SECONDS y
     * MAX_SECONDS, en el orden de relevancia de YouTube.
     */
    public List<Video> searchShort(String query, String language, int maxResults) {
        if (!isConfigured()) {
            return List.of();
        }
        try {
            JsonNode search = restClient.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("part", "snippet")
                            .queryParam("type", "video")
                            .queryParam("q", query)
                            .queryParam("maxResults", maxResults)
                            .queryParam("videoDuration", "short")
                            .queryParam("videoEmbeddable", "true")
                            .queryParam("videoSyndicated", "true")
                            .queryParam("safeSearch", "strict")
                            .queryParam("relevanceLanguage", language)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            List<String> ids = new ArrayList<>();
            if (search != null) {
                for (JsonNode item : search.path("items")) {
                    String id = item.path("id").path("videoId").asText("");
                    if (!id.isEmpty() && !ids.contains(id)) {
                        ids.add(id);
                    }
                }
            }
            if (ids.isEmpty()) {
                return List.of();
            }
            JsonNode details = restClient.get()
                    .uri(uri -> uri.path("/videos")
                            .queryParam("part", "snippet,contentDetails,status,statistics")
                            .queryParam("id", String.join(",", ids))
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            Map<String, Video> byId = new LinkedHashMap<>();
            if (details != null) {
                for (JsonNode item : details.path("items")) {
                    Video video = toVideo(item);
                    if (video != null) {
                        byId.put(video.id(), video);
                    }
                }
            }
            return ids.stream().map(byId::get).filter(video -> video != null).toList();
        } catch (RestClientException failure) {
            // Sin la clave ni la consulta en el log (la URL lleva la clave).
            log.warn("YouTube search failed: {}", failure.getClass().getSimpleName());
            throw new YouTubeUnavailableException("YouTube search failed");
        }
    }

    /** null si no es publico, no se puede embeber, es un directo o no dura lo que buscamos. */
    static Video toVideo(JsonNode item) {
        JsonNode status = item.path("status");
        if (!"public".equals(status.path("privacyStatus").asText()) || !status.path("embeddable").asBoolean(false)) {
            return null;
        }
        if (!"none".equals(item.path("snippet").path("liveBroadcastContent").asText("none"))) {
            return null;
        }
        int seconds = parseDuration(item.path("contentDetails").path("duration").asText(""));
        if (seconds < MIN_SECONDS || seconds > MAX_SECONDS) {
            return null;
        }
        JsonNode snippet = item.path("snippet");
        String description = snippet.path("description").asText("");
        return new Video(
                item.path("id").asText(),
                snippet.path("title").asText(""),
                snippet.path("channelTitle").asText(""),
                description.length() > 300 ? description.substring(0, 300) : description,
                seconds,
                item.path("statistics").path("viewCount").asLong(0));
    }

    /** "PT1M24S" → 84; -1 si no se entiende. */
    static int parseDuration(String iso) {
        Matcher matcher = ISO_DURATION.matcher(iso == null ? "" : iso);
        if (!matcher.matches()) {
            return -1;
        }
        int hours = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1));
        int minutes = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        int seconds = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return hours * 3600 + minutes * 60 + seconds;
    }
}
