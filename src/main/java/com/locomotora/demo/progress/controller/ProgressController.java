package com.locomotora.demo.progress.controller;

import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.progress.dto.BodyMetricRequest;
import com.locomotora.demo.progress.dto.BodyMetricResponse;
import com.locomotora.demo.progress.dto.WorkoutActivityResponse;
import com.locomotora.demo.progress.dto.ProgressSummaryResponse;
import com.locomotora.demo.progress.service.BodyMetricService;
import com.locomotora.demo.progress.energy.WorkoutEnergyService;
import com.locomotora.demo.progress.service.ProgressService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProgressController {
    private final ProgressService progressService;
    private final BodyMetricService bodyMetricService;
    private final WorkoutEnergyService energyService;

    public ProgressController(
            ProgressService progressService, BodyMetricService bodyMetricService, WorkoutEnergyService energyService) {
        this.progressService = progressService;
        this.bodyMetricService = bodyMetricService;
        this.energyService = energyService;
    }

    /** Datos del perfil que usa el calculo de kcal, para estimarlas en el telefono mientras se entrena. */
    @GetMapping("/progress/energy-profile")
    public WorkoutEnergyService.EnergyProfile energyProfile() {
        return energyService.currentProfile(CurrentUser.id(), Instant.now());
    }

    @GetMapping("/progress/summary")
    public ProgressSummaryResponse summary() {
        return progressService.summary();
    }

    @GetMapping("/progress/activity")
    public List<WorkoutActivityResponse> recentActivity() {
        return progressService.recentActivity();
    }

    @GetMapping("/metrics")
    public List<BodyMetricResponse> metrics() {
        return bodyMetricService.findRecentMetrics();
    }

    @PostMapping("/metrics")
    public BodyMetricResponse addMetric(@Valid @RequestBody BodyMetricRequest request) {
        return bodyMetricService.addMetric(request);
    }
}
