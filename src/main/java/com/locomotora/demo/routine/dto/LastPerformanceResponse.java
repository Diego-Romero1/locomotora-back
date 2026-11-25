package com.locomotora.demo.routine.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo ultimo registrado de un ejercicio y la mejor serie historica, para prellenar
 * la sesion y avisar cuando una serie seria record.
 */
public record LastPerformanceResponse(
        String exerciseId,
        LastSession lastSession,
        BestSet bestSet,
        Energy energy
) {
    /**
     * Para estimar las kcal mientras se entrena con la misma formula del servidor: clase del ejercicio y mejor
     * fuerza estimada de los ultimos 90 dias (null si no hay).
     */
    public record Energy(String energyClass, BigDecimal bestE1rmKg) {
    }

    public record LastSession(String sessionId, String completedAt, List<SetValue> sets) {
    }

    public record SetValue(int setNumber, Integer reps, BigDecimal weightKg) {
    }

    /** Mejor serie por fuerza estimada (Epley; con 1 rep es el peso levantado). */
    public record BestSet(Integer reps, BigDecimal weightKg, BigDecimal estimatedOneRepMaxKg, String completedAt) {
    }
}
