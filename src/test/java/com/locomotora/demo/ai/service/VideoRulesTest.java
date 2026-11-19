package com.locomotora.demo.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.locomotora.demo.ai.youtube.YouTubeClient.Video;
import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class VideoRulesTest {
    private static ExerciseCatalogItemResponse exercise(String name, String nameEn, String equipment) {
        return new ExerciseCatalogItemResponse("id", name, null, "Pecho", equipment, null, null, null, false, List.of(),
                "Pecho", List.of(), nameEn, "STRENGTH", "COMPOUND", true, null, List.of(), null, null, null);
    }

    private static boolean ok(ExerciseCatalogItemResponse exercise, String title) {
        return VideoRules.acceptable(exercise, new Video("v", title, "Canal", "", 60, 0));
    }

    private final ExerciseCatalogItemResponse bench = exercise("Press de banca plano", "Barbell Bench Press - Medium Grip", "BARBELL");

    @Test
    void rejectsCompilationsAndComparisons() {
        assertThat(ok(bench, "Press de banca: técnica correcta")).isTrue();
        assertThat(ok(bench, "TOP 5 ejercicios de pecho")).isFalse();
        assertThat(ok(bench, "Rutina de pecho completa")).isFalse();
        assertThat(ok(bench, "Press banca vs flexiones")).isFalse();
    }

    @Test
    void rejectsOtherEquipmentUnlessTheExerciseNamesIt() {
        assertThat(ok(bench, "Press de banca con mancuernas")).isFalse();
        assertThat(ok(bench, "Bench press on Smith machine")).isFalse();
        ExerciseCatalogItemResponse cableCurl = exercise("Curl en polea con barra", "Cable Curl", "CABLE");
        assertThat(ok(cableCurl, "Curl de bíceps en polea con barra")).isTrue();
        ExerciseCatalogItemResponse dumbbell = exercise("Press con mancuernas plano", "Dumbbell Bench Press", "DUMBBELL");
        assertThat(ok(dumbbell, "Press plano con barra")).isFalse();
        assertThat(ok(dumbbell, "Dumbbell bench press form")).isTrue();
    }

    @Test
    void inclineAndDeclineOnlyMatterForPressesFliesAndCurls() {
        assertThat(ok(bench, "Press de banca inclinado")).isFalse();
        assertThat(ok(bench, "Decline bench press")).isFalse();
        ExerciseCatalogItemResponse incline = exercise("Press de banca inclinado", "Barbell Incline Bench Press", "BARBELL");
        assertThat(ok(incline, "Press inclinado con barra: técnica")).isTrue();
        assertThat(ok(incline, "Press declinado")).isFalse();
        ExerciseCatalogItemResponse row = exercise("Remo con barra", "Bent Over Barbell Row", "BARBELL");
        assertThat(ok(row, "Remo inclinado con barra: técnica")).isTrue();
    }

    @Test
    void oneSideOnlyWhenTheExerciseIsOneSided() {
        assertThat(ok(bench, "Press de banca a una mano")).isFalse();
        ExerciseCatalogItemResponse oneArm = exercise("Remo a una mano con mancuerna", "One-Arm Dumbbell Row", "DUMBBELL");
        assertThat(ok(oneArm, "One arm dumbbell row proper form")).isTrue();
    }
}
