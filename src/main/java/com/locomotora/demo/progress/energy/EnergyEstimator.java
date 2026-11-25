package com.locomotora.demo.progress.energy;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Estimacion de kilocalorias de un entrenamiento de fuerza, serie por serie (modelo v1).
 *
 * <p>Por cada serie hecha: {@code MET de trabajo × gasto en reposo (kcal/min) × reps · 4 s}, con el MET segun la clase
 * del ejercicio (masa muscular, Reis et al. 2017) y la intensidad relativa (peso / mejor 1RM estimado). Antes de cada
 * serie se suma el descanso a 2,0 MET, con tope de 5 min. El gasto en reposo usa Harris-Benedict (METs corregidos del
 * Compendio 2024) o, si falta el perfil, 1 MET estandar.
 *
 * <p>La app tiene la misma formula en {@code src/features/progress/energyEstimate.ts}; las dos pasan los casos de
 * {@code src/test/resources/energy/energy-fixtures.json}. Cambiar una sin la otra rompe esos tests.
 */
public final class EnergyEstimator {
    public static final int MODEL_VERSION = 1;
    /** Margen que se muestra como rango (± 20 %). */
    public static final double UNCERTAINTY = 0.20;

    static final double REST_MET = 2.0;
    static final double REST_CAP_SECONDS = 300;
    static final double SECONDS_PER_REP = 4;
    static final double PLYOMETRIC_SECONDS_PER_REP = 2;
    /** Calistenia vigorosa del Compendio: tope para series sin peso externo. */
    static final double BODYWEIGHT_MET_CAP = 8.0;
    static final double DEFAULT_BODY_WEIGHT_KG = 70;
    /** 1 MET estandar: 3,5 ml O2/kg/min × 5 kcal/L. */
    static final double STANDARD_KCAL_PER_KG_MIN = 0.0175;
    static final double MIN_INTENSITY = 0.20;
    static final double MAX_INTENSITY = 1.0;

    private static final Set<String> LOWER_BODY =
            Set.of("cuadriceps", "isquiotibiales", "gluteos", "aductores", "abductores", "lumbares");
    private static final Set<String> SMALL_GROUPS = Set.of("abdomen", "pantorrillas", "antebrazos", "cuello");
    private static final Set<String> GUIDED_EQUIPMENT = Set.of("MACHINE", "SMITH_MACHINE", "CABLE", "BAND");
    /** Patrones de pierna y cadera que el catalogo a veces guarda como espalda u hombros (peso muerto, thruster). */
    private static final List<String> LEG_PATTERNS = List.of(
            "peso muerto", "deadlift", "buenos dias", "good morning", "swing", "sentadilla", "squat", "zancada",
            "lunge", "hip thrust", "cargada", "clean", "arranque", "snatch", "thruster");

    private EnergyEstimator() {
    }

    /** Clase de gasto segun la masa muscular que mueve el ejercicio. */
    public enum EnergyClass {
        /** Pierna y cadera, compuesto libre: MET = 2 + 30·I. */
        A(2, 30),
        /** Torso compuesto: MET = 0,5 + 15·I. */
        B(0.5, 15),
        /** Pierna en maquina o aislado: MET = 1 + 20·I. */
        C(1, 20),
        /** Aislado pequeno y core: MET = 0,8 + 7·I. */
        D(0.8, 7),
        /** Pliometria y cardio: 8,0 MET fijo. */
        E(8.0, 0),
        /** Estiramiento: 2,3 MET fijo. */
        F(2.3, 0);

        private final double base;
        private final double slope;

        EnergyClass(double base, double slope) {
            this.base = base;
            this.slope = slope;
        }

        boolean fixed() {
            return this == E || this == F;
        }

        double met(double intensity) {
            return base + slope * intensity;
        }
    }

    public record Profile(String sex, Integer ageYears, Double heightCm, Double bodyWeightKg) {
    }

    public record SetInput(Integer reps, Double weightKg, Integer durationSeconds, Instant completedAt) {
    }

    /** Un ejercicio de la sesion. {@code bestE1rmKg}: mejor 1RM estimado anterior a esta sesion (null si no hay). */
    public record ExerciseInput(
            String name,
            String nameEn,
            String primaryMuscleGroup,
            String mechanic,
            String equipment,
            String exerciseType,
            Double bestE1rmKg,
            List<SetInput> sets
    ) {
    }

    /** {@code activeSeconds}: tiempo entrenando sin pausas. {@code completedAt}: fin (para el descanso por hora). */
    public record SessionInput(Profile profile, long activeSeconds, Instant completedAt, List<ExerciseInput> exercises) {
    }

