package com.locomotora.demo.user.dto;

import java.util.List;

public record MeResponse(
        String id,
        String name,
        String email,
        String profilePhotoUrl,
        ProfileResponse profile,
        List<GoalResponse> goals,
        PreferenceResponse preferences,
        List<OnboardingResponse.LimitationItem> limitations,
        List<String> equipment
) {
}
