package com.locomotora.demo.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.JsonCodes;
import com.locomotora.demo.user.dto.OnboardingRequest;
import com.locomotora.demo.user.dto.OnboardingRequest.LimitationItem;
import com.locomotora.demo.user.dto.ProfilePatchRequest;
import com.locomotora.demo.user.repository.ProfileRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class OnboardingRulesTest {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    /** Fecha de nacimiento para una edad dada (cumpleanos ya pasado este ano). */
    private static LocalDate bornYearsAgo(int years) {
        return LocalDate.now().minusYears(years).minusDays(10);
    }

    private static OnboardingRequest request(String goal, List<String> equipment, List<LimitationItem> limitations,
            Integer age, List<String> healthFlags) {
        return new OnboardingRequest(goal, "INTERMEDIATE", 4, 60, new BigDecimal("178"), new BigDecimal("80"),
                null, equipment, limitations, age == null ? null : bornYearsAgo(age), "FEMALE", "MODERATE",
                "FULL_GYM", healthFlags);
    }

    private static OnboardingRequest valid() {
        return request("BUILD_MUSCLE", List.of("BODYWEIGHT", "dumbbells"),
                List.of(new LimitationItem("KNEE", null), new LimitationItem("OTHER", "menisco")), 30, List.of());
    }

    @Test
    void validRequestPasses() {
        assertEquals(0, VALIDATOR.validate(valid()).size());
        OnboardingRules.validate(valid());
    }

    @Test
    void requiredFieldsAndRangesAreChecked() {
        assertEquals(1, VALIDATOR.validate(request("BUILD_MUSCLE", List.of(), List.of(), 30, List.of())).size());
        assertEquals(1, VALIDATOR.validate(request("BUILD_MUSCLE", List.of("BODYWEIGHT"), List.of(), 30, null)).size());
        OnboardingRequest tiny = new OnboardingRequest("BUILD_MUSCLE", "BEGINNER", 3, 45, new BigDecimal("17"),
                new BigDecimal("80"), null, List.of("BODYWEIGHT"), List.of(), bornYearsAgo(30), null, null, null, List.of());
        assertEquals(1, VALIDATOR.validate(tiny).size());
    }

    @Test
    void unknownCodesAreRejected() {
        assertThrows(ApiException.class, () -> OnboardingRules.validate(
                request("GET_HUGE", List.of("BODYWEIGHT"), List.of(), 30, List.of())));
        assertThrows(ApiException.class, () -> OnboardingRules.validate(
                request("BUILD_MUSCLE", List.of("JETPACK"), List.of(), 30, List.of())));
        assertThrows(ApiException.class, () -> OnboardingRules.validate(
                request("BUILD_MUSCLE", List.of("BODYWEIGHT"), List.of(new LimitationItem("SPLEEN", null)), 30, List.of())));
        assertThrows(ApiException.class, () -> OnboardingRules.validate(
                request("BUILD_MUSCLE", List.of("BODYWEIGHT"), List.of(), 30, List.of("ALIENS"))));
    }

    @Test
    void otherLimitationNeedsNotes() {
        assertThrows(ApiException.class, () -> OnboardingRules.validate(
                request("BUILD_MUSCLE", List.of("BODYWEIGHT"), List.of(new LimitationItem("OTHER", " ")), 30, List.of())));
    }

    @Test
    void goalAliasesAreAccepted() {
        OnboardingRules.validate(request("hypertrophy", List.of("BODYWEIGHT"), List.of(), 30, List.of()));
    }

    @Test
    void ageIsCalculatedFromBirthDate() {
        LocalDate today = LocalDate.of(2026, 11, 16);
        assertEquals(36, OnboardingRules.ageOf(LocalDate.of(1990, 3, 2), today));
        assertEquals(35, OnboardingRules.ageOf(LocalDate.of(1990, 12, 1), today));
        assertNull(OnboardingRules.ageOf(null, today));
    }

    @Test
    void birthDateMustGiveAnAgeBetween13And100() {
        LocalDate today = LocalDate.of(2026, 11, 16);
        OnboardingRules.validateBirthDate(LocalDate.of(2013, 11, 16), today);
        OnboardingRules.validateBirthDate(LocalDate.of(1926, 11, 17), today);
        assertThrows(ApiException.class, () -> OnboardingRules.validateBirthDate(LocalDate.of(2013, 11, 17), today));
        assertThrows(ApiException.class, () -> OnboardingRules.validateBirthDate(LocalDate.of(1920, 1, 1), today));
        assertThrows(ApiException.class, () -> OnboardingRules.validateBirthDate(LocalDate.of(2030, 1, 1), today));
        assertThrows(ApiException.class, () -> OnboardingRules.validate(
                request("BUILD_MUSCLE", List.of("BODYWEIGHT"), List.of(), 12, List.of())));
        assertEquals(1, VALIDATOR.validate(request("BUILD_MUSCLE", List.of("BODYWEIGHT"), List.of(), null, List.of())).size());
    }

    @Test
    void codesAreNormalized() {
        assertEquals(List.of("BARBELL", "MACHINES"), OnboardingRules.normalizedCodes(List.of(" barbell", "BARBELL", "machines", "")));
    }

    private static ProfilePatchRequest patch(String goal, String sex, List<String> equipment, String style) {
        return new ProfilePatchRequest(null, goal, null, null, null, null, null, style, equipment, null, null, sex,
                null, null, null, null, null);
    }

    @Test
    void patchOnlyChecksWhatCame() {
        OnboardingRules.validate(patch(null, null, null, null));
        OnboardingRules.validate(patch("IMPROVE_STRENGTH", "male", List.of("MACHINES"), ""));
        assertThrows(ApiException.class, () -> OnboardingRules.validate(patch("GET_HUGE", null, null, null)));
        assertThrows(ApiException.class, () -> OnboardingRules.validate(patch(null, "ROBOT", null, null)));
        assertThrows(ApiException.class, () -> OnboardingRules.validate(patch(null, null, List.of("mancuernas"), null)));
        assertThrows(ApiException.class, () -> OnboardingRules.validate(patch(null, null, null, "CROSSFIT")));
        assertEquals(1, VALIDATOR.validate(patch(null, null, List.of(), null)).size());
    }

    @Test
    void profileIsCompleteWhenRequiredDataIsThere() {
        ProfileRepository.OnboardingProfileSnapshot full = new ProfileRepository.OnboardingProfileSnapshot(
                new BigDecimal("178"), new BigDecimal("80"), "BEGINNER", 3, 45, false, LocalDate.of(1990, 1, 1),
                null, null, null, List.of(), true, List.of(), null);
        assertTrue(OnboardingRules.isComplete(full, true, true));
        assertFalse(OnboardingRules.isComplete(full, false, true));
        assertFalse(OnboardingRules.isComplete(full, true, false));
        ProfileRepository.OnboardingProfileSnapshot noHealth = new ProfileRepository.OnboardingProfileSnapshot(
                new BigDecimal("178"), new BigDecimal("80"), "BEGINNER", 3, 45, false, LocalDate.of(1990, 1, 1),
                null, null, null, null, true, List.of(), null);
        assertFalse(OnboardingRules.isComplete(noHealth, true, true));
    }

    @Test
    void photoMustBeAnImageNotALocalPath() {
        OnboardingRules.validatePhoto("data:image/jpeg;base64,/9j/4AAQ");
        OnboardingRules.validatePhoto("https://cdn.example.com/a.jpg");
        assertThrows(ApiException.class, () -> OnboardingRules.validatePhoto("file:///data/user/0/cache/x.jpg"));
        assertThrows(ApiException.class, () -> OnboardingRules.validatePhoto("data:text/html;base64,PGgxPg=="));
    }

    @Test
    void jsonCodesRoundTrip() {
        assertEquals("[\"CHEST_PAIN\",\"PREGNANCY\"]", JsonCodes.toArray(List.of("CHEST_PAIN", "PREGNANCY")));
        assertEquals(List.of("CHEST_PAIN", "PREGNANCY"), JsonCodes.parse("[\"CHEST_PAIN\", \"PREGNANCY\"]"));
        assertEquals(List.of(), JsonCodes.parse("[]"));
        assertNull(JsonCodes.parse(null));
    }
}
