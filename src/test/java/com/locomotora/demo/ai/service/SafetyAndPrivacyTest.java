package com.locomotora.demo.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.locomotora.demo.ai.service.SafetyGuard.Issue;
import com.locomotora.demo.ai.service.SafetyGuard.Risk;
import com.locomotora.demo.ai.tools.ExerciseCatalogFilter;
import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Filtro de datos personales, controles de seguridad y filtro de catalogo por equipo. */
class SafetyAndPrivacyTest {

    @Test
    void personalDataIsRemoved() {
        assertEquals("escribeme a [email] o al [telefono]",
                PersonalDataFilter.redact("escribeme a ana.perez+gym@mail.com o al +54 11 5555-1234"));
        assertEquals("llama al [telefono]", PersonalDataFilter.redact("llama al 011-5555-1234"));
        assertEquals("mi DNI es [numero]", PersonalDataFilter.redact("mi DNI es 30123456"));
        assertEquals("tarjeta [numero]", PersonalDataFilter.redact("tarjeta 4111 1111 1111 1111"));
    }

    @Test
    void trainingNumbersAreKept() {
        for (String text : List.of("hice 3x10 con 100 kg", "series de 80 80 80 kg", "el 2026-11-14 entrene", "120-150 s de descanso",
                "como 2.500 kcal", "peso 80,5 kg")) {
            assertEquals(text, PersonalDataFilter.redact(text));
        }
    }

    @Test
    void riskSignalsAreDetectedWithoutAccents() {
        assertEquals(Set.of(Risk.CARDIO), SafetyGuard.detect("Me dio DOLOR EN EL PECHO y mareo"));
        assertEquals(Set.of(Risk.EATING_DISORDER), SafetyGuard.detect("a veces vomito despues de comer"));
        assertEquals(Set.of(Risk.PREGNANCY), SafetyGuard.detect("Estoy embarazada de 3 meses"));
        assertEquals(Set.of(Risk.INJURY), SafetyGuard.detect("tengo una lesión en la rodilla"));
        assertEquals(Set.of(Risk.SUBSTANCES), SafetyGuard.detect("¿Que tal un ciclo de testosterona?"));
        assertEquals(Set.of(Risk.MINOR), SafetyGuard.detect("tengo 14 años, ¿puedo entrenar?"));
    }

    @Test
    void ordinaryTrainingQuestionsHaveNoRisk() {
        for (String text : List.of("¿Como mejoro mi press de pecho?", "quiero volver al entorno del gimnasio",
                "armame un ciclo de entrenamiento de 8 semanas", "¿cuanta proteina como?", "tengo 24 años")) {
            assertTrue(SafetyGuard.detect(text).isEmpty(), text);
        }
    }

    @Test
    void reviewFlagsDangerousNutritionAdvice() {
        assertEquals(Set.of(Issue.VERY_LOW_CALORIES), SafetyGuard.review("Come 900 kcal al dia"));
        assertEquals(Set.of(Issue.LONG_FAST), SafetyGuard.review("Haz un ayuno de 48 horas"));
        assertEquals(Set.of(Issue.LONG_FAST), SafetyGuard.review("prueba ayunar dos dias"));
        assertTrue(SafetyGuard.review("Apunta a 2200 kcal y un ayuno de 16 horas si te sirve").isEmpty());
        assertTrue(SafetyGuard.review("En esa sesion quemas unas 500 kcal").isEmpty());
        assertEquals(Set.of(Issue.VERY_LOW_CALORIES), SafetyGuard.review("Puedes comer 900 kcal al dia durante un mes."));
        // Calorias quemadas, no ingeridas: no es una dieta peligrosa.
        assertTrue(SafetyGuard.review("Con tu peso de 78 kg, correr 30 minutos quema entre 300 y 380 kcal al día.").isEmpty());
        assertTrue(SafetyGuard.review("Una sesion de pesas gasta unas 400 calorias.").isEmpty());
    }

    @Test
    void noticesAreAppendedOnce() {
        String answer = SafetyGuard.withNotices("Respuesta.", Set.of(Risk.CARDIO), Set.of(Issue.VERY_LOW_CALORIES));
        assertTrue(answer.startsWith("Respuesta.\n\n⚠️ "));
        assertTrue(answer.contains("consulta a un médico"));
        assertTrue(answer.contains("1200 kcal"));
        assertEquals("Respuesta.", SafetyGuard.withNotices("Respuesta.", Set.of(), Set.of()));
    }

    private static ExerciseCatalogItemResponse item(String name, String equipment) {
        return new ExerciseCatalogItemResponse(name, name, null, "Pecho", equipment, null, null, null, false);
    }

    @Test
    void catalogFilterMapsOnboardingEquipment() {
        Set<String> allowed = ExerciseCatalogFilter.allowedEquipment(List.of("DUMBBELLS", "MACHINES"));
        assertEquals(Set.of("BODYWEIGHT", "DUMBBELL", "FREE_WEIGHT", "MACHINE", "CABLE", "SMITH_MACHINE"), allowed);
        assertEquals(null, ExerciseCatalogFilter.allowedEquipment(List.of()));
        assertTrue(ExerciseCatalogFilter.fits(item("Sin equipo", null), Set.of("BODYWEIGHT")));
        assertFalse(ExerciseCatalogFilter.fits(item("Prensa", "MACHINE"), Set.of("BODYWEIGHT")));
    }

    @Test
    void catalogFilterFallsBackToTheWholeCatalogWhenTooSmall() {
        List<ExerciseCatalogItemResponse> catalog = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            catalog.add(item("Maquina " + index, "MACHINE"));
        }
        catalog.add(item("Flexiones", "BODYWEIGHT"));

        ExerciseCatalogFilter.Result bodyweightOnly = ExerciseCatalogFilter.forUser(catalog, List.of("BODYWEIGHT"));
        assertFalse(bodyweightOnly.filtered());
        assertEquals(21, bodyweightOnly.exercises().size());

        ExerciseCatalogFilter.Result machines = ExerciseCatalogFilter.forUser(catalog, List.of("MACHINES"));
        assertTrue(machines.filtered());
        assertEquals(21, machines.exercises().size());
        assertEquals("cuadriceps", ExerciseCatalogFilter.normalize(" Cuádriceps "));
    }
}
