package com.locomotora.demo.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import com.locomotora.demo.routine.dto.ExerciseGroups;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExerciseCatalogFilterTest {

    private static ExerciseCatalogItemResponse item(String name, String muscle, String equipment, String type,
            String mechanic, boolean core) {
        return new ExerciseCatalogItemResponse(name, name, null, muscle, equipment, "BEGINNER", null, null, false,
                List.of(), ExerciseGroups.groupOf(muscle, type), List.of(), null, type, mechanic, core, null,
                List.of(), null, null, null);
    }

    @Test
    void promptCatalogKeepsCoreAndDropsStretchingAndCardio() {
        List<ExerciseCatalogItemResponse> catalog = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            catalog.add(item("Core " + index, "Pecho", "BARBELL", "STRENGTH", "COMPOUND", true));
        }
        catalog.add(item("Estiramiento", "Pecho", "BODYWEIGHT", "STRETCHING", null, true));
        catalog.add(item("Cinta", "Cuádriceps", "MACHINE", "CARDIO", null, true));
        catalog.add(item("Extra pecho", "Pecho", "CABLE", "STRENGTH", "ISOLATION", false));

        List<ExerciseCatalogItemResponse> prompt = ExerciseCatalogFilter.forPrompt(catalog, List.of());

        assertThat(prompt).hasSize(20).allMatch(ExerciseCatalogItemResponse::core);
    }

    @Test
    void promptCatalogAddsFocusMusclesAndRespectsTheCap() {
        List<ExerciseCatalogItemResponse> catalog = new ArrayList<>();
        for (int index = 0; index < 150; index++) {
            catalog.add(item("Core " + index, "Pecho", "BARBELL", "STRENGTH", "COMPOUND", true));
        }
        catalog.add(item("Antebrazo compuesto", "Antebrazos", "OTHER", "STRENGTH", "COMPOUND", false));
        catalog.add(item("Antebrazo aislado", "Antebrazos", "DUMBBELL", "STRENGTH", "ISOLATION", false));
        catalog.add(item("Lumbar extra", "Lumbares", "BODYWEIGHT", "STRENGTH", "ISOLATION", false));

        List<ExerciseCatalogItemResponse> noFocus = ExerciseCatalogFilter.forPrompt(catalog, null);
        assertThat(noFocus).hasSize(ExerciseCatalogFilter.MAX_PROMPT_EXERCISES).allMatch(ExerciseCatalogItemResponse::core);

        // Pocos "core": los grupos pedidos entran (compuestos primero); el grupo "Espalda" incluye Lumbares.
        List<ExerciseCatalogItemResponse> small = new ArrayList<>(catalog.subList(0, 15));
        small.addAll(catalog.subList(150, 153));
        List<ExerciseCatalogItemResponse> focused = ExerciseCatalogFilter.forPrompt(small, List.of("Antebrazos", "Espalda"));
        assertThat(focused).extracting(ExerciseCatalogItemResponse::name)
                .contains("Antebrazo compuesto", "Antebrazo aislado", "Lumbar extra");
    }

    @Test
    void userEquipmentMapsToTheNewCatalogCodes() {
        assertThat(ExerciseCatalogFilter.allowedEquipment(List.of("KETTLEBELLS", "RESISTANCE_BANDS", "BARBELL")))
                .contains("KETTLEBELL", "BAND", "EZ_BAR", "BARBELL", "BODYWEIGHT")
                .doesNotContain("OTHER", "MACHINE");
    }

    @Test
    void groupsJoinNearbyMusclesAndSeparateStretchingAndCardio() {
        assertThat(ExerciseGroups.groupOf("Lumbares", "STRENGTH")).isEqualTo("Espalda");
        assertThat(ExerciseGroups.groupOf("Aductores", "STRENGTH")).isEqualTo("Piernas");
        assertThat(ExerciseGroups.groupOf("Cuello", null)).isEqualTo("Hombros");
        assertThat(ExerciseGroups.groupOf("Isquiotibiales", "STRETCHING")).isEqualTo(ExerciseGroups.STRETCHING);
        assertThat(ExerciseGroups.groupOf("Cuádriceps", "PLYOMETRICS")).isEqualTo(ExerciseGroups.CARDIO);
        assertThat(ExerciseGroups.groupOf(null, "STRENGTH")).isNull();
        assertThat(Set.copyOf(ExerciseGroups.MUSCLES)).hasSize(16);
    }
}
