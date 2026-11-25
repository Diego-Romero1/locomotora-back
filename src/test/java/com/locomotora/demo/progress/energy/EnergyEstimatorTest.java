package com.locomotora.demo.progress.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.locomotora.demo.progress.energy.EnergyEstimator.EnergyClass;
import com.locomotora.demo.progress.energy.EnergyEstimator.ExerciseEnergy;
import com.locomotora.demo.progress.energy.EnergyEstimator.ExerciseInput;
import com.locomotora.demo.progress.energy.EnergyEstimator.Profile;
import com.locomotora.demo.progress.energy.EnergyEstimator.SessionEnergy;
import com.locomotora.demo.progress.energy.EnergyEstimator.SessionInput;
import com.locomotora.demo.progress.energy.EnergyEstimator.SetEnergy;
import com.locomotora.demo.progress.energy.EnergyEstimator.SetInput;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Casos compartidos con la app (energy-fixtures.json): las dos implementaciones deben dar lo mismo. */
class EnergyEstimatorTest {
    private static final JsonNode FIXTURES = load();
    private static final double TOLERANCE = FIXTURES.get("tolerance").asDouble();

    @Test
    void fixturesMatchTheModelVersion() {
        assertEquals(EnergyEstimator.MODEL_VERSION, FIXTURES.get("modelVersion").asInt());
    }

