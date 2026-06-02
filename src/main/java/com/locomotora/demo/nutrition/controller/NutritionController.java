package com.locomotora.demo.nutrition.controller;

import com.locomotora.demo.nutrition.dto.NutritionEntryRequest;
import com.locomotora.demo.nutrition.dto.NutritionEntryResponse;
import com.locomotora.demo.nutrition.dto.NutritionSummaryResponse;
import com.locomotora.demo.nutrition.service.NutritionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NutritionController {
    private final NutritionService nutritionService;

    public NutritionController(NutritionService nutritionService) {
        this.nutritionService = nutritionService;
    }

    @GetMapping("/nutrition")
    public List<NutritionEntryResponse> entries(@RequestParam(required = false) LocalDate date) {
        return nutritionService.entries(date);
    }

    @PostMapping("/nutrition")
    public NutritionEntryResponse create(@Valid @RequestBody NutritionEntryRequest request) {
        return nutritionService.create(request);
    }

    @GetMapping("/nutrition/summary")
    public NutritionSummaryResponse summary(@RequestParam(required = false) LocalDate date) {
        return nutritionService.summary(date);
    }
}
