package com.locomotora.demo.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.locomotora.demo.ai.service.SafetyGuard.Risk;
import com.locomotora.demo.ai.tools.ExerciseCatalogFilter;
import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Precauciones que salen del perfil del onboarding (edad, chequeo de salud, lesiones). */
class ProfileSafetyTest {

    private static UserContext context(Integer age, List<String> healthFlags, List<String> limitations) {
        return new UserContext(
                new UserContext.Profile(age, "FEMALE", 165.0, 60.0, "LIGHT", "BEGINNER", 3, 45, null, "HOME_EQUIPPED", healthFlags),
                null, List.of("BUILD_MUSCLE"), limitations, List.of("DUMBBELLS"), List.of(), List.of(), 0, List.of());
    }

    @Test
    void profileRisksComeFromAgeFlagsAndInjuries() {
        assertEquals(Set.of(), SafetyGuard.fromProfile(30, List.of(), false));
        assertEquals(Set.of(), SafetyGuard.fromProfile(null, null, false));
        assertEquals(Set.of(Risk.MINOR), SafetyGuard.fromProfile(16, List.of(), false));
        assertEquals(Set.of(Risk.CARDIO), SafetyGuard.fromProfile(40, List.of("HIGH_BLOOD_PRESSURE"), false));
        assertEquals(Set.of(Risk.PREGNANCY, Risk.INJURY), SafetyGuard.fromProfile(32, List.of("PREGNANCY"), true));
    }

    @Test
    void promptGetsPrecautionsButNotRawHealthCodes() {
        String prompt = context(16, List.of("CHEST_PAIN"), List.of("KNEE", "OTHER: menisco")).coachPrompt();
        assertTrue(prompt.contains("Precauciones del perfil"), prompt);
        assertTrue(prompt.contains("menor de edad"), prompt);
        assertTrue(prompt.contains("RPE 7"), prompt);
        assertTrue(prompt.contains("Lesiones o limitaciones: rodilla, menisco"), prompt);
        assertTrue(prompt.contains("Lugar de entrenamiento: en casa con algo de equipo"), prompt);
        assertFalse(prompt.contains("CHEST_PAIN"), prompt);
        assertFalse(prompt.contains("KNEE"), prompt);
    }

    @Test
    void healthyAdultHasNoPrecautions() {
        assertFalse(context(30, List.of(), List.of()).routinePrompt().contains("Precauciones"));
    }

    @Test
    void injuredZonesSkipFreeNotes() {
        assertEquals(Set.of("KNEE", "SHOULDER"), context(30, List.of(), List.of("KNEE", "SHOULDER: operado", "OTHER: psoas")).injuredZones());
    }

    @Test
    void catalogDropsExercisesThatLoadInjuredZones() {
        List<ExerciseCatalogItemResponse> catalog = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            catalog.add(exercise("Seguro " + i, List.of()));
        }
        catalog.add(exercise("Sentadilla trasera", List.of("KNEE", "LOWER_BACK")));
        catalog.add(exercise("Press militar", List.of("SHOULDER")));

        ExerciseCatalogFilter.Result knee = ExerciseCatalogFilter.withoutStress(catalog, Set.of("KNEE"));
        assertTrue(knee.filtered());
        assertEquals(15, knee.exercises().size());
        assertTrue(knee.exercises().stream().noneMatch(e -> e.name().equals("Sentadilla trasera")));

        assertFalse(ExerciseCatalogFilter.withoutStress(catalog, Set.of()).filtered());
        // Si casi todo carga la zona, no se filtra (el prompt igual pide cuidarla).
        List<ExerciseCatalogItemResponse> legs = List.of(exercise("A", List.of("KNEE")), exercise("B", List.of()));
        assertFalse(ExerciseCatalogFilter.withoutStress(legs, Set.of("KNEE")).filtered());
    }

    private static ExerciseCatalogItemResponse exercise(String name, List<String> zones) {
        return new ExerciseCatalogItemResponse(name, name, null, "Piernas", "BODYWEIGHT", null, null, null, false, zones);
    }
}
