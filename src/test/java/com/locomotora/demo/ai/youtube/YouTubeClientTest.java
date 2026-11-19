package com.locomotora.demo.ai.youtube;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class YouTubeClientTest {
    private MockRestServiceServer server;
    private YouTubeClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(YouTubeClient.BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new YouTubeClient("yt-test", builder.build());
    }

    private static String video(String id, String title, String duration, boolean embeddable, String live) {
        return """
                {"id":"%s","snippet":{"title":"%s","channelTitle":"Coach","description":"Técnica paso a paso",
                 "liveBroadcastContent":"%s"},
                 "contentDetails":{"duration":"%s"},
                 "status":{"privacyStatus":"public","embeddable":%s},
                 "statistics":{"viewCount":"1200"}}
                """.formatted(id, title, live, duration, embeddable);
    }

    @Test
    void searchesShortEmbeddableVideosAndKeepsOnlyTheUsableOnes() {
        server.expect(requestTo(startsWith(YouTubeClient.BASE_URL + "/search")))
                .andExpect(queryParam("videoDuration", "short"))
                .andExpect(queryParam("videoEmbeddable", "true"))
                .andExpect(queryParam("safeSearch", "strict"))
                .andExpect(queryParam("relevanceLanguage", "es"))
                .andExpect(queryParam("key", "yt-test"))
                .andRespond(withSuccess("""
                        {"items":[{"id":{"videoId":"aaa"}},{"id":{"videoId":"bbb"}},{"id":{"videoId":"ccc"}},
                                  {"id":{"videoId":"ddd"}},{"id":{"videoId":"eee"}}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(startsWith(YouTubeClient.BASE_URL + "/videos")))
                .andExpect(queryParam("id", "aaa,bbb,ccc,ddd,eee"))
                .andRespond(withSuccess("{\"items\":[" + String.join(",",
                        video("bbb", "Press de banca: técnica", "PT1M24S", true, "none"),
                        video("aaa", "Press de banca correcto", "PT45S", true, "none"),
                        video("ccc", "Rutina completa de pecho", "PT12M3S", true, "none"),
                        video("ddd", "Press banca (no embebible)", "PT1M", false, "none"),
                        video("eee", "Directo de pecho", "PT2M", true, "live")) + "]}", MediaType.APPLICATION_JSON));

        List<YouTubeClient.Video> videos = client.searchShort("Press de banca plano técnica correcta", "es", 8);

        // Orden de relevancia de la busqueda; fuera los largos, los no embebibles y los directos.
        assertThat(videos).extracting(YouTubeClient.Video::id).containsExactly("aaa", "bbb");
        assertThat(videos.get(1).durationSeconds()).isEqualTo(84);
        server.verify();
    }

    @Test
    void failuresAreReportedWithoutLeakingTheKey() {
        server.expect(requestTo(startsWith(YouTubeClient.BASE_URL + "/search")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).body("{\"error\":{\"message\":\"quotaExceeded\"}}"));
        assertThatThrownBy(() -> client.searchShort("x", "es", 8))
                .isInstanceOf(YouTubeClient.YouTubeUnavailableException.class)
                .hasMessageNotContaining("yt-test");
    }

    @Test
    void withoutKeyDoesNothing() {
        YouTubeClient disabled = new YouTubeClient("", RestClient.builder().build());
        assertThat(disabled.isConfigured()).isFalse();
        assertThat(disabled.searchShort("x", "es", 8)).isEmpty();
    }

    @Test
    void availabilityLookupCostsOneUnitPer50AndIgnoresDuration() {
        List<int[]> spent = new java.util.ArrayList<>();
        RestClient.Builder builder = RestClient.builder().baseUrl(YouTubeClient.BASE_URL);
        MockRestServiceServer local = MockRestServiceServer.bindTo(builder).build();
        YouTubeClient counting = new YouTubeClient("yt-test", builder.build(), (units, searches) -> spent.add(new int[] {units, searches}));
        local.expect(requestTo(startsWith(YouTubeClient.BASE_URL + "/videos")))
                .andExpect(queryParam("id", "aaa,bbb,ccc"))
                .andRespond(withSuccess("{\"items\":[" + String.join(",",
                        video("aaa", "Largo pero disponible", "PT12M", true, "none"),
                        video("bbb", "Ya no se puede embeber", "PT1M", false, "none")) + "]}", MediaType.APPLICATION_JSON));

        java.util.Map<String, YouTubeClient.Video> available = counting.available(List.of("aaa", "bbb", "ccc"));

        // ccc no vino (borrado o privado) y bbb no se puede embeber; aaa sigue aunque dure 12 minutos.
        assertThat(available).containsOnlyKeys("aaa");
        assertThat(spent).hasSize(1);
        assertThat(spent.get(0)).containsExactly(1, 0);
        local.verify();
    }

    @Test
    void parsesIsoDurations() {
        assertThat(YouTubeClient.parseDuration("PT1M24S")).isEqualTo(84);
        assertThat(YouTubeClient.parseDuration("PT45S")).isEqualTo(45);
        assertThat(YouTubeClient.parseDuration("PT1H2M")).isEqualTo(3720);
        assertThat(YouTubeClient.parseDuration("P1D")).isEqualTo(-1);
    }
}