    public record SetEnergy(
            Double intensity,
            Double met,
            double workSeconds,
            double workKcal,
            double restSeconds,
            double restKcal,
            double kcal
    ) {
    }

    public record ExerciseEnergy(EnergyClass energyClass, double workKcal, double restKcal, double kcal, List<SetEnergy> sets) {
    }

    public record SessionEnergy(
            double restingKcalPerMin,
            double bodyWeightKg,
            boolean bodyWeightEstimated,
            boolean personalized,
            double workKcal,
            double restKcal,
            double totalKcal,
            List<ExerciseEnergy> exercises
    ) {
    }

    public static EnergyClass classify(
            String name, String nameEn, String primaryMuscleGroup, String mechanic, String equipment, String exerciseType) {
        if ("PLYOMETRICS".equals(exerciseType) || "CARDIO".equals(exerciseType)) {
            return EnergyClass.E;
        }
        if ("STRETCHING".equals(exerciseType)) {
            return EnergyClass.F;
        }
        if ("OLYMPIC".equals(exerciseType) || "STRONGMAN".equals(exerciseType)) {
            return EnergyClass.A;
        }
        boolean guided = equipment != null && GUIDED_EQUIPMENT.contains(equipment);
        String names = normalize(name) + " " + normalize(nameEn);
        if (LEG_PATTERNS.stream().anyMatch(names::contains)) {
            return guided ? EnergyClass.C : EnergyClass.A;
        }
        String group = normalize(primaryMuscleGroup);
        if (LOWER_BODY.contains(group)) {
            return "ISOLATION".equals(mechanic) || guided ? EnergyClass.C : EnergyClass.A;
        }
        if (SMALL_GROUPS.contains(group)) {
            return EnergyClass.D;
        }
        if ("COMPOUND".equals(mechanic)) {
            return EnergyClass.B;
        }
        if ("ISOLATION".equals(mechanic)) {
            return EnergyClass.D;
        }
        return "pecho".equals(group) || "espalda".equals(group) ? EnergyClass.B : EnergyClass.D;
    }

    public static EnergyClass classify(ExerciseInput exercise) {
        return classify(exercise.name(), exercise.nameEn(), exercise.primaryMuscleGroup(), exercise.mechanic(),
                exercise.equipment(), exercise.exerciseType());
    }

    /** Gasto en reposo y de donde salio: perfil completo (Harris-Benedict) o 1 MET estandar. */
    record RestingRate(double kcalPerMin, double bodyWeightKg, boolean bodyWeightEstimated, boolean personalized) {
    }

    static RestingRate restingRate(Profile profile) {
        Double weight = profile == null ? null : profile.bodyWeightKg();
        boolean estimated = weight == null || weight <= 0;
        double kg = estimated ? DEFAULT_BODY_WEIGHT_KG : weight;
        boolean personalized = !estimated && profile.sex() != null && profile.ageYears() != null
                && profile.ageYears() > 0 && profile.heightCm() != null && profile.heightCm() > 0;
        if (!personalized) {
            return new RestingRate(STANDARD_KCAL_PER_KG_MIN * kg, kg, estimated, false);
        }
        double cm = profile.heightCm();
        int age = profile.ageYears();
        double male = 66.4730 + 5.0033 * cm + 13.7516 * kg - 6.7550 * age;
        double female = 655.0955 + 1.8496 * cm + 9.5634 * kg - 4.6756 * age;
        double perDay = switch (profile.sex()) {
            case "MALE" -> male;
            case "FEMALE" -> female;
            default -> (male + female) / 2;
        };
        return new RestingRate(perDay / 1440, kg, false, true);
    }

    public static SessionEnergy estimate(SessionInput input) {
        RestingRate rate = restingRate(input.profile());
        double kcalPerMin = rate.kcalPerMin();

        List<EnergyClass> classes = new ArrayList<>();
        List<List<MutableSet>> perExercise = new ArrayList<>();
        List<MutableSet> all = new ArrayList<>();
        for (ExerciseInput exercise : input.exercises()) {
            EnergyClass energyClass = classify(exercise);
            double best = bestE1rm(exercise);
            List<MutableSet> sets = new ArrayList<>();
            for (SetInput set : exercise.sets()) {
                MutableSet computed = work(energyClass, set, best, kcalPerMin);
                sets.add(computed);
                all.add(computed);
            }
            classes.add(energyClass);
            perExercise.add(sets);
        }
        assignRest(input, all, kcalPerMin);

        List<ExerciseEnergy> exercises = new ArrayList<>();
        double work = 0;
        double rest = 0;
        for (int i = 0; i < perExercise.size(); i++) {
            List<SetEnergy> sets = perExercise.get(i).stream().map(MutableSet::toEnergy).toList();
            double exerciseWork = sets.stream().mapToDouble(SetEnergy::workKcal).sum();
            double exerciseRest = sets.stream().mapToDouble(SetEnergy::restKcal).sum();
            exercises.add(new ExerciseEnergy(classes.get(i), exerciseWork, exerciseRest, exerciseWork + exerciseRest, sets));
            work += exerciseWork;
            rest += exerciseRest;
        }
        return new SessionEnergy(kcalPerMin, rate.bodyWeightKg(), rate.bodyWeightEstimated(), rate.personalized(),
                work, rest, work + rest, exercises);
    }

