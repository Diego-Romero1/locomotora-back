package com.locomotora.demo.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.locomotora.demo.ai.youtube.YouTubeClient;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import com.locomotora.demo.routine.repository.RoutineRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
    private RoutineRepository routineRepository;
    @Mock
    private ExerciseGuideRepository guideRepository;

    private ExerciseGuideService service;

    private ExerciseCatalogItemResponse exercise() {
        return new ExerciseCatalogItemResponse(exerciseId.toString(), "Press de banca plano", "Hipertrofia", "Pecho",
                "BARBELL", "INTERMEDIATE", null, "1. Acuéstate.\n2. Baja la barra.\n3. Empuja.", false, List.of(),
                "Pecho", List.of("Hombros", "Tríceps"), "Barbell Bench Press - Medium Grip", "STRENGTH", "COMPOUND", true, null,
                List.of(), null, null, null);
    }

    private static YouTubeClient.Video video(String id, String title) {
        return new YouTubeClient.Video(id, title, "Coach", "Técnica", 70, 1000);
    }

    private static Completion reply(String text) {
        return new Completion(text, "qwen/qwen3.8-27b", StopReason.END, 100, 40);
    }

    @BeforeEach
    void setUp() {
        AiProperties properties = AiProperties.withKey("gsk-test", "");
        AiSettingsService settings = new AiSettingsService(new AiModelCatalog(properties), properties, aiRepository, client);
        service = new ExerciseGuideService(client, settings, properties, youtube, routineRepository, guideRepository);
        when(client.isConfigured()).thenReturn(true);
        when(youtube.isConfigured()).thenReturn(true);
        when(aiRepository.findModelChoice(userId)).thenReturn(new ModelChoice(null, null));
        when(routineRepository.findVisibleExercise(exerciseId, userId)).thenReturn(Optional.of(exercise()));
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
    void firstRequestGeneratesTipsAndPicksTheVideoTheModelIsSureAbout() {
        when(youtube.searchShort(anyString(), eq("es"), anyInt()))
                .thenReturn(List.of(video("v1", "Press inclinado con mancuernas"), video("v2", "Press de banca plano: técnica correcta")));
        when(client.complete(any())).thenAnswer(call -> {
            AiModelClient.Request request = call.getArgument(0);
            requests.add(request);
            return reply("exercise_tips".equals(request.schemaName()) ? TIPS : "{\"choice\":2,\"confidence\":\"high\"}");
        });

        ExerciseGuideResponse guide = service.guide(exerciseId);

        assertThat(guide.tips().cues()).hasSize(3);
        assertThat(guide.video().youtubeId()).isEqualTo("v2");
        assertThat(guide.video().watchUrl()).isEqualTo("https://www.youtube.com/watch?v=v2");
        assertThat(guide.videoStatus()).isEqualTo("FOUND");
        assertThat(guide.searchUrl()).startsWith("https://www.youtube.com/results?search_query=Press+de+banca+plano");
        verify(guideRepository).saveTips(eq(exerciseId), anyString(), eq("qwen/qwen3.8-27b"));
        verify(guideRepository).saveVideo(exerciseId, "FOUND", "v2", "Press de banca plano: técnica correcta", "Coach", 70);
        // El juez recibe el ejercicio con su equipo y los candidatos numerados; una sola fila de uso.
        String judge = requests.get(1).turns().get(0).text();
        assertThat(judge).contains("Equipo: barra", "Barbell Bench Press", "2 | Press de banca plano: técnica correcta");
        verify(aiRepository).recordUsage(userId, "COACH", "qwen/qwen3.8-27b", 200, 80);
    }

    @Test
    void doubtfulChoicesAreRejectedAndEnglishIsTriedBeforeGivingUp() {
        when(youtube.searchShort(anyString(), anyString(), anyInt())).thenReturn(List.of(video("v1", "Press de banca")));
        when(client.complete(any())).thenAnswer(call -> {
            AiModelClient.Request request = call.getArgument(0);
            return reply("exercise_tips".equals(request.schemaName()) ? TIPS : "{\"choice\":1,\"confidence\":\"medium\"}");
        });

        ExerciseGuideResponse guide = service.guide(exerciseId);

        assertThat(guide.video()).isNull();
        assertThat(guide.videoStatus()).isEqualTo("NOT_FOUND");
        verify(youtube).searchShort(eq("Press de banca plano técnica correcta"), eq("es"), anyInt());
        verify(youtube).searchShort(eq("Barbell Bench Press - Medium Grip proper form"), eq("en"), anyInt());
        verify(guideRepository).saveVideo(exerciseId, "NOT_FOUND", null, null, null, null);
    }

    @Test
    void storedGuideIsServedWithoutCallingTheModelOrYouTube() {
        when(guideRepository.find(exerciseId)).thenReturn(Optional.of(new ExerciseGuideRepository.Stored(
                TIPS, "FOUND", "v9", "Press de banca", "Coach", 65, Instant.now())));

        ExerciseGuideResponse guide = service.guide(exerciseId);

        assertThat(guide.tips().summary()).startsWith("Empuje horizontal");
        assertThat(guide.video().youtubeId()).isEqualTo("v9");
        verify(client, never()).complete(any());
        verify(youtube, never()).searchShort(anyString(), anyString(), anyInt());
        verify(aiRepository, never()).recordUsage(any(), any(), any(), any(Long.class), any(Long.class));
    }

    @Test
    void withoutYouTubeKeyReturnsTipsAndTheSearchLink() {
        when(youtube.isConfigured()).thenReturn(false);
        when(client.complete(any())).thenReturn(reply(TIPS));

        ExerciseGuideResponse guide = service.guide(exerciseId);

        assertThat(guide.tips()).isNotNull();
        assertThat(guide.video()).isNull();
        assertThat(guide.videoStatus()).isEqualTo("UNAVAILABLE");
        verify(guideRepository, never()).saveVideo(any(), any(), any(), any(), any(), any());
    }

    @Test
    void youTubeFailureKeepsTheTipsAndRetriesLater() {
        when(youtube.searchShort(anyString(), anyString(), anyInt()))
                .thenThrow(new YouTubeClient.YouTubeUnavailableException("quota"));
        when(client.complete(any())).thenReturn(reply(TIPS));

        ExerciseGuideResponse guide = service.guide(exerciseId);

        assertThat(guide.tips()).isNotNull();
        assertThat(guide.videoStatus()).isEqualTo("UNAVAILABLE");
        verify(guideRepository, never()).saveVideo(any(), any(), any(), any(), any(), any());
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
        assertThat(ExerciseGuideService.videoIsDue(new ExerciseGuideRepository.Stored(TIPS, "NOT_FOUND", null, null, null, null,
                now.minus(10, ChronoUnit.DAYS)), now)).isFalse();
        assertThat(ExerciseGuideService.videoIsDue(new ExerciseGuideRepository.Stored(TIPS, "NOT_FOUND", null, null, null, null,
                now.minus(31, ChronoUnit.DAYS)), now)).isTrue();
        assertThat(ExerciseGuideService.videoIsDue(new ExerciseGuideRepository.Stored(TIPS, "FOUND", "v", "t", "c", 60, now), now)).isFalse();
    }

    @Test
    void tipsAreValidatedAndTrimmed() {
        assertThat(ExerciseGuideService.parseTips("texto suelto")).isNull();
        assertThat(ExerciseGuideService.parseTips("{\"summary\":\"x\",\"cues\":[\"una\"],\"mistakes\":[],\"breathing\":\"\"}")).isNull();
        ExerciseGuideResponse.Tips tips = ExerciseGuideService.parseTips("Aquí va: " + TIPS + " listo");
        assertThat(tips).isNotNull();
        assertThat(tips.mistakes()).hasSize(2);
        assertThat(ExerciseGuideService.chosen("{\"choice\":5,\"confidence\":\"high\"}", List.of(video("a", "x")))).isNull();
        assertThat(ExerciseGuideService.chosen("{\"choice\":0,\"confidence\":\"high\"}", List.of(video("a", "x")))).isNull();
    }
}
