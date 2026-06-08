package com.locomotora.demo.progress.controller;

import com.locomotora.demo.progress.dto.BodyMetricRequest;
import com.locomotora.demo.progress.dto.BodyMetricResponse;
import com.locomotora.demo.progress.dto.ExerciseActivityResponse;
import com.locomotora.demo.progress.dto.ProgressSummaryResponse;
import com.locomotora.demo.progress.service.BodyMetricService;
import com.locomotora.demo.progress.service.ProgressService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProgressController {
    private final ProgressService progressService;
    private final BodyMetricService bodyMetricService;

    public ProgressController(ProgressService progressService, BodyMetricService bodyMetricService) {
        this.progressService = progressService;
        this.bodyMetricService = bodyMetricService;
    }

    @GetMapping("/progress/summary")
    public ProgressSummaryResponse summary() {
        return progressService.summary();
    }

    @GetMapping("/progress/activity")
    public List<ExerciseActivityResponse> recentActivity() {
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
