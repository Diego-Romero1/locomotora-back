package com.locomotora.demo.routine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.routine.dto.RoutineDraftDtos.RoutineDraftRequest;
import com.locomotora.demo.routine.dto.RoutineDraftDtos.RoutineDraftResponse;
import com.locomotora.demo.routine.repository.RoutineDraftRepository;
import com.locomotora.demo.routine.repository.RoutineDraftRepository.DraftRow;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Borradores de rutinas (crear o editar) guardados en el servidor, para no perderlos si se cambia de
 * telefono o se reinstala la app. La app los guarda antes en el telefono y los sube cuando hay conexion.
 */
@Service
public class RoutineDraftService {
    /** Tope por usuario: los borradores son para retomar, no un segundo lugar para guardar rutinas. */
    static final int MAX_DRAFTS = 30;
    /** Un borrador de 7 dias con 20 ejercicios cada uno ocupa ~60 KB. */
    static final int MAX_PAYLOAD_CHARS = 256 * 1024;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RoutineDraftRepository repository;

    public RoutineDraftService(RoutineDraftRepository repository) {
        this.repository = repository;
    }

    public List<RoutineDraftResponse> list() {
        return repository.findByUser(CurrentUser.id()).stream().map(RoutineDraftService::toResponse).toList();
    }

    /**
     * Guarda el borrador. Si el servidor ya tiene uno mas nuevo (por ejemplo, desde otro telefono), no lo pisa
     * y devuelve el guardado, para que la app se quede con el mas nuevo.
     */
    public RoutineDraftResponse save(UUID id, RoutineDraftRequest request) {
        UUID userId = CurrentUser.id();
        String kind = request.kind() == null ? "" : request.kind().trim().toUpperCase();
        if (!kind.equals("NEW") && !kind.equals("EDIT")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "kind must be NEW or EDIT");
        }
        UUID routineId = parseUuid(request.routineId());
        if (kind.equals("EDIT") && (routineId == null || !repository.isActiveRoutineOf(routineId, userId))) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Routine not found");
        }
        if (request.payload() == null || !request.payload().isObject()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "payload must be a JSON object");
        }
        String payload = request.payload().toString();
        if (payload.length() > MAX_PAYLOAD_CHARS) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Draft too large");
        }
        Instant updatedAt = parseInstant(request.updatedAt());

        var existing = repository.findById(id);
        if (existing.isPresent() && !existing.get().userId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Draft not found");
        }
        if (existing.isEmpty() && repository.countByUser(userId) >= MAX_DRAFTS) {
            throw new ApiException(HttpStatus.CONFLICT, "Too many drafts");
        }

        DraftRow row = new DraftRow(
                id,
                userId,
                kind,
                kind.equals("EDIT") ? routineId : null,
                trimTitle(request.title()),
                clamp(request.step(), 1, 3),
                clamp(request.dayCount(), 0, 7),
                clamp(request.exerciseCount(), 0, 7 * 20),
                payload,
                updatedAt
        );
        repository.upsert(row);
        return repository.findById(id).map(RoutineDraftService::toResponse).orElseThrow();
    }

    public void delete(UUID id) {
        // Borrar uno que no existe (ya borrado desde otro telefono) no es un error: la app reintenta igual.
        repository.delete(id, CurrentUser.id());
    }

    private static RoutineDraftResponse toResponse(DraftRow row) {
        JsonNode payload;
        try {
            payload = JSON.readTree(row.payload());
        } catch (JsonProcessingException exception) {
            payload = JSON.createObjectNode();
        }
        return new RoutineDraftResponse(
                row.id().toString(),
                row.kind(),
                row.routineId() == null ? null : row.routineId().toString(),
                row.title(),
                row.step(),
                row.dayCount(),
                row.exerciseCount(),
                payload,
                row.updatedAt().toString()
        );
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid routineId");
        }
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid updatedAt");
        }
    }

    private static String trimTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        String trimmed = title.trim();
        return trimmed.length() > 180 ? trimmed.substring(0, 180) : trimmed;
    }

    private static int clamp(Integer value, int min, int max) {
        return value == null ? min : Math.max(min, Math.min(max, value));
    }
}
