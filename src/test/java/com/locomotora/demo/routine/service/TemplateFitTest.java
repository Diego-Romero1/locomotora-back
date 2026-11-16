package com.locomotora.demo.routine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.locomotora.demo.ai.tools.ExerciseCatalogFilter;
import com.locomotora.demo.routine.service.TemplateFit.ExerciseInfo;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TemplateFitTest {
    private static final List<ExerciseInfo> GYM_LEGS = List.of(
            new ExerciseInfo("BARBELL", List.of("KNEE", "LOWER_BACK")),
            new ExerciseInfo("MACHINE", List.of("KNEE")),
            new ExerciseInfo("MACHINE", List.of()),
            new ExerciseInfo("BODYWEIGHT", List.of()),
            new ExerciseInfo(null, List.of("KNEE")));

    @Test
    void noEquipmentDeclaredMeansEverythingFits() {
        assertEquals(0, TemplateFit.penalty(GYM_LEGS, null, Set.of()));
    }

    @Test
    void countsMissingEquipmentAndInjuredZones() {
        Set<String> home = ExerciseCatalogFilter.allowedEquipment(List.of("BODYWEIGHT", "DUMBBELLS"));
        assertEquals(3, TemplateFit.penalty(GYM_LEGS, home, Set.of()));
        assertEquals(3, TemplateFit.penalty(GYM_LEGS, null, Set.of("KNEE")));
        assertEquals(1, TemplateFit.penalty(GYM_LEGS, null, Set.of("LOWER_BACK")));
        // Un ejercicio con equipo faltante y zona lesionada suma por las dos razones.
        assertEquals(6, TemplateFit.penalty(GYM_LEGS, home, Set.of("KNEE")));
    }

    @Test
    void minutesOverOnlyCountsExcess() {
        assertEquals(25, TemplateFit.minutesOver(70, 45));
        assertEquals(0, TemplateFit.minutesOver(55, 60));
        assertEquals(0, TemplateFit.minutesOver(null, 30));
    }

    @Test
    void cautionLowersOneLevel() {
        assertEquals("INTERMEDIATE", TemplateFit.cautiousLevel("ADVANCED", true));
        assertEquals("BEGINNER", TemplateFit.cautiousLevel("INTERMEDIATE", true));
        assertEquals("BEGINNER", TemplateFit.cautiousLevel("BEGINNER", true));
        assertEquals("ADVANCED", TemplateFit.cautiousLevel("ADVANCED", false));
    }
}
