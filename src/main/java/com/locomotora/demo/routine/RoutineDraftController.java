package com.locomotora.demo.routine;

import com.locomotora.demo.routine.dto.RoutineDraftDtos.RoutineDraftRequest;
import com.locomotora.demo.routine.dto.RoutineDraftDtos.RoutineDraftResponse;
import com.locomotora.demo.routine.service.RoutineDraftService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Borradores de rutinas: la app los guarda en el telefono y los sube aca cuando hay conexion. */
@RestController
public class RoutineDraftController {
    private final RoutineDraftService draftService;

    public RoutineDraftController(RoutineDraftService draftService) {
        this.draftService = draftService;
    }

    @GetMapping("/routine-drafts")
    public List<RoutineDraftResponse> list() {
        return draftService.list();
    }

    /** Crea o actualiza (id generado en el telefono). Devuelve el borrador que quedo guardado. */
    @PutMapping("/routine-drafts/{id}")
    public RoutineDraftResponse save(@PathVariable UUID id, @RequestBody RoutineDraftRequest request) {
        return draftService.save(id, request);
    }

    @DeleteMapping("/routine-drafts/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        draftService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
