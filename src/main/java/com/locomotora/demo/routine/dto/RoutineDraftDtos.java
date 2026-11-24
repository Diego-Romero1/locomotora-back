package com.locomotora.demo.routine.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** Borradores de rutinas: la app manda el borrador tal cual (payload) y un resumen para listarlo. */
public final class RoutineDraftDtos {
    private RoutineDraftDtos() {
    }

    /**
     * {@code kind}: NEW (rutina nueva) o EDIT (cambios sin guardar de {@code routineId}).
     * {@code updatedAt}: hora del cambio en el telefono (ISO-8601); gana el borrador mas nuevo.
     */
    public record RoutineDraftRequest(
            String kind,
            String routineId,
            String title,
            Integer step,
            Integer dayCount,
            Integer exerciseCount,
            JsonNode payload,
            String updatedAt
    ) {
    }

    public record RoutineDraftResponse(
            String id,
            String kind,
            String routineId,
            String title,
            int step,
            int dayCount,
            int exerciseCount,
            JsonNode payload,
            String updatedAt
    ) {
    }
}
