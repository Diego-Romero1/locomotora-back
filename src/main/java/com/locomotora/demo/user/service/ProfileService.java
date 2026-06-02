package com.locomotora.demo.user.service;

import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.user.dto.MeResponse;
import com.locomotora.demo.user.dto.OnboardingRequest;
import com.locomotora.demo.user.dto.OnboardingResponse;
import com.locomotora.demo.user.dto.PhotoRequest;
import com.locomotora.demo.user.dto.PhotoResponse;
import com.locomotora.demo.user.dto.PreferenceResponse;
import com.locomotora.demo.user.dto.ProfileRequest;
import com.locomotora.demo.user.dto.ProfileResponse;
import com.locomotora.demo.user.repository.GoalRepository;
import com.locomotora.demo.user.repository.ProfileRepository;
import com.locomotora.demo.user.repository.ProfileRepository.OnboardingProfileSnapshot;
import com.locomotora.demo.user.repository.ProfileRepository.UserAccount;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final GoalRepository goalRepository;

    public ProfileService(ProfileRepository profileRepository, GoalRepository goalRepository) {
        this.profileRepository = profileRepository;
        this.goalRepository = goalRepository;
    }

    public MeResponse me() {
        UUID userId = CurrentUser.id();
        profileRepository.ensureProfile(userId);
        UserAccount account = profileRepository.findAccount(userId);
        return new MeResponse(
                account.id().toString(),
                account.name(),
                account.email(),
                account.profilePhotoUrl(),
                profileRepository.findProfile(userId),
                goalRepository.findByUserId(userId),
                findPreference(userId),
                profileRepository.findLimitations(userId),
                profileRepository.findEquipment(userId)
        );
    }

    public ProfileResponse profile() {
        UUID userId = CurrentUser.id();
        profileRepository.ensureProfile(userId);
        return profileRepository.findProfile(userId);
    }

    @Transactional
    public ProfileResponse updateProfile(ProfileRequest request) {
        UUID userId = CurrentUser.id();
        profileRepository.ensureProfile(userId);

        String normalizedName = normalizeValue(request.name());
        if (normalizedName != null) {
            profileRepository.updateName(userId, normalizedName);
        }

        profileRepository.updateProfile(userId, request);
        if (request.goalType() != null) {
            replaceGoalsForOnboarding(userId, request.goalType());
        }
        if (request.preferredTrainingStyle() != null) {
            replacePreference(userId, request.preferredTrainingStyle());
        }
        if (request.equipment() != null) {
            replaceEquipment(userId, request.equipment());
        }
        if (request.limitations() != null) {
            replaceLimitations(userId, request.limitations());
        }
        return profileRepository.findProfile(userId);
    }

    public PhotoResponse updateProfilePhoto(PhotoRequest request) {
        UUID userId = CurrentUser.id();
        String profilePhotoUrl = normalizeValue(request.profilePhotoUrl());
        profileRepository.updateProfilePhoto(userId, profilePhotoUrl);
        return new PhotoResponse(profilePhotoUrl);
    }

    public PhotoResponse deleteProfilePhoto() {
        profileRepository.deleteProfilePhoto(CurrentUser.id());
        return new PhotoResponse(null);
    }

    public OnboardingResponse onboarding() {
        UUID userId = CurrentUser.id();
        profileRepository.ensureProfile(userId);
        return buildOnboardingResponse(userId);
    }

    @Transactional
    public OnboardingResponse updateOnboarding(OnboardingRequest request) {
        UUID userId = CurrentUser.id();
        profileRepository.ensureProfile(userId);
        profileRepository.updateOnboardingProfile(userId, request, normalizeValue(request.experienceLevel()));
        replaceGoalsForOnboarding(userId, request.goalType());
        replacePreference(userId, request.preferredTrainingStyle());
        replaceEquipment(userId, request.equipment());
        replaceLimitations(userId, request.limitations());
        return buildOnboardingResponse(userId);
    }

    private OnboardingResponse buildOnboardingResponse(UUID userId) {
        OnboardingProfileSnapshot profile = profileRepository.findOnboardingProfile(userId);
        return new OnboardingResponse(
                goalRepository.findPrimaryGoal(userId),
                profile.experienceLevel(),
                profile.trainingDaysPerWeek(),
                profile.sessionDurationMinutes(),
                profile.heightCm(),
                profile.weightKg(),
                profileRepository.findPreferredTrainingStyle(userId),
                profileRepository.findEquipment(userId),
                profileRepository.findLimitations(userId),
                profile.onboardingCompleted()
        );
    }

    private PreferenceResponse findPreference(UUID userId) {
        String preferredTrainingStyle = profileRepository.findPreferredTrainingStyle(userId);
        return preferredTrainingStyle == null ? null : new PreferenceResponse(preferredTrainingStyle);
    }

    private void replaceGoalsForOnboarding(UUID userId, String goalType) {
        goalRepository.deleteByUserId(userId);
        String normalizedGoalType = normalizeValue(goalType);
        if (StringUtils.hasText(normalizedGoalType)) {
            goalRepository.insertPrimaryActive(userId, normalizedGoalType);
        }
    }

    private void replacePreference(UUID userId, String preferredTrainingStyle) {
        profileRepository.deletePreference(userId);
        String normalizedTrainingStyle = normalizeValue(preferredTrainingStyle);
        if (StringUtils.hasText(normalizedTrainingStyle)) {
            profileRepository.replacePreference(userId, normalizedTrainingStyle);
        }
    }

    private void replaceEquipment(UUID userId, List<String> equipment) {
        profileRepository.deleteEquipment(userId);
        if (equipment == null) {
            return;
        }
        for (String entry : equipment) {
            String normalizedEquipment = normalizeValue(entry);
            if (StringUtils.hasText(normalizedEquipment)) {
                profileRepository.insertEquipment(userId, normalizedEquipment);
            }
        }
    }

    private void replaceLimitations(UUID userId, List<OnboardingRequest.LimitationItem> limitations) {
        profileRepository.deleteLimitations(userId);
        if (limitations == null) {
            return;
        }
        for (OnboardingRequest.LimitationItem limitation : limitations) {
            if (limitation == null) {
                continue;
            }
            String limitationType = normalizeValue(limitation.limitationType());
            if (StringUtils.hasText(limitationType)) {
                profileRepository.insertLimitation(userId, limitationType, normalizeValue(limitation.notes()));
            }
        }
    }

    private String normalizeValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
