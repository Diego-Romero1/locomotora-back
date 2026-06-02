package com.locomotora.demo.user.service;

import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.user.dto.GoalRequest;
import com.locomotora.demo.user.dto.GoalResponse;
import com.locomotora.demo.user.repository.GoalRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {
    private final GoalRepository goalRepository;

    public GoalService(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    public List<GoalResponse> goals() {
        return goalRepository.findByUserId(CurrentUser.id());
    }

    @Transactional
    public List<GoalResponse> replaceGoals(List<GoalRequest> goals) {
        UUID userId = CurrentUser.id();
        goalRepository.deleteByUserId(userId);
        for (GoalRequest goal : goals) {
            goalRepository.insert(userId, goal);
        }
        return goalRepository.findByUserId(userId);
    }
}
