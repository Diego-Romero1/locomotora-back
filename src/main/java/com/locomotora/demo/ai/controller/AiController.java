package com.locomotora.demo.ai.controller;

import com.locomotora.demo.ai.dto.ChatRequest;
import com.locomotora.demo.ai.dto.ChatResponse;
import com.locomotora.demo.ai.dto.ConversationResponse;
import com.locomotora.demo.ai.dto.MessageResponse;
import com.locomotora.demo.ai.dto.RecommendationResponse;
import com.locomotora.demo.ai.service.AiConversationService;
import com.locomotora.demo.ai.service.AiService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {
    private final AiService aiService;
    private final AiConversationService aiConversationService;

    public AiController(AiService aiService, AiConversationService aiConversationService) {
        this.aiService = aiService;
        this.aiConversationService = aiConversationService;
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
