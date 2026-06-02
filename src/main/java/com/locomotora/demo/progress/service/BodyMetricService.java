package com.locomotora.demo.progress.service;

import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.progress.dto.BodyMetricRequest;
import com.locomotora.demo.progress.dto.BodyMetricResponse;
import com.locomotora.demo.progress.repository.BodyMetricRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BodyMetricService {
    private final BodyMetricRepository bodyMetricRepository;

    public BodyMetricService(BodyMetricRepository bodyMetricRepository) {
        this.bodyMetricRepository = bodyMetricRepository;
    }

    public List<BodyMetricResponse> findRecentMetrics() {
        return bodyMetricRepository.findRecentByUserId(CurrentUser.id());
    }

    public BodyMetricResponse addMetric(BodyMetricRequest request) {
        UUID userId = CurrentUser.id();
        UUID id = bodyMetricRepository.create(userId, request);
        return bodyMetricRepository.findByIdAndUserId(id, userId).orElseThrow();
    }
}
