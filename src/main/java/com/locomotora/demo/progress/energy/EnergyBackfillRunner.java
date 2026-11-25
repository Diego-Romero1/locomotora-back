package com.locomotora.demo.progress.energy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Al arrancar calcula las kcal de las sesiones que no las tienen (sesiones viejas o un modelo nuevo). */
@Component
public class EnergyBackfillRunner {
    private static final Logger log = LoggerFactory.getLogger(EnergyBackfillRunner.class);

    private final WorkoutEnergyService service;
    private final boolean enabled;

    public EnergyBackfillRunner(
            WorkoutEnergyService service,
            @Value("${locomotora.energy.backfill-on-startup:true}") boolean enabled) {
        this.service = service;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!enabled) {
            return;
        }
        try {
            service.backfill();
        } catch (RuntimeException failure) {
            // No debe impedir que la app arranque: las sesiones quedan pendientes para el proximo arranque.
            log.warn("Workout energy backfill stopped: {}", failure.toString());
        }
    }
}
