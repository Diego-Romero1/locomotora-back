package com.locomotora.demo.ai.service;

import com.locomotora.demo.ai.client.AiModelClient;
import com.locomotora.demo.ai.client.AiUnavailableException;
import com.locomotora.demo.ai.repository.AiRepository;
import com.locomotora.demo.ai.repository.ExerciseGuideRepository;
import com.locomotora.demo.ai.youtube.YouTubeClient;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Proceso diario de "Videos del catalogo": busca por adelantado el video de cada ejercicio (y sus consejos),
 * por prioridad, usando la cuota diaria de YouTube (~100 busquedas, ~75 ejercicios) sin pasarse; al dia
 * siguiente sigue donde quedo. Tambien refresca los videos guardados cada 7 dias. Sin claves de YouTube y de
 * la IA no hace nada.
 */
@Component
public class CatalogVideoJob {
    private static final Logger log = LoggerFactory.getLogger(CatalogVideoJob.class);
    /** Un ejercicio puede necesitar 2 busquedas (español e ingles). */
    static final int SEARCHES_PER_EXERCISE = 2;
    static final int RETRY_NOT_FOUND_DAYS = 30;
    static final int REFRESH_DAYS = 7;
    static final int REFRESH_BATCH = 100;

    private final ExerciseGuideService guides;
    private final ExerciseGuideRepository guideRepository;
    private final AiRepository aiRepository;
    private final AiModelClient client;
    private final YouTubeClient youtube;
    private final AiSettingsService settings;
    private final boolean enabled;
    private final long dailyTokenCap;
    private final Duration pause;
    private final Duration rateLimitWait;
    /** Esperas seguidas por limite de la IA antes de dejarlo para la proxima vuelta. */
    static final int MAX_RATE_LIMIT_WAITS = 3;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CatalogVideoJob(
            ExerciseGuideService guides,
            ExerciseGuideRepository guideRepository,
            AiRepository aiRepository,
            AiModelClient client,
            YouTubeClient youtube,
            AiSettingsService settings,
            @Value("${locomotora.youtube.fill.enabled:true}") boolean enabled,
            @Value("${locomotora.youtube.fill.daily-token-cap:100000}") long dailyTokenCap,
            // Groq gratis limita los tokens por minuto: ~1.200 por ejercicio, uno cada 20 s.
            @Value("${locomotora.youtube.fill.pause:PT20S}") Duration pause,
            @Value("${locomotora.youtube.fill.rate-limit-wait:PT65S}") Duration rateLimitWait
    ) {
        this.guides = guides;
        this.guideRepository = guideRepository;
        this.aiRepository = aiRepository;
        this.client = client;
        this.youtube = youtube;
        this.settings = settings;
        this.enabled = enabled;
        this.dailyTokenCap = dailyTokenCap;
        this.pause = pause;
        this.rateLimitWait = rateLimitWait;
    }

    /** Cada 30 minutos: si queda cuota hoy, sigue llenando. */
    @Scheduled(initialDelayString = "${locomotora.youtube.fill.initial-delay:PT1M}",
            fixedDelayString = "${locomotora.youtube.fill.every:PT30M}")
    public void fillScheduled() {
        if (enabled) {
            fill();
        }
    }

    /** Cada 6 horas refresca los videos guardados hace mas de 7 dias. */
    @Scheduled(initialDelayString = "${locomotora.youtube.refresh.initial-delay:PT3M}",
            fixedDelayString = "${locomotora.youtube.refresh.every:PT6H}")
    public void refreshScheduled() {
        if (enabled) {
            refresh();
        }
    }

    /** Llena ejercicios hasta agotar la cuota de busquedas o el tope de tokens del dia. Devuelve cuantos proceso. */
    public int fill() {
        if (!youtube.isConfigured() || !client.isConfigured() || !running.compareAndSet(false, true)) {
            return 0;
        }
        int done = 0;
        int waits = 0;
        try {
            while (canContinue()) {
                List<UUID> next = guideRepository.nextToFill(5, RETRY_NOT_FOUND_DAYS);
                if (next.isEmpty()) {
                    break;
                }
                for (UUID exerciseId : next) {
                    if (!canContinue()) {
                        break;
                    }
                    try {
                        guides.fill(exerciseId);
                        done++;
                        waits = 0;
                    } catch (AiUnavailableException unavailable) {
                        if (unavailable.rateLimited() && waits < MAX_RATE_LIMIT_WAITS) {
                            // Limite por minuto de la IA: se espera y se sigue (el ejercicio vuelve a la cola).
                            waits++;
                            sleep(rateLimitWait);
                            break;
                        }
                        // La IA esta caida o sigue saturada: se sigue en la proxima vuelta.
                        log.info("Catalog video fill paused: AI unavailable");
                        return done;
                    }
                    sleep(pause);
                }
            }
            return done;
        } catch (RuntimeException failure) {
            log.warn("Catalog video fill stopped: {}", failure.getClass().getSimpleName());
            return done;
        } finally {
            if (done > 0) {
                log.info("Catalog video fill: {} exercises processed, {} searches left today", done, guides.searchesLeft());
            }
            running.set(false);
        }
    }

    /** Refresca por tandas los videos con mas de 7 dias sin verificar. */
    public int refresh() {
        if (!youtube.isConfigured()) {
            return 0;
        }
        int total = 0;
        try {
            List<UUID> due;
            while (!(due = guideRepository.dueForRefresh(REFRESH_DAYS, REFRESH_BATCH)).isEmpty()) {
                total += guides.refresh(due);
                if (due.size() < REFRESH_BATCH) {
                    break;
                }
            }
        } catch (RuntimeException failure) {
            log.warn("Catalog video refresh stopped: {}", failure.getClass().getSimpleName());
        }
        return total;
    }

    private boolean canContinue() {
        return guides.searchesLeft() >= SEARCHES_PER_EXERCISE
                && aiRepository.sumSystemTokensLastDay(ExerciseGuideService.SYSTEM_FEATURE) < dailyTokenCap
                && !settings.globalBudgetExhausted();
    }

    private void sleep(Duration duration) {
        if (duration.isZero()) {
            return;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
