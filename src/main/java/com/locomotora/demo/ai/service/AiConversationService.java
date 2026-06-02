package com.locomotora.demo.ai.service;

import com.locomotora.demo.ai.dto.ConversationResponse;
import com.locomotora.demo.ai.dto.MessageResponse;
import com.locomotora.demo.ai.repository.AiRepository;
import com.locomotora.demo.common.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiConversationService {
    private final AiRepository aiRepository;

    public AiConversationService(AiRepository aiRepository) {
        this.aiRepository = aiRepository;
    }

    public List<ConversationResponse> conversations() {
        return aiRepository.findConversations(CurrentUser.id());
    }

    public List<MessageResponse> messages(UUID conversationId) {
        return aiRepository.findMessages(conversationId, CurrentUser.id());
    }
}
