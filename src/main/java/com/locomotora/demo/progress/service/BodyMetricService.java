package com.locomotora.demo.progress.service;

import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.progress.dto.BodyMetricRequest;
import com.locomotora.demo.progress.dto.BodyMetricResponse;
import com.locomotora.demo.progress.energy.WorkoutEnergyService;
import com.locomotora.demo.progress.repository.BodyMetricRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BodyMetricService {
    private final BodyMetricRepository bodyMetricRepository;
    private final WorkoutEnergyService energyService;

    public BodyMetricService(BodyMetricRepository bodyMetricRepository, WorkoutEnergyService energyService) {
        this.bodyMetricRepository = bodyMetricRepository;
        this.energyService = energyService;
    }

    public List<BodyMetricResponse> findRecentMetrics() {
        return bodyMetricRepository.findRecentByUserId(CurrentUser.id());
    }

    @Transactional
    public BodyMetricResponse addMetric(BodyMetricRequest request) {
        UUID userId = CurrentUser.id();
        UUID id = bodyMetricRepository.create(userId, request);
        if (request.weightKg() != null) {
            energyService.refreshIncomplete(userId);
        }
        return bodyMetricRepository.findByIdAndUserId(id, userId).orElseThrow();
    }
}
