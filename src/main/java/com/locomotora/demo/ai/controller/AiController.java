package com.locomotora.demo.ai.controller;

import com.locomotora.demo.ai.dto.AiModelsResponse;
import com.locomotora.demo.ai.dto.AiPreferencesRequest;
import com.locomotora.demo.ai.dto.ChatRequest;
import com.locomotora.demo.ai.dto.ChatResponse;
import com.locomotora.demo.ai.dto.ConversationResponse;
import com.locomotora.demo.ai.dto.GeneratedRoutineResponse;
import com.locomotora.demo.ai.dto.MessageResponse;
import com.locomotora.demo.ai.dto.RecommendationResponse;
import com.locomotora.demo.ai.dto.RoutineGenerationRequest;
import com.locomotora.demo.ai.service.AiConversationService;
import com.locomotora.demo.ai.service.AiService;
import com.locomotora.demo.ai.service.AiSettingsService;
import com.locomotora.demo.ai.service.RoutineGenerationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {
    private final AiService aiService;
    private final AiConversationService aiConversationService;
    private final AiSettingsService aiSettingsService;
    private final RoutineGenerationService routineGenerationService;

    public AiController(
            AiService aiService,
            AiConversationService aiConversationService,
            AiSettingsService aiSettingsService,
            RoutineGenerationService routineGenerationService
    ) {
        this.aiService = aiService;
        this.aiConversationService = aiConversationService;
        this.aiSettingsService = aiSettingsService;
        this.routineGenerationService = routineGenerationService;
    }

    /** Modelos de Claude disponibles y el que usa el usuario para el coach y para rutinas. */
    @GetMapping("/ai/models")
    public AiModelsResponse models() {
        return aiSettingsService.models();
    }

    @PutMapping("/ai/preferences")
    public AiModelsResponse updatePreferences(@RequestBody AiPreferencesRequest request) {
        return aiSettingsService.updatePreferences(request);
    }

    /** Borrador de rutina generado con Claude. No se guarda; 503 si la IA no esta configurada. */
    @PostMapping("/routines/generate")
    public GeneratedRoutineResponse generateRoutine(@Valid @RequestBody RoutineGenerationRequest request) {
        return routineGenerationService.generate(request);
    }

    @GetMapping("/ai/conversations")
    public List<ConversationResponse> conversations() {
        return aiConversationService.conversations();
    }

    @GetMapping("/ai/conversations/{id}/messages")
    public List<MessageResponse> messages(@PathVariable UUID id) {
        return aiConversationService.messages(id);
    }

    @PostMapping("/ai/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return aiService.chat(request);
    }

    @GetMapping("/ai/recommendations")
    public List<RecommendationResponse> recommendations() {
        return aiService.recommendations();
    }
}
