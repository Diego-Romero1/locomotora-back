package com.locomotora.demo.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.locomotora.demo.ai.client.AiModelClient;
import com.locomotora.demo.ai.client.AiUnavailableException;
import com.locomotora.demo.ai.repository.AiRepository;
import com.locomotora.demo.ai.repository.ExerciseGuideRepository;
import com.locomotora.demo.ai.youtube.YouTubeClient;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogVideoJobTest {
    private final ExerciseGuideService guides = mock(ExerciseGuideService.class);
    private final ExerciseGuideRepository repository = mock(ExerciseGuideRepository.class);
    private final AiRepository aiRepository = mock(AiRepository.class);
    private final AiModelClient client = mock(AiModelClient.class);
    private final YouTubeClient youtube = mock(YouTubeClient.class);
    private final AiSettingsService settings = mock(AiSettingsService.class);
    private CatalogVideoJob job;

    @BeforeEach
    void setUp() {
        job = new CatalogVideoJob(guides, repository, aiRepository, client, youtube, settings, true, 100_000, Duration.ZERO, Duration.ZERO);
        when(client.isConfigured()).thenReturn(true);
        when(youtube.isConfigured()).thenReturn(true);
    }

    @Test
    void fillsByPriorityUntilTheDailySearchesRunOut() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        when(repository.nextToFill(anyInt(), anyInt())).thenReturn(List.of(a, b, c));
        // Quedan busquedas para dos ejercicios (cada uno puede necesitar 2).
        when(guides.searchesLeft()).thenReturn(5, 5, 3, 1);

        int done = job.fill();

        assertThat(done).isEqualTo(2);
        verify(guides).fill(a);
        verify(guides).fill(b);
        verify(guides, never()).fill(c);
    }

    @Test
    void stopsAtTheTokenCapAndWhenTheAiIsBusy() {
        UUID a = UUID.randomUUID();
        when(repository.nextToFill(anyInt(), anyInt())).thenReturn(List.of(a));
        when(guides.searchesLeft()).thenReturn(50);
        when(aiRepository.sumSystemTokensLastDay("CATALOG")).thenReturn(100_000L);
        assertThat(job.fill()).isZero();

        when(aiRepository.sumSystemTokensLastDay("CATALOG")).thenReturn(0L);
        when(guides.fill(a)).thenThrow(new AiUnavailableException("busy"));
        assertThat(job.fill()).isZero();
        verify(guides, times(1)).fill(a);
    }

    @Test
    void waitsOnTheAiPerMinuteLimitAndThenContinues() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(repository.nextToFill(anyInt(), anyInt())).thenReturn(List.of(a, b), List.of(b), List.of());
        when(guides.searchesLeft()).thenReturn(50);
        when(guides.fill(a)).thenThrow(new AiUnavailableException("rate limited", 429, null)).thenReturn(100L);

        int done = job.fill();

        // a choca con el limite por minuto: se espera y se sigue con la cola (a queda para otra vuelta).
        assertThat(done).isEqualTo(1);
        verify(guides).fill(b);
    }

    @Test
    void withoutKeysDoesNothing() {
        when(youtube.isConfigured()).thenReturn(false);
        assertThat(job.fill()).isZero();
        assertThat(job.refresh()).isZero();
        verify(repository, never()).nextToFill(anyInt(), anyInt());
    }

    @Test
    void refreshesInBatches() {
        UUID a = UUID.randomUUID();
        when(repository.dueForRefresh(anyInt(), anyInt())).thenReturn(List.of(a));
        when(guides.refresh(any())).thenReturn(1);
        assertThat(job.refresh()).isEqualTo(1);
    }
}