    @TestFactory
    Stream<DynamicTest> classifiesCatalogExercises() {
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonNode item : FIXTURES.get("classification")) {
            JsonNode exercise = item.get("exercise");
            tests.add(DynamicTest.dynamicTest(exercise.get("name").asText(), () -> assertEquals(
                    EnergyClass.valueOf(item.get("energyClass").asText()),
                    EnergyEstimator.classify(text(exercise, "name"), text(exercise, "nameEn"),
                            text(exercise, "primaryMuscleGroup"), text(exercise, "mechanic"),
                            text(exercise, "equipment"), text(exercise, "exerciseType")))));
        }
        return tests.stream();
    }

    @TestFactory
    Stream<DynamicTest> estimatesSharedSessions() {
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonNode session : FIXTURES.get("sessions")) {
            tests.add(DynamicTest.dynamicTest(session.get("name").asText(), () -> {
                SessionEnergy actual = EnergyEstimator.estimate(input(session.get("input")));
                JsonNode expected = session.get("expected");
                close(expected, "restingKcalPerMin", actual.restingKcalPerMin());
                close(expected, "bodyWeightKg", actual.bodyWeightKg());
                assertEquals(expected.get("bodyWeightEstimated").asBoolean(), actual.bodyWeightEstimated());
                assertEquals(expected.get("personalized").asBoolean(), actual.personalized());
                close(expected, "workKcal", actual.workKcal());
                close(expected, "restKcal", actual.restKcal());
                close(expected, "totalKcal", actual.totalKcal());
                assertEquals(expected.get("exercises").size(), actual.exercises().size());
                for (int i = 0; i < actual.exercises().size(); i++) {
                    JsonNode expectedExercise = expected.get("exercises").get(i);
                    ExerciseEnergy exercise = actual.exercises().get(i);
                    assertEquals(expectedExercise.get("energyClass").asText(), exercise.energyClass().name());
                    close(expectedExercise, "kcal", exercise.kcal());
                    for (int j = 0; j < exercise.sets().size(); j++) {
                        JsonNode expectedSet = expectedExercise.get("sets").get(j);
                        SetEnergy set = exercise.sets().get(j);
                        nullableClose(expectedSet, "intensity", set.intensity());
                        nullableClose(expectedSet, "met", set.met());
                        close(expectedSet, "workSeconds", set.workSeconds());
                        close(expectedSet, "restSeconds", set.restSeconds());
                        close(expectedSet, "kcal", set.kcal());
                    }
                }
            }));
        }
        return tests.stream();
    }

    @Test
    void heavierLoadAndMoreRepsCostMore() {
        Profile profile = new Profile("MALE", 30, 178.0, 75.0);
        double light = single(profile, 8, 60.0);
        double heavy = single(profile, 8, 80.0);
        double moreReps = single(profile, 12, 60.0);
        assertTrue(heavy > light, "mas peso, mas kcal");
        assertTrue(moreReps > light, "mas repeticiones, mas kcal");
    }

    @Test
    void legsCostMoreThanArmsWithTheSameWork() {
        Profile profile = new Profile("FEMALE", 25, 165.0, 62.0);
        SessionEnergy legs = EnergyEstimator.estimate(new SessionInput(profile, 300, null, List.of(new ExerciseInput(
                "Sentadilla trasera", null, "Cuádriceps", "COMPOUND", "BARBELL", "STRENGTH", 80.0,
                List.of(new SetInput(10, 55.0, null, null))))));
        SessionEnergy arms = EnergyEstimator.estimate(new SessionInput(profile, 300, null, List.of(new ExerciseInput(
                "Curl con barra", null, "Bíceps", "ISOLATION", "BARBELL", "STRENGTH", 30.0,
                List.of(new SetInput(10, 20.5, null, null))))));
        assertTrue(legs.workKcal() > 2 * arms.workKcal());
    }

    private static double single(Profile profile, int reps, double weight) {
        return EnergyEstimator.estimate(new SessionInput(profile, 0, null, List.of(new ExerciseInput(
                "Press de banca plano", null, "Pecho", "COMPOUND", "BARBELL", "STRENGTH", 100.0,
                List.of(new SetInput(reps, weight, null, null)))))).workKcal();
    }

    private static SessionInput input(JsonNode node) {
        JsonNode profile = node.get("profile");
        List<ExerciseInput> exercises = new ArrayList<>();
        for (JsonNode exercise : node.get("exercises")) {
            List<SetInput> sets = new ArrayList<>();
            for (JsonNode set : exercise.get("sets")) {
                sets.add(new SetInput(integer(set, "reps"), decimal(set, "weightKg"), integer(set, "durationSeconds"),
                        instant(set, "completedAt")));
            }
            exercises.add(new ExerciseInput(text(exercise, "name"), text(exercise, "nameEn"),
                    text(exercise, "primaryMuscleGroup"), text(exercise, "mechanic"), text(exercise, "equipment"),
                    text(exercise, "exerciseType"), decimal(exercise, "bestE1rmKg"), sets));
        }
        return new SessionInput(
                new Profile(text(profile, "sex"), integer(profile, "ageYears"), decimal(profile, "heightCm"),
                        decimal(profile, "bodyWeightKg")),
                node.get("activeSeconds").asLong(),
                instant(node, "completedAt"),
                exercises);
    }

    private static void close(JsonNode expected, String field, double actual) {
        assertEquals(expected.get(field).asDouble(), actual, TOLERANCE, field);
    }

    private static void nullableClose(JsonNode expected, String field, Double actual) {
        if (isNull(expected, field)) {
            assertEquals(null, actual, field);
        } else {
            assertEquals(expected.get(field).asDouble(), actual, TOLERANCE, field);
        }
    }

    private static boolean isNull(JsonNode node, String field) {
        return node.get(field) == null || node.get(field).isNull();
    }

    private static String text(JsonNode node, String field) {
        return isNull(node, field) ? null : node.get(field).asText();
    }

    private static Integer integer(JsonNode node, String field) {
        return isNull(node, field) ? null : node.get(field).asInt();
    }

    private static Double decimal(JsonNode node, String field) {
        return isNull(node, field) ? null : node.get(field).asDouble();
    }

    private static Instant instant(JsonNode node, String field) {
        return isNull(node, field) ? null : Instant.parse(node.get(field).asText());
    }

    private static JsonNode load() {
        try (InputStream stream = EnergyEstimatorTest.class.getResourceAsStream("/energy/energy-fixtures.json")) {
            return new ObjectMapper().readTree(stream);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
