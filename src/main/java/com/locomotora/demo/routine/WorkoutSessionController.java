package com.locomotora.demo.routine;

import com.locomotora.demo.routine.dto.WorkoutSessionRequest;
import com.locomotora.demo.routine.dto.WorkoutSessionResponse;
import com.locomotora.demo.routine.service.WorkoutSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkoutSessionController {
    private final WorkoutSessionService workoutSessionService;

    public WorkoutSessionController(WorkoutSessionService workoutSessionService) {
        this.workoutSessionService = workoutSessionService;
    }

    /** 201 si la sesion se creo, 200 si ya existia (reintento con el mismo clientSessionId). */
    @PostMapping("/workout-sessions")
    public ResponseEntity<WorkoutSessionResponse> saveSession(@Valid @RequestBody WorkoutSessionRequest request) {
        WorkoutSessionService.Result result = workoutSessionService.saveSession(request);
        return ResponseEntity
                .status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(result.session());
    }
}