    private static double bestE1rm(ExerciseInput exercise) {
        double best = exercise.bestE1rmKg() == null ? 0 : exercise.bestE1rmKg();
        for (SetInput set : exercise.sets()) {
            double weight = positive(set.weightKg());
            int reps = positive(set.reps());
            if (weight > 0 && reps > 0) {
                best = Math.max(best, reps == 1 ? weight : weight * (1 + reps / 30.0));
            }
        }
        return best;
    }

    private static MutableSet work(EnergyClass energyClass, SetInput set, double bestE1rm, double kcalPerMin) {
        int reps = positive(set.reps());
        double weight = positive(set.weightKg());
        Double intensity = null;
        Double met = null;
        if (energyClass.fixed()) {
            met = energyClass.met(0);
        } else if (reps > 0) {
            double relative = weight > 0 ? weight / bestE1rm : 1 / (1 + (reps + 2) / 30.0);
            intensity = Math.min(MAX_INTENSITY, Math.max(MIN_INTENSITY, relative));
            met = energyClass.met(intensity);
            if (weight <= 0) {
                met = Math.min(met, BODYWEIGHT_MET_CAP);
            }
        }
        double secondsPerRep = energyClass == EnergyClass.E ? PLYOMETRIC_SECONDS_PER_REP : SECONDS_PER_REP;
        int duration = positive(set.durationSeconds());
        double workSeconds = met == null ? 0 : duration > 0 ? duration : reps * secondsPerRep;
        double workKcal = met == null ? 0 : met * kcalPerMin * workSeconds / 60;
        return new MutableSet(intensity, met, workSeconds, workKcal, set.completedAt());
    }

    /**
     * Descanso antes de cada serie, con tope de 5 min. Con la hora de cada serie se usa el tiempo real desde la serie
     * anterior (o desde el inicio); si falta alguna, el tiempo activo que no fue trabajo se reparte en partes iguales.
     */
    private static void assignRest(SessionInput input, List<MutableSet> sets, double kcalPerMin) {
        if (sets.isEmpty()) {
            return;
        }
        boolean timed = input.completedAt() != null && sets.stream().allMatch(set -> set.completedAt != null);
        if (timed) {
            Instant previous = input.completedAt().minusSeconds(input.activeSeconds());
            List<MutableSet> ordered = sets.stream().sorted(Comparator.comparing(set -> set.completedAt)).toList();
            for (MutableSet set : ordered) {
                double gap = (set.completedAt.toEpochMilli() - previous.toEpochMilli()) / 1000.0 - set.workSeconds;
                set.restSeconds = Math.min(REST_CAP_SECONDS, Math.max(0, gap));
                previous = set.completedAt;
            }
        } else {
            double totalWork = sets.stream().mapToDouble(set -> set.workSeconds).sum();
            double each = Math.max(0, input.activeSeconds() - totalWork) / sets.size();
            sets.forEach(set -> set.restSeconds = Math.min(REST_CAP_SECONDS, each));
        }
        sets.forEach(set -> set.restKcal = REST_MET * kcalPerMin * set.restSeconds / 60);
    }

    private static int positive(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private static double positive(Double value) {
        return value == null || value < 0 ? 0 : value;
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim();
    }

    private static final class MutableSet {
        private final Double intensity;
        private final Double met;
        private final double workSeconds;
        private final double workKcal;
        private final Instant completedAt;
        private double restSeconds;
        private double restKcal;

        private MutableSet(Double intensity, Double met, double workSeconds, double workKcal, Instant completedAt) {
            this.intensity = intensity;
            this.met = met;
            this.workSeconds = workSeconds;
            this.workKcal = workKcal;
            this.completedAt = completedAt;
        }

        private SetEnergy toEnergy() {
            return new SetEnergy(intensity, met, workSeconds, workKcal, restSeconds, restKcal, workKcal + restKcal);
        }
    }
}
