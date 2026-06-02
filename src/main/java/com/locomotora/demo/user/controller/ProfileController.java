package com.locomotora.demo.user.controller;

import com.locomotora.demo.user.dto.GoalRequest;
import com.locomotora.demo.user.dto.GoalResponse;
import com.locomotora.demo.user.dto.MeResponse;
import com.locomotora.demo.user.dto.OnboardingRequest;
import com.locomotora.demo.user.dto.OnboardingResponse;
import com.locomotora.demo.user.dto.PhotoRequest;
import com.locomotora.demo.user.dto.PhotoResponse;
import com.locomotora.demo.user.dto.ProfileRequest;
import com.locomotora.demo.user.dto.ProfileResponse;
import com.locomotora.demo.user.service.GoalService;
import com.locomotora.demo.user.service.ProfileService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController {
    private final ProfileService profileService;
    private final GoalService goalService;

    public ProfileController(ProfileService profileService, GoalService goalService) {
        this.profileService = profileService;
        this.goalService = goalService;
    }

    @GetMapping("/me")
    public MeResponse me() {
        return profileService.me();
    }

    @GetMapping("/profile")
    public ProfileResponse profile() {
        return profileService.profile();
    }

    @PutMapping("/profile")
    public ProfileResponse updateProfile(@Valid @RequestBody ProfileRequest request) {
        return profileService.updateProfile(request);
    }

    @PutMapping("/profile/photo")
    public PhotoResponse updateProfilePhoto(@Valid @RequestBody PhotoRequest request) {
        return profileService.updateProfilePhoto(request);
    }

    @DeleteMapping("/profile/photo")
    public PhotoResponse deleteProfilePhoto() {
        return profileService.deleteProfilePhoto();
    }

    @GetMapping("/profile/onboarding")
    public OnboardingResponse onboarding() {
        return profileService.onboarding();
    }

    @PutMapping("/profile/onboarding")
    public OnboardingResponse updateOnboarding(@Valid @RequestBody OnboardingRequest request) {
        return profileService.updateOnboarding(request);
    }

    @GetMapping("/goals")
    public List<GoalResponse> goals() {
        return goalService.goals();
    }

    @PutMapping("/goals")
    public List<GoalResponse> replaceGoals(@RequestBody List<GoalRequest> goals) {
        return goalService.replaceGoals(goals);
    }
}
