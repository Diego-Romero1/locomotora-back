package com.locomotora.demo.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.locomotora.demo.ai.client.AiModelClient;
import com.locomotora.demo.ai.client.AiModelClient.Completion;
import com.locomotora.demo.ai.client.AiModelClient.StopReason;
import com.locomotora.demo.ai.client.AiUnavailableException;
import com.locomotora.demo.ai.config.AiModelCatalog;
import com.locomotora.demo.ai.config.AiProperties;
import com.locomotora.demo.ai.dto.ExerciseGuideResponse;
import com.locomotora.demo.ai.repository.AiRepository;
import com.locomotora.demo.ai.repository.AiRepository.ModelChoice;
import com.locomotora.demo.ai.repository.ExerciseGuideRepository;
import com.locomotora.demo.ai.repository.ExerciseGuideRepository.Stored;
import com.locomotora.demo.ai.repository.ExerciseGuideRepository.StoredVideo;
import com.locomotora.demo.ai.repository.YouTubeQuotaRepository;
import com.locomotora.demo.ai.youtube.YouTubeClient;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import com.locomotora.demo.routine.repository.RoutineRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExerciseGuideServiceTest {
    private static final String TIPS = """
            {"summary":"Empuje horizontal para pecho, hombros y tríceps.",
             "cues":["Junta los omóplatos","Pies firmes en el suelo","Baja la barra al pecho medio"],
             "mistakes":["Rebotar la barra en el pecho","Despegar la cadera del banco"],
             "breathing":"Toma aire al bajar y suéltalo al empujar."}""";
    private static final String QWEN = "qwen/qwen3.8-27b";

    private final UUID userId = UUID.randomUUID();
    private final UUID exerciseId = UUID.randomUUID();
    private final List<AiModelClient.Request> requests = new ArrayList<>();

    @Mock
    private AiModelClient client;
    @Mock
    private AiRepository aiRepository;
    @Mock
    private YouTubeClient youtube;
    @Mock
    private YouTubeQuotaRepository quota;
    @Mock
    private RoutineRepository routineRepository;
    @Mock
    private ExerciseGuideRepository guideRepository;

    private ExerciseGuideService service;

    private ExerciseCatalogItemResponse exercise(boolean custom) {
        return new ExerciseCatalogItemResponse(exerciseId.toString(), "Press de banca plano", "Hipertrofia", "Pecho",
                "BARBELL", "INTERMEDIATE", null, "1. Acuéstate.\n2. Baja la barra.\n3. Empuja.", custom, List.of(),
                "Pecho", List.of("Hombros", "Tríceps"), "Barbell Bench Press - Medium Grip", "STRENGTH", "COMPOUND", true, null,
                List.of(), null, null, null);
    }

    private static YouTubeClient.Video video(String id, String title) {
        return new YouTubeClient.Video(id, title, "Coach", "Técnica", 70, 1000);
    }

    private static StoredVideo stored(String id) {
        return new StoredVideo(id, "Press de banca " + id, "Coach", 60);
    }

    private static Completion reply(String text) {
        return new Completion(text, QWEN, StopReason.END, 100, 40);
    }

    /** El modelo responde consejos o la eleccion de videos segun lo que se le pide. */
    private void modelAnswers(String judge) {
        when(client.complete(any())).thenAnswer(call -> {
            AiModelClient.Request request = call.getArgument(0);
            requests.add(request);
            return reply("exercise_tips".equals(request.schemaName()) ? TIPS : judge);
        });
    }

    @BeforeEach
    void setUp() {
        AiProperties properties = AiProperties.withKey("gsk-test", "");
        AiSettingsService settings = new AiSettingsService(new AiModelCatalog(properties), properties, aiRepository, client);
        service = new ExerciseGuideService(client, settings, properties, aiRepository, youtube, quota, routineRepository,
                guideRepository, 95, 5);
        when(client.isConfigured()).thenReturn(true);
        when(youtube.isConfigured()).thenReturn(true);
        when(aiRepository.findModelChoice(userId)).thenReturn(new ModelChoice(null, null));
        when(routineRepository.findVisibleExercise(exerciseId, userId)).thenReturn(Optional.of(exercise(false)));
        when(routineRepository.findVisibleExercise(exerciseId, null)).thenReturn(Optional.of(exercise(false)));
        when(guideRepository.find(exerciseId)).thenReturn(Optional.empty());
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void catalogExerciseGetsTipsOnDemandButItsVideoComesFromTheDailyFill() {
        modelAnswers("{\"matches\":[]}");

        ExerciseGuideResponse guide = service.guide(exerciseId);

        assertThat(guide.tips().cues()).hasSize(3);
        assertThat(guide.video()).isNull();
        assertThat(guide.videoStatus()).isEqualTo("PENDING");
        verify(youtube, never()).searchShort(anyString(), anyString(), anyInt());
        verify(aiRepository).recordUsage(userId, "COACH", QWEN, 100, 40);
    }

    @Test
    void ownExerciseSearchesOnDemandAndKeepsTheRankedAlternatives() {
        when(routineRepository.findVisibleExercise(exerciseId, userId)).thenReturn(Optional.of(exercise(true)));
        when(youtube.searchShort(anyString(), eq("es"), anyInt())).thenReturn(List.of(
                video("v1", "Press de banca plano paso a paso"),
                video("v2", "Press de banca: técnica correcta"),
                video("v3", "Press de banca con mancuernas")));
        modelAnswers("{\"matches\":[{\"n\":2,\"confidence\":\"high\"},{\"n\":1,\"confidence\":\"high\"},{\"n\":1,\"confidence\":\"high\"}]}");

        service.guide(exerciseId);

        // v3 (mancuernas) lo descartan las reglas antes de la IA; la IA ordena v2, v1.
        assertThat(requests.get(1).turns().get(0).text()).doesNotContain("mancuernas").contains("2 | Press de banca: técnica correcta");
        verify(guideRepository).saveVideo(eq(exerciseId), eq("FOUND"), eq(new StoredVideo("v2", "Press de banca: técnica correcta", "Coach", 70)),
                eq(List.of(new StoredVideo("v1", "Press de banca plano paso a paso", "Coach", 70))), eq(List.of()), eq("ON_DEMAND"));
    }

    @Test
    void dailyFillGeneratesTipsAndVideosAsSystemUsage() {
        when(youtube.searchShort(anyString(), eq("es"), anyInt())).thenReturn(List.of(video("v1", "Press de banca plano")));
        modelAnswers("{\"matches\":[{\"n\":1,\"confidence\":\"high\"}]}");

        long tokens = service.fill(exerciseId);

        assertThat(tokens).isEqualTo(280);
        verify(guideRepository).saveTips(eq(exerciseId), anyString(), eq(QWEN));
        verify(guideRepository).saveVideo(eq(exerciseId), eq("FOUND"), any(), eq(List.of()), eq(List.of()), eq("FILL"));
        verify(aiRepository).recordUsage(isNull(), eq("CATALOG"), eq(QWEN), eq(200L), eq(80L));
    }

    @Test
    void doubtfulChoicesAreRejectedAndEnglishIsTriedBeforeGivingUp() {
        when(youtube.searchShort(anyString(), anyString(), anyInt())).thenReturn(List.of(video("v1", "Press de banca")));
        modelAnswers("{\"matches\":[{\"n\":1,\"confidence\":\"medium\"}]}");

        service.fill(exerciseId);

        verify(youtube).searchShort(eq("Press de banca plano técnica correcta"), eq("es"), anyInt());
        verify(youtube).searchShort(eq("Barbell Bench Press - Medium Grip proper form"), eq("en"), anyInt());
        verify(guideRepository).saveVideo(exerciseId, "NOT_FOUND", null, List.of(), List.of(), "FILL");
    }

    @Test
    void withoutQuotaLeftNothingIsSearchedOrSaved() {
        when(quota.searchesToday()).thenReturn(95);
        modelAnswers("{\"matches\":[]}");

        service.fill(exerciseId);

        verify(youtube, never()).searchShort(anyString(), anyString(), anyInt());
        verify(guideRepository, never()).saveVideo(any(), any(), any(), anyList(), anyList(), any());
    }

    @Test
    void storedGuideIsServedWithoutCallingTheModelOrYouTube() {
        when(guideRepository.find(exerciseId)).thenReturn(Optional.of(new Stored(
                TIPS, "FOUND", "v9", "Press de banca", "Coach", 65, Instant.now())));

        ExerciseGuideResponse guide = service.guide(exerciseId);

        assertThat(guide.video().youtubeId()).isEqualTo("v9");
        verify(client, never()).complete(any());
        verify(youtube, never()).searchShort(anyString(), anyString(), anyInt());
    }

    private Stored found(String primary, List<StoredVideo> alternatives) {
        return new Stored(TIPS, "FOUND", primary, "Press de banca " + primary, "Coach", 60, Instant.now(),
                alternatives, List.of(), "FILL", Instant.now());
    }

    @Test
    void unavailableVideoThatYouTubeStillServesIsKept() {
        when(guideRepository.find(exerciseId)).thenReturn(Optional.of(found("v1", List.of(stored("v2")))));
        when(youtube.available(List.of("v1"))).thenReturn(Map.of("v1", video("v1", "Press de banca")));

        ExerciseGuideResponse guide = service.reportUnavailable(exerciseId, "v1");

        assertThat(guide.notice()).isEqualTo("STILL_AVAILABLE");
        verify(guideRepository, never()).saveVideo(any(), any(), any(), anyList(), anyList(), any());
    }

    @Test
    void reallyUnavailableVideoMovesToTheNextAlternativeForEveryone() {
        when(guideRepository.find(exerciseId)).thenReturn(Optional.of(found("v1", List.of(stored("v2"), stored("v3")))));
        when(youtube.available(List.of("v1"))).thenReturn(Map.of());

        ExerciseGuideResponse guide = service.reportUnavailable(exerciseId, "v1");

        assertThat(guide.notice()).isEqualTo("REPLACED");
        verify(guideRepository).saveVideo(exerciseId, "FOUND", stored("v2"), List.of(stored("v3")), List.of("v1"), "REPLACEMENT");
        verify(youtube, never()).searchShort(anyString(), anyString(), anyInt());
        verify(client, never()).complete(any());
    }

    @Test
    void withoutAlternativesItSearchesAgainUnlessTheUserHitTheDailyLimit() {
        when(guideRepository.find(exerciseId)).thenReturn(Optional.of(found("v1", List.of())));
        when(youtube.available(List.of("v1"))).thenReturn(Map.of());
        when(aiRepository.countUsageLastDay(userId, "VIDEO_SEARCH")).thenReturn(5);

        ExerciseGuideResponse guide = service.reportUnavailable(exerciseId, "v1");

        assertThat(guide.notice()).isEqualTo("LIMIT");
        verify(guideRepository).saveVideo(exerciseId, "PENDING", null, List.of(), List.of("v1"), "FILL");
        verify(youtube, never()).searchShort(anyString(), anyString(), anyInt());
    }

    @Test
    void replacementSearchSkipsTheVideosThatAlreadyFailed() {
        when(guideRepository.find(exerciseId)).thenReturn(Optional.of(found("v1", List.of())));
        when(youtube.available(List.of("v1"))).thenReturn(Map.of());
        when(youtube.searchShort(anyString(), eq("es"), anyInt())).thenReturn(List.of(
                video("v1", "Press de banca plano"), video("v4", "Press de banca: técnica")));
        modelAnswers("{\"matches\":[{\"n\":1,\"confidence\":\"high\"}]}");

        service.reportUnavailable(exerciseId, "v1");

        // v1 ya fallo: el unico candidato que ve la IA es v4.
        assertThat(requests.get(0).turns().get(0).text()).contains("1 | Press de banca: técnica").doesNotContain("2 |");
        verify(aiRepository).recordUsage(userId, "VIDEO_SEARCH", QWEN, 100, 40);
    }

    @Test
    void reportForAVideoThatWasAlreadyReplacedReturnsTheCurrentOne() {
        when(guideRepository.find(exerciseId)).thenReturn(Optional.of(found("v2", List.of())));

        ExerciseGuideResponse guide = service.reportUnavailable(exerciseId, "v1");

        assertThat(guide.video().youtubeId()).isEqualTo("v2");
        verify(youtube, never()).available(anyList());
    }

    @Test
    void refreshKeepsAvailableVideosPromotesAlternativesAndRequeuesDeadOnes() {
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        when(guideRepository.find(exerciseId)).thenReturn(Optional.of(found("a1", List.of(stored("a2")))));
        when(guideRepository.find(second)).thenReturn(Optional.of(found("b1", List.of(stored("b2")))));
        when(guideRepository.find(third)).thenReturn(Optional.of(found("c1", List.of())));
        when(youtube.available(anyList())).thenReturn(Map.of(
                "a1", video("a1", "Nuevo titulo"), "a2", video("a2", "x"), "b2", video("b2", "y")));

        int refreshed = service.refresh(List.of(exerciseId, second, third));

        assertThat(refreshed).isEqualTo(3);
        verify(guideRepository).saveRefresh(exerciseId, new StoredVideo("a1", "Nuevo titulo", "Coach", 70),
                List.of(new StoredVideo("a2", "x", "Coach", 70)), List.of());
        verify(guideRepository).saveVideo(second, "FOUND", new StoredVideo("b2", "y", "Coach", 70), List.of(), List.of("b1"), "REPLACEMENT");
        verify(guideRepository).saveVideo(third, "PENDING", null, List.of(), List.of("c1"), "FILL");
    }

    @Test
    void modelUnavailableWithoutTipsIsAnError() {
        when(client.complete(any())).thenThrow(new AiUnavailableException("down"));
        assertThatThrownBy(() -> service.guide(exerciseId))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void unknownExerciseIsNotFound() {
        when(routineRepository.findVisibleExercise(exerciseId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.guide(exerciseId))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.status()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void notFoundIsSearchedAgainAfterThirtyDays() {
        Instant now = Instant.now();
        assertThat(ExerciseGuideService.videoIsDue(null, now)).isTrue();
        assertThat(ExerciseGuideService.videoIsDue(new Stored(TIPS, "NOT_FOUND", null, null, null, null,
                now.minus(10, ChronoUnit.DAYS)), now)).isFalse();
        assertThat(ExerciseGuideService.videoIsDue(new Stored(TIPS, "NOT_FOUND", null, null, null, null,
                now.minus(31, ChronoUnit.DAYS)), now)).isTrue();
        assertThat(ExerciseGuideService.videoIsDue(new Stored(TIPS, "FOUND", "v", "t", "c", 60, now), now)).isFalse();
    }

    @Test
    void tipsAndChoicesAreValidated() {
        assertThat(ExerciseGuideService.parseTips("texto suelto")).isNull();
        assertThat(ExerciseGuideService.parseTips("Aquí va: " + TIPS + " listo")).isNotNull();
        List<YouTubeClient.Video> candidates = List.of(video("a", "x"), video("b", "y"));
        assertThat(ExerciseGuideService.chosen("{\"matches\":[{\"n\":5,\"confidence\":\"high\"}]}", candidates)).isEmpty();
        assertThat(ExerciseGuideService.chosen("{\"matches\":[{\"n\":2,\"confidence\":\"high\"},{\"n\":1,\"confidence\":\"low\"}]}", candidates))
                .extracting(YouTubeClient.Video::id).containsExactly("b");
    }
}
