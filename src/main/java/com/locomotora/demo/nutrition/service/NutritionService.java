package com.locomotora.demo.nutrition.service;

import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.nutrition.dto.NutritionEntryRequest;
import com.locomotora.demo.nutrition.dto.NutritionEntryResponse;
import com.locomotora.demo.nutrition.dto.NutritionSummaryResponse;
import com.locomotora.demo.nutrition.repository.NutritionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NutritionService {
    private final NutritionRepository nutritionRepository;

    public NutritionService(NutritionRepository nutritionRepository) {
        this.nutritionRepository = nutritionRepository;
    }

    public List<NutritionEntryResponse> entries(LocalDate date) {
        UUID userId = CurrentUser.id();
        if (date != null) {
            return nutritionRepository.findByUserIdAndDate(userId, date);
        }
        return nutritionRepository.findRecentByUserId(userId);
    }

    public NutritionEntryResponse create(NutritionEntryRequest request) {
        UUID userId = CurrentUser.id();
        LocalDate entryDate = request.entryDate() == null ? LocalDate.now() : request.entryDate();
        UUID id = nutritionRepository.create(userId, request, entryDate);
        return nutritionRepository.findByIdAndUserId(id, userId).orElseThrow();
    }

    public NutritionSummaryResponse summary(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        return nutritionRepository.summary(CurrentUser.id(), targetDate);
    }
}
