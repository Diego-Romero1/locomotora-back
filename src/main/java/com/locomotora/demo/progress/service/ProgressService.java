package com.locomotora.demo.progress.service;

import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.progress.dto.BodyMetricResponse;
import com.locomotora.demo.progress.dto.ProgressSummaryResponse;
import com.locomotora.demo.progress.repository.BodyMetricRepository;
import com.locomotora.demo.progress.repository.ProgressRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProgressService {
    private final ProgressRepository progressRepository;
    private final BodyMetricRepository bodyMetricRepository;

    public ProgressService(ProgressRepository progressRepository, BodyMetricRepository bodyMetricRepository) {
        this.progressRepository = progressRepository;
        this.bodyMetricRepository = bodyMetricRepository;
    }

    public ProgressSummaryResponse summary() {
        UUID userId = CurrentUser.id();
        BodyMetricResponse latestMetric = bodyMetricRepository.findLatestByUserId(userId).orElse(null);
        return new ProgressSummaryResponse(
                progressRepository.countCompletedWorkouts(userId),
                progressRepository.countActiveRoutines(userId),
                progressRepository.countActiveDaysLast30(userId),
                latestMetric
        );
    }
}
