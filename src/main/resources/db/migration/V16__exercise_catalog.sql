-- Catalogo ampliado de ejercicios (C1). GENERADO por tools/exercise-catalog/build_catalog.py:
-- no editar a mano. Fuente: free-exercise-db (Unlicense), nombres e instrucciones traducidos.
-- Los ejercicios actuales conservan su id (rutinas e historial) y se enriquecen; los nuevos
-- usan un uuid determinista por id de la fuente, asi la migracion es idempotente.

ALTER TABLE exercises
    ADD COLUMN source varchar(40) NOT NULL DEFAULT 'LOCOMOTORA',
    ADD COLUMN external_id varchar(120),
    ADD COLUMN name_en varchar(180),
    ADD COLUMN exercise_type varchar(30) NOT NULL DEFAULT 'STRENGTH',
    ADD COLUMN mechanic varchar(20),
    ADD COLUMN force_type varchar(20),
    ADD COLUMN is_core boolean NOT NULL DEFAULT false,
    ADD COLUMN image_url text,
    ADD COLUMN thumb_url text,
    ADD COLUMN media_license varchar(80),
    ADD COLUMN media_author varchar(180),
    ADD COLUMN media_source_url text,
    ADD CONSTRAINT exercises_exercise_type_check CHECK (exercise_type IN
        ('STRENGTH', 'STRETCHING', 'CARDIO', 'PLYOMETRICS', 'POWERLIFTING', 'OLYMPIC', 'STRONGMAN')),
    ADD CONSTRAINT exercises_mechanic_check CHECK (mechanic IS NULL OR mechanic IN ('COMPOUND', 'ISOLATION')),
    ADD CONSTRAINT exercises_force_type_check CHECK (force_type IS NULL OR force_type IN ('PUSH', 'PULL', 'STATIC'));

CREATE UNIQUE INDEX idx_exercises_unique_external_id ON exercises (external_id)
    WHERE external_id IS NOT NULL AND created_by_user_id IS NULL;
CREATE INDEX idx_exercises_core ON exercises (is_core) WHERE is_core;
CREATE INDEX idx_exercises_type ON exercises (exercise_type);

-- 1) Ejercicios actuales: referencia a la fuente, datos que faltaban y nombres en espanol.
CREATE TEMP TABLE catalog_existing (
    name text PRIMARY KEY, new_name text, external_id text, name_en text, exercise_type text,
    mechanic text, force_type text, difficulty text, secondary jsonb
) ON COMMIT DROP;
INSERT INTO catalog_existing VALUES
    ('Abdominal Crunch Machine', 'Crunch en máquina', 'Ab_Crunch_Machine', 'Ab Crunch Machine', 'STRENGTH', 'ISOLATION', 'PULL', 'INTERMEDIATE', '[]'::jsonb),
    ('Abductor Machine', 'Abductores en máquina', 'Thigh_Abductor', 'Thigh Abductor', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '["Glúteos"]'::jsonb),
    ('Aperturas con mancuernas', NULL, 'Dumbbell_Flyes', 'Dumbbell Flyes', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '[]'::jsonb),
    ('Aperturas en polea alta', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '[]'::jsonb),
    ('Aperturas en polea baja', NULL, 'Low_Cable_Crossover', 'Low Cable Crossover', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '["Hombros"]'::jsonb),
    ('Belt Squat', 'Sentadilla con cinturón (belt squat)', NULL, NULL, NULL, NULL, NULL, NULL, '[]'::jsonb),
    ('Buenos días', NULL, 'Good_Morning', 'Good Morning', 'POWERLIFTING', 'COMPOUND', 'PUSH', 'INTERMEDIATE', '["Abdomen", "Glúteos", "Lumbares"]'::jsonb),
    ('Bulgarian Split Squat', 'Sentadilla búlgara', 'Split_Squat_with_Dumbbells', 'Split Squat with Dumbbells', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Glúteos", "Isquiotibiales"]'::jsonb),
    ('Chest Press Machine', 'Press de pecho en máquina', 'Leverage_Chest_Press', 'Leverage Chest Press', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Hombros", "Tríceps"]'::jsonb),
    ('Contractor de pecho', NULL, 'Butterfly', 'Butterfly', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Crossover en polea', NULL, 'Cable_Crossover', 'Cable Crossover', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '["Hombros"]'::jsonb),
    ('Crunch', NULL, 'Crunches', 'Crunches', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Crunch en polea', NULL, 'Cable_Crunch', 'Cable Crunch', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Crunch inverso', NULL, 'Reverse_Crunch', 'Reverse Crunch', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Curl Machine', 'Curl de bíceps en máquina', 'Machine_Bicep_Curl', 'Machine Bicep Curl', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Curl alternado con mancuernas', NULL, 'Dumbbell_Alternate_Bicep_Curl', 'Dumbbell Alternate Bicep Curl', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '["Antebrazos"]'::jsonb),
    ('Curl con barra', NULL, 'Barbell_Curl', 'Barbell Curl', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '["Antebrazos"]'::jsonb),
    ('Curl con barra EZ', NULL, 'EZ-Bar_Curl', 'EZ-Bar Curl', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Curl en polea baja', NULL, 'Standing_Biceps_Cable_Curl', 'Standing Biceps Cable Curl', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Curl femoral acostado', NULL, 'Lying_Leg_Curls', 'Lying Leg Curls', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Curl femoral de pie', NULL, 'Standing_Leg_Curl', 'Standing Leg Curl', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Curl femoral sentado', NULL, 'Seated_Leg_Curl', 'Seated Leg Curl', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Curl inclinado con mancuernas', NULL, 'Incline_Dumbbell_Curl', 'Incline Dumbbell Curl', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Curl martillo', NULL, 'Hammer_Curls', 'Hammer Curls', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Curl unilateral en polea', NULL, 'Standing_One-Arm_Cable_Curl', 'Standing One-Arm Cable Curl', 'STRENGTH', 'ISOLATION', 'PULL', 'INTERMEDIATE', '[]'::jsonb),
    ('Dominadas lastradas', NULL, 'Weighted_Pull_Ups', 'Weighted Pull Ups', 'STRENGTH', 'COMPOUND', 'PULL', 'INTERMEDIATE', '["Bíceps"]'::jsonb),
    ('Dominadas neutras', NULL, 'V-Bar_Pullup', 'V-Bar Pullup', 'STRENGTH', 'COMPOUND', 'PULL', 'BEGINNER', '["Bíceps", "Hombros"]'::jsonb),
    ('Dominadas pronas', NULL, 'Pullups', 'Pullups', 'STRENGTH', 'COMPOUND', 'PULL', 'BEGINNER', '["Bíceps"]'::jsonb),
    ('Dominadas supinas', NULL, 'Chin-Up', 'Chin-Up', 'STRENGTH', 'COMPOUND', 'PULL', 'BEGINNER', '["Bíceps", "Antebrazos"]'::jsonb),
    ('Donkey Calf Raise', 'Elevación de talones tipo burro', 'Donkey_Calf_Raises', 'Donkey Calf Raises', 'STRENGTH', 'ISOLATION', 'PUSH', 'INTERMEDIATE', '[]'::jsonb),
    ('Elevaciones de piernas', NULL, 'Hanging_Leg_Raise', 'Hanging Leg Raise', 'STRENGTH', 'ISOLATION', 'PULL', 'ADVANCED', '[]'::jsonb),
    ('Elevaciones frontales', NULL, 'Front_Dumbbell_Raise', 'Front Dumbbell Raise', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '[]'::jsonb),
    ('Elevaciones laterales', NULL, 'Side_Lateral_Raise', 'Side Lateral Raise', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '[]'::jsonb),
    ('Elevaciones laterales en polea', NULL, 'Cable_Seated_Lateral_Raise', 'Cable Seated Lateral Raise', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '["Espalda", "Trapecio"]'::jsonb),
    ('Elevación de talones de pie', NULL, 'Standing_Dumbbell_Calf_Raise', 'Standing Dumbbell Calf Raise', 'STRENGTH', 'ISOLATION', 'PUSH', 'INTERMEDIATE', '[]'::jsonb),
    ('Elevación de talones sentado', NULL, 'Barbell_Seated_Calf_Raise', 'Barbell Seated Calf Raise', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '[]'::jsonb),
    ('Elevación frontal en polea', NULL, 'Front_Cable_Raise', 'Front Cable Raise', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '[]'::jsonb),
    ('Encogimientos con mancuernas', NULL, 'Dumbbell_Shrug', 'Dumbbell Shrug', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Extensión de piernas', NULL, 'Leg_Extensions', 'Leg Extensions', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '[]'::jsonb),
    ('Extensión de tríceps con mancuerna sobre la cabeza', NULL, 'Dumbbell_One-Arm_Triceps_Extension', 'Dumbbell One-Arm Triceps Extension', 'STRENGTH', 'ISOLATION', 'PUSH', 'INTERMEDIATE', '[]'::jsonb),
    ('Extensión overhead en polea', 'Extensión de tríceps sobre la cabeza en polea', 'Cable_Rope_Overhead_Triceps_Extension', 'Cable Rope Overhead Triceps Extension', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '[]'::jsonb),
    ('Face Pull', NULL, 'Face_Pull', 'Face Pull', 'STRENGTH', 'COMPOUND', 'PULL', 'INTERMEDIATE', '["Espalda"]'::jsonb),
    ('Flexiones clásicas', NULL, 'Pushups', 'Pushups', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Hombros", "Tríceps"]'::jsonb),
    ('Flexiones declinadas', NULL, 'Decline_Push-Up', 'Decline Push-Up', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Hombros", "Tríceps"]'::jsonb),
    ('Flexiones inclinadas', NULL, 'Incline_Push-Up', 'Incline Push-Up', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Hombros", "Tríceps"]'::jsonb),
    ('Flexiones lastradas', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '[]'::jsonb),
    ('Fondos asistidos', NULL, 'Dip_Machine', 'Dip Machine', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Pecho", "Hombros"]'::jsonb),
    ('Fondos en paralelas', NULL, 'Dips_-_Triceps_Version', 'Dips - Triceps Version', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Pecho", "Hombros"]'::jsonb),
    ('Glute Drive Machine', 'Empuje de cadera en máquina', NULL, NULL, NULL, NULL, NULL, NULL, '[]'::jsonb),
    ('Hack Squat', 'Sentadilla hack', 'Hack_Squat', 'Hack Squat', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb),
    ('High Row Machine', 'Remo alto en máquina', 'Leverage_High_Row', 'Leverage High Row', 'STRENGTH', 'COMPOUND', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Hip Thrust', NULL, 'Barbell_Hip_Thrust', 'Barbell Hip Thrust', 'POWERLIFTING', 'COMPOUND', 'PUSH', 'INTERMEDIATE', '["Pantorrillas", "Isquiotibiales"]'::jsonb),
    ('Hip Thrust unilateral', NULL, 'Single_Leg_Glute_Bridge', 'Single Leg Glute Bridge', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '["Isquiotibiales"]'::jsonb),
    ('Hollow Hold', 'Cuerpo hueco (hollow hold)', NULL, NULL, NULL, NULL, NULL, NULL, '[]'::jsonb),
    ('Jalón al pecho', NULL, 'Wide-Grip_Lat_Pulldown', 'Wide-Grip Lat Pulldown', 'STRENGTH', 'COMPOUND', 'PULL', 'BEGINNER', '["Bíceps", "Hombros"]'::jsonb),
    ('Jalón cerrado', NULL, 'Close-Grip_Front_Lat_Pulldown', 'Close-Grip Front Lat Pulldown', 'STRENGTH', 'COMPOUND', 'PULL', 'BEGINNER', '["Bíceps", "Hombros"]'::jsonb),
    ('Jalón supino', NULL, 'Underhand_Cable_Pulldowns', 'Underhand Cable Pulldowns', 'STRENGTH', 'COMPOUND', 'PULL', 'BEGINNER', '["Bíceps", "Hombros"]'::jsonb),
    ('Lat Pulldown Machine', 'Jalón en máquina', NULL, NULL, NULL, NULL, NULL, NULL, '[]'::jsonb),
    ('Lateral Raise Machine', 'Elevaciones laterales en máquina', NULL, NULL, NULL, NULL, NULL, NULL, '[]'::jsonb),
    ('Patada de glúteo en polea', NULL, 'One-Legged_Cable_Kickback', 'One-Legged Cable Kickback', 'STRENGTH', 'ISOLATION', 'PUSH', 'INTERMEDIATE', '["Isquiotibiales"]'::jsonb),
    ('Pec Deck', 'Pec deck', NULL, NULL, NULL, NULL, NULL, NULL, '[]'::jsonb),
    ('Pendulum Squat', 'Sentadilla péndulo', NULL, NULL, NULL, NULL, NULL, NULL, '[]'::jsonb),
    ('Peso muerto', NULL, 'Barbell_Deadlift', 'Barbell Deadlift', 'STRENGTH', 'COMPOUND', 'PULL', 'INTERMEDIATE', '["Pantorrillas", "Antebrazos", "Glúteos", "Isquiotibiales", "Espalda", "Cuádriceps", "Trapecio"]'::jsonb),
    ('Peso muerto piernas rígidas', NULL, 'Stiff-Legged_Barbell_Deadlift', 'Stiff-Legged Barbell Deadlift', 'STRENGTH', 'COMPOUND', 'PULL', 'INTERMEDIATE', '["Glúteos", "Lumbares"]'::jsonb),
    ('Peso muerto rumano', NULL, 'Romanian_Deadlift', 'Romanian Deadlift', 'STRENGTH', 'COMPOUND', 'PULL', 'INTERMEDIATE', '["Pantorrillas", "Glúteos", "Lumbares"]'::jsonb),
    ('Peso muerto rumano con mancuernas', NULL, 'Stiff-Legged_Dumbbell_Deadlift', 'Stiff-Legged Dumbbell Deadlift', 'STRENGTH', 'COMPOUND', 'PULL', 'BEGINNER', '["Glúteos", "Lumbares"]'::jsonb),
    ('Peso muerto rumano enfocado en isquiotibiales', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '[]'::jsonb),
    ('Peso muerto sumo', NULL, 'Sumo_Deadlift', 'Sumo Deadlift', 'POWERLIFTING', 'COMPOUND', 'PULL', 'INTERMEDIATE', '["Aductores", "Antebrazos", "Glúteos", "Lumbares", "Espalda", "Cuádriceps", "Trapecio"]'::jsonb),
    ('Plancha', NULL, 'Plank', 'Plank', 'STRENGTH', 'ISOLATION', 'STATIC', 'BEGINNER', '[]'::jsonb),
    ('Preacher Curl Machine', 'Curl predicador en máquina', 'Machine_Preacher_Curls', 'Machine Preacher Curls', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Prensa 45 grados', NULL, 'Leg_Press', 'Leg Press', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb),
    ('Press Arnold', NULL, 'Arnold_Dumbbell_Press', 'Arnold Dumbbell Press', 'STRENGTH', 'COMPOUND', 'PUSH', 'INTERMEDIATE', '["Tríceps"]'::jsonb),
    ('Press con mancuernas declinado', NULL, 'Decline_Dumbbell_Bench_Press', 'Decline Dumbbell Bench Press', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Hombros", "Tríceps"]'::jsonb),
    ('Press con mancuernas inclinado', NULL, 'Incline_Dumbbell_Press', 'Incline Dumbbell Press', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Hombros", "Tríceps"]'::jsonb),
    ('Press con mancuernas plano', NULL, 'Dumbbell_Bench_Press', 'Dumbbell Bench Press', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Hombros", "Tríceps"]'::jsonb),
    ('Press con mancuernas sentado', NULL, 'Seated_Dumbbell_Press', 'Seated Dumbbell Press', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Tríceps"]'::jsonb),
    ('Press de banca cerrado', NULL, 'Close-Grip_Barbell_Bench_Press', 'Close-Grip Barbell Bench Press', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Pecho", "Hombros"]'::jsonb),
    ('Press de banca declinado', NULL, 'Decline_Barbell_Bench_Press', 'Decline Barbell Bench Press', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Hombros", "Tríceps"]'::jsonb),
    ('Press de banca inclinado', NULL, 'Barbell_Incline_Bench_Press_-_Medium_Grip', 'Barbell Incline Bench Press - Medium Grip', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Hombros", "Tríceps"]'::jsonb),
    ('Press de banca plano', NULL, 'Barbell_Bench_Press_-_Medium_Grip', 'Barbell Bench Press - Medium Grip', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Hombros", "Tríceps"]'::jsonb),
    ('Press francés', NULL, 'EZ-Bar_Skullcrusher', 'EZ-Bar Skullcrusher', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '["Antebrazos"]'::jsonb),
    ('Press inclinado en máquina', NULL, 'Leverage_Incline_Chest_Press', 'Leverage Incline Chest Press', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Hombros", "Tríceps"]'::jsonb),
    ('Press militar', NULL, 'Standing_Military_Press', 'Standing Military Press', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Tríceps"]'::jsonb),
    ('Press unilateral en polea', NULL, 'Standing_Cable_Chest_Press', 'Standing Cable Chest Press', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Hombros", "Tríceps"]'::jsonb),
    ('Pull Through en polea', NULL, 'Pull_Through', 'Pull Through', 'STRENGTH', 'COMPOUND', 'PULL', 'BEGINNER', '["Isquiotibiales", "Lumbares"]'::jsonb),
    ('Pullover con mancuerna', NULL, 'Bent-Arm_Dumbbell_Pullover', 'Bent-Arm Dumbbell Pullover', 'STRENGTH', 'COMPOUND', 'PUSH', 'INTERMEDIATE', '["Espalda", "Hombros", "Tríceps"]'::jsonb),
    ('Pullover en polea', NULL, 'Straight-Arm_Pulldown', 'Straight-Arm Pulldown', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Pushdown con barra', 'Extensión de tríceps en polea con barra', 'Triceps_Pushdown', 'Triceps Pushdown', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '[]'::jsonb),
    ('Pushdown con cuerda', 'Extensión de tríceps en polea con cuerda', 'Triceps_Pushdown_-_Rope_Attachment', 'Triceps Pushdown - Rope Attachment', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '[]'::jsonb),
    ('Pushdown unilateral', 'Extensión de tríceps en polea a una mano', 'Cable_One_Arm_Tricep_Extension', 'Cable One Arm Tricep Extension', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '[]'::jsonb),
    ('Pájaros con mancuernas', NULL, 'Reverse_Flyes', 'Reverse Flyes', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Rear Delt Machine', 'Deltoide posterior en máquina', 'Reverse_Machine_Flyes', 'Reverse Machine Flyes', 'STRENGTH', 'ISOLATION', 'PULL', 'BEGINNER', '[]'::jsonb),
    ('Remo Hammer', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '[]'::jsonb),
    ('Remo Pendlay', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '[]'::jsonb),
    ('Remo con barra', NULL, 'Bent_Over_Barbell_Row', 'Bent Over Barbell Row', 'STRENGTH', 'COMPOUND', 'PULL', 'BEGINNER', '["Bíceps", "Hombros"]'::jsonb),
    ('Remo con mancuerna', NULL, 'One-Arm_Dumbbell_Row', 'One-Arm Dumbbell Row', 'STRENGTH', 'COMPOUND', 'PULL', 'BEGINNER', '["Bíceps", "Hombros"]'::jsonb),
    ('Remo convergente', NULL, 'Leverage_Iso_Row', 'Leverage Iso Row', 'STRENGTH', 'COMPOUND', 'PULL', 'BEGINNER', '["Bíceps"]'::jsonb),
    ('Remo sentado en polea', NULL, 'Seated_Cable_Rows', 'Seated Cable Rows', 'STRENGTH', 'COMPOUND', 'PULL', 'BEGINNER', '["Bíceps", "Hombros"]'::jsonb),
    ('Seated Calf Raise', 'Elevación de talones sentado en máquina', 'Seated_Calf_Raise', 'Seated Calf Raise', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '[]'::jsonb),
    ('Sentadilla frontal', NULL, 'Front_Barbell_Squat', 'Front Barbell Squat', 'STRENGTH', 'COMPOUND', 'PUSH', 'ADVANCED', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb),
    ('Sentadilla goblet', NULL, 'Goblet_Squat', 'Goblet Squat', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Hombros"]'::jsonb),
    ('Sentadilla guiada', NULL, 'Smith_Machine_Squat', 'Smith Machine Squat', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb),
    ('Sentadilla sumo', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '[]'::jsonb),
    ('Sentadilla trasera', NULL, 'Barbell_Squat', 'Barbell Squat', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb),
    ('Shoulder Press Machine', 'Press de hombros en máquina', 'Machine_Shoulder_Military_Press', 'Machine Shoulder (Military) Press', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Tríceps"]'::jsonb),
    ('Standing Calf Raise', 'Elevación de talones de pie en máquina', 'Standing_Calf_Raises', 'Standing Calf Raises', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '[]'::jsonb),
    ('Triceps Extension Machine', 'Extensión de tríceps en máquina', 'Machine_Triceps_Extension', 'Machine Triceps Extension', 'STRENGTH', 'ISOLATION', 'PUSH', 'BEGINNER', '[]'::jsonb),
    ('Woodchopper en polea', 'Leñador en polea', 'Standing_Cable_Wood_Chop', 'Standing Cable Wood Chop', 'STRENGTH', 'COMPOUND', 'PULL', 'BEGINNER', '["Hombros"]'::jsonb),
    ('Zancadas con mancuernas', NULL, 'Dumbbell_Lunges', 'Dumbbell Lunges', 'STRENGTH', 'COMPOUND', 'PUSH', 'BEGINNER', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb);

UPDATE exercises e
SET name = COALESCE(c.new_name, e.name),
    external_id = c.external_id,
    name_en = c.name_en,
    exercise_type = COALESCE(c.exercise_type, e.exercise_type),
    mechanic = c.mechanic,
    force_type = c.force_type,
    difficulty = COALESCE(e.difficulty, c.difficulty),
    secondary_muscle_groups = CASE WHEN e.secondary_muscle_groups = '[]'::jsonb
        THEN c.secondary - COALESCE(e.primary_muscle_group, '') ELSE e.secondary_muscle_groups END,
    is_core = true,
    updated_at = now()
FROM catalog_existing c
WHERE e.created_by_user_id IS NULL AND e.name = c.name;

-- 2) Ejercicios nuevos.
INSERT INTO exercises (id, name, name_en, source, external_id, primary_muscle_group, secondary_muscle_groups,
    equipment, difficulty, exercise_type, category, mechanic, force_type, instructions, stress_zones, is_core)
VALUES
    ('7323292f-dbe8-5ea1-a91c-9d26c2bb5656', 'Abdominal 3/4', '3/4 Sit-Up', 'FREE_EXERCISE_DB', '3_4_Sit-Up', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Acuéstate con las rodillas flexionadas, los pies sujetos y las manos detrás de la cabeza.
2. Sube el torso hasta quedar perpendicular al suelo.
3. Baja solo tres cuartos del recorrido y repite.', '[]'::jsonb, false),
    ('e114163b-5b48-56d9-a3ae-bab92dec39ae', 'Estiramiento de isquiotibiales 90/90', '90/90 Hamstring', 'FREE_EXERCISE_DB', '90_90_Hamstring', 'Isquiotibiales', '["Pantorrillas"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'PUSH', '1. Acostado boca arriba, flexiona cadera y rodilla de una pierna a 90° y sostenla con las manos.
2. Extiende la pierna hacia el techo, pausa arriba y vuelve.
3. Haz 10 a 20 repeticiones y cambia de pierna.', '[]'::jsonb, false),
    ('d682c95b-7c8f-57ca-8ee0-4a81a21e063a', 'Rueda abdominal', 'Ab Roller', 'FREE_EXERCISE_DB', 'Ab_Roller', 'Abdomen', '["Hombros"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De rodillas, apoya la rueda en el suelo frente a ti con ambas manos.
2. Rueda hacia adelante despacio, estirando el cuerpo sin tocar el suelo.
3. Haz una pausa y vuelve contrayendo el abdomen.', '[]'::jsonb, false),
    ('1b46e0dd-4045-592e-b60a-ae527e63b971', 'Liberación de aductores con rodillo', 'Adductor', 'FREE_EXERCISE_DB', 'Adductor', 'Aductores', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Boca abajo, apoya la cara interna del muslo sobre el rodillo.
2. Rueda entre la cadera y la rodilla, deteniéndote 10 a 30 s en los puntos tensos.
3. Repite con la otra pierna.', '[]'::jsonb, false),
    ('ee7d388c-1aa1-53d2-9caa-2239425f4555', 'Estiramiento de aductores con compañero', 'Adductor/Groin', 'FREE_EXERCISE_DB', 'Adductor_Groin', 'Aductores', '[]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Boca arriba con las piernas hacia el techo, tu compañero sostiene tus tobillos.
2. Abre las piernas, intenta cerrarlas 10 s mientras él lo impide y luego relaja.
3. Deja que separe tus piernas solo hasta donde sea cómodo.', '[]'::jsonb, false),
    ('dc2a8a08-63d4-5afe-b9e2-213628753e4d', 'Molino avanzado con kettlebell', 'Advanced Kettlebell Windmill', 'FREE_EXERCISE_DB', 'Advanced_Kettlebell_Windmill', 'Abdomen', '["Glúteos", "Isquiotibiales", "Hombros"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Sostén la kettlebell arriba con el brazo bloqueado y los pies girados 45°.
2. Lleva la cadera hacia el lado de la kettlebell e inclínate todo lo que puedas mirándola.
3. Pausa un segundo y vuelve a la posición inicial.', '[]'::jsonb, false),
    ('ab944a32-ea61-5eb0-9a64-c3182f19c770', 'Abdominal bicicleta', 'Air Bike', 'FREE_EXERCISE_DB', 'Air_Bike', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Boca arriba con la zona lumbar pegada al suelo y las manos junto a la cabeza.
2. Sube los hombros y las rodillas a 90°.
3. Pedalea llevando el codo hacia la rodilla contraria, alternando lados.', '[]'::jsonb, false),
    ('66bbf229-2418-573b-a5d6-33c9bd9d3baa', 'Estiramiento de cuádriceps en cuadrupedia', 'All Fours Quad Stretch', 'FREE_EXERCISE_DB', 'All_Fours_Quad_Stretch', 'Cuádriceps', '[]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. En cuatro apoyos, levanta una pierna y toma el pie con la mano.
2. Mantén la rodilla flexionada y empuja la cadera hacia el suelo.
3. Sostén 10 a 20 s y cambia de lado.', '[]'::jsonb, false),
    ('f96fc08b-a00d-532b-be01-4babd1bf98b6', 'Curl martillo alterno', 'Alternate Hammer Curl', 'FREE_EXERCISE_DB', 'Alternate_Hammer_Curl', 'Bíceps', '["Antebrazos"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie con una mancuerna en cada mano, palmas hacia el cuerpo y codos pegados.
2. Sube una mancuerna hasta el hombro sin mover el brazo y aprieta un segundo.
3. Baja despacio y repite con el otro brazo.', '[]'::jsonb, false),
    ('293d6712-f8c8-55c8-a77d-a8061030915b', 'Toques de talón alternos', 'Alternate Heel Touchers', 'FREE_EXERCISE_DB', 'Alternate_Heel_Touchers', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca arriba con las rodillas flexionadas y los brazos a los costados.
2. Inclina el torso hacia un lado hasta tocar el talón y sostén un segundo.
3. Vuelve al centro y repite hacia el otro lado.', '[]'::jsonb, false),
    ('4f223ae4-7a9b-502d-88b6-a93b64671362', 'Curl alterno en banco inclinado', 'Alternate Incline Dumbbell Curl', 'FREE_EXERCISE_DB', 'Alternate_Incline_Dumbbell_Curl', 'Bíceps', '["Antebrazos"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado en un banco inclinado, con las mancuernas colgando y los codos pegados.
2. Sube una mancuerna girando la palma hacia arriba hasta el hombro.
3. Aprieta un segundo, baja despacio y alterna.', '[]'::jsonb, false),
    ('6be388e5-fa9f-5f32-a573-783d8985ab6d', 'Saltos diagonales alternos', 'Alternate Leg Diagonal Bound', 'FREE_EXERCISE_DB', 'Alternate_Leg_Diagonal_Bound', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie con un pie un poco adelantado.
2. Impúlsate con la pierna delantera llevando la rodilla contraria arriba y salta en diagonal.
3. Aterriza y repite hacia el otro lado buscando distancia.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('128654db-4bdb-5e8d-bd12-dd9dab865471', 'Press de hombros alterno en polea', 'Alternating Cable Shoulder Press', 'FREE_EXERCISE_DB', 'Alternating_Cable_Shoulder_Press', 'Hombros', '["Tríceps"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con las poleas abajo, toma los agarres a la altura de los hombros con las palmas al frente.
2. Empuja un brazo por encima de la cabeza manteniendo el pecho arriba.
3. Pausa, baja y repite con el otro lado.', '["SHOULDER"]'::jsonb, false),
    ('33a9e5ac-9202-5507-af55-74e46340ba55', 'Elevación frontal y lateral alterna', 'Alternating Deltoid Raise', 'FREE_EXERCISE_DB', 'Alternating_Deltoid_Raise', 'Hombros', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie con una mancuerna en cada mano y los codos apenas flexionados.
2. Sube las mancuernas al frente hasta la altura de los hombros y baja.
3. En la siguiente repetición súbelas a los costados; sigue alternando.', '[]'::jsonb, false),
    ('da68d188-bd30-5be0-b641-1d7737bec178', 'Press en el suelo alterno con kettlebells', 'Alternating Floor Press', 'FREE_EXERCISE_DB', 'Alternating_Floor_Press', 'Pecho', '["Abdomen", "Hombros", "Tríceps"]'::jsonb, 'KETTLEBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado en el suelo, sostén dos kettlebells sobre el pecho con los brazos extendidos.
2. Baja una kettlebell al pecho mientras la otra queda arriba.
3. Súbela y repite con el otro brazo.', '[]'::jsonb, false),
    ('a190c3c4-75f9-5823-84e2-396c97cbaded', 'Cargada colgante alterna con kettlebells', 'Alternating Hang Clean', 'FREE_EXERCISE_DB', 'Alternating_Hang_Clean', 'Isquiotibiales', '["Bíceps", "Pantorrillas", "Antebrazos", "Glúteos", "Lumbares", "Trapecio"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con dos kettlebells entre los pies, lleva la cadera atrás y la mirada al frente.
2. Sube una kettlebell al hombro extendiendo piernas y cadera, girando la muñeca.
3. Bájala y repite con la otra.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('46ee8565-a13f-57bd-8518-c7d8723c93b2', 'Press alterno con kettlebells', 'Alternating Kettlebell Press', 'FREE_EXERCISE_DB', 'Alternating_Kettlebell_Press', 'Hombros', '["Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Lleva dos kettlebells a los hombros.
2. Empuja una por encima de la cabeza girando la palma al frente; la otra queda quieta.
3. Bájala y empuja enseguida con el otro brazo.', '[]'::jsonb, false),
    ('4c19dfba-8222-5ddd-85e0-030b8218b9cf', 'Remo alterno con kettlebells', 'Alternating Kettlebell Row', 'FREE_EXERCISE_DB', 'Alternating_Kettlebell_Row', 'Espalda', '["Bíceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Con las rodillas apenas flexionadas, inclina el torso y toma dos kettlebells.
2. Lleva una hacia el abdomen juntando el omóplato.
3. Bájala y repite con el otro brazo.', '[]'::jsonb, false),
    ('adfbec52-905f-5579-b9ca-34a85b9a8795', 'Remo renegado alterno', 'Alternating Renegade Row', 'FREE_EXERCISE_DB', 'Alternating_Renegade_Row', 'Espalda', '["Abdomen", "Bíceps", "Pecho", "Tríceps"]'::jsonb, 'KETTLEBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. En posición de flexión, apoyado sobre dos kettlebells con el cuerpo recto.
2. Empuja una kettlebell contra el suelo y rema con la otra hacia el costado.
3. Bájala y alterna sin girar la cadera.', '[]'::jsonb, false),
    ('c202b689-4851-59f5-815c-c0499bbb934a', 'Círculos de tobillo', 'Ankle Circles', 'FREE_EXERCISE_DB', 'Ankle_Circles', 'Pantorrillas', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'PULL', '1. Sostente de un soporte y levanta un pie unos centímetros del suelo.
2. Dibuja círculos grandes con la punta del pie, respirando normal.
3. Repite en ambos sentidos y cambia de pie.', '[]'::jsonb, false),
    ('46fbe3b1-39dd-507f-9986-9b2fafab2692', 'Estiramiento de glúteo tobillo sobre rodilla', 'Ankle On The Knee', 'FREE_EXERCISE_DB', 'Ankle_On_The_Knee', 'Glúteos', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Boca arriba con las rodillas flexionadas, apoya un tobillo sobre la rodilla contraria.
2. Toma el muslo de abajo y lleva ambas piernas hacia el pecho.
3. Sostén 10 a 20 s y cambia de lado.', '[]'::jsonb, false),
    ('3be53bcd-89e1-5ad6-900e-2baf53d53c48', 'Liberación del tibial anterior', 'Anterior Tibialis-SMR', 'FREE_EXERCISE_DB', 'Anterior_Tibialis-SMR', 'Pantorrillas', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado con las rodillas flexionadas, presiona la parte externa de la tibia con un rodillo.
2. Recorre desde debajo de la rodilla hasta el tobillo, deteniéndote 10 a 30 s en los puntos tensos.
3. Repite en la otra pierna.', '[]'::jsonb, false),
    ('16ef7cb0-f9f3-5980-bc88-ba9ce4e33c15', 'Press antigravedad con barra', 'Anti-Gravity Press', 'FREE_EXERCISE_DB', 'Anti-Gravity_Press', 'Hombros', '["Espalda", "Trapecio", "Tríceps"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Boca abajo en un banco inclinado, toma la barra con agarre prono y llévala cerca del pecho.
2. Empuja la barra al frente extendiendo los codos, con los brazos paralelos al suelo.
3. Vuelve despacio y repite.', '[]'::jsonb, false),
    ('42c89f5b-9c54-5a09-8a01-dc69e9fcabda', 'Círculos de brazos', 'Arm Circles', 'FREE_EXERCISE_DB', 'Arm_Circles', 'Hombros', '["Trapecio"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'PUSH', '1. De pie con los brazos extendidos a los costados, paralelos al suelo.
2. Haz círculos pequeños durante unos diez segundos.
3. Cambia el sentido de giro.', '[]'::jsonb, false),
    ('f61d4e95-89ca-5402-815a-b86a206ba67b', 'Vuelta al mundo con mancuernas', 'Around The Worlds', 'FREE_EXERCISE_DB', 'Around_The_Worlds', 'Pecho', '["Hombros"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado en banco plano, con las mancuernas junto a los muslos y los codos apenas flexionados.
2. Llévalas en semicírculo hasta encima de la cabeza, con los brazos paralelos al suelo.
3. Vuelve por el mismo recorrido.', '[]'::jsonb, false),
    ('1b9ea6d2-b49c-52de-9614-98a0e7fdbd6d', 'Entrenador de piedra Atlas', 'Atlas Stone Trainer', 'FREE_EXERCISE_DB', 'Atlas_Stone_Trainer', 'Lumbares', '["Bíceps", "Antebrazos", "Glúteos", "Isquiotibiales", "Cuádriceps"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRONGMAN', 'Strongman', 'COMPOUND', 'PULL', '1. Abraza el implemento flexionando la cadera.
2. Súbelo por encima de las rodillas, apóyalo en los muslos y abrázalo contra el pecho.
3. Termina extendiendo la cadera hasta quedar de pie.', '["LOWER_BACK"]'::jsonb, false),
    ('2427a0fb-0516-5a05-874a-8a36813dadee', 'Piedras Atlas', 'Atlas Stones', 'FREE_EXERCISE_DB', 'Atlas_Stones', 'Lumbares', '["Abdomen", "Aductores", "Bíceps", "Pantorrillas", "Antebrazos", "Glúteos", "Isquiotibiales", "Espalda", "Cuádriceps", "Trapecio"]'::jsonb, 'OTHER', 'ADVANCED', 'STRONGMAN', 'Strongman', 'COMPOUND', 'PULL', '1. Con la piedra entre los pies, rodéala con los brazos y mete los dedos por debajo.
2. Despégala del suelo, apóyala en los muslos y llévala al pecho.
3. Extiende la cadera para subirla a la plataforma.', '["LOWER_BACK"]'::jsonb, false),
    ('c4b47c99-1b61-51af-b473-b313683668e3', 'Peso muerto con barra gruesa (axle)', 'Axle Deadlift', 'FREE_EXERCISE_DB', 'Axle_Deadlift', 'Lumbares', '["Antebrazos", "Glúteos", "Isquiotibiales", "Espalda", "Cuádriceps", "Trapecio"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRONGMAN', 'Strongman', 'COMPOUND', 'PULL', '1. Con la barra sobre el medio del pie, tómala al ancho de los hombros.
2. Baja la cadera hasta que las tibias toquen la barra, pecho arriba y espalda firme.
3. Empuja con los talones y extiende la cadera hasta quedar de pie.', '["LOWER_BACK"]'::jsonb, false),
    ('58dc9bbc-36f4-5094-891d-782cd8a39284', 'Aperturas posteriores con banda', 'Back Flyes - With Bands', 'FREE_EXERCISE_DB', 'Back_Flyes_-_With_Bands', 'Hombros', '["Espalda", "Tríceps"]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la banda anclada al frente, sostén los agarres con los brazos extendidos.
2. Abre los brazos hacia los costados y atrás, paralelos al suelo.
3. Vuelve despacio a la posición inicial.', '[]'::jsonb, false),
    ('c9d844b2-3934-5137-ae1d-69c5c65caf1f', 'Arrastre de trineo hacia atrás', 'Backward Drag', 'FREE_EXERCISE_DB', 'Backward_Drag', 'Cuádriceps', '["Pantorrillas", "Antebrazos", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'OTHER', 'BEGINNER', 'STRONGMAN', 'Strongman', 'COMPOUND', 'PULL', '1. Sostén las cuerdas del trineo cargado.
2. Inclínate hacia atrás y camina hacia atrás con pasos cortos y rápidos.
3. Recorre la distancia indicada.', '[]'::jsonb, false),
    ('9749f40d-52c9-5ddf-899d-2a769a53842b', 'Lanzamiento hacia atrás con balón medicinal', 'Backward Medicine Ball Throw', 'FREE_EXERCISE_DB', 'Backward_Medicine_Ball_Throw', 'Hombros', '[]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie, sostén el balón entre las piernas.
2. Haz una sentadilla y extiende el cuerpo con fuerza, lanzando el balón por encima de la cabeza hacia atrás.
3. Recupéralo y repite.', '[]'::jsonb, false),
    ('2b25a03a-29e3-51a8-8506-9b8a6b52e952', 'Tabla de equilibrio', 'Balance Board', 'FREE_EXERCISE_DB', 'Balance_Board', 'Pantorrillas', '["Isquiotibiales", "Cuádriceps"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', NULL, '1. Súbete a la tabla de equilibrio.
2. Mantén el equilibrio el tiempo que te propongas.', '[]'::jsonb, false),
    ('c05bb8b7-e863-54f1-bb01-3babae583003', 'Curl femoral en fitball', 'Ball Leg Curl', 'FREE_EXERCISE_DB', 'Ball_Leg_Curl', 'Isquiotibiales', '["Pantorrillas", "Glúteos"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca arriba con los talones sobre el fitball.
2. Sube la cadera y lleva el balón hacia ti flexionando las rodillas.
3. Pausa y vuelve a estirar las piernas.', '[]'::jsonb, false),
    ('8e896943-45c0-5e61-ae2b-4e5c26804e59', 'Dominadas asistidas con banda', 'Band Assisted Pull-Up', 'FREE_EXERCISE_DB', 'Band_Assisted_Pull-Up', 'Espalda', '["Abdomen", "Antebrazos"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', NULL, '1. Engancha la banda en la barra y apoya una rodilla en el lazo.
2. Tira hasta pasar la barbilla por encima de la barra, sin balancearte.
3. Baja con control.', '[]'::jsonb, false),
    ('2ff175e6-0f55-5a9e-8d58-7a622f5a79a0', 'Buenos días con banda', 'Band Good Morning', 'FREE_EXERCISE_DB', 'Band_Good_Morning', 'Isquiotibiales', '["Glúteos", "Lumbares"]'::jsonb, 'BAND', 'BEGINNER', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PULL', '1. Pisa la banda y pásala por detrás del cuello inclinado hacia adelante.
2. Con las piernas casi rectas, extiende la cadera hasta quedar erguido.
3. Baja sin redondear la espalda.', '["LOWER_BACK"]'::jsonb, false),
    ('c3344ef3-4492-5e1b-ab89-eb8421f3a776', 'Buenos días con banda desde atrás', 'Band Good Morning (Pull Through)', 'FREE_EXERCISE_DB', 'Band_Good_Morning_Pull_Through', 'Isquiotibiales', '["Glúteos", "Lumbares"]'::jsonb, 'BAND', 'BEGINNER', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PULL', '1. Ancla la banda a un poste y pásala por detrás del cuello, de espaldas al poste.
2. Inclínate llevando la cadera atrás con la espalda recta hasta unos 90°.
3. Empuja la cadera hacia adelante para volver a quedar de pie.', '["LOWER_BACK"]'::jsonb, false),
    ('76df3e75-03fa-502c-ae3f-4a64c63a23d8', 'Aducción de cadera con banda', 'Band Hip Adductions', 'FREE_EXERCISE_DB', 'Band_Hip_Adductions', 'Aductores', '[]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De costado al poste, pasa la banda por el tobillo de la pierna exterior.
2. Con la rodilla recta, mueve la pierna hacia el costado lo más que puedas.
3. Vuelve y repite; luego cambia de lado.', '["HIP"]'::jsonb, false),
    ('2db8a22e-0bef-5002-b343-260ffb464ace', 'Separación de banda', 'Band Pull Apart', 'FREE_EXERCISE_DB', 'Band_Pull_Apart', 'Hombros', '["Espalda", "Trapecio"]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sostén la banda frente a ti con los brazos extendidos.
2. Sepárala llevando las manos hacia los costados hasta el pecho, codos rectos.
3. Pausa y vuelve con control.', '[]'::jsonb, true),
    ('6e1349e6-10be-522b-ba29-1da639d08e20', 'Rompecráneos con banda', 'Band Skull Crusher', 'FREE_EXERCISE_DB', 'Band_Skull_Crusher', 'Tríceps', '[]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Acostado en un banco, toma la banda anclada atrás con los codos apuntando al techo.
2. Extiende los codos sin mover los brazos.
3. Pausa arriba y vuelve despacio.', '["ELBOW"]'::jsonb, false),
    ('b85ae535-00dd-5df6-b135-0786e9e85bc6', 'Rodada abdominal con barra', 'Barbell Ab Rollout', 'FREE_EXERCISE_DB', 'Barbell_Ab_Rollout', 'Abdomen', '["Lumbares", "Hombros"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. En posición de flexión, toma una barra con poco peso.
2. Rueda la barra hacia los pies con el abdomen firme.
3. Vuelve rodando a la posición inicial.', '[]'::jsonb, false),
    ('a97b2bcc-43f8-5890-96e0-3496344a570e', 'Rodada abdominal con barra de rodillas', 'Barbell Ab Rollout - On Knees', 'FREE_EXERCISE_DB', 'Barbell_Ab_Rollout_-_On_Knees', 'Abdomen', '["Lumbares", "Hombros"]'::jsonb, 'BARBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De rodillas, apoya la barra en el suelo frente a ti.
2. Rueda hacia adelante estirando el cuerpo sin tocar el suelo.
3. Pausa y vuelve contrayendo el abdomen.', '[]'::jsonb, false),
    ('15d16036-e316-5971-80e6-9f5c68613073', 'Curl con barra en banco inclinado', 'Barbell Curls Lying Against An Incline', 'FREE_EXERCISE_DB', 'Barbell_Curls_Lying_Against_An_Incline', 'Bíceps', '[]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Recostado en un banco inclinado, con la barra colgando de los brazos.
2. Sube la barra solo moviendo los antebrazos y aprieta los bíceps.
3. Baja despacio hasta abajo.', '["ELBOW", "WRIST"]'::jsonb, false),
    ('588bbb1a-e714-53e5-a9e6-f2de3326949a', 'Sentadilla profunda con barra', 'Barbell Full Squat', 'FREE_EXERCISE_DB', 'Barbell_Full_Squat', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra sobre la espalda alta, pies al ancho de los hombros.
2. Baja hasta que los isquiotibiales toquen las pantorrillas, pecho arriba.
3. Sube empujando con los talones.', '["KNEE"]'::jsonb, false),
    ('9a033483-ecbb-540b-85a6-9d8f38b5792d', 'Puente de glúteos con barra', 'Barbell Glute Bridge', 'FREE_EXERCISE_DB', 'Barbell_Glute_Bridge', 'Glúteos', '["Pantorrillas", "Isquiotibiales"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. Sentado en el suelo, coloca la barra sobre la cadera y acuéstate.
2. Empuja con los talones y sube la cadera apoyado en la espalda alta.
3. Baja con control.', '[]'::jsonb, false),
    ('863d9171-2b45-50e2-9656-a64dba7d7c74', 'Press de banca guillotina', 'Barbell Guillotine Bench Press', 'FREE_EXERCISE_DB', 'Barbell_Guillotine_Bench_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sostén la barra sobre el cuello con agarre medio y brazos extendidos.
2. Bájala despacio hasta unos centímetros del cuello.
3. Empújala de vuelta apretando el pecho.', '["SHOULDER"]'::jsonb, false),
    ('a23ea124-07f6-5482-8082-b5430e9b521d', 'Sentadilla hack con barra', 'Barbell Hack Squat', 'FREE_EXERCISE_DB', 'Barbell_Hack_Squat', 'Cuádriceps', '["Pantorrillas", "Antebrazos", "Isquiotibiales"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sostén la barra detrás de las piernas con los brazos extendidos.
2. Baja hasta que los muslos queden paralelos al suelo, espalda recta.
3. Sube empujando con los talones.', '["KNEE"]'::jsonb, false),
    ('f226123a-4b54-5310-937d-3b2a4a11d6ca', 'Elevación de hombros con barra en banco inclinado', 'Barbell Incline Shoulder Raise', 'FREE_EXERCISE_DB', 'Barbell_Incline_Shoulder_Raise', 'Hombros', '["Pecho"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Recostado en banco inclinado, sostén la barra con los brazos extendidos.
2. Sin flexionar los codos, separa los hombros del banco empujando la barra.
3. Vuelve despacio.', '[]'::jsonb, false),
    ('e0258609-d5cf-5ad0-a776-667c9fdc4af6', 'Zancada con barra', 'Barbell Lunge', 'FREE_EXERCISE_DB', 'Barbell_Lunge', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra sobre la espalda alta, de pie.
2. Da un paso al frente y baja hasta que la rodilla trasera casi toque el suelo.
3. Vuelve empujando con el talón delantero y alterna.', '["KNEE", "HIP", "ANKLE"]'::jsonb, false),
    ('954b06ee-2caa-5a0d-aedc-15a802e977ae', 'Remo al deltoide posterior con barra', 'Barbell Rear Delt Row', 'FREE_EXERCISE_DB', 'Barbell_Rear_Delt_Row', 'Hombros', '["Bíceps", "Espalda"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Inclinado con el torso paralelo al suelo y agarre ancho.
2. Tira la barra hacia el pecho con los codos abiertos hacia afuera.
3. Baja despacio.', '[]'::jsonb, false),
    ('94b21de4-3d46-58fb-97d8-7f6246bbfcf9', 'Rodada con barra desde banco', 'Barbell Rollout from Bench', 'FREE_EXERCISE_DB', 'Barbell_Rollout_from_Bench', 'Abdomen', '["Glúteos", "Isquiotibiales", "Espalda", "Hombros"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De rodillas sobre un banco, toma la barra en el suelo.
2. Rueda la barra hacia adelante extendiendo la cadera y los hombros, brazos rectos.
3. Vuelve a la posición inicial.', '[]'::jsonb, false),
    ('bee232ef-edec-5388-98d5-6dfd625a7735', 'Press de hombros con barra sentado', 'Barbell Shoulder Press', 'FREE_EXERCISE_DB', 'Barbell_Shoulder_Press', 'Hombros', '["Pecho", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sentado con respaldo, sostén la barra sobre la cabeza con agarre prono.
2. Bájala hasta los hombros.
3. Empújala de nuevo hacia arriba.', '["SHOULDER"]'::jsonb, false),
    ('6cda0e97-b890-53a4-9249-a5963adb9525', 'Encogimientos con barra', 'Barbell Shrug', 'FREE_EXERCISE_DB', 'Barbell_Shrug', 'Trapecio', '[]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie, sostén la barra frente a los muslos con agarre prono.
2. Sube los hombros lo más alto posible y sostén un segundo.
3. Baja despacio sin flexionar los codos.', '["NECK"]'::jsonb, true),
    ('15c20c57-dab6-5885-8310-f922bd015fd1', 'Encogimientos con barra por detrás', 'Barbell Shrug Behind The Back', 'FREE_EXERCISE_DB', 'Barbell_Shrug_Behind_The_Back', 'Trapecio', '["Antebrazos", "Espalda"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie, sostén la barra detrás del cuerpo con las palmas hacia atrás.
2. Sube los hombros y sostén un segundo.
3. Baja despacio con los brazos extendidos.', '["NECK"]'::jsonb, false),
    ('a796c485-50cf-52ac-ad87-7c41dc0f4b0e', 'Flexión lateral con barra', 'Barbell Side Bend', 'FREE_EXERCISE_DB', 'Barbell_Side_Bend', 'Abdomen', '["Lumbares"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie con la barra sobre la espalda alta.
2. Inclínate hacia un lado solo desde la cintura.
3. Vuelve al centro y repite hacia el otro lado.', '[]'::jsonb, false),
    ('a44507e1-2639-566e-9a11-78311d51fba1', 'Sentadilla lateral con barra', 'Barbell Side Split Squat', 'FREE_EXERCISE_DB', 'Barbell_Side_Split_Squat', 'Cuádriceps', '["Pantorrillas", "Isquiotibiales", "Lumbares"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra en la espalda y los pies muy separados, un pie girado hacia afuera.
2. Baja hacia ese lado flexionando la rodilla, la otra pierna casi recta.
3. Vuelve empujando con la pierna flexionada.', '["KNEE", "HIP", "ANKLE"]'::jsonb, false),
    ('d5db4e49-d6f1-58a0-844a-c3faa9792e3c', 'Sentadilla con barra a banco', 'Barbell Squat To A Bench', 'FREE_EXERCISE_DB', 'Barbell_Squat_To_A_Bench', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'BARBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con un banco detrás, lleva la barra sobre la espalda alta.
2. Baja llevando la cadera atrás hasta rozar el banco.
3. Sube empujando con los talones.', '["KNEE", "LOWER_BACK"]'::jsonb, false),
    ('8f637746-4ed1-5729-bd68-43476f2e84ef', 'Subidas al cajón con barra', 'Barbell Step Ups', 'FREE_EXERCISE_DB', 'Barbell_Step_Ups', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra en la espalda, frente a un cajón o banco.
2. Apoya un pie y sube extendiendo cadera y rodilla.
3. Baja y repite; alterna las piernas.', '["KNEE"]'::jsonb, false),
    ('a9e2f793-7661-5de9-8f13-eabdc3f7d7a1', 'Zancadas caminando con barra', 'Barbell Walking Lunge', 'FREE_EXERCISE_DB', 'Barbell_Walking_Lunge', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra sobre la espalda alta.
2. Da un paso largo y baja hasta que la rodilla trasera casi toque el suelo.
3. Sube y avanza con la otra pierna.', '["KNEE", "HIP", "ANKLE"]'::jsonb, false),
    ('a0d09b76-68aa-5bfa-9dd3-6c201207f5b2', 'Cuerdas de batalla', 'Battling Ropes', 'FREE_EXERCISE_DB', 'Battling_Ropes', 'Hombros', '["Pecho", "Antebrazos"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Toma un extremo de la cuerda con cada mano.
2. Sube y baja los brazos alternados lo más rápido que puedas, haciendo ondas.
3. Mantén el ritmo el tiempo indicado.', '[]'::jsonb, false),
    ('cf973308-6197-5e2e-ac7a-e74728a7a375', 'Arrastre de trineo en cuadrupedia', 'Bear Crawl Sled Drags', 'FREE_EXERCISE_DB', 'Bear_Crawl_Sled_Drags', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'STRONGMAN', 'Strongman', 'COMPOUND', 'PUSH', '1. Con el trineo enganchado a la cintura, apoya las manos en el suelo y la espalda plana.
2. Avanza empujando con las piernas, alternando.
3. Recorre la distancia indicada.', '[]'::jsonb, false),
    ('c05cd1cf-134b-5ad2-9a49-d8846ebdf59e', 'Estiramiento de pecho manos tras la nuca', 'Behind Head Chest Stretch', 'FREE_EXERCISE_DB', 'Behind_Head_Chest_Stretch', 'Pecho', '["Hombros"]'::jsonb, 'OTHER', 'ADVANCED', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Sentado, manos detrás de la cabeza y codos atrás; tu compañero los sostiene.
2. Empuja los codos hacia adelante 10 s mientras él lo impide.
3. Relaja y deja que lleve los codos atrás hasta donde sea cómodo.', '[]'::jsonb, false),
    ('032e9b51-330f-5535-afa6-8fad95d8c5e8', 'Fondos en banco', 'Bench Dips', 'FREE_EXERCISE_DB', 'Bench_Dips', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Apoya las manos en el borde de un banco detrás de ti, piernas al frente.
2. Baja flexionando los codos hasta unos 90°.
3. Sube extendiendo los brazos.', '["SHOULDER", "ELBOW"]'::jsonb, false),
    ('8e6a135e-956b-5b41-b411-1a72c83d855d', 'Salto sobre banco', 'Bench Jump', 'FREE_EXERCISE_DB', 'Bench_Jump', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie junto a un banco bajo.
2. Flexiona un poco las rodillas y salta por encima, impulsando con los brazos.
3. Aterriza suave, gira y salta de vuelta.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('080e7d6c-5ff3-5ffb-89de-25f59676e1b1', 'Press de banca de powerlifting', 'Bench Press - Powerlifting', 'FREE_EXERCISE_DB', 'Bench_Press_-_Powerlifting', 'Tríceps', '["Pecho", "Antebrazos", "Espalda", "Hombros"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. Acostado con la espalda arqueada, omóplatos juntos y pies firmes.
2. Saca la barra, bájala al pecho con control.
3. Empújala manteniendo la posición tensa.', '["SHOULDER"]'::jsonb, false),
    ('2b6b557c-b628-5ff9-be91-262b8278454c', 'Press de banca con banda', 'Bench Press - With Bands', 'FREE_EXERCISE_DB', 'Bench_Press_-_With_Bands', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado en banco, con la banda anclada debajo y un agarre en cada mano.
2. Empuja los agarres hacia arriba al ancho de los hombros.
3. Baja hasta que los codos formen 90°.', '["SHOULDER"]'::jsonb, false),
    ('16590a83-923c-523f-8a32-49c08e9e0f18', 'Press de banca con cadenas', 'Bench Press with Chains', 'FREE_EXERCISE_DB', 'Bench_Press_with_Chains', 'Tríceps', '["Pecho", "Espalda", "Hombros"]'::jsonb, 'BARBELL', 'ADVANCED', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. Coloca las cadenas en los extremos de la barra.
2. Acostado y bien firme, baja la barra al pecho.
3. Empújala; la carga aumenta al subir.', '["SHOULDER"]'::jsonb, false),
    ('daa5bb1e-a1a0-503f-acff-ee2a754ed090', 'Sprint en banco', 'Bench Sprint', 'FREE_EXERCISE_DB', 'Bench_Sprint', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie con un pie apoyado sobre un banco.
2. Impúlsate con ese pie y cambia de pierna en el aire.
3. Alterna rápido hasta completar la serie.', '["ANKLE"]'::jsonb, false),
    ('4e672ae3-1ea1-5526-aaab-3341ce57948a', 'Pullover con barra y brazos flexionados', 'Bent-Arm Barbell Pullover', 'FREE_EXERCISE_DB', 'Bent-Arm_Barbell_Pullover', 'Espalda', '["Pecho", "Hombros", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Acostado en banco plano, sostén la barra sobre el pecho con los codos algo flexionados.
2. Llévala en arco por detrás de la cabeza hasta sentir el estiramiento.
3. Vuelve por el mismo arco.', '["SHOULDER"]'::jsonb, false),
    ('0f95e29f-7f5b-5c16-80cb-f41cf132051e', 'Elevación de cadera con rodillas flexionadas', 'Bent-Knee Hip Raise', 'FREE_EXERCISE_DB', 'Bent-Knee_Hip_Raise', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Boca arriba con los brazos a los costados y las rodillas flexionadas.
2. Lleva las rodillas al pecho y despega la cadera del suelo usando el abdomen.
3. Baja despacio.', '[]'::jsonb, false),
    ('3d9a1536-7794-5484-83b9-53493fffd7bf', 'Pájaros con la frente apoyada en banco', 'Bent Over Dumbbell Rear Delt Raise With Head On Bench', 'FREE_EXERCISE_DB', 'Bent_Over_Dumbbell_Rear_Delt_Raise_With_Head_On_Bench', 'Hombros', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Inclinado con la frente apoyada en un banco inclinado y las mancuernas colgando.
2. Abre los brazos hacia los costados con los codos apenas flexionados.
3. Baja con control.', '[]'::jsonb, false),
    ('30947986-2469-59d0-b56a-93411fa38c88', 'Elevación posterior a una mano en polea baja', 'Bent Over Low-Pulley Side Lateral', 'FREE_EXERCISE_DB', 'Bent_Over_Low-Pulley_Side_Lateral', 'Hombros', '["Lumbares", "Espalda", "Trapecio"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Inclinado con el torso casi paralelo al suelo, toma la polea baja con una mano.
2. Sube el brazo hacia el costado hasta la altura del hombro.
3. Baja despacio y cambia de lado.', '[]'::jsonb, false),
    ('a06adec2-0032-5838-bf2c-4aabffc7fb7f', 'Remo en barra T a una mano', 'Bent Over One-Arm Long Bar Row', 'FREE_EXERCISE_DB', 'Bent_Over_One-Arm_Long_Bar_Row', 'Espalda', '["Bíceps", "Lumbares", "Trapecio"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Fija un extremo de la barra y carga el otro; inclínate con la espalda recta.
2. Toma la barra con una mano y tira hacia el abdomen.
3. Baja con control y cambia de lado.', '["LOWER_BACK"]'::jsonb, false),
    ('b1b111c5-964e-55b7-ace9-a5e436e0e946', 'Remo en barra T', 'Bent Over Two-Arm Long Bar Row', 'FREE_EXERCISE_DB', 'Bent_Over_Two-Arm_Long_Bar_Row', 'Espalda', '["Bíceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Fija un extremo de la barra y carga el otro; inclínate con la espalda recta.
2. Toma la barra con ambas manos y tira hacia el pecho.
3. Baja con control.', '["LOWER_BACK"]'::jsonb, false),
    ('de7af26a-855d-5ea6-aba9-034cfd06f13e', 'Remo inclinado con mancuernas', 'Bent Over Two-Dumbbell Row', 'FREE_EXERCISE_DB', 'Bent_Over_Two-Dumbbell_Row', 'Espalda', '["Bíceps", "Hombros"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Inclinado con la espalda recta y una mancuerna en cada mano.
2. Lleva las mancuernas hacia los costados juntando los omóplatos.
3. Baja despacio.', '["LOWER_BACK"]'::jsonb, false),
    ('9b6878fa-a79c-57a8-965f-1f3ddb0b698e', 'Remo inclinado con mancuernas agarre neutro', 'Bent Over Two-Dumbbell Row With Palms In', 'FREE_EXERCISE_DB', 'Bent_Over_Two-Dumbbell_Row_With_Palms_In', 'Espalda', '["Bíceps"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Inclinado con la espalda recta y las palmas enfrentadas.
2. Lleva las mancuernas hacia los costados juntando los omóplatos.
3. Baja despacio.', '["LOWER_BACK"]'::jsonb, false),
    ('5ccad2e3-46cb-5658-b590-e084b6f68c16', 'Press inclinado lateral con kettlebell', 'Bent Press', 'FREE_EXERCISE_DB', 'Bent_Press', 'Abdomen', '["Glúteos", "Isquiotibiales", "Lumbares", "Cuádriceps", "Hombros", "Tríceps"]'::jsonb, 'KETTLEBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Lleva la kettlebell al hombro.
2. Inclínate hacia el lado opuesto hasta tocar el suelo mientras extiendes el brazo.
3. Vuelve a quedar de pie con la kettlebell arriba.', '[]'::jsonb, false),
    ('12965bc3-5521-5ce8-bdd0-4e22374d6d44', 'Ciclismo', 'Bicycling', 'FREE_EXERCISE_DB', 'Bicycling', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'CARDIO', 'Cardio', NULL, NULL, '1. Ajusta el asiento a tu altura.
2. Pedalea a un ritmo constante el tiempo indicado.', '[]'::jsonb, false),
    ('2d5fe958-b4bd-5f14-ba99-8dab7cf134d9', 'Bicicleta fija', 'Bicycling, Stationary', 'FREE_EXERCISE_DB', 'Bicycling_Stationary', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'MACHINE', 'BEGINNER', 'CARDIO', 'Cardio', NULL, NULL, '1. Ajusta el asiento a tu altura y elige un programa o el modo manual.
2. Pedalea ajustando la resistencia para mantener la intensidad.', '[]'::jsonb, false),
    ('75ea1133-c4ce-50b1-a8b9-04a39ca179b4', 'Press con tablas', 'Board Press', 'FREE_EXERCISE_DB', 'Board_Press', 'Tríceps', '["Pecho", "Antebrazos", "Espalda", "Hombros"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. Acostado y firme como en el press de banca, con tablas sobre el pecho.
2. Baja la barra hasta tocar las tablas.
3. Empújala de nuevo hacia arriba.', '[]'::jsonb, false),
    ('fdd27710-8920-58ee-b40f-489906c28e06', 'Plancha a extensión de brazos', 'Body-Up', 'FREE_EXERCISE_DB', 'Body-Up', 'Tríceps', '["Abdomen", "Antebrazos"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. En plancha sobre los antebrazos.
2. Empuja con las palmas y extiende los codos para subir el cuerpo.
3. Baja los antebrazos al suelo con control.', '[]'::jsonb, false),
    ('fc0e088f-efa6-55dd-a811-43e06dcc17ae', 'Extensión de tríceps con peso corporal', 'Body Tricep Press', 'FREE_EXERCISE_DB', 'Body_Tricep_Press', 'Tríceps', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Con una barra a la altura del pecho, tómala y aléjate uno o dos pasos con los brazos extendidos.
2. Flexiona los codos y baja hacia la barra.
3. Extiende los codos para volver.', '[]'::jsonb, false),
    ('1363cc12-9283-53a8-85aa-e029c438f57e', 'Aperturas con peso corporal sobre barras', 'Bodyweight Flyes', 'FREE_EXERCISE_DB', 'Bodyweight_Flyes', 'Pecho', '["Abdomen", "Hombros", "Tríceps"]'::jsonb, 'EZ_BAR', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. En posición de flexión, apoya las manos sobre dos barras Z que rueden.
2. Separa las barras despacio rodándolas hacia afuera.
3. Júntalas de nuevo apretando el pecho.', '[]'::jsonb, false),
    ('095d6fec-7a4f-59c4-a7eb-ec55e7d9637b', 'Remo invertido colgado', 'Bodyweight Mid Row', 'FREE_EXERCISE_DB', 'Bodyweight_Mid_Row', 'Espalda', '["Bíceps"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Cuelga de una barra con agarre prono y apoya las piernas sobre ella.
2. Tira hasta que las piernas toquen la barra, juntando los omóplatos.
3. Baja despacio.', '[]'::jsonb, false),
    ('4e8aba8d-8810-5b43-8d50-8d67aeed63b2', 'Sentadilla con peso corporal', 'Bodyweight Squat', 'FREE_EXERCISE_DB', 'Bodyweight_Squat', 'Cuádriceps', '["Glúteos", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie con los pies al ancho de los hombros, manos detrás de la cabeza.
2. Baja llevando la cadera atrás, con el pecho arriba y las rodillas hacia afuera.
3. Sube con fuerza.', '["KNEE"]'::jsonb, true),
    ('ddd924cc-fd6e-5d2c-996c-6289adc5016e', 'Zancadas caminando sin peso', 'Bodyweight Walking Lunge', 'FREE_EXERCISE_DB', 'Bodyweight_Walking_Lunge', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie con las manos en la cintura.
2. Da un paso largo y baja hasta que la rodilla trasera casi toque el suelo.
3. Sube y avanza con la otra pierna.', '["KNEE", "HIP", "ANKLE"]'::jsonb, false),
    ('83579a82-6520-580f-b6ff-a066c9c32e05', 'Crunch en polea sobre bosu con flexión lateral', 'Bosu Ball Cable Crunch With Side Bends', 'FREE_EXERCISE_DB', 'Bosu_Ball_Cable_Crunch_With_Side_Bends', 'Abdomen', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Acostado sobre un bosu frente a las poleas bajas, toma un agarre en cada mano.
2. Haz un crunch llevando las manos entre las rodillas.
3. Alterna con inclinaciones hacia cada lado.', '[]'::jsonb, false),
    ('4a7ad500-0e87-5b52-a140-b7b881d2a3dc', 'Cargada invertida con kettlebell', 'Bottoms-Up Clean From The Hang Position', 'FREE_EXERCISE_DB', 'Bottoms-Up_Clean_From_The_Hang_Position', 'Antebrazos', '["Bíceps", "Hombros"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De pie con una kettlebell en una mano.
2. Balancéala hacia atrás y súbela con fuerza al hombro, con la base hacia arriba.
3. Aprieta fuerte el asa para mantenerla estable.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('f525c1c0-8197-5432-a84c-4903b99b801c', 'Elevación de piernas y cadera', 'Bottoms Up', 'FREE_EXERCISE_DB', 'Bottoms_Up', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Boca arriba con las piernas rectas y los brazos a los costados.
2. Lleva las rodillas al pecho y luego extiende las piernas hacia el techo.
3. Despega la cadera del suelo, pausa y vuelve.', '[]'::jsonb, false),
    ('ec292cd9-765c-5cf9-b723-43cb59e137d3', 'Saltos al cajón continuos', 'Box Jump (Multiple Response)', 'FREE_EXERCISE_DB', 'Box_Jump_Multiple_Response', 'Isquiotibiales', '["Abductores", "Aductores", "Pantorrillas", "Glúteos", "Cuádriceps"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie frente al cajón, a un brazo de distancia.
2. Salta con ayuda de los brazos y cae con ambos pies sobre el cajón.
3. Baja y repite enseguida.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('ff494257-ed80-544f-b6ba-15091b01fd5a', 'Saltos entre cajones', 'Box Skip', 'FREE_EXERCISE_DB', 'Box_Skip', 'Isquiotibiales', '["Abductores", "Aductores", "Pantorrillas", "Glúteos", "Cuádriceps"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. Coloca varios cajones en fila separados unos dos metros.
2. Impúlsate con la pierna trasera hacia el primer cajón y salta hacia adelante desde él.
3. Cae entre los cajones y repite con el siguiente.', '[]'::jsonb, false),
    ('122cf04d-d480-5ede-84ab-2bb2a7fe2142', 'Sentadilla al cajón', 'Box Squat', 'FREE_EXERCISE_DB', 'Box_Squat', 'Cuádriceps', '["Aductores", "Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. En el rack, con un cajón detrás a la altura de una sentadilla paralela.
2. Baja llevando la cadera atrás hasta sentarte en el cajón.
3. Sube con fuerza sin rebotar.', '["KNEE"]'::jsonb, false),
    ('e33f8721-3e09-537c-8e01-390d7c9c1676', 'Sentadilla al cajón con bandas', 'Box Squat with Bands', 'FREE_EXERCISE_DB', 'Box_Squat_with_Bands', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'BARBELL', 'ADVANCED', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. Coloca bandas en la barra y un cajón detrás.
2. Baja hasta sentarte en el cajón.
3. Sube con fuerza contra la tensión de las bandas.', '["KNEE"]'::jsonb, false),
    ('c0665577-0edb-5d6e-9154-16052da689bd', 'Sentadilla al cajón con cadenas', 'Box Squat with Chains', 'FREE_EXERCISE_DB', 'Box_Squat_with_Chains', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'BARBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Coloca cadenas en la barra y un cajón detrás.
2. Baja hasta sentarte en el cajón.
3. Sube con fuerza; la carga aumenta al subir.', '["KNEE"]'::jsonb, false),
    ('c94409eb-6b91-5523-8ada-7e8d2bc19f30', 'Liberación del braquial con rodillo', 'Brachialis-SMR', 'FREE_EXERCISE_DB', 'Brachialis-SMR', 'Bíceps', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. De costado, apoya la parte externa del brazo sobre el rodillo.
2. Despega la cadera y carga el peso sobre el brazo.
3. Sostén 10 a 30 s y cambia de lado.', '[]'::jsonb, false),
    ('bda66c6d-98e3-5806-ab7f-2c50eff98df1', 'Press Bradford', 'Bradford/Rocky Presses', 'FREE_EXERCISE_DB', 'Bradford_Rocky_Presses', 'Hombros', '["Tríceps"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sentado, sostén la barra sobre la cabeza con agarre algo más ancho que los hombros.
2. Bájala por detrás de la cabeza y súbela.
3. Bájala por delante y alterna.', '["SHOULDER"]'::jsonb, false),
    ('977357c1-7bb3-50e9-9d3d-599dd4aae93e', 'Elevación de glúteos en plancha', 'Butt-Ups', 'FREE_EXERCISE_DB', 'Butt-Ups', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. En plancha sobre los antebrazos.
2. Sube los glúteos hacia el techo apretando el abdomen.
3. Vuelve despacio a la plancha.', '[]'::jsonb, false),
    ('dee1f483-0c04-55d6-aab3-6ac87bd08059', 'Puente de glúteos', 'Butt Lift (Bridge)', 'FREE_EXERCISE_DB', 'Butt_Lift_Bridge', 'Glúteos', '["Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Boca arriba con las rodillas flexionadas y los pies al ancho de los hombros.
2. Empuja con los talones y sube la cadera con la espalda recta.
3. Sostén un segundo y baja despacio.', '[]'::jsonb, false),
    ('f4bb623a-522c-5791-93fa-84d941879491', 'Press de pecho en polea', 'Cable Chest Press', 'FREE_EXERCISE_DB', 'Cable_Chest_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sentado, toma los agarres con los codos a 90° y el pecho arriba.
2. Empuja los agarres al frente hasta extender los brazos.
3. Vuelve despacio manteniendo la tensión.', '[]'::jsonb, false),
    ('561517eb-b6eb-5a91-988b-b69b11a2e886', 'Peso muerto en polea', 'Cable Deadlifts', 'FREE_EXERCISE_DB', 'Cable_Deadlifts', 'Cuádriceps', '["Antebrazos", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con las poleas abajo, colócate entre ellas.
2. Baja flexionando cadera y rodillas y toma los agarres.
3. Sube empujando con los talones, pecho arriba.', '["LOWER_BACK"]'::jsonb, false),
    ('2be505bf-47df-5a03-9395-cb36992ea975', 'Curl martillo en polea con cuerda', 'Cable Hammer Curls - Rope Attachment', 'FREE_EXERCISE_DB', 'Cable_Hammer_Curls_-_Rope_Attachment', 'Bíceps', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Frente a la polea baja, toma la cuerda con agarre neutro y codos pegados.
2. Sube la cuerda hacia los hombros moviendo solo los antebrazos.
3. Baja despacio.', '[]'::jsonb, false),
    ('20b7bcd6-4708-5fb9-8967-ff9de63a7182', 'Aducción de cadera en polea', 'Cable Hip Adduction', 'FREE_EXERCISE_DB', 'Cable_Hip_Adduction', 'Cuádriceps', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Con la tobillera enganchada a la polea baja, en la pierna más cercana.
2. Lleva la pierna hacia adentro, cruzando por delante de la otra.
3. Vuelve despacio y cambia de pierna.', '["HIP"]'::jsonb, false),
    ('a16c7564-dcb7-5513-ab82-e6d382c38a80', 'Pullover en polea en banco inclinado', 'Cable Incline Pushdown', 'FREE_EXERCISE_DB', 'Cable_Incline_Pushdown', 'Espalda', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. En un banco inclinado de espaldas a la polea alta, toma la barra sobre la cabeza.
2. Lleva la barra en arco hasta los muslos con los brazos casi rectos.
3. Vuelve despacio.', '[]'::jsonb, false),
    ('d91ebd41-4bd6-5d0c-8e2e-4485a07bfe05', 'Extensión de tríceps en polea en banco inclinado', 'Cable Incline Triceps Extension', 'FREE_EXERCISE_DB', 'Cable_Incline_Triceps_Extension', 'Tríceps', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. En banco inclinado de espaldas a la polea alta, toma la barra con agarre estrecho.
2. Extiende los codos sin mover los brazos.
3. Sostén un segundo y vuelve despacio.', '["ELBOW"]'::jsonb, false),
    ('1b889e50-7791-56aa-a087-0710a772aa23', 'Rotación interna de hombro en polea', 'Cable Internal Rotation', 'FREE_EXERCISE_DB', 'Cable_Internal_Rotation', 'Hombros', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De costado a la polea, codo pegado al cuerpo y flexionado a 90°.
2. Gira el antebrazo hacia el abdomen sin despegar el codo.
3. Vuelve despacio y cambia de lado.', '[]'::jsonb, false),
    ('dd7a3dda-7043-5a20-90b7-94813db4807e', 'Cruz de hierro en polea', 'Cable Iron Cross', 'FREE_EXERCISE_DB', 'Cable_Iron_Cross', 'Pecho', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Entre las poleas altas, con los brazos abiertos en T.
2. Lleva los brazos hacia los costados del cuerpo con los codos extendidos.
3. Pausa y vuelve despacio.', '[]'::jsonb, false),
    ('5c288fef-2c6f-539e-8dcd-fd80ee5d84ec', 'Lanzamiento de judo en polea', 'Cable Judo Flip', 'FREE_EXERCISE_DB', 'Cable_Judo_Flip', 'Abdomen', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De costado a la polea baja, toma la cuerda con ambas manos.
2. Gira el torso llevando la cuerda por encima del hombro y hacia abajo.
3. Vuelve y repite; luego cambia de lado.', '[]'::jsonb, false),
    ('e0ae15b9-0f60-5270-bcca-bb4360664542', 'Extensión de tríceps acostado en polea', 'Cable Lying Triceps Extension', 'FREE_EXERCISE_DB', 'Cable_Lying_Triceps_Extension', 'Tríceps', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Acostado en banco plano, toma la barra de la polea baja sobre el pecho.
2. Flexiona los codos hasta acercar la barra a la frente.
3. Extiende los codos para volver.', '["ELBOW"]'::jsonb, false),
    ('e0909f75-c349-5374-addb-08d3291a3c8e', 'Curl predicador en polea', 'Cable Preacher Curl', 'FREE_EXERCISE_DB', 'Cable_Preacher_Curl', 'Bíceps', '["Antebrazos"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. En un banco predicador frente a la polea baja, con los brazos apoyados.
2. Sube la barra hacia los hombros apretando los bíceps.
3. Baja despacio.', '[]'::jsonb, false),
    ('c3e07307-8c13-5b17-bb57-c3c1500a2281', 'Aperturas posteriores en polea', 'Cable Rear Delt Fly', 'FREE_EXERCISE_DB', 'Cable_Rear_Delt_Fly', 'Hombros', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Con las poleas altas, toma cada agarre con la mano contraria, cruzados al frente.
2. Abre los brazos hacia atrás y afuera, rectos.
3. Pausa y vuelve despacio.', '[]'::jsonb, false),
    ('fcc77a99-8d44-5489-b1e4-e9a3576a542b', 'Crunch inverso en polea', 'Cable Reverse Crunch', 'FREE_EXERCISE_DB', 'Cable_Reverse_Crunch', 'Abdomen', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Acostado con las tobilleras enganchadas a la polea baja y las rodillas a 90°.
2. Lleva las rodillas al pecho despegando la cadera.
3. Baja despacio.', '[]'::jsonb, false),
    ('f78578bd-6554-51dd-ba05-714028b9c55d', 'Remo al deltoide posterior en polea con cuerda', 'Cable Rope Rear-Delt Rows', 'FREE_EXERCISE_DB', 'Cable_Rope_Rear-Delt_Rows', 'Hombros', '["Bíceps", "Espalda"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Sentado en la polea baja, toma la cuerda con los codos abiertos.
2. Tira hacia el pecho alto con los codos arriba y afuera.
3. Vuelve despacio.', '[]'::jsonb, false),
    ('6d9f66f5-49b1-5aea-95af-7254ce38cdf2', 'Giros rusos en polea', 'Cable Russian Twists', 'FREE_EXERCISE_DB', 'Cable_Russian_Twists', 'Abdomen', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Acostado de costado a la polea sobre un fitball, sostén el agarre con los brazos extendidos.
2. Gira el torso alejando el agarre de la polea.
3. Vuelve despacio y cambia de lado.', '[]'::jsonb, false),
    ('9287651b-d9b7-52d8-afb6-ddb6f52b9a73', 'Crunch sentado en polea', 'Cable Seated Crunch', 'FREE_EXERCISE_DB', 'Cable_Seated_Crunch', 'Abdomen', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado de espaldas a la polea alta, sostén la cuerda junto a los hombros.
2. Flexiona la cintura llevando los codos hacia la cadera.
3. Vuelve despacio.', '[]'::jsonb, false),
    ('3392f5de-aaf7-5380-bd6f-3bcbf8be8217', 'Press de hombros en polea', 'Cable Shoulder Press', 'FREE_EXERCISE_DB', 'Cable_Shoulder_Press', 'Hombros', '["Tríceps"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con las poleas abajo, toma los agarres a la altura de los hombros.
2. Empuja ambos agarres por encima de la cabeza.
3. Pausa y vuelve despacio.', '["SHOULDER"]'::jsonb, false),
    ('e5945ae0-6353-5297-a904-38e53a0ab254', 'Encogimientos en polea', 'Cable Shrugs', 'FREE_EXERCISE_DB', 'Cable_Shrugs', 'Trapecio', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Toma la barra de la polea baja con los brazos extendidos.
2. Sube los hombros lo más alto posible y sostén un segundo.
3. Baja despacio.', '["NECK"]'::jsonb, false),
    ('bf7b4c57-1fb0-5bb4-87ce-9ddf3609d361', 'Curl de muñeca en polea', 'Cable Wrist Curl', 'FREE_EXERCISE_DB', 'Cable_Wrist_Curl', 'Antebrazos', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado frente a la polea baja, antebrazos sobre los muslos y palmas hacia arriba.
2. Sube la barra flexionando solo las muñecas.
3. Baja despacio.', '["WRIST"]'::jsonb, false),
    ('9d1349a1-a7d6-5658-8c9e-c126ab1a30b6', 'Encogimientos en máquina de pantorrillas', 'Calf-Machine Shoulder Shrug', 'FREE_EXERCISE_DB', 'Calf-Machine_Shoulder_Shrug', 'Trapecio', '[]'::jsonb, 'MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Colócate con las almohadillas sobre los hombros y los brazos relajados.
2. Sube los hombros hacia las orejas y sostén un segundo.
3. Baja despacio.', '["NECK"]'::jsonb, false),
    ('865d6789-1a5e-5850-bfcf-ea73c027e678', 'Press de pantorrillas en máquina', 'Calf Press', 'FREE_EXERCISE_DB', 'Calf_Press', 'Pantorrillas', '[]'::jsonb, 'MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Siéntate con las puntas de los pies en la plataforma y las piernas casi extendidas.
2. Empuja con las puntas lo más lejos posible.
3. Pausa y vuelve despacio.', '[]'::jsonb, false),
    ('c1fb2d7f-5241-5e1b-ab18-eb07675f0017', 'Pantorrillas en prensa', 'Calf Press On The Leg Press Machine', 'FREE_EXERCISE_DB', 'Calf_Press_On_The_Leg_Press_Machine', 'Pantorrillas', '[]'::jsonb, 'MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. En la prensa, apoya las puntas de los pies en el borde de la plataforma con las piernas extendidas.
2. Empuja con las puntas extendiendo los tobillos.
3. Vuelve despacio estirando la pantorrilla.', '["KNEE"]'::jsonb, false),
    ('1280e5e7-a34e-5fa5-a6de-efcf72be7d6d', 'Elevación de talones sobre mancuerna', 'Calf Raise On A Dumbbell', 'FREE_EXERCISE_DB', 'Calf_Raise_On_A_Dumbbell', 'Pantorrillas', '[]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Sostente de un soporte y párate sobre el mango de una mancuerna.
2. Sube en puntas de pie lo más alto posible.
3. Baja despacio estirando la pantorrilla.', '["ANKLE"]'::jsonb, false),
    ('1a3a8d7f-3076-5f72-89cc-0a5b5c311483', 'Elevación de talones con banda', 'Calf Raises - With Bands', 'FREE_EXERCISE_DB', 'Calf_Raises_-_With_Bands', 'Pantorrillas', '[]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Pisa la banda con las puntas y lleva los agarres a la altura de los hombros.
2. Sube en puntas de pie.
3. Baja despacio.', '["ANKLE"]'::jsonb, false),
    ('7fe337f9-73d4-5902-8523-29ea88b41b2d', 'Estiramiento de pantorrillas con antebrazos en la pared', 'Calf Stretch Elbows Against Wall', 'FREE_EXERCISE_DB', 'Calf_Stretch_Elbows_Against_Wall', 'Pantorrillas', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. De pie frente a la pared, apoya los antebrazos en ella.
2. Mantén los talones en el suelo.
3. Sostén 10 a 20 s.', '[]'::jsonb, false),
    ('860dcf1e-c979-507b-97eb-e7fdb7fe4497', 'Estiramiento de pantorrilla con manos en la pared', 'Calf Stretch Hands Against Wall', 'FREE_EXERCISE_DB', 'Calf_Stretch_Hands_Against_Wall', 'Pantorrillas', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Frente a la pared con un pie adelantado, apoya las manos en ella.
2. Mantén el talón de atrás en el suelo y el cuerpo en línea.
3. Sostén 10 a 20 s y cambia de lado.', '[]'::jsonb, false),
    ('3c50f440-7b16-523f-9761-75fba820049f', 'Liberación de pantorrillas con rodillo', 'Calves-SMR', 'FREE_EXERCISE_DB', 'Calves-SMR', 'Pantorrillas', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado, apoya la pantorrilla sobre el rodillo y despega la cadera.
2. Rueda desde debajo de la rodilla hasta el tobillo, deteniéndote 10 a 30 s en los puntos tensos.
3. Repite en la otra pierna.', '[]'::jsonb, false),
    ('ba9b5ed5-1a9d-5f08-8c24-5355d39662f4', 'Peso muerto de auto', 'Car Deadlift', 'FREE_EXERCISE_DB', 'Car_Deadlift', 'Cuádriceps', '["Antebrazos", "Glúteos", "Isquiotibiales", "Lumbares", "Trapecio"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRONGMAN', 'Strongman', 'COMPOUND', 'PULL', '1. Colócate entre los agarres del implemento con los pies al ancho de la cadera.
2. Baja la cadera, pecho arriba y espalda firme.
3. Sube empujando con los talones hasta quedar de pie.', '["LOWER_BACK"]'::jsonb, false),
    ('39b57d48-85d0-5fb2-9778-64cf1a92cc58', 'Volante con disco', 'Car Drivers', 'FREE_EXERCISE_DB', 'Car_Drivers', 'Hombros', '["Antebrazos"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie, sostén un disco con los brazos extendidos, como un volante.
2. Gíralo todo lo posible hacia un lado.
3. Gíralo hacia el otro lado y repite.', '[]'::jsonb, false),
    ('075e2e5f-749a-586d-8389-9ac9b740eeac', 'Carioca de pasos rápidos', 'Carioca Quick Step', 'FREE_EXERCISE_DB', 'Carioca_Quick_Step', 'Aductores', '["Abdomen", "Abductores", "Pantorrillas", "Glúteos", "Isquiotibiales", "Cuádriceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', NULL, NULL, '1. De pie en posición atlética.
2. Cruza un pie por detrás del otro rápido y sube la rodilla.
3. Desplázate de costado mirando al frente.', '[]'::jsonb, false),
    ('2f4807bc-5318-5794-8d9a-a914b7f8a8c6', 'Estiramiento del gato', 'Cat Stretch', 'FREE_EXERCISE_DB', 'Cat_Stretch', 'Lumbares', '["Espalda", "Trapecio"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. En cuatro apoyos.
2. Mete el abdomen y redondea la espalda dejando caer la cabeza.
3. Sostén 15 s.', '[]'::jsonb, false),
    ('421543bb-c5bd-57fc-8dd8-607cb578c8f1', 'Lanzamiento por encima de la cabeza con balón medicinal', 'Catch and Overhead Throw', 'FREE_EXERCISE_DB', 'Catch_and_Overhead_Throw', 'Espalda', '["Abdomen", "Pecho", "Hombros"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De frente a una pared o compañero, lleva el balón detrás de la cabeza.
2. Lánzalo con fuerza hacia adelante.
3. Recibe el rebote y repite.', '["SHOULDER"]'::jsonb, false),
    ('73aecb3c-03aa-5b27-9440-bac30a9f0c7e', 'Extensión de tríceps con cadenas', 'Chain Handle Extension', 'FREE_EXERCISE_DB', 'Chain_Handle_Extension', 'Tríceps', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'ISOLATION', 'PUSH', '1. Acostado en banco plano con cadenas sujetas a dos agarres, codos hacia el techo.
2. Extiende los codos sin mover los brazos.
3. Pausa arriba y baja despacio.', '[]'::jsonb, false),
    ('5786ec0f-80dd-5953-ae34-2340ab2f2928', 'Press con cadenas', 'Chain Press', 'FREE_EXERCISE_DB', 'Chain_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. Acostado en banco plano con cadenas sujetas a dos agarres, brazos extendidos.
2. Baja flexionando los codos hasta 90°.
3. Empuja de nuevo hasta extender.', '[]'::jsonb, false),
    ('bc5f4793-c0bb-5923-a12c-779f52afa559', 'Estiramiento de pierna en silla', 'Chair Leg Extended Stretch', 'FREE_EXERCISE_DB', 'Chair_Leg_Extended_Stretch', 'Isquiotibiales', '["Aductores"]'::jsonb, 'OTHER', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Sentado erguido, sujeta el asiento de los costados.
2. Extiende una pierna con el tobillo flexionado y llévala hacia afuera.
3. Vuelve al centro y cambia de pierna.', '[]'::jsonb, false),
    ('e642e4df-cf45-5d69-8471-205170dbaf67', 'Estiramiento lateral en silla', 'Chair Lower Back Stretch', 'FREE_EXERCISE_DB', 'Chair_Lower_Back_Stretch', 'Espalda', '["Lumbares"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Sentado erguido en una silla.
2. Inclínate hacia un lado con el brazo por encima de la cabeza.
3. Sostén 10 s y cambia de lado.', '[]'::jsonb, false),
    ('c33ce269-da49-553b-aecd-84f6aade6bbc', 'Sentadilla en máquina tipo silla', 'Chair Squat', 'FREE_EXERCISE_DB', 'Chair_Squat', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra de la máquina sobre los hombros, adelanta los pies.
2. Baja como si te sentaras, espalda neutra y mirada al frente.
3. Sube empujando con los talones.', '["KNEE"]'::jsonb, false),
    ('ed4f59aa-d1c4-5974-b56d-22b6d0eb06db', 'Estiramiento de tren superior en silla', 'Chair Upper Body Stretch', 'FREE_EXERCISE_DB', 'Chair_Upper_Body_Stretch', 'Hombros', '["Bíceps", "Pecho"]'::jsonb, 'OTHER', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado en el borde de una silla, sujeta el respaldo detrás de ti.
2. Con los brazos rectos, lleva el torso hacia adelante.
3. Sostén 20 a 30 s.', '[]'::jsonb, false),
    ('e375d59c-909b-5058-9f09-b26bdba31915', 'Estiramiento de pecho y hombro anterior', 'Chest And Front Of Shoulder Stretch', 'FREE_EXERCISE_DB', 'Chest_And_Front_Of_Shoulder_Stretch', 'Pecho', '["Hombros"]'::jsonb, 'OTHER', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. De pie, sostén un palo con agarre algo más ancho que los hombros.
2. Súbelo con cuidado por encima y detrás de la cabeza.
3. Sostén el estiramiento sin dolor.', '[]'::jsonb, false),
    ('cc07b7b6-66b0-50b8-88cb-942144f98a6d', 'Lanzamiento de pecho desde tres apoyos', 'Chest Push from 3 point stance', 'FREE_EXERCISE_DB', 'Chest_Push_from_3_point_stance', 'Pecho', '["Abdomen", "Hombros", "Tríceps"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. Agachado en tres apoyos con el balón medicinal delante.
2. Da un paso llevando el balón al pecho.
3. En el segundo paso, lánzalo al frente con toda la fuerza.', '[]'::jsonb, false),
    ('dab10aef-5e5b-5371-b08d-6e9c3c7d8380', 'Lanzamiento de pecho continuo', 'Chest Push (multiple response)', 'FREE_EXERCISE_DB', 'Chest_Push_multiple_response', 'Pecho', '["Abdomen", "Hombros", "Tríceps"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De rodillas frente a una pared, con el balón contra el pecho.
2. Lánzalo empujando con la cadera y los brazos.
3. Cae sobre las manos, vuelve a subir y repite.', '[]'::jsonb, false),
    ('c8db5ded-48a7-5aad-8714-19f6d0be69d6', 'Lanzamiento de pecho', 'Chest Push (single response)', 'FREE_EXERCISE_DB', 'Chest_Push_single_response', 'Pecho', '["Abdomen", "Hombros", "Tríceps"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De rodillas con el balón contra el pecho.
2. Lánzalo lo más lejos posible empujando con la cadera y los brazos.
3. Cae hacia adelante apoyando las manos.', '[]'::jsonb, false),
    ('b254cfa5-09a1-5e3b-820a-0b3e02e83d23', 'Lanzamiento de pecho con carrera', 'Chest Push with Run Release', 'FREE_EXERCISE_DB', 'Chest_Push_with_Run_Release', 'Pecho', '["Abdomen", "Hombros", "Tríceps"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. En posición atlética con el balón cerca de las piernas.
2. Da un paso llevándolo al pecho y en el segundo lánzalo al frente.
3. Sal corriendo unos 10 metros.', '[]'::jsonb, false),
    ('23527fa2-b444-57a2-aa3c-906f6e3a41fb', 'Estiramiento de pecho en fitball', 'Chest Stretch on Stability Ball', 'FREE_EXERCISE_DB', 'Chest_Stretch_on_Stability_Ball', 'Pecho', '[]'::jsonb, 'OTHER', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. En cuatro apoyos junto al fitball, apoya el codo encima con el brazo abierto.
2. Baja el torso hacia el suelo.
3. Sostén 20 a 30 s y cambia de brazo.', '[]'::jsonb, false),
    ('b5f7ec73-bf5b-5374-916f-027a80a3aa9f', 'Postura del niño', 'Child''s Pose', 'FREE_EXERCISE_DB', 'Childs_Pose', 'Lumbares', '["Glúteos", "Espalda"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. En cuatro apoyos, camina con las manos hacia adelante.
2. Siéntate sobre los talones y apoya la frente en el suelo.
3. Relaja y respira hacia la espalda.', '[]'::jsonb, false),
    ('1d19dcca-e925-5a67-9935-6bdb3a4eca9e', 'Estiramiento de cuello mentón al pecho', 'Chin To Chest Stretch', 'FREE_EXERCISE_DB', 'Chin_To_Chest_Stretch', 'Cuello', '["Trapecio"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado, entrelaza las manos detrás de la cabeza.
2. Lleva la cabeza hacia el pecho despacio.
3. Sostén 20 a 30 s.', '[]'::jsonb, false),
    ('3ba11c18-9107-5174-b5e5-94f54a165503', 'Mancuerna de circo', 'Circus Bell', 'FREE_EXERCISE_DB', 'Circus_Bell', 'Hombros', '["Antebrazos", "Glúteos", "Isquiotibiales", "Lumbares", "Trapecio", "Tríceps"]'::jsonb, 'OTHER', 'ADVANCED', 'STRONGMAN', 'Strongman', 'COMPOUND', 'PUSH', '1. Toma el mango grueso con ambas manos.
2. Llévala a un hombro extendiendo cadera y rodillas.
3. Flexiona las rodillas y empújala por encima de la cabeza.', '[]'::jsonb, false),
    ('be2c9191-d88b-5a34-adb9-7f03e361a84f', 'Cargada', 'Clean', 'FREE_EXERCISE_DB', 'Clean', 'Isquiotibiales', '["Pantorrillas", "Antebrazos", "Glúteos", "Lumbares", "Cuádriceps", "Hombros", "Trapecio"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con la barra junto a las tibias, espalda recta y pecho arriba.
2. Sube la barra extendiendo rodillas y cadera de forma explosiva.
3. Métete debajo y recíbela sobre los hombros.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('40bc8eb4-7afa-583b-9719-9bcd7a7b80d6', 'Peso muerto de cargada', 'Clean Deadlift', 'FREE_EXERCISE_DB', 'Clean_Deadlift', 'Isquiotibiales', '["Antebrazos", "Glúteos", "Lumbares", "Espalda", "Cuádriceps", "Trapecio"]'::jsonb, 'BARBELL', 'BEGINNER', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con la barra junto a las tibias y agarre al ancho de los hombros.
2. Empuja el suelo con los talones manteniendo el ángulo de la espalda.
3. Termina erguido con la barra en los muslos.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('ee11ae7d-d34f-5448-95ef-4a0648b55128', 'Tirón de cargada', 'Clean Pull', 'FREE_EXERCISE_DB', 'Clean_Pull', 'Cuádriceps', '["Antebrazos", "Glúteos", "Isquiotibiales", "Lumbares", "Trapecio"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. En posición de cargada, con la barra junto a las tibias.
2. Sube la barra y extiende cadera y rodillas de forma explosiva.
3. Termina con un encogimiento, sin recibir la barra.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('10373c46-056f-533c-bfd2-3f18600b28e8', 'Encogimiento de cargada', 'Clean Shrug', 'FREE_EXERCISE_DB', 'Clean_Shrug', 'Trapecio', '["Antebrazos", "Hombros"]'::jsonb, 'BARBELL', 'BEGINNER', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Sostén la barra a mitad del muslo, espalda recta e inclinada un poco al frente.
2. Encoge los hombros hacia las orejas con velocidad.', '["LOWER_BACK", "WRIST", "NECK"]'::jsonb, false),
    ('c016edc1-dabe-52a8-8cf9-7b315966f5d7', 'Cargada y envión', 'Clean and Jerk', 'FREE_EXERCISE_DB', 'Clean_and_Jerk', 'Hombros', '["Abdomen", "Glúteos", "Isquiotibiales", "Lumbares", "Cuádriceps", "Trapecio", "Tríceps"]'::jsonb, 'BARBELL', 'ADVANCED', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PUSH', '1. Haz una cargada y recibe la barra sobre los hombros.
2. Flexiona un poco las rodillas.
3. Empuja la barra por encima de la cabeza con un impulso de piernas.', '["LOWER_BACK", "SHOULDER", "WRIST"]'::jsonb, false),
    ('996e9bcb-9c5e-5e7b-b43c-72f860cd3211', 'Cargada y press', 'Clean and Press', 'FREE_EXERCISE_DB', 'Clean_and_Press', 'Hombros', '["Abdomen", "Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares", "Espalda", "Cuádriceps", "Trapecio", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Toma la barra con agarre algo más ancho que los hombros, espalda plana.
2. Súbela a los hombros con una cargada.
3. Empújala por encima de la cabeza y baja con control.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('1e7477a4-1f3d-5d1a-9e2b-d36e3b47a943', 'Cargada desde bloques', 'Clean from Blocks', 'FREE_EXERCISE_DB', 'Clean_from_Blocks', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Hombros", "Trapecio"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con la barra sobre bloques a la altura deseada.
2. Extiende cadera y rodillas de forma explosiva.
3. Recibe la barra sobre los hombros.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('d325c648-4984-5f75-99a3-c4dd18acc177', 'Flexiones de reloj', 'Clock Push-Up', 'FREE_EXERCISE_DB', 'Clock_Push-Up', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En posición de flexión.
2. Baja y empuja con fuerza para despegar las manos del suelo.
3. Aterriza desplazado hacia un lado y sigue girando como un reloj.', '["WRIST"]'::jsonb, false),
    ('e63bff90-884e-5704-9ce1-731895cd4a8d', 'Press estrecho con mancuerna', 'Close-Grip Dumbbell Press', 'FREE_EXERCISE_DB', 'Close-Grip_Dumbbell_Press', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con los hombros sobre un banco, sostén una mancuerna con ambas manos sobre el pecho.
2. Bájala al pecho con los codos pegados.
3. Empújala arriba usando los tríceps.', '[]'::jsonb, false),
    ('88e43b41-45c2-5c51-9d81-7697b3c6394b', 'Curl con barra Z agarre cerrado y banda', 'Close-Grip EZ-Bar Curl with Band', 'FREE_EXERCISE_DB', 'Close-Grip_EZ-Bar_Curl_with_Band', 'Bíceps', '["Antebrazos"]'::jsonb, 'EZ_BAR', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Con una banda en cada extremo de la barra, pisa el centro.
2. Sube la barra con agarre cerrado y los codos pegados.
3. Aprieta un segundo y baja despacio.', '[]'::jsonb, false),
    ('d5dd7c40-a28f-5940-bb34-2df956f17375', 'Press cerrado con barra Z', 'Close-Grip EZ-Bar Press', 'FREE_EXERCISE_DB', 'Close-Grip_EZ-Bar_Press', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'EZ_BAR', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado en banco plano, sostén la barra Z con agarre estrecho.
2. Bájala al pecho bajo con los codos pegados.
3. Empújala usando los tríceps.', '[]'::jsonb, false),
    ('172b47c4-b7ad-53e7-a7ab-9948095763fd', 'Curl con barra Z agarre cerrado', 'Close-Grip EZ Bar Curl', 'FREE_EXERCISE_DB', 'Close-Grip_EZ_Bar_Curl', 'Bíceps', '["Antebrazos"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie, toma la barra Z por el agarre interno con los codos pegados.
2. Sube la barra moviendo solo los antebrazos.
3. Aprieta un segundo y baja despacio.', '[]'::jsonb, false),
    ('060aa5c2-b1f1-52f9-b6f5-0bab174527e7', 'Flexiones cerradas sobre mancuerna', 'Close-Grip Push-Up off of a Dumbbell', 'FREE_EXERCISE_DB', 'Close-Grip_Push-Up_off_of_a_Dumbbell', 'Tríceps', '["Abdomen", "Pecho", "Hombros"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Apoya ambas manos sobre una mancuerna parada, cuerpo recto.
2. Baja con los codos pegados.
3. Empuja hasta extender los brazos.', '["WRIST"]'::jsonb, false),
    ('64891021-27e6-5547-82f5-98e90b5fa64d', 'Curl con barra agarre cerrado', 'Close-Grip Standing Barbell Curl', 'FREE_EXERCISE_DB', 'Close-Grip_Standing_Barbell_Curl', 'Bíceps', '["Antebrazos"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie, toma la barra con las manos juntas y las palmas arriba.
2. Sube la barra en semicírculo sin mover los codos.
3. Aprieta un segundo y baja despacio.', '["ELBOW", "WRIST"]'::jsonb, false),
    ('b0204ff5-aef8-57e3-b648-a64d6e602dc6', 'Capullo abdominal', 'Cocoons', 'FREE_EXERCISE_DB', 'Cocoons', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Boca arriba con las piernas rectas y los brazos detrás de la cabeza.
2. Lleva las rodillas al pecho y los brazos sobre ellas, despegando la cadera.
3. Pausa y vuelve a estirar.', '[]'::jsonb, false),
    ('c5c04bab-ffee-529c-aa17-2fdda3935721', 'Rueda de Conan', 'Conan''s Wheel', 'FREE_EXERCISE_DB', 'Conans_Wheel', 'Cuádriceps', '["Abdomen", "Bíceps", "Pantorrillas", "Antebrazos", "Lumbares", "Hombros", "Trapecio"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRONGMAN', 'Strongman', 'COMPOUND', NULL, '1. Sostén el extremo del implemento en el pliegue de los codos.
2. Levántalo y camina en círculo con pasos cortos y rápidos.
3. Mantén la postura erguida y respira.', '[]'::jsonb, false),
    ('28e2d543-6045-5f26-8ec2-8877ba30b78b', 'Curl concentrado', 'Concentration Curls', 'FREE_EXERCISE_DB', 'Concentration_Curls', 'Bíceps', '["Antebrazos"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado, apoya la parte de atrás del brazo en la cara interna del muslo con la mancuerna abajo.
2. Sube la mancuerna sin mover el brazo y aprieta el bíceps.
3. Baja despacio y cambia de brazo.', '[]'::jsonb, false),
    ('8f370415-c1a5-5822-aec8-1a6f5d54d8d5', 'Crunch cruzado', 'Cross-Body Crunch', 'FREE_EXERCISE_DB', 'Cross-Body_Crunch', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Boca arriba con las rodillas flexionadas y las manos detrás de la cabeza.
2. Lleva un codo hacia la rodilla contraria mientras la acercas.
3. Vuelve y alterna.', '[]'::jsonb, false),
    ('1cf4e3d6-35d2-5a15-a99d-57abf5592c58', 'Curl martillo cruzado', 'Cross Body Hammer Curl', 'FREE_EXERCISE_DB', 'Cross_Body_Hammer_Curl', 'Bíceps', '["Antebrazos"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie con una mancuerna en cada mano y las palmas hacia adentro.
2. Sube una mancuerna cruzando hacia el hombro contrario.
3. Baja por el mismo camino y alterna.', '[]'::jsonb, false),
    ('8d0491de-259c-50da-831e-3430e7660331', 'Cruce de pecho con banda', 'Cross Over - With Bands', 'FREE_EXERCISE_DB', 'Cross_Over_-_With_Bands', 'Pecho', '["Bíceps", "Hombros"]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De espaldas al poste donde está la banda, con los brazos abiertos en T.
2. Junta las manos frente al pecho con los codos apenas flexionados.
3. Vuelve despacio.', '[]'::jsonb, false),
    ('457832d0-369d-5de1-9c15-be53387146be', 'Zancada atrás con giro', 'Crossover Reverse Lunge', 'FREE_EXERCISE_DB', 'Crossover_Reverse_Lunge', 'Lumbares', '["Abdomen", "Abductores", "Glúteos", "Isquiotibiales", "Cuádriceps"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'PULL', '1. De pie con los pies al ancho de los hombros.
2. Da un paso atrás y gira el torso hacia la pierna de adelante.
3. Vuelve y alterna.', '[]'::jsonb, false),
    ('4ccdd051-185d-527c-8a16-28d774b17aa0', 'Crucifijo', 'Crucifix', 'FREE_EXERCISE_DB', 'Crucifix', 'Hombros', '["Antebrazos"]'::jsonb, 'OTHER', 'BEGINNER', 'STRONGMAN', 'Strongman', 'ISOLATION', 'STATIC', '1. De pie, sostén los implementos con los brazos abiertos y paralelos al suelo.
2. Mantén la posición el mayor tiempo posible.', '[]'::jsonb, false),
    ('8df0a4dc-5982-55fe-a9f5-ef1ff5cf67ca', 'Crunch con brazos extendidos', 'Crunch - Hands Overhead', 'FREE_EXERCISE_DB', 'Crunch_-_Hands_Overhead', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca arriba con las rodillas flexionadas y los brazos estirados sobre la cabeza.
2. Sube los omóplatos del suelo sin adelantar los brazos.
3. Sostén un segundo y baja despacio.', '["SHOULDER"]'::jsonb, false),
    ('4357c953-4927-5ed9-951d-718c9cbf3a7f', 'Crunch con piernas sobre fitball', 'Crunch - Legs On Exercise Ball', 'FREE_EXERCISE_DB', 'Crunch_-_Legs_On_Exercise_Ball', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca arriba con los pies sobre el fitball y las rodillas a 90°.
2. Despega los hombros presionando la zona lumbar contra el suelo.
3. Baja despacio.', '[]'::jsonb, false),
    ('9f8385c8-111a-5385-8dc4-bd7f3d21d568', 'Press cubano', 'Cuban Press', 'FREE_EXERCISE_DB', 'Cuban_Press', 'Hombros', '["Trapecio"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie, con los brazos paralelos al suelo y los antebrazos colgando.
2. Gira los hombros hasta que las muñecas queden sobre los codos.
3. Empuja las mancuernas arriba y vuelve en orden inverso.', '[]'::jsonb, false),
    ('c053c333-f50a-540f-8aa3-ba9c0603a47c', 'Estiramiento de bailarín', 'Dancer''s Stretch', 'FREE_EXERCISE_DB', 'Dancers_Stretch', 'Lumbares', '["Abductores", "Glúteos"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado, cruza una pierna flexionada sobre la otra extendida.
2. Gira el torso hacia la pierna flexionada apoyando el brazo contrario en ella.
3. Sostén 10 a 20 s y cambia de lado.', '[]'::jsonb, false),
    ('ce2e6b6d-9bf5-5eb2-b59f-3146edd4c6f3', 'Bicho muerto', 'Dead Bug', 'FREE_EXERCISE_DB', 'Dead_Bug', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Boca arriba, brazos al techo y caderas y rodillas a 90°, zona lumbar pegada al suelo.
2. Extiende una pierna y el brazo contrario sin despegar la espalda.
3. Vuelve y alterna.', '[]'::jsonb, false),
    ('06e1d454-859f-5128-a8ac-666f93216a92', 'Peso muerto con bandas', 'Deadlift with Bands', 'FREE_EXERCISE_DB', 'Deadlift_with_Bands', 'Lumbares', '["Antebrazos", "Glúteos", "Isquiotibiales", "Espalda", "Cuádriceps", "Trapecio"]'::jsonb, 'BARBELL', 'ADVANCED', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PULL', '1. Coloca las bandas en la barra y pisa sobre ellas.
2. Baja la cadera con el pecho arriba y la espalda firme.
3. Sube empujando con los talones contra la tensión.', '["LOWER_BACK"]'::jsonb, false),
    ('fd47fd4e-5133-5c3e-a46c-46bcf9ace10d', 'Peso muerto con cadenas', 'Deadlift with Chains', 'FREE_EXERCISE_DB', 'Deadlift_with_Chains', 'Lumbares', '["Antebrazos", "Glúteos", "Isquiotibiales", "Espalda", "Cuádriceps", "Trapecio"]'::jsonb, 'BARBELL', 'ADVANCED', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PULL', '1. Coloca las cadenas en la barra.
2. Con la barra sobre el medio del pie, baja la cadera, pecho arriba.
3. Sube empujando con los talones; la carga aumenta al subir.', '["LOWER_BACK"]'::jsonb, false),
    ('fd446133-42e9-529d-8ce3-40a3a43380a3', 'Press cerrado declinado a rompecráneos', 'Decline Close-Grip Bench To Skull Crusher', 'FREE_EXERCISE_DB', 'Decline_Close-Grip_Bench_To_Skull_Crusher', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En banco declinado, sostén la barra con agarre estrecho y codos adentro.
2. Bájala al pecho y empújala; luego llévala a la frente flexionando los codos.
3. Extiende los codos para volver.', '["ELBOW", "WRIST"]'::jsonb, false),
    ('602500ef-ebb7-5823-a158-4f04cc403a91', 'Crunch declinado', 'Decline Crunch', 'FREE_EXERCISE_DB', 'Decline_Crunch', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Acostado en banco declinado con las piernas sujetas y las manos junto a la cabeza.
2. Despega los hombros unos centímetros contrayendo el abdomen.
3. Baja despacio.', '[]'::jsonb, false),
    ('3d19e47d-043d-56dc-9349-aab8b650c176', 'Aperturas declinadas con mancuernas', 'Decline Dumbbell Flyes', 'FREE_EXERCISE_DB', 'Decline_Dumbbell_Flyes', 'Pecho', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En banco declinado, sostén las mancuernas sobre el pecho con las palmas enfrentadas.
2. Abre los brazos hacia los costados con los codos apenas flexionados.
3. Vuelve por el mismo arco.', '[]'::jsonb, false),
    ('a8be2819-92bd-57b8-91ef-917b566b154c', 'Extensión de tríceps declinada con mancuernas', 'Decline Dumbbell Triceps Extension', 'FREE_EXERCISE_DB', 'Decline_Dumbbell_Triceps_Extension', 'Tríceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. En banco declinado, sostén las mancuernas sobre el pecho.
2. Baja las mancuernas hacia la cabeza flexionando solo los codos.
3. Extiende los codos para volver.', '["ELBOW"]'::jsonb, false),
    ('872035db-e3fb-513c-8201-78cfa9e4fa79', 'Extensión de tríceps declinada con barra Z', 'Decline EZ Bar Triceps Extension', 'FREE_EXERCISE_DB', 'Decline_EZ_Bar_Triceps_Extension', 'Tríceps', '[]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. En banco declinado, sostén la barra Z sobre el pecho con agarre estrecho.
2. Bájala hacia la frente flexionando solo los codos.
3. Extiende los codos para volver.', '["ELBOW"]'::jsonb, false),
    ('480152b5-95fd-5d9c-b6a6-19be2f36502c', 'Crunch oblicuo declinado', 'Decline Oblique Crunch', 'FREE_EXERCISE_DB', 'Decline_Oblique_Crunch', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. En banco declinado, con el torso a unos 40° y una mano junto a la cabeza.
2. Sube girando el torso hasta llevar el codo a la rodilla contraria.
3. Baja despacio y cambia de lado.', '[]'::jsonb, false),
    ('2edc62e7-87ad-519b-83a7-13d3ed16cc92', 'Crunch inverso declinado', 'Decline Reverse Crunch', 'FREE_EXERCISE_DB', 'Decline_Reverse_Crunch', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Boca arriba en banco declinado, sujetándote del extremo superior.
2. Lleva las piernas hacia el torso despegando la cadera.
3. Baja despacio.', '[]'::jsonb, false),
    ('41c8a706-7a08-5002-943f-c5ea827728b1', 'Press declinado en máquina Smith', 'Decline Smith Press', 'FREE_EXERCISE_DB', 'Decline_Smith_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'SMITH_MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con un banco declinado bajo la máquina Smith, toma la barra con agarre ancho.
2. Bájala hasta rozar el pecho.
3. Empújala de nuevo hacia arriba.', '[]'::jsonb, false),
    ('9505a1cc-2b78-572d-bc68-bef70dc4f000', 'Peso muerto en déficit', 'Deficit Deadlift', 'FREE_EXERCISE_DB', 'Deficit_Deadlift', 'Lumbares', '["Antebrazos", "Glúteos", "Isquiotibiales", "Espalda", "Cuádriceps", "Trapecio"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PULL', '1. Párate sobre una plataforma baja con la barra sobre el medio del pie.
2. Baja la cadera hasta tomar la barra, pecho arriba.
3. Sube empujando con los talones.', '["LOWER_BACK"]'::jsonb, false),
    ('18f30cff-d94b-5ea7-9efe-426e14d32063', 'Salto en profundidad a cajón', 'Depth Jump Leap', 'FREE_EXERCISE_DB', 'Depth_Jump_Leap', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie sobre un cajón bajo con otro más alto delante.
2. Déjate caer y salta enseguida hacia el cajón alto.
3. Aterriza suave sobre él.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('4a7a5058-bd12-5fa3-87f9-2287a3c1e8a2', 'Fondos para pecho', 'Dips - Chest Version', 'FREE_EXERCISE_DB', 'Dips_-_Chest_Version', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sostente sobre las paralelas con los brazos extendidos.
2. Baja inclinando el torso unos 30° y con los codos algo abiertos.
3. Sube apretando el pecho.', '["SHOULDER", "ELBOW"]'::jsonb, false),
    ('786e7a69-5469-52b1-bf54-1a2f64cbd962', 'Cargada alterna con dos kettlebells', 'Double Kettlebell Alternating Hang Clean', 'FREE_EXERCISE_DB', 'Double_Kettlebell_Alternating_Hang_Clean', 'Isquiotibiales', '["Bíceps", "Pantorrillas", "Antebrazos", "Glúteos", "Lumbares", "Cuádriceps", "Trapecio"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con dos kettlebells entre los pies, lleva una al hombro.
2. En un solo movimiento baja esa y sube la otra.
3. Sigue alternando con fluidez.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('5c25a545-5167-55f0-a80a-5ab3ff2d10eb', 'Envión con dos kettlebells', 'Double Kettlebell Jerk', 'FREE_EXERCISE_DB', 'Double_Kettlebell_Jerk', 'Hombros', '["Pantorrillas", "Cuádriceps", "Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Lleva dos kettlebells a los hombros.
2. Flexiona un poco las rodillas y empuja con las piernas.
3. Bloquea las kettlebells arriba y vuelve a los hombros.', '["SHOULDER"]'::jsonb, false),
    ('d868991b-4140-55c9-bc2b-40d68b76390f', 'Push press con dos kettlebells', 'Double Kettlebell Push Press', 'FREE_EXERCISE_DB', 'Double_Kettlebell_Push_Press', 'Hombros', '["Pantorrillas", "Cuádriceps", "Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Lleva dos kettlebells a los hombros.
2. Flexiona un poco las rodillas y usa el impulso para subirlas por encima de la cabeza.
3. Bájalas a los hombros y repite.', '["SHOULDER"]'::jsonb, false),
    ('8905c4e6-fd65-5386-8e2f-544d66ccf3ce', 'Arrancada con dos kettlebells', 'Double Kettlebell Snatch', 'FREE_EXERCISE_DB', 'Double_Kettlebell_Snatch', 'Hombros', '["Glúteos", "Isquiotibiales", "Cuádriceps"]'::jsonb, 'KETTLEBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con dos kettlebells detrás de los pies, siéntate atrás para tomarlas.
2. Balancéalas entre las piernas.
3. Empuja con la cadera y súbelas por encima de la cabeza en un solo movimiento.', '["LOWER_BACK", "SHOULDER"]'::jsonb, false),
    ('40d995e1-8715-5d35-bae1-be9ec1365ea5', 'Molino con dos kettlebells', 'Double Kettlebell Windmill', 'FREE_EXERCISE_DB', 'Double_Kettlebell_Windmill', 'Abdomen', '["Glúteos", "Isquiotibiales", "Hombros", "Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', NULL, 'PULL', '1. Una kettlebell arriba bloqueada y otra frente al pie delantero.
2. Lleva la cadera hacia el lado de la de arriba e inclínate para tomar la de abajo.
3. Vuelve a quedar de pie.', '[]'::jsonb, false),
    ('d5c12f0e-38e1-58a9-8303-10350df8dc46', 'Salto con talones a los glúteos', 'Double Leg Butt Kick', 'FREE_EXERCISE_DB', 'Double_Leg_Butt_Kick', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie con las rodillas apenas flexionadas.
2. Salta lo más alto posible llevando los talones a los glúteos.
3. Aterriza amortiguando con las piernas.', '[]'::jsonb, false),
    ('71354d02-a1a2-5ad3-a606-a147e9d09c24', 'Equilibrio boca abajo en fitball', 'Downward Facing Balance', 'FREE_EXERCISE_DB', 'Downward_Facing_Balance', 'Glúteos', '["Abdomen", "Isquiotibiales"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'STATIC', '1. Boca abajo sobre el fitball.
2. Camina con las manos hacia adelante y levanta las piernas extendidas.
3. Mantén el equilibrio.', '[]'::jsonb, false),
    ('04448845-6bce-542a-b029-6a45d140d48d', 'Curl arrastrado', 'Drag Curl', 'FREE_EXERCISE_DB', 'Drag_Curl', 'Bíceps', '["Antebrazos"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De pie, toma la barra con las palmas al frente y los codos atrás.
2. Sube la barra arrastrándola pegada al torso, llevando los codos hacia atrás.
3. Baja por el mismo camino.', '[]'::jsonb, false),
    ('86837b6a-912b-5aff-8c76-4e9e1725f6e1', 'Flexión con caída', 'Drop Push', 'FREE_EXERCISE_DB', 'Drop_Push', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. En posición de flexión con las manos sobre dos cajones bajos.
2. Empuja y deja caer las manos al suelo al ancho de los hombros.
3. Amortigua la caída con los brazos.', '[]'::jsonb, false),
    ('9ec6d0b6-4aee-5035-bb76-6bec4b848e22', 'Press con mancuernas agarre neutro', 'Dumbbell Bench Press with Neutral Grip', 'FREE_EXERCISE_DB', 'Dumbbell_Bench_Press_with_Neutral_Grip', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado en banco plano, mancuernas arriba con las palmas enfrentadas.
2. Baja las mancuernas a los costados del torso.
3. Pausa y empuja de nuevo.', '["SHOULDER"]'::jsonb, false),
    ('9ece0cd0-7edf-5bf8-aa1f-202828c0644e', 'Curl de bíceps con mancuernas', 'Dumbbell Bicep Curl', 'FREE_EXERCISE_DB', 'Dumbbell_Bicep_Curl', 'Bíceps', '["Antebrazos"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie con las mancuernas a los lados, palmas al frente y codos pegados.
2. Sube las mancuernas hasta los hombros sin mover los brazos.
3. Aprieta y baja despacio.', '[]'::jsonb, false),
    ('7324e4f1-0886-58db-b9ff-5c49477263a9', 'Cargada con mancuernas', 'Dumbbell Clean', 'FREE_EXERCISE_DB', 'Dumbbell_Clean', 'Isquiotibiales', '["Pantorrillas", "Antebrazos", "Glúteos", "Lumbares", "Cuádriceps", "Hombros", "Trapecio"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con las mancuernas en el suelo, cadera atrás y espalda recta.
2. Salta extendiendo cadera, rodillas y tobillos.
3. Recibe las mancuernas sobre los hombros flexionando las rodillas.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('6455d80a-1ba5-5f05-bac8-d468fba35153', 'Press en el suelo con mancuernas', 'Dumbbell Floor Press', 'FREE_EXERCISE_DB', 'Dumbbell_Floor_Press', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. Acostado en el suelo con las mancuernas arriba.
2. Baja hasta que los brazos toquen el suelo.
3. Pausa y empuja de nuevo.', '[]'::jsonb, false),
    ('e4d92f23-0561-55a5-8f68-0bf839b59b5b', 'Remo con mancuernas en banco inclinado', 'Dumbbell Incline Row', 'FREE_EXERCISE_DB', 'Dumbbell_Incline_Row', 'Espalda', '["Bíceps", "Antebrazos", "Hombros"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Boca abajo en un banco inclinado, con las mancuernas colgando.
2. Junta los omóplatos y lleva las mancuernas hacia los costados.
3. Pausa y baja.', '[]'::jsonb, false),
    ('5226ce42-82a1-5ebe-b856-4dac93c0af04', 'Elevación de hombros con mancuernas en banco inclinado', 'Dumbbell Incline Shoulder Raise', 'FREE_EXERCISE_DB', 'Dumbbell_Incline_Shoulder_Raise', 'Hombros', '["Tríceps"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Recostado en banco inclinado con las mancuernas arriba.
2. Con los brazos rectos, separa los hombros del banco.
3. Baja despacio.', '[]'::jsonb, false),
    ('3ee23bf5-ea60-5dcb-8048-de245d654771', 'Elevación posterior a una mano acostado', 'Dumbbell Lying One-Arm Rear Lateral Raise', 'FREE_EXERCISE_DB', 'Dumbbell_Lying_One-Arm_Rear_Lateral_Raise', 'Hombros', '["Espalda"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca abajo en un banco algo inclinado, con una mancuerna colgando.
2. Sube el brazo hacia el costado hasta la altura del hombro.
3. Baja despacio y cambia de brazo.', '[]'::jsonb, false),
    ('90a94d2d-6bfd-5eb4-bcca-4f4a6bd17a75', 'Pronación con mancuerna acostado', 'Dumbbell Lying Pronation', 'FREE_EXERCISE_DB', 'Dumbbell_Lying_Pronation', 'Antebrazos', '[]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca abajo en un banco, brazo a 90° con el antebrazo vertical.
2. Gira el antebrazo hacia adentro con la mancuerna.
3. Vuelve despacio.', '[]'::jsonb, false),
    ('5b4a359d-1147-50b9-93f8-9aaad05cf843', 'Pájaros acostado en banco inclinado', 'Dumbbell Lying Rear Lateral Raise', 'FREE_EXERCISE_DB', 'Dumbbell_Lying_Rear_Lateral_Raise', 'Hombros', '[]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca abajo en un banco algo inclinado, con una mancuerna en cada mano.
2. Abre los brazos hacia los costados hasta la altura de los hombros.
3. Baja despacio.', '[]'::jsonb, false),
    ('0debe539-41fa-53b0-945c-e272f5a80682', 'Supinación con mancuerna acostado', 'Dumbbell Lying Supination', 'FREE_EXERCISE_DB', 'Dumbbell_Lying_Supination', 'Antebrazos', '[]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De costado en un banco, codo a 90° y antebrazo al frente.
2. Gira el antebrazo hacia afuera con la mancuerna.
3. Vuelve despacio.', '[]'::jsonb, false),
    ('fff04e27-acbb-5cf9-b543-757609035814', 'Press de hombros con mancuerna a una mano', 'Dumbbell One-Arm Shoulder Press', 'FREE_EXERCISE_DB', 'Dumbbell_One-Arm_Shoulder_Press', 'Hombros', '["Tríceps"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sentado o de pie con la mancuerna a la altura del hombro.
2. Empújala hasta extender el brazo.
3. Baja despacio y cambia de lado.', '["SHOULDER"]'::jsonb, false),
    ('b7b95564-2252-59df-89ff-a134b3955800', 'Remo al mentón con mancuerna a una mano', 'Dumbbell One-Arm Upright Row', 'FREE_EXERCISE_DB', 'Dumbbell_One-Arm_Upright_Row', 'Hombros', '["Bíceps", "Trapecio"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De pie con la mancuerna frente al muslo.
2. Súbela pegada al cuerpo hasta el pecho, codo arriba.
3. Baja despacio y cambia de lado.', '["SHOULDER"]'::jsonb, false),
    ('3464c621-181b-581d-8f04-cc9d8b7fd5cb', 'Curl boca abajo en banco inclinado', 'Dumbbell Prone Incline Curl', 'FREE_EXERCISE_DB', 'Dumbbell_Prone_Incline_Curl', 'Bíceps', '[]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca abajo en un banco inclinado, con los brazos colgando y las palmas al frente.
2. Sube las mancuernas contrayendo los bíceps.
3. Baja despacio.', '[]'::jsonb, false),
    ('08935040-6671-54ce-9c63-cf860bf9c789', 'Elevación de mancuernas al costado', 'Dumbbell Raise', 'FREE_EXERCISE_DB', 'Dumbbell_Raise', 'Hombros', '["Bíceps"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De pie con las mancuernas junto a los muslos.
2. Súbelas a los costados guiando con los codos hasta casi la altura del mentón.
3. Baja despacio.', '[]'::jsonb, false),
    ('4e6eb6cb-f370-5acf-883c-b2d9476d69cd', 'Zancada atrás con mancuernas', 'Dumbbell Rear Lunge', 'FREE_EXERCISE_DB', 'Dumbbell_Rear_Lunge', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie con una mancuerna en cada mano.
2. Da un paso atrás y baja con el torso erguido.
3. Vuelve y alterna.', '["KNEE", "HIP", "ANKLE"]'::jsonb, false),
    ('fe82c7e0-5b9f-54eb-90c1-37a7658adf5e', 'Elevación en plano escapular', 'Dumbbell Scaption', 'FREE_EXERCISE_DB', 'Dumbbell_Scaption', 'Hombros', '["Trapecio"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie con mancuernas livianas y los pulgares hacia arriba.
2. Sube los brazos al frente, unos 30° hacia afuera, hasta la altura de los hombros.
3. Baja despacio.', '[]'::jsonb, false),
    ('eb2d31e8-2942-596e-abe9-7a94d6a19d96', 'Salto al cajón desde sentado con mancuerna', 'Dumbbell Seated Box Jump', 'FREE_EXERCISE_DB', 'Dumbbell_Seated_Box_Jump', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. Sentado en un banco frente al cajón, con una mancuerna contra el pecho.
2. Inclínate y salta hacia el cajón.
3. Aterriza suave, baja y vuelve a sentarte.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('3ef59ccd-c4c3-59c0-b661-4905ab457790', 'Elevación de talón sentado a una pierna con mancuerna', 'Dumbbell Seated One-Leg Calf Raise', 'FREE_EXERCISE_DB', 'Dumbbell_Seated_One-Leg_Calf_Raise', 'Pantorrillas', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Sentado, con la punta del pie sobre un bloque y la mancuerna sobre el muslo.
2. Sube el talón lo más alto posible.
3. Baja estirando y cambia de pierna.', '["ANKLE"]'::jsonb, false),
    ('0e287c3e-14df-506a-af34-8ab9c69cdc19', 'Press de hombros con mancuernas', 'Dumbbell Shoulder Press', 'FREE_EXERCISE_DB', 'Dumbbell_Shoulder_Press', 'Hombros', '["Tríceps"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sentado con respaldo, mancuernas a la altura de los hombros y palmas al frente.
2. Empújalas hasta que se toquen arriba.
3. Baja despacio.', '["SHOULDER"]'::jsonb, false),
    ('9139e523-1251-5d8e-87c6-3e7aab696fbb', 'Flexión lateral con mancuerna', 'Dumbbell Side Bend', 'FREE_EXERCISE_DB', 'Dumbbell_Side_Bend', 'Abdomen', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie con una mancuerna en una mano.
2. Inclínate hacia el lado contrario solo desde la cintura.
3. Vuelve y cambia de lado.', '[]'::jsonb, false),
    ('a322e332-2377-5e91-b8e4-bfb37ab6965a', 'Sentadilla con mancuernas', 'Dumbbell Squat', 'FREE_EXERCISE_DB', 'Dumbbell_Squat', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie con una mancuerna en cada mano a los costados.
2. Baja con la espalda recta y la mirada al frente.
3. Sube empujando con los talones.', '["KNEE"]'::jsonb, false),
    ('1404b195-51d5-5b30-a3cc-c83bc6a61128', 'Sentadilla con mancuernas a banco', 'Dumbbell Squat To A Bench', 'FREE_EXERCISE_DB', 'Dumbbell_Squat_To_A_Bench', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie con un banco detrás y una mancuerna en cada mano.
2. Baja hasta rozar el banco.
3. Sube empujando con los talones.', '["KNEE"]'::jsonb, false),
    ('6af98da2-eb4f-5edc-b967-893ea40c77ab', 'Subidas al cajón con mancuernas', 'Dumbbell Step Ups', 'FREE_EXERCISE_DB', 'Dumbbell_Step_Ups', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie frente a un cajón con una mancuerna en cada mano.
2. Apoya un pie y sube extendiendo cadera y rodilla.
3. Baja y alterna las piernas.', '["KNEE"]'::jsonb, false),
    ('518d53d4-becc-58af-836d-3fa824618537', 'Extensión de tríceps acostado con mancuernas en pronación', 'Dumbbell Tricep Extension -Pronated Grip', 'FREE_EXERCISE_DB', 'Dumbbell_Tricep_Extension_-Pronated_Grip', 'Tríceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Acostado en banco plano con las mancuernas sobre los hombros, palmas al frente.
2. Baja las mancuernas cerca de las orejas sin mover los brazos.
3. Extiende los codos para volver.', '["ELBOW"]'::jsonb, false),
    ('2b62cd26-86ed-587b-8364-161972ae5b5e', 'Estiramiento dinámico de espalda', 'Dynamic Back Stretch', 'FREE_EXERCISE_DB', 'Dynamic_Back_Stretch', 'Espalda', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'PULL', '1. De pie con los brazos rectos.
2. Balancea los brazos al frente y arriba 5 a 10 veces, cada vez más alto.', '[]'::jsonb, false),
    ('f268aa1c-0f1f-5797-9ec9-4b3e3d935f4c', 'Estiramiento dinámico de pecho', 'Dynamic Chest Stretch', 'FREE_EXERCISE_DB', 'Dynamic_Chest_Stretch', 'Pecho', '["Espalda"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'PULL', '1. De pie con los brazos extendidos al frente.
2. Llévalos hacia atrás lo más que puedas y vuelve, como un aplauso exagerado.
3. Repite 5 a 10 veces aumentando la velocidad.', '[]'::jsonb, false),
    ('10a94a09-56bc-5bd7-b6e0-5eaed402a52f', 'Círculos de codos', 'Elbow Circles', 'FREE_EXERCISE_DB', 'Elbow_Circles', 'Hombros', '["Trapecio"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'PULL', '1. Con las manos en los hombros y los codos a la altura de los hombros.
2. Dibuja círculos lentos con los codos.', '[]'::jsonb, false),
    ('be9ce46b-803a-5974-9457-01d22a6f3268', 'Codo a rodilla', 'Elbow to Knee', 'FREE_EXERCISE_DB', 'Elbow_to_Knee', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Boca arriba con una pierna cruzada sobre la rodilla contraria y las manos detrás de la cabeza.
2. Gira el torso llevando el codo a la rodilla cruzada.
3. Vuelve y, al terminar, cambia de lado.', '[]'::jsonb, false),
    ('4a9784a6-f315-5af0-9e6e-5494432ff89c', 'Codos atrás', 'Elbows Back', 'FREE_EXERCISE_DB', 'Elbows_Back', 'Pecho', '["Hombros"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. De pie con las manos en la zona lumbar y los codos hacia afuera.
2. Lleva los codos hacia atrás intentando juntarlos.', '[]'::jsonb, false),
    ('e2b79844-886a-5118-99f3-ea3d88d160b3', 'Zancada atrás desde plataforma con barra', 'Elevated Back Lunge', 'FREE_EXERCISE_DB', 'Elevated_Back_Lunge', 'Cuádriceps', '["Glúteos", "Isquiotibiales"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra en la espalda, párate sobre una plataforma baja.
2. Da un paso atrás y baja hasta que la rodilla toque el suelo.
3. Vuelve a la plataforma y alterna.', '["KNEE", "HIP", "ANKLE"]'::jsonb, false),
    ('42a8c991-c273-5aa3-88ab-0d2edc3e5cf9', 'Remo en polea sentado elevado', 'Elevated Cable Rows', 'FREE_EXERCISE_DB', 'Elevated_Cable_Rows', 'Espalda', '["Trapecio"]'::jsonb, 'CABLE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Sentado sobre una plataforma en la máquina de remo, toma el agarre en V.
2. Tira hacia el abdomen juntando los omóplatos.
3. Vuelve estirando los brazos.', '[]'::jsonb, false),
    ('99384fb5-7f7a-5ef3-afaa-28b8d0683799', 'Elíptica', 'Elliptical Trainer', 'FREE_EXERCISE_DB', 'Elliptical_Trainer', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'MACHINE', 'INTERMEDIATE', 'CARDIO', 'Cardio', NULL, NULL, '1. Súbete a la elíptica y elige un programa o el modo manual.
2. Mantén un ritmo constante ajustando la inclinación según la intensidad buscada.', '[]'::jsonb, false),
    ('cd0ef9fa-150f-57bf-bf80-b432917dbc4e', 'Crunch en fitball', 'Exercise Ball Crunch', 'FREE_EXERCISE_DB', 'Exercise_Ball_Crunch', 'Abdomen', '[]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Recostado sobre el fitball con la zona lumbar apoyada y los pies firmes.
2. Sube el torso contrayendo el abdomen.
3. Baja hasta estirar.', '[]'::jsonb, false),
    ('25437e8b-ba53-5466-9b37-d5d3eefe1124', 'Recogimiento de rodillas en fitball', 'Exercise Ball Pull-In', 'FREE_EXERCISE_DB', 'Exercise_Ball_Pull-In', 'Abdomen', '[]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. En posición de flexión con las tibias sobre el fitball.
2. Lleva las rodillas al pecho rodando el balón.
3. Vuelve a estirar las piernas.', '[]'::jsonb, false),
    ('d69ec225-cd37-592d-9dde-5c943ac70cfb', 'Press en el suelo con kettlebell a una mano, rango extendido', 'Extended Range One-Arm Kettlebell Floor Press', 'FREE_EXERCISE_DB', 'Extended_Range_One-Arm_Kettlebell_Floor_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'KETTLEBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado en el suelo, kettlebell en una mano y la rodilla del mismo lado cruzada.
2. Empuja la kettlebell hacia arriba y hacia el centro del cuerpo.
3. Baja despacio y cambia de lado.', '[]'::jsonb, false),
    ('ac8a0618-85c7-5972-8b3e-8a6624491f58', 'Rotación externa con mancuerna', 'External Rotation', 'FREE_EXERCISE_DB', 'External_Rotation', 'Hombros', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De costado en un banco, codo pegado al cuerpo a 90°.
2. Gira el antebrazo hacia arriba sin mover el codo.
3. Baja despacio y cambia de lado.', '[]'::jsonb, false),
    ('0b412a1c-ccf8-5099-ad2e-2f897f11070d', 'Rotación externa con banda', 'External Rotation with Band', 'FREE_EXERCISE_DB', 'External_Rotation_with_Band', 'Hombros', '[]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De costado a la banda, codo pegado al cuerpo a 90°.
2. Gira el antebrazo hacia afuera sin despegar el codo.
3. Vuelve despacio y cambia de lado.', '[]'::jsonb, false),
    ('d11cbbbc-ae11-5def-a510-2732a431bf66', 'Rotación externa en polea', 'External Rotation with Cable', 'FREE_EXERCISE_DB', 'External_Rotation_with_Cable', 'Hombros', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De costado a la polea a la altura del codo, codo pegado a 90°.
2. Gira el antebrazo hacia afuera sin despegar el codo.
3. Vuelve despacio y cambia de lado.', '[]'::jsonb, false),
    ('514c2259-6475-5856-abe3-f21709ebeaca', 'Paseo del granjero', 'Farmer''s Walk', 'FREE_EXERCISE_DB', 'Farmers_Walk', 'Antebrazos', '["Abdomen", "Glúteos", "Isquiotibiales", "Lumbares", "Cuádriceps", "Trapecio"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRONGMAN', 'Strongman', 'COMPOUND', NULL, '1. Toma una carga pesada en cada mano.
2. Levántala con la espalda recta.
3. Camina con pasos cortos y rápidos la distancia indicada.', '[]'::jsonb, true),
    ('cf69665d-328d-566d-a738-425a58975ba5', 'Skipping rápido', 'Fast Skipping', 'FREE_EXERCISE_DB', 'Fast_Skipping', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie con una pierna un poco adelantada.
2. Salta alternando paso y rebote con cada pierna.
3. Mantén poco tiempo en el aire y ve lo más rápido posible.', '["ANKLE"]'::jsonb, false),
    ('3e2e27bb-e7a3-5d49-8a07-8f9870f9b99e', 'Curl de dedos con barra', 'Finger Curls', 'FREE_EXERCISE_DB', 'Finger_Curls', 'Antebrazos', '[]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado, sostén la barra con las palmas arriba.
2. Deja rodar la barra hasta la punta de los dedos.
3. Ciérralos y sube la barra flexionando las muñecas.', '[]'::jsonb, false),
    ('d10774f6-65c0-5271-9096-4cb5df3a3ecc', 'Aperturas en polea en banco plano', 'Flat Bench Cable Flyes', 'FREE_EXERCISE_DB', 'Flat_Bench_Cable_Flyes', 'Pecho', '[]'::jsonb, 'CABLE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Acostado en banco plano entre dos poleas bajas, con los brazos abiertos.
2. Junta los agarres sobre el pecho con los codos apenas flexionados.
3. Vuelve despacio.', '[]'::jsonb, false),
    ('5f0dc7d0-3845-5b5d-86c6-786b2230d489', 'Recogimiento de piernas en banco', 'Flat Bench Leg Pull-In', 'FREE_EXERCISE_DB', 'Flat_Bench_Leg_Pull-In', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Acostado con las piernas fuera del banco, sujetándote de los costados.
2. Lleva las rodillas hacia el pecho.
3. Vuelve a estirar despacio.', '[]'::jsonb, false),
    ('858b7745-835d-5b93-a4ea-307d88755008', 'Elevación de piernas acostado en banco', 'Flat Bench Lying Leg Raise', 'FREE_EXERCISE_DB', 'Flat_Bench_Lying_Leg_Raise', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Acostado con las piernas extendidas fuera del banco.
2. Sube las piernas casi rectas hasta 90°.
3. Baja despacio sin tocar el suelo.', '["HIP"]'::jsonb, false),
    ('eda58acc-4330-548f-847e-7415bd28e9d5', 'Curl inclinado con muñecas atrás', 'Flexor Incline Dumbbell Curls', 'FREE_EXERCISE_DB', 'Flexor_Incline_Dumbbell_Curls', 'Bíceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado en banco inclinado a 45° con las mancuernas colgando.
2. Sube las mancuernas manteniendo las muñecas hacia atrás.
3. Baja despacio.', '[]'::jsonb, false),
    ('e38ee885-2c28-5b2b-a50c-2b22a0ff14a5', 'Curl nórdico en el suelo', 'Floor Glute-Ham Raise', 'FREE_EXERCISE_DB', 'Floor_Glute-Ham_Raise', 'Isquiotibiales', '["Pantorrillas", "Glúteos"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De rodillas con los pies sujetos y el torso erguido.
2. Baja hacia adelante extendiendo las rodillas sin doblar la cadera.
3. Apóyate con las manos y vuelve usando los isquiotibiales.', '[]'::jsonb, false),
    ('d633cc7d-47d4-5eae-ade9-c232ce7d3ab4', 'Press en el suelo con barra', 'Floor Press', 'FREE_EXERCISE_DB', 'Floor_Press', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. Acostado en el suelo dentro del rack, saca la barra.
2. Bájala hasta que los brazos toquen el suelo, codos pegados.
3. Pausa y empuja.', '[]'::jsonb, false),
    ('53479472-9907-568a-b1ff-d749f442d916', 'Press en el suelo con cadenas', 'Floor Press with Chains', 'FREE_EXERCISE_DB', 'Floor_Press_with_Chains', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. Acostado en el suelo dentro del rack, con cadenas colgando de la barra.
2. Baja la barra hasta que los brazos toquen el suelo, codos pegados.
3. Pausa y empuja; la carga aumenta al subir.', '[]'::jsonb, false),
    ('fa225db1-7654-59f7-8bb2-59a8d5023b28', 'Patadas alternas boca abajo', 'Flutter Kicks', 'FREE_EXERCISE_DB', 'Flutter_Kicks', 'Glúteos', '["Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Boca abajo en un banco con la cadera en el borde y las piernas rectas.
2. Sube las piernas a la altura de la cadera apretando glúteos.
3. Alterna subiendo una y bajando la otra, como un pataleo.', '[]'::jsonb, false),
    ('4c99018c-9482-593c-a881-2c07b2453f3a', 'Liberación de la planta del pie', 'Foot-SMR', 'FREE_EXERCISE_DB', 'Foot-SMR', 'Pantorrillas', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado y descalzo, apoya el arco del pie sobre un rodillo pequeño.
2. Presiona y rueda a lo largo del arco.
3. Sostén 10 a 30 s en los puntos tensos y cambia de pie.', '[]'::jsonb, false),
    ('40b4b95c-390b-5637-9821-f378b7eb31fa', 'Arrastre de trineo hacia adelante con press', 'Forward Drag with Press', 'FREE_EXERCISE_DB', 'Forward_Drag_with_Press', 'Pecho', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Cuádriceps", "Hombros", "Tríceps"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRONGMAN', 'Strongman', 'COMPOUND', 'PUSH', '1. De espaldas al trineo, con un agarre en cada mano.
2. Da un paso inclinado hacia adelante.
3. En cada paso empuja las manos al frente extendiendo los codos.', '[]'::jsonb, false),
    ('c8151517-170d-5909-89a5-aae5c38d3270', 'Sentadilla Frankenstein', 'Frankenstein Squat', 'FREE_EXERCISE_DB', 'Frankenstein_Squat', 'Cuádriceps', '["Abdomen", "Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PUSH', '1. Con la barra apoyada al frente de los hombros, suelta las manos y extiende los brazos al frente.
2. Baja en sentadilla con el torso erguido.
3. Sube manteniendo la barra en su lugar.', '["KNEE"]'::jsonb, false),
    ('b7e85796-49ed-5f42-8312-7ff9fe65f12c', 'Sentadilla con salto', 'Freehand Jump Squat', 'FREE_EXERCISE_DB', 'Freehand_Jump_Squat', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie con los brazos cruzados sobre el pecho.
2. Baja hasta que los muslos queden paralelos al suelo.
3. Salta lo más alto posible y al caer encadena la siguiente.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('0757ab2e-8e2d-58cd-9310-33ff0ea3be34', 'Saltos de rana', 'Frog Hops', 'FREE_EXERCISE_DB', 'Frog_Hops', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', 'COMPOUND', 'PUSH', '1. En sentadilla con las manos detrás de la cabeza y el torso erguido.
2. Salta hacia adelante unos metros sin subir de más.
3. Amortigua al caer y vuelve a saltar 5 a 10 veces.', '[]'::jsonb, false),
    ('9d17c3ca-ed5e-52a3-aa91-7adaf6f865ed', 'Abdominal rana', 'Frog Sit-Ups', 'FREE_EXERCISE_DB', 'Frog_Sit-Ups', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca arriba con las plantas de los pies juntas y las rodillas abiertas.
2. Cruza los brazos sobre el pecho y sube el torso.
3. Baja despacio.', '[]'::jsonb, false),
    ('7c6f62a2-368c-5cc8-8862-a2577f57a2c4', 'Sentadilla frontal con barra a banco', 'Front Barbell Squat To A Bench', 'FREE_EXERCISE_DB', 'Front_Barbell_Squat_To_A_Bench', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BARBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con un banco detrás, apoya la barra al frente de los hombros con los codos altos.
2. Baja hasta rozar el banco con el torso erguido.
3. Sube empujando con los talones.', '["KNEE", "LOWER_BACK"]'::jsonb, false),
    ('6cfe42ba-37e8-5732-8a60-5befed33e8c1', 'Salto al cajón', 'Front Box Jump', 'FREE_EXERCISE_DB', 'Front_Box_Jump', 'Isquiotibiales', '["Abductores", "Aductores", "Pantorrillas", "Glúteos", "Cuádriceps"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie frente a un cajón.
2. Flexiona un poco y salta impulsando con los brazos.
3. Aterriza suave sobre el cajón y baja caminando.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('72638cda-266a-5296-9707-e501b96068d2', 'Saltos frontales sobre conos', 'Front Cone Hops (or hurdle hops)', 'FREE_EXERCISE_DB', 'Front_Cone_Hops_or_hurdle_hops', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. Coloca conos en fila a un metro de distancia.
2. Salta con ambos pies sobre cada cono impulsando con los brazos.
3. Amortigua y encadena el siguiente salto.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('eda61d43-ccd9-5cd9-999b-a83bf3185c9b', 'Elevación frontal en banco inclinado', 'Front Incline Dumbbell Raise', 'FREE_EXERCISE_DB', 'Front_Incline_Dumbbell_Raise', 'Hombros', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Recostado en banco inclinado con las mancuernas sobre los muslos, palmas abajo.
2. Sube las mancuernas al frente hasta un poco por encima de los hombros.
3. Aprieta un segundo y baja despacio.', '[]'::jsonb, false),
    ('1573be5f-b70c-52b9-a6cb-a592c5c16da3', 'Balanceo frontal de pierna', 'Front Leg Raises', 'FREE_EXERCISE_DB', 'Front_Leg_Raises', 'Isquiotibiales', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'PULL', '1. De pie junto a un apoyo.
2. Balancea una pierna recta hacia adelante y atrás.
3. Repite 5 a 10 veces y cambia de pierna.', '[]'::jsonb, false),
    ('7181d5ba-80fc-5369-8825-7b70645d58bd', 'Elevación frontal con disco', 'Front Plate Raise', 'FREE_EXERCISE_DB', 'Front_Plate_Raise', 'Hombros', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie, sostén un disco con ambas manos frente a la cadera.
2. Súbelo con los brazos casi rectos hasta un poco por encima de los hombros.
3. Sostén un segundo y baja despacio.', '[]'::jsonb, false),
    ('5fbb39af-3650-5873-96c3-dbd529c28596', 'Elevación frontal y pullover con barra', 'Front Raise And Pullover', 'FREE_EXERCISE_DB', 'Front_Raise_And_Pullover', 'Pecho', '["Espalda", "Hombros", "Tríceps"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Acostado en banco plano con la barra sobre los muslos.
2. Llévala en arco con los brazos rectos hasta detrás de la cabeza.
3. Vuelve por el mismo camino.', '["SHOULDER"]'::jsonb, false),
    ('2d2ecafa-c9b7-5167-884d-890ba92f83d4', 'Sentadilla frontal con agarre de cargada', 'Front Squat (Clean Grip)', 'FREE_EXERCISE_DB', 'Front_Squat_Clean_Grip', 'Cuádriceps', '["Abdomen", "Glúteos", "Isquiotibiales"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Apoya la barra al frente de los hombros, sostenida con los dedos y los codos altos.
2. Baja con el torso erguido.
3. Sube empujando con los talones.', '["KNEE", "LOWER_BACK", "WRIST"]'::jsonb, false),
    ('8c725170-3269-505a-9713-a8f92c141c82', 'Sentadilla frontal con dos kettlebells', 'Front Squats With Two Kettlebells', 'FREE_EXERCISE_DB', 'Front_Squats_With_Two_Kettlebells', 'Cuádriceps', '["Pantorrillas", "Glúteos"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Lleva dos kettlebells a los hombros.
2. Baja lo más que puedas con las rodillas hacia afuera y el torso erguido.
3. Sube empujando con los talones.', '["KNEE", "LOWER_BACK", "WRIST"]'::jsonb, true),
    ('b152ac4c-707b-51bb-a384-3a14cdd36c57', 'Elevación frontal con dos mancuernas', 'Front Two-Dumbbell Raise', 'FREE_EXERCISE_DB', 'Front_Two-Dumbbell_Raise', 'Hombros', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie con las mancuernas frente a los muslos.
2. Súbelas al frente con los codos apenas flexionados hasta pasar la altura de los hombros.
3. Pausa y baja despacio.', '[]'::jsonb, false),
    ('af756a04-c801-5898-a1ec-256b897adb37', 'Jalón cruzado de recorrido completo', 'Full Range-Of-Motion Lat Pulldown', 'FREE_EXERCISE_DB', 'Full_Range-Of-Motion_Lat_Pulldown', 'Espalda', '["Bíceps", "Hombros"]'::jsonb, 'CABLE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Toma los agarres de las poleas altas con los brazos cruzados.
2. Tira hacia abajo en arco, girando las palmas hasta enfrentarlas.
3. Vuelve despacio.', '[]'::jsonb, false),
    ('6f00ceb6-595f-5d4e-a693-fa389b9f0e46', 'Dominadas al esternón (Gironda)', 'Gironda Sternum Chins', 'FREE_EXERCISE_DB', 'Gironda_Sternum_Chins', 'Espalda', '["Bíceps"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Cuelga con agarre supino y el pecho hacia afuera.
2. Tira arqueando la espalda hasta llevar el esternón a la barra.
3. Baja despacio.', '[]'::jsonb, false),
    ('fb0a840a-5aa8-5984-b2b0-d92b98b802c8', 'Elevación glúteo-isquiotibial en máquina', 'Glute Ham Raise', 'FREE_EXERCISE_DB', 'Glute_Ham_Raise', 'Isquiotibiales', '["Pantorrillas", "Glúteos"]'::jsonb, 'MACHINE', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PULL', '1. Boca abajo en la máquina con los pies contra la plataforma.
2. Flexiona las rodillas hasta quedar erguido.
3. Baja con control.', '[]'::jsonb, false),
    ('2867cccd-bf5a-519b-a6d8-a1183fb07205', 'Patada de glúteo en cuadrupedia', 'Glute Kickback', 'FREE_EXERCISE_DB', 'Glute_Kickback', 'Glúteos', '["Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En cuatro apoyos con las rodillas a 90°.
2. Sube una pierna hasta que el muslo quede en línea con la espalda.
3. Baja despacio y alterna.', '[]'::jsonb, true),
    ('6b3a5a0b-522e-532e-824a-a9e3ce7b0c5c', 'Buenos días desde soportes', 'Good Morning off Pins', 'FREE_EXERCISE_DB', 'Good_Morning_off_Pins', 'Isquiotibiales', '["Abdomen", "Glúteos", "Lumbares"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. Con la barra en los soportes a la altura del abdomen, colócala sobre la espalda inclinado.
2. Extiende la cadera hasta quedar erguido, espalda firme.
3. Vuelve a apoyar la barra en los soportes.', '["LOWER_BACK"]'::jsonb, false),
    ('f53e225d-8597-5306-93d9-bd08d54b2d46', 'Dominada gorila con crunch', 'Gorilla Chin/Crunch', 'FREE_EXERCISE_DB', 'Gorilla_Chin_Crunch', 'Abdomen', '["Bíceps", "Espalda"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Cuelga con agarre supino y las rodillas a 90°.
2. Sube llevando las rodillas al pecho al mismo tiempo.
3. Baja despacio.', '[]'::jsonb, false),
    ('928d310a-08cf-5816-bc94-2000c3f8d843', 'Estiramiento de ingle y espalda', 'Groin and Back Stretch', 'FREE_EXERCISE_DB', 'Groin_and_Back_Stretch', 'Aductores', '[]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', 'COMPOUND', 'STATIC', '1. Sentado con las plantas de los pies juntas y las manos detrás de la cabeza.
2. Inclínate hacia adelante llevando los codos hacia adentro de los muslos.
3. Vuelve erguido; repite 10 a 20 veces.', '[]'::jsonb, false),
    ('8e585c9b-c664-5288-820d-cba71b8e7cd2', 'Saltos a zancada lateral (groiners)', 'Groiners', 'FREE_EXERCISE_DB', 'Groiners', 'Aductores', '[]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', 'COMPOUND', 'PULL', '1. En posición de flexión.
2. Salta llevando ambos pies junto a las manos, cabeza arriba.
3. Vuelve y repite 10 a 20 veces.', '[]'::jsonb, false),
    ('0087ea7c-b441-5425-8291-60d75afb1f26', 'Press inclinado con mancuernas agarre neutro', 'Hammer Grip Incline DB Bench Press', 'FREE_EXERCISE_DB', 'Hammer_Grip_Incline_DB_Bench_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En banco inclinado, con las mancuernas a la altura de los hombros y palmas enfrentadas.
2. Empuja las mancuernas hacia arriba.
3. Baja despacio.', '["SHOULDER"]'::jsonb, false),
    ('619998a8-d32a-5243-b900-adce339be85b', 'Liberación de isquiotibiales con rodillo', 'Hamstring-SMR', 'FREE_EXERCISE_DB', 'Hamstring-SMR', 'Isquiotibiales', '[]'::jsonb, 'OTHER', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Sentado con el rodillo bajo la parte trasera del muslo, apoyado en las manos.
2. Rueda desde debajo de la cadera hasta la rodilla.
3. Sostén 10 a 30 s en los puntos tensos y cambia de pierna.', '[]'::jsonb, false),
    ('ae85bdcb-3eb3-5adb-a0ef-1ce86a3bee20', 'Estiramiento de isquiotibiales con cinta', 'Hamstring Stretch', 'FREE_EXERCISE_DB', 'Hamstring_Stretch', 'Isquiotibiales', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Boca arriba, pasa una cinta por la planta de un pie con la pierna hacia el techo.
2. Tira suave de la cinta.
3. Sostén 10 a 30 s y cambia de pierna.', '[]'::jsonb, false),
    ('e2bbb22d-3d19-5e93-afbd-e5181ef497c6', 'Flexiones en vertical', 'Handstand Push-Ups', 'FREE_EXERCISE_DB', 'Handstand_Push-Ups', 'Hombros', '["Tríceps"]'::jsonb, 'BODYWEIGHT', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Apoya las manos al ancho de los hombros y sube los pies contra la pared.
2. Baja hasta que la cabeza casi toque el suelo.
3. Empuja hasta extender los brazos.', '["SHOULDER", "WRIST"]'::jsonb, false),
    ('9e6dfb00-3dc0-5182-ae4a-4b63d89ac324', 'Cargada colgante', 'Hang Clean', 'FREE_EXERCISE_DB', 'Hang_Clean', 'Cuádriceps', '["Pantorrillas", "Antebrazos", "Glúteos", "Isquiotibiales", "Lumbares", "Hombros", "Trapecio"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con la barra a mitad del muslo y la espalda algo inclinada.
2. Extiende cadera, rodillas y tobillos con fuerza y encoge los hombros.
3. Recibe la barra sobre los hombros con los codos altos.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('f00f1c24-5b46-54f9-8f30-386031ab887f', 'Cargada colgante desde debajo de las rodillas', 'Hang Clean - Below the Knees', 'FREE_EXERCISE_DB', 'Hang_Clean_-_Below_the_Knees', 'Cuádriceps', '["Pantorrillas", "Antebrazos", "Glúteos", "Isquiotibiales", "Lumbares", "Hombros", "Trapecio"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con la barra justo debajo de las rodillas.
2. Extiende cadera y rodillas con fuerza y tira con los codos arriba.
3. Métete debajo y recibe la barra sobre los hombros.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('c7419599-f01e-578a-8467-14e727ab336b', 'Arrancada colgante', 'Hang Snatch', 'FREE_EXERCISE_DB', 'Hang_Snatch', 'Isquiotibiales', '["Abdomen", "Pantorrillas", "Antebrazos", "Glúteos", "Lumbares", "Cuádriceps", "Hombros", "Trapecio"]'::jsonb, 'BARBELL', 'ADVANCED', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con agarre ancho y la barra a la altura de la cadera.
2. Extiende piernas y cadera con fuerza y encoge los hombros.
3. Métete debajo y recibe la barra con los brazos extendidos arriba.', '["LOWER_BACK", "SHOULDER"]'::jsonb, false),
    ('e726a056-b7ed-5248-8b7e-3b31054a9621', 'Arrancada colgante desde debajo de las rodillas', 'Hang Snatch - Below Knees', 'FREE_EXERCISE_DB', 'Hang_Snatch_-_Below_Knees', 'Isquiotibiales', '["Abdomen", "Pantorrillas", "Antebrazos", "Glúteos", "Lumbares", "Cuádriceps", "Hombros", "Trapecio"]'::jsonb, 'BARBELL', 'ADVANCED', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con agarre ancho y la barra justo debajo de las rodillas.
2. Extiende piernas y cadera con fuerza.
3. Recibe la barra arriba con los brazos extendidos.', '["LOWER_BACK", "SHOULDER"]'::jsonb, false),
    ('59092366-cc13-547d-b224-0ebfec59bbcc', 'Buenos días con barra suspendida', 'Hanging Bar Good Morning', 'FREE_EXERCISE_DB', 'Hanging_Bar_Good_Morning', 'Isquiotibiales', '["Abdomen", "Glúteos", "Lumbares"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. Con la barra colgada de cadenas a la altura del abdomen, colócala sobre la espalda.
2. Extiende la cadera hasta quedar erguido.
3. Vuelve con control.', '["LOWER_BACK"]'::jsonb, false),
    ('354fb9f1-b567-5e0f-955f-a7b71b629211', 'Pica colgado', 'Hanging Pike', 'FREE_EXERCISE_DB', 'Hanging_Pike', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Cuelga de la barra con las rodillas a 90°.
2. Sube las piernas hasta casi tocar la barra con las tibias.
3. Baja despacio.', '[]'::jsonb, false),
    ('85744e8f-9644-5cad-b7e3-a3602ceac132', 'Equilibrio de arrancada con impulso', 'Heaving Snatch Balance', 'FREE_EXERCISE_DB', 'Heaving_Snatch_Balance', 'Cuádriceps', '["Abdomen", "Antebrazos", "Glúteos", "Isquiotibiales", "Hombros", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PUSH', '1. Con poco peso sobre la espalda y los pies como en la sentadilla.
2. Flexiona un poco y descarga la barra con un impulso.
3. Métete debajo en sentadilla profunda con la barra arriba.', '["LOWER_BACK", "SHOULDER"]'::jsonb, false),
    ('8d35714a-884e-56f4-bd63-6a026c7056be', 'Empuje al saco', 'Heavy Bag Thrust', 'FREE_EXERCISE_DB', 'Heavy_Bag_Thrust', 'Pecho', '["Abdomen", "Hombros", "Tríceps"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie junto al saco con los pies separados, una mano a la altura del pecho.
2. Gira la cintura y empuja el saco con fuerza.
3. Recíbelo al volver y repite.', '[]'::jsonb, false),
    ('f794c336-fa25-5a4a-ac60-d24859e02397', 'Curl en polea alta', 'High Cable Curls', 'FREE_EXERCISE_DB', 'High_Cable_Curls', 'Bíceps', '[]'::jsonb, 'CABLE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Entre dos poleas altas, con los brazos paralelos al suelo.
2. Lleva los agarres hacia las orejas flexionando solo los codos.
3. Aprieta y vuelve despacio.', '[]'::jsonb, false),
    ('94f7ad51-b078-54c9-ba4d-2fabdfadb7d1', 'Círculos de cadera en cuadrupedia', 'Hip Circles (prone)', 'FREE_EXERCISE_DB', 'Hip_Circles_prone', 'Abductores', '["Aductores"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'PULL', '1. En cuatro apoyos, levanta una rodilla flexionada.
2. Dibuja círculos grandes con la rodilla.
3. Repite y cambia de lado.', '[]'::jsonb, false),
    ('06d2f27f-ce44-54cb-9164-9f7b97998fa8', 'Extensión de cadera con banda', 'Hip Extension with Bands', 'FREE_EXERCISE_DB', 'Hip_Extension_with_Bands', 'Glúteos', '["Isquiotibiales"]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la banda en el tobillo y anclada abajo, de frente al poste.
2. Lleva la pierna recta hacia atrás lo más que puedas.
3. Vuelve y cambia de pierna.', '[]'::jsonb, true),
    ('301cf92e-5465-5c87-92c2-14ff68fdf476', 'Flexión de cadera con banda', 'Hip Flexion with Band', 'FREE_EXERCISE_DB', 'Hip_Flexion_with_Band', 'Cuádriceps', '[]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la banda en el tobillo y anclada abajo, de espaldas al poste.
2. Sube la rodilla hasta 90° y pausa.
3. Vuelve y cambia de pierna.', '[]'::jsonb, false),
    ('82a7d750-ed93-5137-9449-474b5160b438', 'Elevación de cadera con banda', 'Hip Lift with Band', 'FREE_EXERCISE_DB', 'Hip_Lift_with_Band', 'Glúteos', '["Pantorrillas", "Isquiotibiales"]'::jsonb, 'BAND', 'BEGINNER', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. Boca arriba en el rack con la banda sobre la cadera, rodillas flexionadas.
2. Empuja con los talones y sube la cadera contra la banda.
3. Baja con control.', '[]'::jsonb, false),
    ('22ca48cc-ecbe-507c-953d-034394bf6669', 'Abrazo al fitball', 'Hug A Ball', 'FREE_EXERCISE_DB', 'Hug_A_Ball', 'Lumbares', '["Pantorrillas", "Glúteos"]'::jsonb, 'OTHER', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Sentado en el suelo, con el fitball entre las piernas.
2. Baja la cadera y abraza el balón con los brazos.
3. Relaja la espalda manteniendo el equilibrio.', '[]'::jsonb, false),
    ('4de446ee-581d-5ffe-aa1b-974fcc7eb753', 'Rodillas al pecho', 'Hug Knees To Chest', 'FREE_EXERCISE_DB', 'Hug_Knees_To_Chest', 'Lumbares', '["Glúteos"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Boca arriba, lleva ambas rodillas al pecho.
2. Sujétalas por debajo y llévalas hacia los hombros.
3. Sostén el estiramiento.', '[]'::jsonb, false),
    ('a402b14d-79ab-5ac1-b8c2-ed95acf255c3', 'Saltos de vallas', 'Hurdle Hops', 'FREE_EXERCISE_DB', 'Hurdle_Hops', 'Isquiotibiales', '["Abductores", "Aductores", "Pantorrillas", "Glúteos"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. Coloca vallas bajas en fila.
2. Salta con ambos pies cada valla impulsando con los brazos.
3. Amortigua y encadena el siguiente salto.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('0a99df22-9707-5151-9354-537d360d5d8b', 'Hiperextensiones', 'Hyperextensions (Back Extensions)', 'FREE_EXERCISE_DB', 'Hyperextensions_Back_Extensions', 'Lumbares', '["Glúteos", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. En el banco de hiperextensiones, con los tobillos sujetos y los brazos cruzados.
2. Baja el torso doblando la cintura con la espalda recta.
3. Sube hasta que el cuerpo quede en línea.', '["LOWER_BACK"]'::jsonb, true),
    ('c8a4cd90-3d42-56fb-bef4-bb94152e2a20', 'Hiperextensiones en banco plano', 'Hyperextensions With No Hyperextension Bench', 'FREE_EXERCISE_DB', 'Hyperextensions_With_No_Hyperextension_Bench', 'Lumbares', '["Glúteos", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Boca abajo con la cadera en el borde del banco y alguien sosteniendo tus piernas.
2. Baja el torso doblando la cintura.
3. Sube hasta quedar en línea.', '["LOWER_BACK"]'::jsonb, false),
    ('e50f16ed-63aa-56d1-bfe7-288f169206fa', 'Estiramiento de cintilla iliotibial y glúteo', 'IT Band and Glute Stretch', 'FREE_EXERCISE_DB', 'IT_Band_and_Glute_Stretch', 'Abductores', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Boca arriba, pasa una cinta por un pie y cruza esa pierna recta hacia el otro lado.
2. Tira de la cinta llevando la punta del pie hacia ti.
3. Sostén 10 a 20 s y cambia de lado.', '[]'::jsonb, false),
    ('a6f4c8b8-7859-57ec-8a4e-301a5d516616', 'Liberación de cintilla iliotibial con rodillo', 'Iliotibial Tract-SMR', 'FREE_EXERCISE_DB', 'Iliotibial_Tract-SMR', 'Abductores', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. De costado con el rodillo bajo el muslo de abajo.
2. Rueda desde la cadera hasta la rodilla.
3. Sostén 10 a 30 s en los puntos tensos y cambia de lado.', '[]'::jsonb, false),
    ('77c74f73-7950-5841-899c-037b7bfdbf31', 'Gusano', 'Inchworm', 'FREE_EXERCISE_DB', 'Inchworm', 'Isquiotibiales', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'COMPOUND', NULL, '1. De pie, apoya las manos en el suelo con las piernas rectas.
2. Camina con las manos hasta la posición de flexión.
3. Camina con los pies hacia las manos con pasos cortos.', '[]'::jsonb, false),
    ('8fa53db5-25b7-50a5-aad6-9b501c092362', 'Extensión de tríceps con barra en banco inclinado', 'Incline Barbell Triceps Extension', 'FREE_EXERCISE_DB', 'Incline_Barbell_Triceps_Extension', 'Tríceps', '["Antebrazos"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Recostado en banco inclinado, con la barra arriba y los codos adentro.
2. Bájala en semicírculo por detrás de la cabeza.
3. Extiende los codos para volver.', '["ELBOW"]'::jsonb, false),
    ('28d67cfa-9e40-5e6e-99cb-dc23cf7f7ca9', 'Remo boca abajo en banco inclinado', 'Incline Bench Pull', 'FREE_EXERCISE_DB', 'Incline_Bench_Pull', 'Espalda', '["Hombros"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca abajo en banco inclinado a 30°, con las mancuernas colgando y los codos abiertos.
2. Tira las mancuernas hacia arriba como un press de banca invertido.
3. Baja despacio.', '[]'::jsonb, false),
    ('bef04371-e2b2-5cf0-a54e-e396441d3601', 'Press inclinado en polea', 'Incline Cable Chest Press', 'FREE_EXERCISE_DB', 'Incline_Cable_Chest_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sentado con respaldo inclinado, toma los agarres con los codos a 90°.
2. Empuja los agarres al frente y arriba.
3. Vuelve despacio manteniendo la tensión.', '[]'::jsonb, false),
    ('02259360-a572-502b-994e-d485c82b640a', 'Aperturas inclinadas en polea', 'Incline Cable Flye', 'FREE_EXERCISE_DB', 'Incline_Cable_Flye', 'Pecho', '["Hombros"]'::jsonb, 'CABLE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. En banco inclinado entre dos poleas bajas, con los agarres arriba.
2. Abre los brazos hacia los costados con los codos apenas flexionados.
3. Júntalos de nuevo apretando el pecho.', '[]'::jsonb, false),
    ('ae37cd67-1ce5-5c68-8190-bd402336b37f', 'Press inclinado con mancuernas palmas adentro', 'Incline Dumbbell Bench With Palms Facing In', 'FREE_EXERCISE_DB', 'Incline_Dumbbell_Bench_With_Palms_Facing_In', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En banco inclinado, con las mancuernas a la altura de los hombros y palmas enfrentadas.
2. Empújalas hacia arriba.
3. Baja despacio.', '[]'::jsonb, false),
    ('9758fb01-6981-538a-a866-ab33419b9d83', 'Aperturas inclinadas con mancuernas', 'Incline Dumbbell Flyes', 'FREE_EXERCISE_DB', 'Incline_Dumbbell_Flyes', 'Pecho', '["Hombros"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En banco inclinado a 30° como máximo, mancuernas arriba con los codos algo flexionados.
2. Abre los brazos hacia los costados.
3. Vuelve por el mismo arco.', '[]'::jsonb, false),
    ('56f628b1-617f-5252-a3fd-3bf48348d0cc', 'Aperturas inclinadas con giro', 'Incline Dumbbell Flyes - With A Twist', 'FREE_EXERCISE_DB', 'Incline_Dumbbell_Flyes_-_With_A_Twist', 'Pecho', '["Hombros"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En banco inclinado, mancuernas arriba con los meñiques juntos.
2. Abre los brazos girando las palmas hasta enfrentarlas.
3. Vuelve girando de nuevo.', '[]'::jsonb, false),
    ('d9c1feab-8bca-5a9f-a231-525073b61456', 'Curl martillo en banco inclinado', 'Incline Hammer Curls', 'FREE_EXERCISE_DB', 'Incline_Hammer_Curls', 'Bíceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado en banco inclinado con las mancuernas colgando y agarre neutro.
2. Sube las mancuernas sin mover los brazos.
3. Pausa y baja despacio.', '[]'::jsonb, false),
    ('d16dbb75-1356-5d35-964e-2a41c8f46731', 'Curl inclinado abierto', 'Incline Inner Biceps Curl', 'FREE_EXERCISE_DB', 'Incline_Inner_Biceps_Curl', 'Bíceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', NULL, '1. Recostado en banco inclinado con las palmas hacia afuera.
2. Sube las mancuernas hacia afuera y arriba, a los costados de los hombros.
3. Aprieta y baja despacio.', '[]'::jsonb, false),
    ('7d329c04-87a7-5d92-9474-06efc4a1ead7', 'Flexiones inclinadas cerradas', 'Incline Push-Up Close-Grip', 'FREE_EXERCISE_DB', 'Incline_Push-Up_Close-Grip', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con las manos juntas sobre una barra o plataforma elevada.
2. Baja el pecho a la barra con el cuerpo recto.
3. Empuja hasta extender los codos.', '["WRIST"]'::jsonb, false),
    ('861efd46-aa12-5c52-b5e0-0bcfcd98388f', 'Flexión pliométrica inclinada', 'Incline Push-Up Depth Jump', 'FREE_EXERCISE_DB', 'Incline_Push-Up_Depth_Jump', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. Con los pies en un cajón y las manos en el suelo entre dos plataformas.
2. Baja y empuja con fuerza para despegar las manos.
3. Cae sobre las plataformas amortiguando con los codos.', '["KNEE", "WRIST", "ANKLE"]'::jsonb, false),
    ('792487d3-f6c6-5bc1-b98c-b8822c846f69', 'Flexiones inclinadas en barra', 'Incline Push-Up Medium', 'FREE_EXERCISE_DB', 'Incline_Push-Up_Medium', 'Pecho', '["Abdomen", "Hombros", "Tríceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con las manos al ancho de los hombros sobre una barra o plataforma elevada.
2. Baja el pecho a la barra con el cuerpo recto.
3. Empuja hasta extender los codos.', '["WRIST"]'::jsonb, false),
    ('676c8983-f549-5836-8b7b-6dfcf3399e0d', 'Flexiones inclinadas con agarre invertido', 'Incline Push-Up Reverse Grip', 'FREE_EXERCISE_DB', 'Incline_Push-Up_Reverse_Grip', 'Pecho', '["Abdomen", "Hombros", "Tríceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con las palmas hacia arriba sobre una barra elevada.
2. Baja el pecho a la barra con el cuerpo recto.
3. Empuja hasta extender los codos.', '["WRIST"]'::jsonb, false),
    ('5de7bc82-e88d-5de1-9889-798d47b78f53', 'Flexiones inclinadas abiertas', 'Incline Push-Up Wide', 'FREE_EXERCISE_DB', 'Incline_Push-Up_Wide', 'Pecho', '["Abdomen", "Hombros", "Tríceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con las manos más abiertas que los hombros sobre una barra elevada.
2. Baja el pecho a la barra con el cuerpo recto.
3. Empuja hasta extender los codos.', '["WRIST"]'::jsonb, false),
    ('84921889-1e13-54f7-838c-f89ff5fbf03b', 'Estiramiento de ingle con cinta', 'Intermediate Groin Stretch', 'FREE_EXERCISE_DB', 'Intermediate_Groin_Stretch', 'Isquiotibiales', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Boca arriba, pasa una cinta por un pie y lleva esa pierna hacia el costado.
2. Tira suave de la cinta.
3. Sostén 10 a 20 s y cambia de lado.', '[]'::jsonb, false),
    ('c73f833c-883e-57a3-ba00-c8d1cf4889ea', 'Estiramiento de flexores de cadera y cuádriceps con cinta', 'Intermediate Hip Flexor and Quad Stretch', 'FREE_EXERCISE_DB', 'Intermediate_Hip_Flexor_and_Quad_Stretch', 'Cuádriceps', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', 'COMPOUND', 'STATIC', '1. Boca abajo, con una cinta en un pie y la rodilla flexionada.
2. Tira de la cinta hasta despegar la rodilla del suelo.
3. Sostén 10 a 20 s y cambia de pierna.', '[]'::jsonb, false),
    ('f490aeed-76b6-5932-8e24-32ec37a276de', 'Rotación interna con banda', 'Internal Rotation with Band', 'FREE_EXERCISE_DB', 'Internal_Rotation_with_Band', 'Hombros', '[]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', NULL, '1. De costado a la banda, codo pegado al cuerpo a 90° y mano hacia afuera.
2. Gira el antebrazo hacia el abdomen sin despegar el codo.
3. Vuelve despacio y cambia de lado.', '[]'::jsonb, false),
    ('4ef26aa5-d080-5f00-ae36-38a9a312fdff', 'Remo invertido', 'Inverted Row', 'FREE_EXERCISE_DB', 'Inverted_Row', 'Espalda', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Cuelga debajo de una barra a la altura de la cintura, cuerpo recto y talones en el suelo.
2. Tira del pecho hacia la barra juntando los omóplatos.
3. Pausa y baja despacio.', '[]'::jsonb, true),
    ('6761d0dd-cbd5-5151-aa39-fc0b8cc76ed2', 'Remo invertido con correas', 'Inverted Row with Straps', 'FREE_EXERCISE_DB', 'Inverted_Row_with_Straps', 'Espalda', '["Bíceps"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Cuelga de unas correas con el cuerpo recto y los talones en el suelo.
2. Tira del pecho hacia las manos juntando los omóplatos.
3. Pausa y baja despacio.', '[]'::jsonb, false),
    ('def6636b-63b8-5df1-92c2-0134f722cd29', 'Cruz de hierro con mancuernas', 'Iron Cross', 'FREE_EXERCISE_DB', 'Iron_Cross', 'Hombros', '["Pecho", "Glúteos", "Isquiotibiales", "Lumbares", "Cuádriceps", "Trapecio"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie con una mancuerna en cada mano frente a los muslos.
2. Sube los brazos al frente hasta los hombros y ábrelos hacia los costados.
3. Vuelve al frente y baja despacio.', '[]'::jsonb, false),
    ('624087fe-fe98-56ec-8653-dff086e58d6d', 'Cruces de pierna boca abajo', 'Iron Crosses (stretch)', 'FREE_EXERCISE_DB', 'Iron_Crosses_stretch', 'Cuádriceps', '[]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', 'COMPOUND', 'PULL', '1. Boca abajo con los brazos abiertos.
2. Lleva una pierna flexionada por detrás hacia la mano contraria.
3. Vuelve y alterna 10 a 20 veces.', '[]'::jsonb, false),
    ('3ae41c3f-1a3b-507f-b90b-cbaac5b18087', 'Contracción isométrica de pecho', 'Isometric Chest Squeezes', 'FREE_EXERCISE_DB', 'Isometric_Chest_Squeezes', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'STATIC', '1. Junta las palmas frente al pecho con los codos a 90°.
2. Empuja una mano contra la otra aumentando la tensión poco a poco.
3. Sostén el tiempo indicado y suelta despacio.', '[]'::jsonb, false),
    ('754dde48-0200-56e3-8f97-e4d3287b6b33', 'Isométrico de cuello adelante y atrás', 'Isometric Neck Exercise - Front And Back', 'FREE_EXERCISE_DB', 'Isometric_Neck_Exercise_-_Front_And_Back', 'Cuello', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'STATIC', '1. Con la cabeza erguida, apoya las manos en la frente.
2. Empuja la cabeza hacia adelante sin moverla, resistiendo con las manos.
3. Sostén y repite con las manos en la nuca.', '["NECK"]'::jsonb, true),
    ('5219ce25-9dcd-5686-ad64-8ca1119a2eea', 'Isométrico de cuello lateral', 'Isometric Neck Exercise - Sides', 'FREE_EXERCISE_DB', 'Isometric_Neck_Exercise_-_Sides', 'Cuello', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'STATIC', '1. Con la cabeza erguida, apoya una mano en el costado de la cabeza.
2. Empuja hacia la mano sin mover la cabeza.
3. Sostén y repite del otro lado.', '["NECK"]'::jsonb, false),
    ('bc06fef7-6d24-5d39-b1b7-556bcbd91739', 'Limpiaparabrisas en flexión', 'Isometric Wipers', 'FREE_EXERCISE_DB', 'Isometric_Wipers', 'Pecho', '["Abdomen", "Hombros", "Tríceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En posición de flexión con las manos algo más abiertas que los hombros.
2. Lleva el peso hacia un lado flexionando ese codo.
3. Empuja y pasa al otro lado.', '[]'::jsonb, false),
    ('5e72e553-0033-5e46-b578-4fb90818995f', 'Press JM', 'JM Press', 'FREE_EXERCISE_DB', 'JM_Press', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado en banco plano con la barra sobre el pecho alto y agarre cerrado.
2. Bájala como una extensión de tríceps y a mitad de camino llévala hacia el pecho.
3. Empuja hasta extender los codos.', '["ELBOW"]'::jsonb, false),
    ('09a8a6e9-41dd-593e-809d-244e62527eb2', 'Abdominal navaja', 'Jackknife Sit-Up', 'FREE_EXERCISE_DB', 'Jackknife_Sit-Up', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Boca arriba con brazos y piernas extendidos.
2. Sube piernas y brazos a la vez hasta que se encuentren.
3. Baja despacio.', '[]'::jsonb, false),
    ('afbcab84-4b51-54fa-9629-73de8e2f0c39', 'Abdominal Janda', 'Janda Sit-Up', 'FREE_EXERCISE_DB', 'Janda_Sit-Up', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca arriba con las rodillas a 90° y los pies en el suelo.
2. Aprieta glúteos e isquiotibiales y sube el torso en 3 a 6 segundos.
3. Baja despacio.', '[]'::jsonb, false),
    ('3992436a-d148-5cee-a613-9b58edeabd2c', 'Sentadilla Jefferson', 'Jefferson Squats', 'FREE_EXERCISE_DB', 'Jefferson_Squats', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares", "Trapecio"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Párate en el medio de una barra en el suelo, a horcajadas.
2. Tómala con una mano adelante y otra atrás.
3. Sube con la espalda recta y baja en sentadilla.', '["KNEE"]'::jsonb, false),
    ('9bcac827-eef6-5bee-a673-3618cc45ef42', 'Equilibrio de envión', 'Jerk Balance', 'FREE_EXERCISE_DB', 'Jerk_Balance', 'Hombros', '["Glúteos", "Isquiotibiales", "Cuádriceps", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PUSH', '1. Con la barra en posición de envión, hombros adelante y pies algo separados en tijera.
2. Flexiona las rodillas con el torso vertical e impulsa hacia arriba con las piernas.
3. Avanza el pie delantero y recibe la barra arriba con los brazos extendidos.', '["SHOULDER"]'::jsonb, false),
    ('02eb198c-1bfa-5bc4-af13-304bcc1b09da', 'Flexión de envión', 'Jerk Dip Squat', 'FREE_EXERCISE_DB', 'Jerk_Dip_Squat', 'Cuádriceps', '["Abdomen", "Pantorrillas"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PUSH', '1. Con la barra apoyada al frente de los hombros y los pies bajo la cadera.
2. Baja flexionando las rodillas sin llevar la cadera atrás, torso vertical.
3. Empuja con fuerza para volver.', '["KNEE", "SHOULDER", "ELBOW"]'::jsonb, false),
    ('4cd8b28b-c962-5fed-9467-1ab18bc29eae', 'Trote en cinta', 'Jogging, Treadmill', 'FREE_EXERCISE_DB', 'Jogging_Treadmill', 'Cuádriceps', '["Glúteos", "Isquiotibiales"]'::jsonb, 'MACHINE', 'BEGINNER', 'CARDIO', 'Cardio', NULL, NULL, '1. Sube a la cinta y elige un programa o el modo manual.
2. Trota a un ritmo cómodo y constante.
3. Ajusta velocidad o inclinación para cambiar la intensidad.', '[]'::jsonb, false),
    ('7264a730-9431-5999-a8e5-25b5b6c63b97', 'Carga de barril', 'Keg Load', 'FREE_EXERCISE_DB', 'Keg_Load', 'Lumbares', '["Abdomen", "Bíceps", "Pantorrillas", "Antebrazos", "Glúteos", "Isquiotibiales", "Espalda", "Cuádriceps", "Hombros", "Trapecio"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRONGMAN', 'Strongman', 'COMPOUND', 'PULL', '1. Toma el barril por el borde, inclínalo y súbelo al pecho.
2. Camina rápido hasta la plataforma sujetándolo firme.
3. Súbelo extendiendo cadera y rodillas con fuerza.', '["LOWER_BACK"]'::jsonb, false),
    ('535ca885-4249-5de1-954b-0f9d5c20b2d2', 'Press Arnold con kettlebell', 'Kettlebell Arnold Press', 'FREE_EXERCISE_DB', 'Kettlebell_Arnold_Press', 'Hombros', '["Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Lleva la kettlebell al hombro con la palma hacia ti.
2. Empújala sobre la cabeza girando la muñeca hasta que la palma mire al frente.
3. Baja girando de nuevo hasta el inicio.', '["SHOULDER"]'::jsonb, false),
    ('1f1ea223-8365-597d-ae12-95f1e4bdc1eb', 'Cargada desde el suelo con kettlebell', 'Kettlebell Dead Clean', 'FREE_EXERCISE_DB', 'Kettlebell_Dead_Clean', 'Isquiotibiales', '["Pantorrillas", "Glúteos", "Lumbares", "Cuádriceps", "Trapecio"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la kettlebell entre los pies, lleva la cadera atrás y mira al frente.
2. Extiende piernas y cadera llevando la kettlebell al hombro, girando la muñeca.
3. Bájala con la espalda recta y repite.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('08eea62d-0d62-5d5d-b36c-e4a30d1aa9ed', 'Ocho con kettlebell', 'Kettlebell Figure 8', 'FREE_EXERCISE_DB', 'Kettlebell_Figure_8', 'Abdomen', '["Isquiotibiales", "Hombros"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', NULL, 'PULL', '1. Con los pies más abiertos que los hombros, inclínate con la espalda plana.
2. Pasa la kettlebell de una mano a otra entre las piernas.
3. La mano que recibe entra por detrás de la pierna; alterna varias veces.', '[]'::jsonb, false),
    ('cd827adc-e93c-5c14-bf45-94f6b32c9b5e', 'Halo con kettlebell', 'Kettlebell Halo', 'FREE_EXERCISE_DB', 'Kettlebell_Halo', 'Hombros', '["Trapecio", "Espalda", "Abdomen", "Tríceps"]'::jsonb, 'KETTLEBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', NULL, '1. De pie, sostén la kettlebell invertida por los cuernos frente al pecho.
2. Rodea la cabeza pasando junto a una oreja y por detrás de la nuca.
3. Vuelve por el otro lado hasta el pecho y alterna la dirección.', '[]'::jsonb, false),
    ('8ace5905-9f9c-57eb-a0a1-4f729f734886', 'Halo con kettlebell y extensión de tríceps', 'Kettlebell Halo with Overhead Extension', 'FREE_EXERCISE_DB', 'Kettlebell_Halo_With_Overhead_Extension', 'Hombros', '["Tríceps", "Trapecio", "Espalda", "Abdomen"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sostén la kettlebell invertida por los cuernos frente al pecho.
2. Rodéala hasta detrás de la cabeza y extiende los codos para subirla.
3. Bájala y completa el círculo hasta el pecho.', '["SHOULDER"]'::jsonb, false),
    ('dafecb6d-7483-5c5f-b8b1-323c99cc1293', 'Cargada colgante con kettlebell', 'Kettlebell Hang Clean', 'FREE_EXERCISE_DB', 'Kettlebell_Hang_Clean', 'Isquiotibiales', '["Pantorrillas", "Glúteos", "Lumbares", "Hombros", "Trapecio"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la kettlebell colgando entre las piernas y la cadera atrás.
2. Extiende piernas y cadera llevándola al hombro, girando la muñeca.
3. Bájala entre las piernas con la cabeza arriba.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('fbb3d17c-c79a-5b8a-a26d-e57662d39c73', 'Peso muerto a una pierna con kettlebell', 'Kettlebell One-Legged Deadlift', 'FREE_EXERCISE_DB', 'Kettlebell_One-Legged_Deadlift', 'Isquiotibiales', '["Glúteos", "Lumbares"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De pie sobre una pierna con la kettlebell en la mano del mismo lado.
2. Inclínate desde la cadera llevando la pierna libre atrás.
3. Baja hasta quedar paralelo al suelo y vuelve erguido.', '["LOWER_BACK"]'::jsonb, true),
    ('52f521ee-bce4-5337-9514-0c0c48a16637', 'Extensión de tríceps sobre la cabeza con kettlebell', 'Kettlebell Overhead Triceps Extension', 'FREE_EXERCISE_DB', 'Kettlebell_Overhead_Triceps_Extension', 'Tríceps', '["Hombros", "Abdomen"]'::jsonb, 'KETTLEBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie, sostén la kettlebell por los cuernos sobre la cabeza.
2. Bájala detrás de la cabeza flexionando los codos, sin abrirlos.
3. Extiende los codos para volver.', '["SHOULDER", "ELBOW"]'::jsonb, false),
    ('11ef1812-1297-51fc-83e6-c25ffe2adcda', 'Pase de kettlebell entre las piernas', 'Kettlebell Pass Between The Legs', 'FREE_EXERCISE_DB', 'Kettlebell_Pass_Between_The_Legs', 'Abdomen', '["Glúteos", "Isquiotibiales", "Hombros"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la espalda plana y la cadera atrás.
2. Pasa la kettlebell de una mano a otra entre las piernas.
3. Repite varias veces alternando.', '[]'::jsonb, false),
    ('22bd650c-b137-51d2-be4a-9d9da71837f3', 'Barco pirata con kettlebell', 'Kettlebell Pirate Ships', 'FREE_EXERCISE_DB', 'Kettlebell_Pirate_Ships', 'Hombros', '["Abdomen"]'::jsonb, 'KETTLEBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con postura amplia, sostén la kettlebell con ambas manos a la altura de la cintura.
2. Gira hacia un lado balanceándola hasta la altura de la cabeza.
3. Déjala bajar y gira hacia el otro lado.', '[]'::jsonb, false),
    ('cb9e1f4c-0b4c-5d7e-8152-f7bef761d220', 'Sentadilla pistol con kettlebell', 'Kettlebell Pistol Squat', 'FREE_EXERCISE_DB', 'Kettlebell_Pistol_Squat', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Hombros"]'::jsonb, 'KETTLEBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sostén la kettlebell por los cuernos frente al pecho y levanta una pierna.
2. Baja sobre la otra pierna llevando la cadera atrás.
3. Pausa abajo y sube empujando con el talón.', '["KNEE"]'::jsonb, false),
    ('cb0baba1-9644-5a78-9c8b-66c1e9b91bad', 'Press sentado con kettlebell', 'Kettlebell Seated Press', 'FREE_EXERCISE_DB', 'Kettlebell_Seated_Press', 'Hombros', '["Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sentado en el suelo con las piernas abiertas y la kettlebell en el hombro.
2. Empújala sobre la cabeza hasta bloquear el codo.
3. Bájala al hombro.', '[]'::jsonb, false),
    ('7d670311-356e-5467-ad1b-2304c18bbae8', 'Press balancín con kettlebells', 'Kettlebell Seesaw Press', 'FREE_EXERCISE_DB', 'Kettlebell_Seesaw_Press', 'Hombros', '["Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Lleva dos kettlebells a los hombros.
2. Empuja una sobre la cabeza.
3. Al bajarla empuja la otra; mismas reps por lado.', '[]'::jsonb, false),
    ('117b768f-d86f-56b3-a818-34e661a5ea72', 'Remo alto sumo con kettlebell', 'Kettlebell Sumo High Pull', 'FREE_EXERCISE_DB', 'Kettlebell_Sumo_High_Pull', 'Trapecio', '["Aductores", "Glúteos", "Isquiotibiales", "Cuádriceps", "Hombros"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con postura amplia, toma la kettlebell entre los pies con ambas manos y la cadera atrás.
2. Extiende cadera y rodillas tirando de la kettlebell hasta los hombros con los codos altos.
3. Baja al inicio.', '["HIP"]'::jsonb, false),
    ('6b68d83e-a0e2-5116-b859-a99ff22497d7', 'Thruster con kettlebells', 'Kettlebell Thruster', 'FREE_EXERCISE_DB', 'Kettlebell_Thruster', 'Hombros', '["Cuádriceps", "Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Lleva dos kettlebells a los hombros.
2. Baja en sentadilla profunda con la espalda recta.
3. Sube con fuerza y empújalas sobre la cabeza en un solo movimiento.', '["KNEE"]'::jsonb, true),
    ('74f2a463-9d63-53be-875e-70f29ea1a73b', 'Levantamiento turco con kettlebell (zancada)', 'Kettlebell Turkish Get-Up (Lunge style)', 'FREE_EXERCISE_DB', 'Kettlebell_Turkish_Get-Up_Lunge_style', 'Hombros', '["Abdomen", "Isquiotibiales", "Cuádriceps", "Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Boca arriba con la kettlebell arriba y la rodilla del mismo lado flexionada.
2. Con el brazo libre, sube a sentado y luego a una rodilla, sin dejar de mirar la kettlebell.
3. Ponte de pie y revierte el movimiento.', '["KNEE", "HIP", "ANKLE"]'::jsonb, false),
    ('f6511b6a-d85e-5f8e-af98-d156cb940aee', 'Levantamiento turco con kettlebell (sentadilla)', 'Kettlebell Turkish Get-Up (Squat style)', 'FREE_EXERCISE_DB', 'Kettlebell_Turkish_Get-Up_Squat_style', 'Hombros', '["Abdomen", "Pantorrillas", "Isquiotibiales", "Cuádriceps", "Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Boca arriba con la kettlebell arriba y la rodilla del mismo lado flexionada.
2. Con el brazo libre, sube a sentado y pasa a sentadilla con el brazo bloqueado.
3. Ponte de pie y revierte el movimiento.', '["KNEE"]'::jsonb, false),
    ('08d42e94-a762-5682-af75-1d5b5bad230c', 'Molino con kettlebell', 'Kettlebell Windmill', 'FREE_EXERCISE_DB', 'Kettlebell_Windmill', 'Abdomen', '["Glúteos", "Isquiotibiales", "Hombros", "Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la kettlebell bloqueada sobre la cabeza y los pies girados 45°.
2. Lleva la cadera hacia el lado de la kettlebell y baja la mano libre por la pierna.
3. Vuelve erguido sin dejar de mirar la kettlebell.', '[]'::jsonb, false),
    ('94ce16bd-bfa5-5bf5-b4ef-3ffe52250b59', 'Muscle up con impulso', 'Kipping Muscle Up', 'FREE_EXERCISE_DB', 'Kipping_Muscle_Up', 'Espalda', '["Abdomen", "Bíceps", "Antebrazos", "Hombros", "Trapecio", "Tríceps"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Cuelga de las anillas con agarre falso.
2. Balancea las piernas atrás y luego adelante tirando con los brazos.
3. Lleva las anillas a las axilas, pasa por encima y termina con un fondo.', '["SHOULDER"]'::jsonb, false),
    ('20649982-e46f-5457-b2f6-677bb84f29f4', 'Rodilla cruzada acostado', 'Knee Across The Body', 'FREE_EXERCISE_DB', 'Knee_Across_The_Body', 'Glúteos', '["Abductores", "Lumbares"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Boca arriba con una pierna recta.
2. Cruza la otra rodilla flexionada hacia el lado contrario sujetándola con la mano.
3. Gira la cabeza al lado opuesto, sostén y cambia de lado.', '[]'::jsonb, false),
    ('8058614f-0084-5719-bb92-4af1af654236', 'Círculos de rodillas', 'Knee Circles', 'FREE_EXERCISE_DB', 'Knee_Circles', 'Pantorrillas', '["Isquiotibiales", "Cuádriceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'COMPOUND', 'PULL', '1. De pie con las piernas juntas y las manos en la cintura.
2. Mueve las rodillas en círculos respirando normal.
3. Repite en ambas direcciones.', '[]'::jsonb, false),
    ('e4733063-9c3e-5b80-8e5a-72081fed2ee5', 'Elevación de piernas en paralelas', 'Knee/Hip Raise On Parallel Bars', 'FREE_EXERCISE_DB', 'Knee_Hip_Raise_On_Parallel_Bars', 'Abdomen', '[]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Apoya los antebrazos en la silla romana con la espalda contra el respaldo.
2. Sube las piernas rectas hasta que queden paralelas al suelo.
3. Baja despacio.', '[]'::jsonb, false),
    ('9d6611b2-18b0-52e6-8d94-57f82a5c8872', 'Salto con rodillas al pecho', 'Knee Tuck Jump', 'FREE_EXERCISE_DB', 'Knee_Tuck_Jump', 'Isquiotibiales', '["Abductores", "Aductores", "Pantorrillas", "Glúteos", "Cuádriceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie con las manos frente al pecho, palmas abajo.
2. Baja un poco y salta llevando las rodillas a las manos.
3. Cae suave flexionando las rodillas.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('b7a8af0f-5912-556c-a1c0-dc4512b62aa0', 'Braceo de carrera arrodillado', 'Kneeling Arm Drill', 'FREE_EXERCISE_DB', 'Kneeling_Arm_Drill', 'Hombros', '["Abdomen"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', NULL, 'PULL', '1. Arrodillado con un pie adelante.
2. Balancea los brazos como al correr, primero largo y luego más rápido.
3. Acelera hasta ritmo de sprint y cambia de rodilla.', '[]'::jsonb, false),
    ('6067d811-e1b5-5217-b2bc-80a95dc2fca5', 'Crunch en polea arrodillado con giro', 'Kneeling Cable Crunch With Alternating Oblique Twists', 'FREE_EXERCISE_DB', 'Kneeling_Cable_Crunch_With_Alternating_Oblique_Twists', 'Abdomen', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Arrodillado frente a la polea alta, con la cuerda junto a las orejas.
2. Contrae el abdomen y baja los codos hacia las rodillas.
3. Alterna el crunch al centro y girando hacia cada rodilla.', '[]'::jsonb, false),
    ('e81ed629-9d21-5c9d-875f-4a8885ff194d', 'Extensión de tríceps arrodillado en polea', 'Kneeling Cable Triceps Extension', 'FREE_EXERCISE_DB', 'Kneeling_Cable_Triceps_Extension', 'Tríceps', '[]'::jsonb, 'CABLE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Arrodillado de espaldas a la polea alta, con los brazos sobre un banco y la barra detrás de la cabeza.
2. Extiende los codos en semicírculo sin mover los brazos.
3. Vuelve despacio.', '["ELBOW"]'::jsonb, false),
    ('48e5837a-aa3c-59b0-bbc2-82907135260a', 'Estiramiento de antebrazo arrodillado', 'Kneeling Forearm Stretch', 'FREE_EXERCISE_DB', 'Kneeling_Forearm_Stretch', 'Antebrazos', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Arrodillado con las palmas en el suelo y los dedos hacia las rodillas.
2. Inclínate atrás despacio sin despegar las palmas.
3. Sostén 20 a 30 s.', '[]'::jsonb, false),
    ('bedde583-b99a-5d1f-ab75-32a8a2d31636', 'Remo en polea alta arrodillado', 'Kneeling High Pulley Row', 'FREE_EXERCISE_DB', 'Kneeling_High_Pulley_Row', 'Espalda', '["Bíceps"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Arrodillado frente a la polea alta, con la cuerda y los brazos extendidos.
2. Tira de la cuerda hacia el pecho alto con los codos abiertos.
3. Junta los omóplatos, pausa y vuelve despacio.', '[]'::jsonb, false),
    ('17b74e3e-35d1-53de-8d12-15080c2e220f', 'Estiramiento de flexor de cadera arrodillado', 'Kneeling Hip Flexor', 'FREE_EXERCISE_DB', 'Kneeling_Hip_Flexor', 'Cuádriceps', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Arrodillado con un pie adelante y la otra rodilla atrás en el suelo.
2. Lleva el peso hacia adelante hasta sentir el estiramiento en la cadera.
3. Sostén 15 s y cambia de lado.', '[]'::jsonb, false),
    ('2ab56093-7069-5ffc-b1c2-2a48eb650eea', 'Sentadilla con salto desde rodillas', 'Kneeling Jump Squat', 'FREE_EXERCISE_DB', 'Kneeling_Jump_Squat', 'Glúteos', '["Pantorrillas", "Isquiotibiales", "Cuádriceps"]'::jsonb, 'BARBELL', 'ADVANCED', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PUSH', '1. Arrodillado con la barra sobre la espalda o sin peso.
2. Lleva los glúteos a los talones y explota con la cadera hasta caer con los pies planos.
3. Termina la sentadilla hasta quedar de pie.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('cde61ca7-abd2-50f7-9664-882581b18e56', 'Remo a una mano en polea alta arrodillado', 'Kneeling Single-Arm High Pulley Row', 'FREE_EXERCISE_DB', 'Kneeling_Single-Arm_High_Pulley_Row', 'Espalda', '["Bíceps"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Arrodillado frente a la polea alta, con el agarre en una mano y la palma al frente.
2. Tira hacia el torso girando la muñeca hasta que la palma mire hacia ti.
3. Pausa y vuelve despacio.', '[]'::jsonb, false),
    ('b9613324-7386-55ba-a43c-6e1b38c2cf97', 'Sentadilla de rodillas con barra', 'Kneeling Squat', 'FREE_EXERCISE_DB', 'Kneeling_Squat', 'Glúteos', '["Abdomen", "Isquiotibiales", "Lumbares"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. Arrodillado en el rack con la barra sobre la espalda.
2. Lleva los glúteos hacia los talones con la cabeza al frente.
3. Extiende la cadera hasta quedar erguido de rodillas.', '["KNEE"]'::jsonb, false),
    ('691c7d26-982d-5a1c-a3dc-d5f3464324cb', 'Landmine 180°', 'Landmine 180''s', 'FREE_EXERCISE_DB', 'Landmine_180s', 'Abdomen', '["Glúteos", "Lumbares", "Hombros"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la barra anclada, súbela a la altura de los hombros con los brazos extendidos.
2. Gira tronco y cadera llevándola hacia un lado.
3. Pásala al otro lado sin flexionar los brazos.', '[]'::jsonb, false),
    ('0c059810-c6d6-5fb7-a772-e32867831490', 'Landmine jammer', 'Landmine Linear Jammer', 'FREE_EXERCISE_DB', 'Landmine_Linear_Jammer', 'Hombros', '["Abdomen", "Pantorrillas", "Pecho", "Isquiotibiales", "Cuádriceps", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra anclada y el extremo a la altura de los hombros.
2. Baja en media sentadilla con los brazos flexionados.
3. Extiende cadera, rodillas y codos con fuerza empujando la barra.', '[]'::jsonb, false),
    ('cdd445f3-618d-592e-abd9-d15bc5708d02', 'Salto lateral', 'Lateral Bound', 'FREE_EXERCISE_DB', 'Lateral_Bound', 'Aductores', '["Abductores", "Pantorrillas", "Glúteos", "Isquiotibiales", "Cuádriceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. En media sentadilla de lado a la dirección del salto.
2. Empuja con la pierna de afuera y salta lo más lejos posible hacia el costado.
3. Al caer, salta de vuelta al otro lado.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('52ce60e9-8c3a-5512-b4c3-76f2420a4ad7', 'Salto lateral al cajón', 'Lateral Box Jump', 'FREE_EXERCISE_DB', 'Lateral_Box_Jump', 'Aductores', '["Abductores", "Pantorrillas", "Glúteos", "Isquiotibiales", "Cuádriceps"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie junto a un cajón bajo.
2. Baja un poco y salta hacia arriba y al costado sobre el cajón.
3. Baja del otro lado y repite de ida y vuelta.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('d7e2a1b2-ade3-587b-ab31-1b70eab85d2a', 'Saltos laterales sobre conos', 'Lateral Cone Hops', 'FREE_EXERCISE_DB', 'Lateral_Cone_Hops', 'Aductores', '["Abductores", "Pantorrillas", "Glúteos", "Isquiotibiales", "Cuádriceps"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De lado a una fila de conos.
2. Salta de lado sobre cada cono.
3. Amortigua y encadena el siguiente salto.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('4ad9d37c-ec5e-503a-9e32-b39071ca22d7', 'Elevaciones laterales con banda', 'Lateral Raise - With Bands', 'FREE_EXERCISE_DB', 'Lateral_Raise_-_With_Bands', 'Hombros', '[]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie sobre la banda, con los agarres a los costados de los muslos.
2. Sube los brazos a los costados con los codos apenas flexionados.
3. Pasa un poco la altura de los hombros y baja despacio.', '[]'::jsonb, true),
    ('ff4f0b93-f38e-5c6b-98c0-82899901c0b0', 'Liberación de dorsales con rodillo', 'Latissimus Dorsi-SMR', 'FREE_EXERCISE_DB', 'Latissimus_Dorsi-SMR', 'Espalda', '[]'::jsonb, 'OTHER', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. De costado con el rodillo justo debajo de la axila.
2. Apoya el peso sobre el dorsal con el brazo estirado arriba.
3. Sostén 10 a 30 s y cambia de lado.', '[]'::jsonb, false),
    ('dd10bb76-5dac-5598-aaea-f98d7e186bca', 'Press en el suelo con pierna cruzada con kettlebell', 'Leg-Over Floor Press', 'FREE_EXERCISE_DB', 'Leg-Over_Floor_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado en el suelo con la kettlebell sobre el pecho.
2. Cruza la pierna del lado que trabaja sobre la otra y empuja hasta extender el brazo.
3. Baja hasta que el codo toque el suelo.', '[]'::jsonb, false),
    ('2e4c8187-3de4-559c-a173-75dac3111ff7', 'Estiramiento de isquiotibiales con pierna arriba', 'Leg-Up Hamstring Stretch', 'FREE_EXERCISE_DB', 'Leg-Up_Hamstring_Stretch', 'Isquiotibiales', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'PUSH', '1. Boca arriba con una rodilla flexionada y el pie en el suelo.
2. Sube la otra pierna con la planta hacia el techo.
3. Estírala lo más posible, acércala a la cara y cambia de lado.', '[]'::jsonb, false),
    ('e74fa289-03bc-5764-89db-0b749310e13f', 'Elevación de pierna atrás de pie', 'Leg Lift', 'FREE_EXERCISE_DB', 'Leg_Lift', 'Glúteos', '["Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie y apoyado en un soporte.
2. Flexiona una rodilla llevando el talón hacia el glúteo, con o sin tobillera.
3. Baja despacio y cambia de pierna.', '[]'::jsonb, false),
    ('4e94621f-b759-5688-8d50-a5034acba161', 'Rodillas al pecho en el suelo', 'Leg Pull-In', 'FREE_EXERCISE_DB', 'Leg_Pull-In', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Boca arriba con las piernas extendidas y las manos junto al cuerpo.
2. Flexiona las rodillas llevando los muslos al abdomen.
3. Pausa un segundo y extiende de nuevo.', '[]'::jsonb, false),
    ('7f882a52-e2d5-5543-aeef-1995d4e867fa', 'Peso muerto en máquina', 'Leverage Deadlift', 'FREE_EXERCISE_DB', 'Leverage_Deadlift', 'Cuádriceps', '["Glúteos", "Isquiotibiales"]'::jsonb, 'MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Colócate entre los agarres y tómalos con la cadera baja y el pecho arriba.
2. Empuja con los talones extendiendo cadera y rodillas.
3. Baja con control.', '["LOWER_BACK"]'::jsonb, false),
    ('b073ebfe-96b9-5b64-b7ff-7e0d00c2f33e', 'Press declinado en máquina', 'Leverage Decline Chest Press', 'FREE_EXERCISE_DB', 'Leverage_Decline_Chest_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Ajusta el asiento para que los agarres queden a la altura del pecho bajo.
2. Empuja los agarres al frente extendiendo los codos.
3. Vuelve sin apoyar el peso entre reps.', '[]'::jsonb, false),
    ('46307760-b85e-580f-a3ae-e4e99fffe307', 'Press de hombros en máquina de palanca', 'Leverage Shoulder Press', 'FREE_EXERCISE_DB', 'Leverage_Shoulder_Press', 'Hombros', '["Tríceps"]'::jsonb, 'MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Ajusta el asiento para que los agarres queden a la altura de los hombros.
2. Empuja hacia arriba extendiendo los codos.
3. Vuelve sin apoyar el peso entre reps.', '["SHOULDER"]'::jsonb, false),
    ('5a2db7fb-f097-564e-bb19-90b585920635', 'Encogimientos en máquina', 'Leverage Shrug', 'FREE_EXERCISE_DB', 'Leverage_Shrug', 'Trapecio', '["Antebrazos"]'::jsonb, 'MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Colócate entre los agarres y súbelos de pie con la espalda recta.
2. Encoge los hombros hacia las orejas con los brazos rectos.
3. Pausa y baja despacio.', '["NECK"]'::jsonb, false),
    ('aec3a881-6b92-5e13-8cb5-954bbac4d928', 'Salida de sprint en 3 apoyos', 'Linear 3-Part Start Technique', 'FREE_EXERCISE_DB', 'Linear_3-Part_Start_Technique', 'Isquiotibiales', '["Pantorrillas", "Cuádriceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. Con un pie adelante, el otro atrás y una mano en la línea.
2. Inclínate con la cabeza por debajo de la cadera y el peso en la pierna de adelante.
3. Sal con fuerza y acelera al sprint.', '[]'::jsonb, false),
    ('94c7087e-b51d-56c3-846a-4d7ead7b5392', 'Drill de aceleración contra la pared', 'Linear Acceleration Wall Drill', 'FREE_EXERCISE_DB', 'Linear_Acceleration_Wall_Drill', 'Isquiotibiales', '["Pantorrillas", "Glúteos", "Cuádriceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', NULL, '1. Inclinado unos 45° contra la pared, pies juntos.
2. Sube una rodilla rápido y clava el pie hacia abajo.
3. Alterna las piernas lo más rápido posible.', '[]'::jsonb, false),
    ('0ce9ceff-c544-5c15-9b6d-350dc0f5e166', 'Salto en profundidad entre cajones', 'Linear Depth Jump', 'FREE_EXERCISE_DB', 'Linear_Depth_Jump', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie sobre un cajón frente a otro.
2. Déjate caer al suelo flexionando rodillas y cadera.
3. Rebota con fuerza y salta al otro cajón.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('93590bad-5963-58df-afbe-1165a5613750', 'Levantamiento de tronco', 'Log Lift', 'FREE_EXERCISE_DB', 'Log_Lift', 'Hombros', '["Abdomen", "Pecho", "Glúteos", "Isquiotibiales", "Lumbares", "Espalda", "Cuádriceps", "Trapecio", "Tríceps"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRONGMAN', 'Strongman', 'COMPOUND', 'PUSH', '1. Toma el tronco y súbelo al pecho extendiendo cadera y rodillas.
2. Apóyalo en el pecho, flexiona un poco las rodillas.
3. Impulsa con las piernas y empújalo sobre la cabeza.', '["LOWER_BACK"]'::jsonb, false),
    ('030379e2-8941-5380-a24f-107eec17a02c', 'Puentes de Londres', 'London Bridges', 'FREE_EXERCISE_DB', 'London_Bridges', 'Espalda', '["Bíceps", "Antebrazos"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De pie sobre una barra fija, sujeto a una cuerda colgada.
2. Con el cuerpo recto, baja hacia atrás mano sobre mano por la cuerda.
3. Al quedar horizontal, sube del mismo modo.', '[]'::jsonb, false),
    ('fe7aee46-6f0c-5e8e-a3fb-5e3bed62e70e', 'Mirada al techo', 'Looking At Ceiling', 'FREE_EXERCISE_DB', 'Looking_At_Ceiling', 'Cuádriceps', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Arrodillado, sujeta los talones con las manos.
2. Empuja la cadera hacia adelante arqueando la espalda.
3. Lleva la cabeza atrás mirando al techo y sostén.', '[]'::jsonb, false),
    ('9fab6a85-ff67-5d52-857e-4b9362b54c00', 'Extensión de tríceps acostado en polea baja', 'Low Cable Triceps Extension', 'FREE_EXERCISE_DB', 'Low_Cable_Triceps_Extension', 'Tríceps', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Boca arriba con la cabeza hacia la polea y la cuerda tomada, codos hacia el techo.
2. Extiende los codos sin mover los brazos.
3. Vuelve despacio.', '["ELBOW"]'::jsonb, false),
    ('65f48aef-8eeb-57ea-851e-bcd0d83cff40', 'Remo al cuello en polea baja', 'Low Pulley Row To Neck', 'FREE_EXERCISE_DB', 'Low_Pulley_Row_To_Neck', 'Hombros', '["Bíceps", "Espalda", "Trapecio"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Sentado frente a la polea baja con la cuerda y la espalda recta.
2. Tira de la cuerda hacia el cuello con los codos altos.
3. Vuelve despacio.', '["NECK"]'::jsonb, false),
    ('21e71963-cfec-5b44-a935-cc0b985bd840', 'Liberación de zona lumbar con rodillo', 'Lower Back-SMR', 'FREE_EXERCISE_DB', 'Lower_Back-SMR', 'Lumbares', '[]'::jsonb, 'OTHER', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado con el rodillo bajo la zona lumbar y los brazos cruzados.
2. Inclínate atrás apoyando el peso a un costado de la columna, no sobre ella.
3. Rueda y sostén 10 a 30 s en los puntos tensos de cada lado.', '[]'::jsonb, false),
    ('1111715a-4a28-5c6c-8a93-953b97e4dde3', 'Extensión lumbar en el suelo', 'Lower Back Curl', 'FREE_EXERCISE_DB', 'Lower_Back_Curl', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Boca abajo con los brazos a los costados.
2. Despega el pecho del suelo usando la zona lumbar, sin empujar con los brazos.
3. Baja y repite 10 a 20 veces.', '[]'::jsonb, false),
    ('15901179-5cc0-5591-86fa-5f7136c237ea', 'Zancada con pase de kettlebell', 'Lunge Pass Through', 'FREE_EXERCISE_DB', 'Lunge_Pass_Through', 'Isquiotibiales', '["Pantorrillas", "Glúteos", "Cuádriceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie con la kettlebell en una mano.
2. Da una zancada y pasa la kettlebell por debajo de la pierna delantera a la otra mano.
3. Vuelve y alterna la pierna.', '["KNEE", "HIP", "ANKLE"]'::jsonb, false),
    ('1cc0467a-a569-55fd-9674-3f78c3ac2b82', 'Zancada explosiva en máquina Smith', 'Lunge Sprint', 'FREE_EXERCISE_DB', 'Lunge_Sprint', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'MACHINE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra sobre la espalda en la Smith y los pies en tijera.
2. Baja la rodilla trasera casi al suelo.
3. Sube con fuerza desde el talón delantero hasta despegar un poco.', '["KNEE", "HIP", "ANKLE"]'::jsonb, false),
    ('09dfdfa5-c005-57ae-8971-d1fcb5235ce7', 'Estiramiento de aductores asistido', 'Lying Bent Leg Groin', 'FREE_EXERCISE_DB', 'Lying_Bent_Leg_Groin', 'Aductores', '[]'::jsonb, 'OTHER', 'ADVANCED', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Boca arriba con las plantas juntas y un compañero sujetando tus rodillas.
2. Intenta juntar las rodillas contra su resistencia 10 a 20 s.
3. Relaja mientras te empuja las rodillas suave hacia el suelo.', '[]'::jsonb, false),
    ('36df3529-2848-5f03-9f05-3c49da82669a', 'Curl acostado en polea baja', 'Lying Cable Curl', 'FREE_EXERCISE_DB', 'Lying_Cable_Curl', 'Bíceps', '[]'::jsonb, 'CABLE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca arriba frente a la polea baja, con la barra en agarre supino y los pies contra la máquina.
2. Flexiona los codos llevando la barra al pecho sin mover los brazos.
3. Aprieta y vuelve despacio.', '[]'::jsonb, false),
    ('f4cbf959-31a5-5104-96d0-a7d874548b43', 'Remo boca abajo con barra curva', 'Lying Cambered Barbell Row', 'FREE_EXERCISE_DB', 'Lying_Cambered_Barbell_Row', 'Espalda', '["Bíceps", "Trapecio"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca abajo en un banco, toma la barra curva por debajo con agarre ancho.
2. Tira hacia el pecho o el abdomen con los codos cerca.
3. Pausa y baja despacio.', '["LOWER_BACK"]'::jsonb, false),
    ('f4e7e0d8-d23c-5add-a206-a6a468aa77d9', 'Curl acostado en polea alta', 'Lying Close-Grip Bar Curl On High Pulley', 'FREE_EXERCISE_DB', 'Lying_Close-Grip_Bar_Curl_On_High_Pulley', 'Bíceps', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca arriba en un banco frente a la polea alta, con la barra en agarre supino y los brazos arriba.
2. Flexiona los codos llevando la barra hacia la barbilla.
3. Aprieta y vuelve despacio.', '[]'::jsonb, false),
    ('bf8c2a0d-7c35-517e-acb2-1e0ccc67f5a4', 'Extensión de tríceps con barra tras la cabeza', 'Lying Close-Grip Barbell Triceps Extension Behind The Head', 'FREE_EXERCISE_DB', 'Lying_Close-Grip_Barbell_Triceps_Extension_Behind_The_Head', 'Tríceps', '[]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Acostado en banco plano, lleva la barra tras la cabeza con los brazos casi paralelos al suelo.
2. Flexiona los codos bajando la barra.
3. Extiéndelos sin mover los brazos.', '["ELBOW"]'::jsonb, false),
    ('6ad9b1c6-1a2a-5f8d-abc1-e677dfa3f1ab', 'Extensión de tríceps a la barbilla con barra Z', 'Lying Close-Grip Barbell Triceps Press To Chin', 'FREE_EXERCISE_DB', 'Lying_Close-Grip_Barbell_Triceps_Press_To_Chin', 'Tríceps', '[]'::jsonb, 'EZ_BAR', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Acostado en banco plano con la barra Z arriba y agarre cerrado.
2. Baja la barra en semicírculo hacia la barbilla.
3. Extiende los codos para volver.', '[]'::jsonb, false),
    ('5fd70708-9b64-5440-b932-4f109b3f7116', 'Estiramiento cruzado asistido', 'Lying Crossover', 'FREE_EXERCISE_DB', 'Lying_Crossover', 'Abductores', '[]'::jsonb, 'BODYWEIGHT', 'ADVANCED', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Boca arriba, cruza una rodilla flexionada sobre el cuerpo; un compañero te sujeta.
2. Intenta subir la rodilla contra su resistencia 10 a 20 s.
3. Relaja mientras la empuja suave hacia el suelo; cambia de lado.', '[]'::jsonb, false),
    ('b7b7d4bd-8f8c-5428-aa92-a3ffb3f4f0b0', 'Extensión de tríceps acostado con mancuernas', 'Lying Dumbbell Tricep Extension', 'FREE_EXERCISE_DB', 'Lying_Dumbbell_Tricep_Extension', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Acostado en banco plano con las mancuernas arriba y palmas enfrentadas.
2. Baja las mancuernas junto a las orejas sin mover los brazos.
3. Extiende los codos para volver.', '["ELBOW"]'::jsonb, false),
    ('44178bd9-a12e-5ce4-9e28-96f5903a0da8', 'Extensión de cuello con disco boca abajo', 'Lying Face Down Plate Neck Resistance', 'FREE_EXERCISE_DB', 'Lying_Face_Down_Plate_Neck_Resistance', 'Cuello', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca abajo en un banco con la cabeza fuera y un disco en la nuca.
2. Baja la cabeza despacio.
3. Súbela en semicírculo hasta el inicio.', '["NECK"]'::jsonb, true),
    ('00a1b248-d68a-5c9b-b85a-4e77116e325d', 'Flexión de cuello con disco boca arriba', 'Lying Face Up Plate Neck Resistance', 'FREE_EXERCISE_DB', 'Lying_Face_Up_Plate_Neck_Resistance', 'Cuello', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca arriba en un banco con la cabeza fuera y un disco en la frente.
2. Baja la cabeza hacia atrás despacio.
3. Súbela en semicírculo hasta el inicio.', '["NECK"]'::jsonb, false),
    ('01c8da29-414f-582d-b14c-92865adc0112', 'Estiramiento de glúteo asistido', 'Lying Glute', 'FREE_EXERCISE_DB', 'Lying_Glute', 'Glúteos', '["Abductores"]'::jsonb, 'BODYWEIGHT', 'ADVANCED', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Boca arriba, sube una pierna con el pie sobre la cadera contraria; un compañero te sujeta.
2. Empuja la pierna contra su resistencia 10 a 20 s.
3. Relaja mientras la lleva suave hacia tu pecho.', '[]'::jsonb, false),
    ('5c49dbb7-582e-5c7f-be46-216df1f9da91', 'Estiramiento de isquiotibiales asistido', 'Lying Hamstring', 'FREE_EXERCISE_DB', 'Lying_Hamstring', 'Isquiotibiales', '["Pantorrillas"]'::jsonb, 'OTHER', 'ADVANCED', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Boca arriba, sube una pierna recta y un compañero te sostiene el tobillo.
2. Intenta flexionar la rodilla contra su resistencia 10 a 20 s.
3. Relaja mientras empuja la pierna suave hacia ti.', '[]'::jsonb, false),
    ('1bca92be-496e-5074-a0fb-6288fd7f5267', 'Curl boca abajo en banco alto con barra', 'Lying High Bench Barbell Curl', 'FREE_EXERCISE_DB', 'Lying_High_Bench_Barbell_Curl', 'Bíceps', '[]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca abajo en un banco alto con el pecho en el borde y la barra colgando, agarre supino.
2. Flexiona los codos subiendo la barra sin mover los brazos.
3. Aprieta y baja despacio.', '["ELBOW", "WRIST"]'::jsonb, false),
    ('ea4b102b-109e-52a8-83d4-322aa49336de', 'Sentadilla acostado en máquina', 'Lying Machine Squat', 'FREE_EXERCISE_DB', 'Lying_Machine_Squat', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'MACHINE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado en la máquina con las rodillas flexionadas y los muslos un poco por debajo de la paralela.
2. Empuja la plataforma hasta casi extender las piernas.
3. Baja despacio.', '["KNEE"]'::jsonb, false),
    ('a47fa97c-38b9-5851-9a31-5ce7bfd22cbe', 'Elevación lateral a una mano boca abajo', 'Lying One-Arm Lateral Raise', 'FREE_EXERCISE_DB', 'Lying_One-Arm_Lateral_Raise', 'Hombros', '[]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca abajo en un banco con una mancuerna colgando, palma hacia ti.
2. Sube el brazo al costado hasta la altura del hombro.
3. Baja despacio y cambia de brazo.', '[]'::jsonb, false),
    ('04f1be0f-d208-5306-9032-7a716e044145', 'Estiramiento de cuádriceps asistido', 'Lying Prone Quadriceps', 'FREE_EXERCISE_DB', 'Lying_Prone_Quadriceps', 'Cuádriceps', '[]'::jsonb, 'BODYWEIGHT', 'ADVANCED', 'STRETCHING', 'Movilidad', NULL, NULL, '1. Boca abajo, flexiona una rodilla y un compañero sujeta tu pierna.
2. Intenta extender la rodilla contra su resistencia 10 a 20 s.
3. Relaja mientras lleva tu pie hacia el glúteo; cambia de lado.', '[]'::jsonb, false),
    ('0c1dc0ea-3b3e-5f1f-8c33-a4695ab69f11', 'Elevación de deltoide posterior boca abajo', 'Lying Rear Delt Raise', 'FREE_EXERCISE_DB', 'Lying_Rear_Delt_Raise', 'Hombros', '[]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca abajo en un banco plano con las mancuernas colgando, palmas hacia ti.
2. Sube los brazos a los costados hasta la altura de los hombros.
3. Baja despacio.', '[]'::jsonb, false),
    ('96130202-4ea0-51a9-aa78-344ce0dafdcc', 'Curl acostado con mancuernas', 'Lying Supine Dumbbell Curl', 'FREE_EXERCISE_DB', 'Lying_Supine_Dumbbell_Curl', 'Bíceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca arriba en banco plano con los brazos colgando a los costados, palmas hacia adentro.
2. Flexiona los codos girando las palmas hacia arriba sin mover los brazos.
3. Aprieta y baja despacio.', '[]'::jsonb, false),
    ('c19d8c60-407f-5ff9-b08c-292e193a970c', 'Remo en T con apoyo de pecho', 'Lying T-Bar Row', 'FREE_EXERCISE_DB', 'Lying_T-Bar_Row', 'Espalda', '["Bíceps"]'::jsonb, 'MACHINE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Boca abajo en la máquina con el pecho alto en el respaldo y los brazos extendidos.
2. Tira de los agarres hacia el torso juntando los omóplatos.
3. Pausa y baja despacio.', '["LOWER_BACK"]'::jsonb, false),
    ('a94d37fd-94b5-58c6-90dd-3e71fb3bf6ad', 'Press francés con barra Z', 'Lying Triceps Press', 'FREE_EXERCISE_DB', 'Lying_Triceps_Press', 'Tríceps', '[]'::jsonb, 'EZ_BAR', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Acostado en banco plano con la barra arriba y agarre medio.
2. Baja la barra hacia la frente sin mover los brazos.
3. Extiende los codos para volver.', '["ELBOW"]'::jsonb, true),
    ('76a229c6-5bac-5cd9-8c46-f0ab496a1620', 'Press de banca en máquina', 'Machine Bench Press', 'FREE_EXERCISE_DB', 'Machine_Bench_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sentado en la máquina con los agarres a la altura del pecho y los codos abiertos.
2. Empuja al frente hasta extender los brazos.
3. Vuelve despacio sin apoyar el peso.', '["SHOULDER"]'::jsonb, false),
    ('40677f4f-ed47-54ff-a18a-674831fd6177', 'Pase de pecho con balón medicinal', 'Medicine Ball Chest Pass', 'FREE_EXERCISE_DB', 'Medicine_Ball_Chest_Pass', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. Frente a un compañero o una pared, con el balón a la altura del pecho.
2. Lánzalo extendiendo los codos con fuerza.
3. Recíbelo con ambas manos a la altura del pecho.', '[]'::jsonb, false),
    ('0c78c5ca-0179-5803-a647-5d0674177b07', 'Giro completo con balón medicinal', 'Medicine Ball Full Twist', 'FREE_EXERCISE_DB', 'Medicine_Ball_Full_Twist', 'Abdomen', '["Hombros"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PULL', '1. Espalda con espalda con un compañero, el balón frente al tronco.
2. Giren los dos en la misma dirección y pásale el balón.
3. Repitan girando hacia el otro lado.', '[]'::jsonb, false),
    ('7a0718fa-7e52-5b9e-87e2-330ff73cea0c', 'Lanzamiento hacia atrás desde sentadilla con balón medicinal', 'Medicine Ball Scoop Throw', 'FREE_EXERCISE_DB', 'Medicine_Ball_Scoop_Throw', 'Hombros', '["Abdomen", "Isquiotibiales", "Cuádriceps"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. En media sentadilla con el balón cerca de los pies.
2. Extiende cadera y piernas saltando y lleva los brazos rectos sobre la cabeza.
3. Suelta el balón en el punto más alto, lo más lejos posible hacia atrás.', '[]'::jsonb, false),
    ('d18b5d24-0cce-57c1-b9df-69797ed0ae68', 'Retracción escapular con mancuernas', 'Middle Back Shrug', 'FREE_EXERCISE_DB', 'Middle_Back_Shrug', 'Espalda', '[]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca abajo en banco inclinado con las mancuernas colgando, palmas enfrentadas.
2. Junta los omóplatos sin flexionar los codos y sostén un segundo.
3. Vuelve despacio.', '["NECK"]'::jsonb, false),
    ('4bdfd8b1-1dcf-5a57-a742-f978d0c4a3b7', 'Estiramiento de espalda media con giro', 'Middle Back Stretch', 'FREE_EXERCISE_DB', 'Middle_Back_Stretch', 'Espalda', '["Abdomen", "Lumbares"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. De pie con los pies al ancho de los hombros y las manos en la cadera.
2. Gira la cintura hasta sentir el estiramiento.
3. Sostén 10 a 15 s y gira al otro lado.', '[]'::jsonb, false),
    ('7ecfbb24-10cb-5323-ab04-c043804292f9', 'Dominadas con agarre mixto', 'Mixed Grip Chin', 'FREE_EXERCISE_DB', 'Mixed_Grip_Chin', 'Espalda', '["Bíceps"]'::jsonb, 'OTHER', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Cuelga de la barra con una palma al frente y la otra hacia ti.
2. Sube hasta pasar la barbilla sobre la barra.
3. Baja despacio y alterna el agarre en la siguiente serie.', '[]'::jsonb, false),
    ('6d20d129-d6de-58f9-8f41-6561999ff9fb', 'Caminata de monstruo con banda', 'Monster Walk', 'FREE_EXERCISE_DB', 'Monster_Walk', 'Abductores', '[]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con una banda en los tobillos y otra en las rodillas.
2. Da pasos cortos hacia adelante manteniendo la tensión.
3. Vuelve caminando hacia atrás.', '[]'::jsonb, true),
    ('1c06a1e6-a6b0-5b23-812e-d169ac1853d4', 'Escaladores', 'Mountain Climbers', 'FREE_EXERCISE_DB', 'Mountain_Climbers', 'Cuádriceps', '["Pecho", "Isquiotibiales", "Hombros"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PULL', '1. En posición de flexión con una rodilla bajo la cadera.
2. Cambia de pierna con un salto rápido.
3. Alterna 20 a 30 s.', '[]'::jsonb, true),
    ('e62551f8-73fa-5d9a-b224-556a72b6adc7', 'Drill de garra en carrera', 'Moving Claw Series', 'FREE_EXERCISE_DB', 'Moving_Claw_Series', 'Isquiotibiales', '["Pantorrillas", "Cuádriceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', NULL, '1. Corre llevando el talón hacia el glúteo al extender la cadera.
2. Vuelve a cargar la rodilla adelante y ataca el suelo en el siguiente paso.
3. Braceo rápido y coordinado.', '[]'::jsonb, false),
    ('f7fb0d08-65f0-5198-ace3-0bcaf9dc36c3', 'Arrancada de fuerza', 'Muscle Snatch', 'FREE_EXERCISE_DB', 'Muscle_Snatch', 'Isquiotibiales', '["Glúteos", "Lumbares", "Cuádriceps", "Hombros", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con agarre ancho y la barra a mitad del muslo, pecho arriba.
2. Extiende cadera, rodillas y tobillos con fuerza.
3. Sigue tirando hasta la cabeza sin meterte debajo, brazos extendidos.', '["LOWER_BACK", "SHOULDER"]'::jsonb, false),
    ('6b819548-39a3-5014-8d82-c5f1139cdbd8', 'Muscle up', 'Muscle Up', 'FREE_EXERCISE_DB', 'Muscle_Up', 'Espalda', '["Abdomen", "Bíceps", "Antebrazos", "Hombros", "Trapecio", "Tríceps"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Cuelga de las anillas con agarre falso.
2. Haz una dominada llevando las anillas a las axilas e inclínate adelante.
3. Pasa por encima y termina extendiendo los codos como en un fondo.', '["SHOULDER"]'::jsonb, false),
    ('9c9c59f3-acf5-525c-88ec-a365d29de6cd', 'Sentadilla hack con pies juntos', 'Narrow Stance Hack Squats', 'FREE_EXERCISE_DB', 'Narrow_Stance_Hack_Squats', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'MACHINE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En la máquina hack con la espalda en el respaldo y los pies juntos en la plataforma.
2. Baja hasta que los muslos queden paralelos.
3. Sube empujando con los talones.', '["KNEE"]'::jsonb, false),
    ('786e0ae3-1eec-57d3-b3ea-a2e37b3483bf', 'Prensa de piernas con pies juntos', 'Narrow Stance Leg Press', 'FREE_EXERCISE_DB', 'Narrow_Stance_Leg_Press', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'MACHINE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En la prensa con los pies juntos en el centro de la plataforma.
2. Baja hasta formar unos 90° con las rodillas.
3. Empuja con los talones sin bloquear las rodillas.', '["KNEE"]'::jsonb, false),
    ('57e77ed7-1052-5514-9d1f-cdedb17381ce', 'Sentadilla con pies juntos', 'Narrow Stance Squats', 'FREE_EXERCISE_DB', 'Narrow_Stance_Squats', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra sobre la espalda y los pies más juntos que los hombros.
2. Baja con el torso erguido hasta que los muslos queden paralelos.
3. Sube empujando con los talones.', '["KNEE"]'::jsonb, false),
    ('c15afb92-cbda-541d-9906-bdad8e607c05', 'Curl nórdico', 'Natural Glute Ham Raise', 'FREE_EXERCISE_DB', 'Natural_Glute_Ham_Raise', 'Isquiotibiales', '["Pantorrillas", "Glúteos", "Lumbares"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De rodillas con los tobillos sujetos bajo un apoyo, torso erguido.
2. Baja hacia adelante con control hasta casi extender las rodillas.
3. Sube con los isquiotibiales; ayúdate con una banda si hace falta.', '[]'::jsonb, true),
    ('66e2710e-b9f0-5fd8-a532-a7d43ce1225a', 'Liberación de cuello con rodillo', 'Neck-SMR', 'FREE_EXERCISE_DB', 'Neck-SMR', 'Cuello', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Coloca un rodillo pequeño detrás del cuello, a un lado de la columna.
2. Rueda despacio desde arriba hacia abajo.
3. Sostén 10 a 30 s en los puntos tensos.', '[]'::jsonb, false),
    ('171bd283-9d5c-5d6f-b247-6a9d61bcbfb7', 'Press al cuello con barra', 'Neck Press', 'FREE_EXERCISE_DB', 'Neck_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado en banco plano con agarre medio y la barra sobre el cuello.
2. Baja despacio hasta que la barra quede sobre el cuello.
3. Pausa y empuja con el pecho.', '["NECK"]'::jsonb, false),
    ('1bdde8c2-c699-53da-8533-894c482e22a1', 'Crunch oblicuo', 'Oblique Crunches', 'FREE_EXERCISE_DB', 'Oblique_Crunches', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca arriba con los pies elevados, una mano junto a la cabeza.
2. Sube ese hombro hacia la rodilla contraria.
3. Baja despacio y cambia de lado.', '[]'::jsonb, false),
    ('65658241-0912-5d58-8560-e168369b383c', 'Crunch oblicuo de costado', 'Oblique Crunches - On The Floor', 'FREE_EXERCISE_DB', 'Oblique_Crunches_-_On_The_Floor', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Acostado de costado con las rodillas algo flexionadas y la mano de arriba detrás de la cabeza.
2. Lleva el codo hacia la cadera contrayendo el oblicuo.
3. Sostén un segundo, baja y cambia de lado.', '[]'::jsonb, false),
    ('53bcb988-d0d5-5da2-a2bd-1cddf1f155a4', 'Sentadilla olímpica', 'Olympic Squat', 'FREE_EXERCISE_DB', 'Olympic_Squat', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PUSH', '1. Con la barra sobre los trapecios, pecho arriba y pies al ancho de la cadera.
2. Baja llevando las rodillas al frente y el torso lo más erguido posible.
3. Baja hasta el fondo y rebota con fuerza para subir.', '["KNEE"]'::jsonb, false),
    ('a5969611-8fd5-5781-b39e-21ff8db60caa', 'Estiramiento de cuádriceps boca arriba', 'On-Your-Back Quad Stretch', 'FREE_EXERCISE_DB', 'On-Your-Back_Quad_Stretch', 'Cuádriceps', '[]'::jsonb, 'OTHER', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Acostado en un banco con una pierna colgando por el borde.
2. Flexiona la rodilla y sujeta el empeine sin arquear la zona lumbar.
3. Empuja el pie contra la mano y cambia de lado.', '[]'::jsonb, false),
    ('b0176c0b-2a54-54e3-970a-4aaf831ef8a7', 'Estiramiento de cuádriceps de costado', 'On Your Side Quad Stretch', 'FREE_EXERCISE_DB', 'On_Your_Side_Quad_Stretch', 'Cuádriceps', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. De costado con la rodilla de abajo flexionada al frente.
2. Lleva el pie de arriba hacia el glúteo y sujétalo.
3. Empuja la cadera adelante, sostén y cambia de lado.', '[]'::jsonb, false),
    ('326aee49-f62b-5bd8-afaa-bb8d7c3dc4bf', 'Aperturas a una mano en banco plano', 'One-Arm Flat Bench Dumbbell Flye', 'FREE_EXERCISE_DB', 'One-Arm_Flat_Bench_Dumbbell_Flye', 'Pecho', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Acostado en banco plano con una mancuerna arriba y palma hacia adentro.
2. Baja el brazo hacia el costado con el codo apenas flexionado.
3. Vuelve por el mismo arco y cambia de brazo.', '[]'::jsonb, false),
    ('4c80bc07-9772-5dad-b7f6-a189c7040ddf', 'Inclinación lateral en polea alta a una mano', 'One-Arm High-Pulley Cable Side Bends', 'FREE_EXERCISE_DB', 'One-Arm_High-Pulley_Cable_Side_Bends', 'Abdomen', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De costado a la polea alta, con el agarre junto al hombro.
2. Contrae el oblicuo inclinándote hacia el lado contrario a la polea.
3. Vuelve despacio y cambia de lado.', '[]'::jsonb, false),
    ('7881eb9b-89ae-5da4-a274-cb0128ed3de7', 'Elevación lateral a una mano en banco inclinado', 'One-Arm Incline Lateral Raise', 'FREE_EXERCISE_DB', 'One-Arm_Incline_Lateral_Raise', 'Hombros', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De costado sobre un banco inclinado con la mancuerna frente al cuerpo.
2. Sube el brazo hasta que apunte al techo.
3. Baja despacio y cambia de lado.', '[]'::jsonb, false),
    ('24425d7d-efa8-555c-82d0-cbf146dbdc79', 'Cargada a una mano con kettlebell', 'One-Arm Kettlebell Clean', 'FREE_EXERCISE_DB', 'One-Arm_Kettlebell_Clean', 'Isquiotibiales', '["Glúteos", "Lumbares", "Hombros", "Trapecio"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la kettlebell entre los pies, cadera atrás y vista al frente.
2. Extiende piernas y cadera llevándola al hombro, girando la muñeca.
3. Bájala y repite.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('2aaf38fa-b386-579f-b037-53f6a4a0dc80', 'Cargada y envión a una mano con kettlebell', 'One-Arm Kettlebell Clean and Jerk', 'FREE_EXERCISE_DB', 'One-Arm_Kettlebell_Clean_and_Jerk', 'Hombros', '[]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Lleva la kettlebell al hombro extendiendo piernas y cadera.
2. Flexiona un poco las rodillas e impulsa hacia arriba.
3. Bloquea el brazo sobre la cabeza y bájala.', '["LOWER_BACK", "SHOULDER", "WRIST"]'::jsonb, false),
    ('c7eb127a-366e-5a11-b19d-51c74ca75388', 'Press en el suelo a una mano con kettlebell', 'One-Arm Kettlebell Floor Press', 'FREE_EXERCISE_DB', 'One-Arm_Kettlebell_Floor_Press', 'Pecho', '["Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado en el suelo con la kettlebell en una mano y el brazo apoyado.
2. Empuja hacia el techo girando la muñeca.
3. Baja al inicio y cambia de brazo.', '[]'::jsonb, false),
    ('7f3f4ffb-ecd8-5ddd-9820-e0c323ce2257', 'Envión a una mano con kettlebell', 'One-Arm Kettlebell Jerk', 'FREE_EXERCISE_DB', 'One-Arm_Kettlebell_Jerk', 'Hombros', '["Pantorrillas", "Cuádriceps", "Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la kettlebell en el hombro, palma al frente.
2. Flexiona un poco las rodillas e impulsa con fuerza.
3. Bloquea el brazo arriba metiéndote debajo y vuelve al hombro.', '["SHOULDER"]'::jsonb, false),
    ('64386854-2779-5347-8fec-19b3cb9bfc9b', 'Press militar a una mano con kettlebell', 'One-Arm Kettlebell Military Press To The Side', 'FREE_EXERCISE_DB', 'One-Arm_Kettlebell_Military_Press_To_The_Side', 'Hombros', '["Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la kettlebell en el hombro y la palma hacia adentro.
2. Empújala arriba y un poco hacia afuera hasta bloquear el codo.
3. Bájala con control apretando glúteos y abdomen.', '["SHOULDER"]'::jsonb, false),
    ('3a8e383a-c164-5a42-82fd-7746fe53b4cd', 'Press con codo abierto con kettlebell', 'One-Arm Kettlebell Para Press', 'FREE_EXERCISE_DB', 'One-Arm_Kettlebell_Para_Press', 'Hombros', '["Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la kettlebell en el hombro y el codo hacia el costado.
2. Empújala arriba hasta bloquear el codo.
3. Bájala con control.', '[]'::jsonb, false),
    ('8178612c-790e-5113-9bc2-6fda9091d55b', 'Push press a una mano con kettlebell', 'One-Arm Kettlebell Push Press', 'FREE_EXERCISE_DB', 'One-Arm_Kettlebell_Push_Press', 'Hombros', '["Pantorrillas", "Cuádriceps", "Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la kettlebell en el hombro, palma al frente.
2. Flexiona un poco las rodillas e impulsa con las piernas.
3. Usa el impulso para bloquearla arriba y bájala.', '["SHOULDER"]'::jsonb, false),
    ('21cf5ed9-b458-528d-867f-a7f6de34ca29', 'Remo a una mano con kettlebell', 'One-Arm Kettlebell Row', 'FREE_EXERCISE_DB', 'One-Arm_Kettlebell_Row', 'Espalda', '["Bíceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con las rodillas algo flexionadas, inclínate llevando la cadera atrás.
2. Tira de la kettlebell hacia el abdomen juntando el omóplato.
3. Bájala con la espalda recta.', '[]'::jsonb, false),
    ('124c9b38-9b85-5883-9cc9-5a3ac186e6f3', 'Arrancada a una mano con kettlebell', 'One-Arm Kettlebell Snatch', 'FREE_EXERCISE_DB', 'One-Arm_Kettlebell_Snatch', 'Hombros', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares", "Trapecio", "Tríceps"]'::jsonb, 'KETTLEBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la kettlebell entre los pies y la cadera atrás.
2. Balancéala entre las piernas y extiende cadera y rodillas con fuerza.
3. Gira la mano y recíbela bloqueada sobre la cabeza.', '["LOWER_BACK", "SHOULDER"]'::jsonb, false),
    ('03f2c6c6-0e63-5b7a-95b0-9d1e28f3fba9', 'Envión en tijera a una mano con kettlebell', 'One-Arm Kettlebell Split Jerk', 'FREE_EXERCISE_DB', 'One-Arm_Kettlebell_Split_Jerk', 'Hombros', '["Glúteos", "Isquiotibiales", "Cuádriceps", "Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la kettlebell en el hombro, palma al frente.
2. Impulsa con las piernas y cae en tijera con el brazo bloqueado.
3. Junta los pies y baja la kettlebell.', '["KNEE", "SHOULDER"]'::jsonb, false),
    ('406e2e44-080d-5a7d-b254-12f3536918b0', 'Arrancada en tijera a una mano con kettlebell', 'One-Arm Kettlebell Split Snatch', 'FREE_EXERCISE_DB', 'One-Arm_Kettlebell_Split_Snatch', 'Hombros', '["Isquiotibiales", "Cuádriceps"]'::jsonb, 'KETTLEBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la kettlebell en una mano, baja en sentadilla.
2. Extiende cadera, rodillas y tobillos subiéndola y cae en zancada.
3. Junta los pies con la kettlebell arriba y bájala.', '["KNEE", "LOWER_BACK", "SHOULDER"]'::jsonb, false),
    ('a5ec4538-8215-5796-854d-0849f159c84a', 'Swing a una mano con kettlebell', 'One-Arm Kettlebell Swings', 'FREE_EXERCISE_DB', 'One-Arm_Kettlebell_Swings', 'Isquiotibiales', '["Pantorrillas", "Glúteos", "Lumbares", "Hombros"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la kettlebell en una mano entre las piernas y la cadera atrás.
2. Extiende la cadera con fuerza y balancéala hasta la altura del pecho.
3. Déjala volver entre las piernas y repite.', '["LOWER_BACK"]'::jsonb, true),
    ('7ee792f6-133d-511d-be1b-4315d2bdc64c', 'Remo a una mano con barra en landmine', 'One-Arm Long Bar Row', 'FREE_EXERCISE_DB', 'One-Arm_Long_Bar_Row', 'Espalda', '["Bíceps"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la barra anclada, tómala con una mano cerca del extremo, inclinado y cadera atrás.
2. Tira hacia el costado juntando el omóplato.
3. Pausa y baja despacio.', '[]'::jsonb, false),
    ('b9507361-e768-5593-8b13-39326db343c8', 'Lanzamiento al suelo a una mano con balón medicinal', 'One-Arm Medicine Ball Slam', 'FREE_EXERCISE_DB', 'One-Arm_Medicine_Ball_Slam', 'Abdomen', '["Espalda", "Hombros"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. En postura escalonada con el balón en una mano.
2. Súbelo sobre la cabeza extendiendo todo el cuerpo.
3. Lánzalo con fuerza al suelo, atrápalo y repite.', '[]'::jsonb, false),
    ('a37ada90-1b20-5bf1-a733-76c53cd9611d', 'Cargada con palma abierta a una mano con kettlebell', 'One-Arm Open Palm Kettlebell Clean', 'FREE_EXERCISE_DB', 'One-Arm_Open_Palm_Kettlebell_Clean', 'Isquiotibiales', '["Antebrazos", "Glúteos", "Lumbares", "Cuádriceps", "Hombros"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la kettlebell entre los pies, súbela rápido con una mano.
2. Déjala girar para atrapar la bola en la palma.
3. Suéltala al frente, atrapa el asa y vuelve abajo.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('768202a9-cc43-59d6-af3a-d16a17291dc1', 'Sentadilla con kettlebell arriba a una mano', 'One-Arm Overhead Kettlebell Squats', 'FREE_EXERCISE_DB', 'One-Arm_Overhead_Kettlebell_Squats', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Hombros"]'::jsonb, 'KETTLEBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la kettlebell bloqueada sobre la cabeza.
2. Baja en sentadilla con el pecho arriba.
3. Pausa abajo y sube sin dejar de bloquear el brazo.', '["KNEE", "SHOULDER"]'::jsonb, false),
    ('29e531c3-e87a-5d2c-9785-68abba9b3132', 'Peso muerto lateral a una mano', 'One-Arm Side Deadlift', 'FREE_EXERCISE_DB', 'One-Arm_Side_Deadlift', 'Cuádriceps', '["Abdomen", "Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares", "Trapecio"]'::jsonb, 'BARBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De costado junto al centro de la barra.
2. Baja y tómala con una mano como un maletín.
3. Sube usando las piernas y baja despacio.', '["LOWER_BACK"]'::jsonb, false),
    ('a643b31a-27a1-558b-9f72-beb0171b38e7', 'Elevación lateral a una mano', 'One-Arm Side Laterals', 'FREE_EXERCISE_DB', 'One-Arm_Side_Laterals', 'Hombros', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie sujeto a un apoyo con la mano libre, inclinado un poco hacia afuera.
2. Sube la mancuerna al costado hasta la altura del hombro.
3. Baja despacio y cambia de lado.', '[]'::jsonb, false),
    ('493c443f-ab28-55a9-b3ae-b911bbecc8bd', 'Estiramiento de dorsal contra la pared', 'One Arm Against Wall', 'FREE_EXERCISE_DB', 'One_Arm_Against_Wall', 'Espalda', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. De pie, apoya un brazo flexionado contra la pared o el marco de una puerta.
2. Inclínate despacio hacia el brazo.
3. Sostén al sentir el estiramiento y cambia de lado.', '[]'::jsonb, false),
    ('46cc1c0f-fd33-514b-86c2-014dc5f1a0fb', 'Dominada a una mano', 'One Arm Chin-Up', 'FREE_EXERCISE_DB', 'One_Arm_Chin-Up', 'Espalda', '["Bíceps", "Antebrazos"]'::jsonb, 'OTHER', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con una mano en la barra y la otra en una toalla colgada de ella.
2. Sube hasta llevar el pecho a la barra.
3. Baja despacio y cambia de lado.', '[]'::jsonb, false),
    ('bb43c1a0-ce3c-5f64-a913-095f4b9426d9', 'Press de banca a una mano con mancuerna', 'One Arm Dumbbell Bench Press', 'FREE_EXERCISE_DB', 'One_Arm_Dumbbell_Bench_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado en banco plano con una mancuerna arriba, palma al frente.
2. Bájala al costado del pecho.
3. Empuja hasta extender el brazo y cambia de lado.', '["SHOULDER"]'::jsonb, false),
    ('b28e8ccf-9747-51ad-b995-1f331dce9dba', 'Curl predicador a una mano con mancuerna', 'One Arm Dumbbell Preacher Curl', 'FREE_EXERCISE_DB', 'One_Arm_Dumbbell_Preacher_Curl', 'Bíceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Con el brazo apoyado en el banco predicador y la mancuerna en la mano.
2. Bájala hasta extender el brazo.
3. Súbela apretando el bíceps.', '[]'::jsonb, false),
    ('9c890313-1abe-5975-b703-7dda2cb5e300', 'Press en el suelo a una mano con barra', 'One Arm Floor Press', 'FREE_EXERCISE_DB', 'One_Arm_Floor_Press', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado en el suelo con las rodillas flexionadas y la barra en una mano, agarre neutro.
2. Bájala hasta que el codo toque el suelo.
3. Empuja hasta extender el brazo.', '[]'::jsonb, false),
    ('57a0ad29-c349-5fdb-8863-18d4e6fc839a', 'Jalón a una mano', 'One Arm Lat Pulldown', 'FREE_EXERCISE_DB', 'One_Arm_Lat_Pulldown', 'Espalda', '["Bíceps"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Sentado en el jalón con las rodillas sujetas y un agarre en una mano.
2. Tira llevando el codo al costado.
3. Pausa y vuelve despacio sin soltar la tensión.', '[]'::jsonb, false),
    ('26f9a0f9-07f9-5854-ac4c-13aa4cae44c1', 'Extensión de tríceps a una mano en pronación', 'One Arm Pronated Dumbbell Triceps Extension', 'FREE_EXERCISE_DB', 'One_Arm_Pronated_Dumbbell_Triceps_Extension', 'Tríceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Acostado en banco con la mancuerna arriba y la palma hacia los pies.
2. Bájala flexionando el codo sin mover el brazo.
3. Extiende el codo para volver.', '["ELBOW"]'::jsonb, false),
    ('4aa6f2eb-42c5-5a19-ad0a-ab94ec5d6d3f', 'Extensión de tríceps a una mano en supinación', 'One Arm Supinated Dumbbell Triceps Extension', 'FREE_EXERCISE_DB', 'One_Arm_Supinated_Dumbbell_Triceps_Extension', 'Tríceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Acostado en banco con la mancuerna arriba y la palma hacia la cara.
2. Bájala flexionando el codo sin mover el brazo.
3. Extiende el codo para volver.', '["ELBOW"]'::jsonb, false),
    ('0af7b44d-8fcf-5bc9-b83d-f6031e20ae71', 'Media langosta', 'One Half Locust', 'FREE_EXERCISE_DB', 'One_Half_Locust', 'Cuádriceps', '["Abdomen", "Bíceps", "Pecho"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Boca abajo, flexiona una rodilla y sujeta el pie con la mano del mismo lado.
2. Sube el pie y despega los hombros del suelo.
3. Sostén y cambia de lado.', '[]'::jsonb, false),
    ('4ed25a99-9d1f-5c81-9938-bd7b353aa42d', 'Colgado a una mano', 'One Handed Hang', 'FREE_EXERCISE_DB', 'One_Handed_Hang', 'Espalda', '["Bíceps"]'::jsonb, 'OTHER', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Toma una barra con una mano, sin despegar los pies del suelo.
2. Deja colgar la mayor parte del peso del brazo.
3. Sostén 10 a 20 s y cambia de lado.', '[]'::jsonb, false),
    ('890a3813-908b-5ff1-a172-1040f262c41a', 'Rodilla al pecho', 'One Knee To Chest', 'FREE_EXERCISE_DB', 'One_Knee_To_Chest', 'Glúteos', '["Isquiotibiales", "Lumbares"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'COMPOUND', 'STATIC', '1. Boca arriba con una pierna extendida.
2. Lleva la otra rodilla al pecho sujetándola por debajo.
3. Tira suave hacia la cara y cambia de lado.', '[]'::jsonb, false),
    ('984fbfad-f0ad-5028-bde4-2d10de7a583d', 'Sentadilla búlgara con barra', 'One Leg Barbell Squat', 'FREE_EXERCISE_DB', 'One_Leg_Barbell_Squat', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BARBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra sobre la espalda y un pie apoyado atrás en un banco.
2. Baja hasta que el muslo delantero quede paralelo.
3. Sube empujando con el talón y cambia de pierna.', '["KNEE", "LOWER_BACK"]'::jsonb, false),
    ('a3496a4b-d87a-5f51-b7f4-df1b57326706', 'Cargada con palma abierta con kettlebell', 'Open Palm Kettlebell Clean', 'FREE_EXERCISE_DB', 'Open_Palm_Kettlebell_Clean', 'Isquiotibiales', '["Glúteos", "Lumbares", "Cuádriceps", "Hombros"]'::jsonb, 'KETTLEBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la kettlebell entre los pies, súbela extendiendo piernas y cadera.
2. Suéltala para atrapar la bola con las palmas.
3. Suéltala al frente, atrapa el asa con ambas manos y baja.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('5e93624b-0837-5778-bb7d-c651c8cc759f', 'Abdominal con press (Otis-up)', 'Otis-Up', 'FREE_EXERCISE_DB', 'Otis-Up', 'Abdomen', '["Pecho", "Hombros", "Tríceps"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Boca arriba con los pies sujetos y un peso en el pecho.
2. Sube el torso y empuja el peso sobre la cabeza.
3. Vuelve sin apoyar la espalda del todo.', '[]'::jsonb, false),
    ('764318ef-e7fa-50cd-b0b5-d85d131a996d', 'Curl en polea alta doble', 'Overhead Cable Curl', 'FREE_EXERCISE_DB', 'Overhead_Cable_Curl', 'Bíceps', '[]'::jsonb, 'CABLE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Entre dos poleas altas, toma los agarres con los brazos paralelos al suelo.
2. Lleva los agarres hacia la cabeza flexionando solo los codos.
3. Aprieta y vuelve despacio.', '["SHOULDER"]'::jsonb, false),
    ('96d9ac98-aed5-5155-8211-70bd3769b9e9', 'Estiramiento de dorsal asistido', 'Overhead Lat', 'FREE_EXERCISE_DB', 'Overhead_Lat', 'Espalda', '["Tríceps"]'::jsonb, 'OTHER', 'ADVANCED', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado, sube un brazo y flexiona el codo detrás de la cabeza; un compañero lo sujeta.
2. Intenta bajar el brazo contra su resistencia 10 a 20 s.
3. Relaja mientras empuja suave el brazo; cambia de lado.', '[]'::jsonb, false),
    ('4078201e-2710-5a40-aae4-5d50d8e3773a', 'Lanzamiento al suelo con balón medicinal', 'Overhead Slam', 'FREE_EXERCISE_DB', 'Overhead_Slam', 'Espalda', '[]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PULL', '1. De pie con el balón en ambas manos.
2. Súbelo sobre la cabeza extendiendo todo el cuerpo.
3. Lánzalo con fuerza al suelo, atrápalo y repite.', '["SHOULDER"]'::jsonb, false),
    ('2df8e7d3-7696-5523-a81f-00585140249f', 'Sentadilla con barra sobre la cabeza', 'Overhead Squat', 'FREE_EXERCISE_DB', 'Overhead_Squat', 'Cuádriceps', '["Abdomen", "Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares", "Hombros", "Tríceps"]'::jsonb, 'BARBELL', 'ADVANCED', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PUSH', '1. Con agarre ancho y la barra bloqueada sobre la cabeza.
2. Baja en sentadilla con la cabeza arriba y los brazos firmes.
3. Sube empujando con los talones.', '["KNEE", "SHOULDER"]'::jsonb, false),
    ('d747dbfc-841f-5133-b133-384e9c2741d6', 'Estiramiento de brazos arriba', 'Overhead Stretch', 'FREE_EXERCISE_DB', 'Overhead_Stretch', 'Abdomen', '["Pecho", "Antebrazos", "Espalda", "Tríceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'COMPOUND', 'STATIC', '1. De pie, entrelaza los dedos con las palmas hacia el techo.
2. Extiende los brazos con los hombros abajo.
3. Estira el torso completo y sostén.', '[]'::jsonb, false),
    ('7c9ecff4-acb0-54e7-a9df-e715c8a53c0b', 'Estiramiento de tríceps asistido', 'Overhead Triceps', 'FREE_EXERCISE_DB', 'Overhead_Triceps', 'Tríceps', '["Espalda"]'::jsonb, 'BODYWEIGHT', 'ADVANCED', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado, sube un brazo y flexiona el codo detrás de la cabeza; un compañero lo sujeta.
2. Intenta extender el brazo contra su resistencia 10 a 20 s.
3. Relaja mientras empuja suave; cambia de lado.', '[]'::jsonb, false),
    ('fa3f0a48-34f2-5a31-b98f-bed17b4fa45d', 'Press Pallof', 'Pallof Press', 'FREE_EXERCISE_DB', 'Pallof_Press', 'Abdomen', '["Pecho", "Hombros", "Tríceps"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De costado a la polea a la altura del pecho, con el agarre en ambas manos.
2. Extiende los brazos al frente sin dejar que el torso gire.
3. Sostén y vuelve al pecho.', '[]'::jsonb, true),
    ('ecbdc78e-d5c0-52ed-9f0c-89ba3aa2ce3a', 'Press Pallof con rotación', 'Pallof Press With Rotation', 'FREE_EXERCISE_DB', 'Pallof_Press_With_Rotation', 'Abdomen', '["Pecho", "Hombros", "Tríceps"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De costado a la polea con el agarre frente al pecho.
2. Extiende los brazos y gira el torso alejándote de la polea.
3. Vuelve al centro con control.', '[]'::jsonb, false),
    ('c0cb0161-ab80-59b6-bb09-16f02bb11465', 'Curl de muñeca en pronación con mancuernas', 'Palms-Down Dumbbell Wrist Curl Over A Bench', 'FREE_EXERCISE_DB', 'Palms-Down_Dumbbell_Wrist_Curl_Over_A_Bench', 'Antebrazos', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Arrodillado, apoya los antebrazos en un banco con las muñecas fuera y palmas abajo.
2. Sube las mancuernas solo con las muñecas.
3. Baja despacio.', '["WRIST"]'::jsonb, false),
    ('f939a08a-2f89-5051-9c62-bb818d87bf2d', 'Curl de muñeca en pronación con barra', 'Palms-Down Wrist Curl Over A Bench', 'FREE_EXERCISE_DB', 'Palms-Down_Wrist_Curl_Over_A_Bench', 'Antebrazos', '[]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Arrodillado, apoya los antebrazos en un banco con las muñecas fuera y palmas abajo.
2. Sube la barra solo con las muñecas.
3. Baja despacio.', '["WRIST"]'::jsonb, true),
    ('58dc8645-76e5-591f-8f55-6dadc2000149', 'Curl de muñeca con barra', 'Palms-Up Barbell Wrist Curl Over A Bench', 'FREE_EXERCISE_DB', 'Palms-Up_Barbell_Wrist_Curl_Over_A_Bench', 'Antebrazos', '[]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Arrodillado, apoya los antebrazos en un banco con las muñecas fuera y palmas arriba.
2. Sube la barra solo con las muñecas.
3. Baja despacio.', '["WRIST"]'::jsonb, true),
    ('99d0d92b-5c55-52a6-8bb8-5fc29b898c34', 'Curl de muñeca con mancuernas', 'Palms-Up Dumbbell Wrist Curl Over A Bench', 'FREE_EXERCISE_DB', 'Palms-Up_Dumbbell_Wrist_Curl_Over_A_Bench', 'Antebrazos', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Arrodillado, apoya los antebrazos en un banco con las muñecas fuera y palmas arriba.
2. Sube las mancuernas solo con las muñecas.
3. Baja despacio.', '["WRIST"]'::jsonb, false),
    ('030377b8-4dd7-5e1a-9c9a-d8e5413a277e', 'Fondos estrictos en paralelas', 'Parallel Bar Dip', 'FREE_EXERCISE_DB', 'Parallel_Bar_Dip', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sube a las paralelas con los brazos bloqueados.
2. Baja flexionando los codos hasta pasar los 90°, sin balancearte.
3. Empuja hasta extender los codos.', '["SHOULDER", "ELBOW"]'::jsonb, false),
    ('acc779f1-7142-5cb0-8aaa-56de84ab5bc5', 'Báscula pélvica a puente', 'Pelvic Tilt Into Bridge', 'FREE_EXERCISE_DB', 'Pelvic_Tilt_Into_Bridge', 'Lumbares', '[]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', 'COMPOUND', 'STATIC', '1. Boca arriba con los pies en el suelo y los talones bajo las rodillas.
2. Sube solo el coxis metiendo el abdomen.
3. Sigue subiendo la columna hasta formar un puente.', '[]'::jsonb, false),
    ('35754223-4b56-54c3-8ace-d8e477432993', 'Liberación de peroneos con rodillo', 'Peroneals-SMR', 'FREE_EXERCISE_DB', 'Peroneals-SMR', 'Pantorrillas', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. De costado, con el rodillo bajo la cara externa de la pantorrilla.
2. Rueda desde debajo de la rodilla hasta el tobillo.
3. Sostén 10 a 30 s en los puntos tensos y cambia de pierna.', '[]'::jsonb, false),
    ('09ab24fa-99e0-55f2-96a8-4415811cc2c1', 'Estiramiento de peroneos', 'Peroneals Stretch', 'FREE_EXERCISE_DB', 'Peroneals_Stretch', 'Pantorrillas', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado, pasa una cinta por un pie con la pierna extendida.
2. Tira llevando la planta hacia adentro.
3. Sostén 10 a 20 s y cambia de lado.', '[]'::jsonb, false),
    ('9c12659d-4228-5c42-8f12-ec278d8d95c6', 'Puente de glúteos en fitball', 'Physioball Hip Bridge', 'FREE_EXERCISE_DB', 'Physioball_Hip_Bridge', 'Glúteos', '["Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la parte alta de la espalda sobre el fitball y los pies en el suelo.
2. Sube la cadera apretando glúteos.
3. Pausa arriba y baja.', '[]'::jsonb, false),
    ('32dc52e7-5dc1-50a2-afc9-74f9df304b13', 'Press desde pines', 'Pin Presses', 'FREE_EXERCISE_DB', 'Pin_Presses', 'Tríceps', '["Pecho", "Antebrazos", "Espalda", "Hombros"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. En el rack con los pines a la altura elegida y la barra apoyada.
2. Empuja la barra desde los pines hasta extender los brazos.
3. Bájala de nuevo a los pines y pausa.', '[]'::jsonb, false),
    ('bdc8ae9e-13f4-526f-b81e-997f04706941', 'Liberación de piriforme con rodillo', 'Piriformis-SMR', 'FREE_EXERCISE_DB', 'Piriformis-SMR', 'Glúteos', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Sentado sobre el rodillo con un tobillo cruzado sobre la rodilla contraria.
2. Inclínate hacia el lado de la pierna cruzada y rueda sobre el glúteo.
3. Sostén 10 a 30 s y cambia de lado.', '[]'::jsonb, false),
    ('774c3326-0d11-598e-ac05-4ee0c20b72d4', 'Pinza con discos', 'Plate Pinch', 'FREE_EXERCISE_DB', 'Plate_Pinch', 'Antebrazos', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'STATIC', '1. Junta dos discos con el lado liso hacia afuera.
2. Sostenlos con los dedos de un lado y el pulgar del otro.
3. Aguanta lo más posible y cambia de mano.', '[]'::jsonb, false),
    ('1feb4d51-3ebd-538f-b05f-ad835381933c', 'Giro ruso con disco', 'Plate Twist', 'FREE_EXERCISE_DB', 'Plate_Twist', 'Abdomen', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Sentado con las piernas cruzadas y elevadas, el disco frente al abdomen.
2. Gira llevando el disco a tocar el suelo de un lado.
3. Pasa al otro lado y alterna.', '[]'::jsonb, false),
    ('95175b28-3cc4-5883-bc4a-3c3c0cf7a1be', 'Deslizamiento de isquiotibiales', 'Platform Hamstring Slides', 'FREE_EXERCISE_DB', 'Platform_Hamstring_Slides', 'Isquiotibiales', '["Glúteos"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca arriba con una toalla bajo el talón en un piso liso.
2. Flexiona la rodilla deslizando el talón hacia ti.
3. Vuelve despacio a extender la pierna.', '[]'::jsonb, false),
    ('3682d3aa-a03b-5c2e-b8fa-01fce12556f6', 'Sentadilla plié con mancuerna', 'Plie Dumbbell Squat', 'FREE_EXERCISE_DB', 'Plie_Dumbbell_Squat', 'Cuádriceps', '["Abdomen", "Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con los pies muy abiertos, puntas afuera, y una mancuerna colgando con ambas manos.
2. Baja hasta que los muslos queden paralelos.
3. Sube empujando con los talones.', '["KNEE"]'::jsonb, false),
    ('4da2150b-67a2-5ec9-87bf-3f689e3945ab', 'Flexión pliométrica con kettlebell', 'Plyo Kettlebell Pushups', 'FREE_EXERCISE_DB', 'Plyo_Kettlebell_Pushups', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'KETTLEBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En posición de flexión con una mano en el suelo y otra sobre la kettlebell.
2. Baja y empuja con fuerza pasando al otro lado de la kettlebell.
3. Cambia de mano y repite de ida y vuelta.', '["WRIST"]'::jsonb, false),
    ('7930cbce-69ba-5411-b6d0-5cc6af6989ce', 'Flexión pliométrica', 'Plyo Push-up', 'FREE_EXERCISE_DB', 'Plyo_Push-up', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. En posición de flexión con las manos al ancho de los hombros.
2. Baja el pecho y empuja lo más rápido posible hasta despegar las manos.
3. Amortigua al caer y repite.', '["WRIST"]'::jsonb, false),
    ('837eba04-28c5-5965-a66d-a5edb80df73d', 'Estiramiento de tibial posterior', 'Posterior Tibialis Stretch', 'FREE_EXERCISE_DB', 'Posterior_Tibialis_Stretch', 'Pantorrillas', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado, pasa una cinta por un pie con la pierna extendida.
2. Tira llevando el borde externo del pie hacia ti.
3. Sostén 10 a 20 s y cambia de lado.', '[]'::jsonb, false),
    ('37c75123-41a7-5b3c-bf05-973386c04090', 'Cargada de potencia', 'Power Clean', 'FREE_EXERCISE_DB', 'Power_Clean', 'Isquiotibiales', '["Pantorrillas", "Antebrazos", "Glúteos", "Lumbares", "Espalda", "Cuádriceps", "Hombros", "Trapecio", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la barra sobre el empeine, agarre algo más ancho que los hombros y espalda plana.
2. Tira extendiendo rodillas y luego cadera con fuerza, encogiendo los hombros.
3. Recibe la barra sobre los hombros en media sentadilla y ponte de pie.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('286c3158-f8ee-5a51-8f95-fad6ceef3e67', 'Cargada de potencia desde bloques', 'Power Clean from Blocks', 'FREE_EXERCISE_DB', 'Power_Clean_from_Blocks', 'Isquiotibiales', '["Cuádriceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con la barra sobre bloques y la espalda recta.
2. Extiende rodillas y cadera con fuerza en un movimiento de salto.
3. Recibe la barra sobre los hombros y ponte de pie.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('bdb13968-982e-539d-9956-24966d25ad49', 'Envión de potencia', 'Power Jerk', 'FREE_EXERCISE_DB', 'Power_Jerk', 'Cuádriceps', '["Abdomen", "Pantorrillas", "Glúteos", "Isquiotibiales", "Hombros", "Tríceps"]'::jsonb, 'BARBELL', 'ADVANCED', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PUSH', '1. Con la barra al frente de los hombros y los pies bajo la cadera.
2. Flexiona un poco las rodillas e impulsa con fuerza.
3. Recibe la barra arriba en media sentadilla y ponte de pie.', '["SHOULDER"]'::jsonb, false),
    ('b447c2da-e60c-5af3-a831-e84b66f7f70f', 'Elevaciones laterales parciales', 'Power Partials', 'FREE_EXERCISE_DB', 'Power_Partials', 'Hombros', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie con una mancuerna en cada mano a los costados.
2. Sube los brazos rectos a los costados hasta la altura de los hombros.
3. Pausa y baja despacio.', '[]'::jsonb, false),
    ('1fbef85b-7ce8-5dd4-8d3a-46183917d80e', 'Arrancada de potencia', 'Power Snatch', 'FREE_EXERCISE_DB', 'Power_Snatch', 'Isquiotibiales', '["Pantorrillas", "Glúteos", "Lumbares", "Cuádriceps", "Hombros", "Trapecio", "Tríceps"]'::jsonb, 'BARBELL', 'ADVANCED', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con la barra en el suelo, agarre ancho y pecho arriba.
2. Tira desde el suelo y extiende cadera, rodillas y tobillos con fuerza.
3. Recibe la barra arriba en media sentadilla y ponte de pie.', '["LOWER_BACK", "SHOULDER"]'::jsonb, false),
    ('023755cd-2c5f-5366-b5d3-32f1357ad4ad', 'Arrancada de potencia desde bloques', 'Power Snatch from Blocks', 'FREE_EXERCISE_DB', 'Power_Snatch_from_Blocks', 'Cuádriceps', '["Pantorrillas", "Antebrazos", "Glúteos", "Isquiotibiales", "Lumbares", "Hombros", "Trapecio", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con la barra sobre bloques y agarre ancho.
2. Extiende cadera, rodillas y tobillos con fuerza.
3. Recibe la barra arriba en media sentadilla y ponte de pie.', '["LOWER_BACK", "SHOULDER"]'::jsonb, false),
    ('04d64cd0-9d8b-583b-957d-c151838867fc', 'Escaleras de potencia', 'Power Stairs', 'FREE_EXERCISE_DB', 'Power_Stairs', 'Isquiotibiales', '["Aductores", "Pantorrillas", "Glúteos", "Lumbares", "Cuádriceps", "Hombros", "Trapecio"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRONGMAN', 'Strongman', 'COMPOUND', 'PULL', '1. Toma el implemento con ambas manos y los pies abiertos.
2. Súbelo extendiendo cadera y rodillas y balancéalo al escalón.
3. Sube tú y repite en el siguiente escalón.', '[]'::jsonb, false),
    ('3bc7cd85-f60a-5234-97c5-115b05cea4cd', 'Curl predicador con barra Z', 'Preacher Curl', 'FREE_EXERCISE_DB', 'Preacher_Curl', 'Bíceps', '[]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Con los brazos sobre el banco predicador, toma la barra Z por el agarre interno.
2. Sube la barra apretando el bíceps.
3. Baja despacio hasta extender los brazos.', '[]'::jsonb, true),
    ('406cde31-15e4-516c-8331-f4afb6477e3c', 'Curl martillo en banco predicador', 'Preacher Hammer Dumbbell Curl', 'FREE_EXERCISE_DB', 'Preacher_Hammer_Dumbbell_Curl', 'Bíceps', '["Antebrazos"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Con los brazos sobre el banco predicador y las mancuernas en agarre neutro.
2. Sube las mancuernas hasta la altura de los hombros.
3. Aprieta un segundo y baja despacio.', '[]'::jsonb, false),
    ('416ba35d-3ea1-5a83-8a9f-5826c3ddfb75', 'Abdominal con press de barra', 'Press Sit-Up', 'FREE_EXERCISE_DB', 'Press_Sit-Up', 'Abdomen', '["Pecho", "Hombros", "Tríceps"]'::jsonb, 'BARBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado en banco de abdominales con los pies sujetos y la barra en el pecho.
2. Sube el torso y empuja la barra sobre la cabeza.
3. Baja despacio.', '[]'::jsonb, false),
    ('f72a7c48-8c5f-527a-8b7b-72cb0885125a', 'Curl femoral con resistencia manual', 'Prone Manual Hamstring', 'FREE_EXERCISE_DB', 'Prone_Manual_Hamstring', 'Isquiotibiales', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'STATIC', '1. Boca abajo con las piernas rectas y un compañero con la mano en tu talón.
2. Flexiona la rodilla contra su resistencia.
3. Vuelve despacio mientras te resiste en sentido contrario.', '[]'::jsonb, false),
    ('4f136235-477a-55ac-8bf3-fa89468f3850', 'Sprint con trineo', 'Prowler Sprint', 'FREE_EXERCISE_DB', 'Prowler_Sprint', 'Isquiotibiales', '["Pantorrillas", "Pecho", "Glúteos", "Cuádriceps", "Hombros"]'::jsonb, 'OTHER', 'BEGINNER', 'CARDIO', 'Cardio', 'COMPOUND', 'PUSH', '1. Con el trineo cargado, toma los agarres con los brazos extendidos.
2. Inclínate y empuja con pasos cortos y rápidos.
3. Recorre una distancia corta lo más rápido posible.', '["ANKLE"]'::jsonb, false),
    ('479f369f-7ce4-5097-a353-732531f8a52b', 'Flexiones abiertas', 'Push-Up Wide', 'FREE_EXERCISE_DB', 'Push-Up_Wide', 'Pecho', '["Abdomen", "Hombros", "Tríceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', NULL, '1. En posición de flexión con las manos más abiertas que los hombros.
2. Baja el pecho al suelo con el cuerpo recto.
3. Empuja hasta extender los codos.', '["WRIST"]'::jsonb, false),
    ('d700b28b-1aeb-57d9-88ef-e80a665599c3', 'Flexiones cerradas para tríceps', 'Push-Ups - Close Triceps Position', 'FREE_EXERCISE_DB', 'Push-Ups_-_Close_Triceps_Position', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En posición de flexión con las manos más juntas que los hombros.
2. Baja hasta casi tocar el suelo con el pecho.
3. Empuja hasta extender los codos.', '["WRIST"]'::jsonb, false),
    ('9bb4f1c8-3e17-51bf-89a6-87124b055168', 'Flexiones con pies elevados', 'Push-Ups With Feet Elevated', 'FREE_EXERCISE_DB', 'Push-Ups_With_Feet_Elevated', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En posición de flexión con los pies sobre un banco.
2. Baja hasta casi tocar el suelo con el pecho.
3. Empuja hasta extender los codos.', '["WRIST"]'::jsonb, false),
    ('28accd69-e38e-5bcf-9807-9cfb4ae5d04e', 'Flexiones con pies en fitball', 'Push-Ups With Feet On An Exercise Ball', 'FREE_EXERCISE_DB', 'Push-Ups_With_Feet_On_An_Exercise_Ball', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En posición de flexión con los pies sobre un fitball.
2. Baja hasta casi tocar el suelo con el pecho.
3. Empuja hasta extender los codos sin perder el equilibrio.', '["WRIST"]'::jsonb, false),
    ('99a31466-657a-57c1-bb57-27877671faa4', 'Push press', 'Push Press', 'FREE_EXERCISE_DB', 'Push_Press', 'Hombros', '["Cuádriceps", "Tríceps"]'::jsonb, 'BARBELL', 'ADVANCED', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PUSH', '1. Con la barra al frente de los hombros y los pies bajo la cadera.
2. Flexiona un poco las rodillas e impulsa con las piernas.
3. Usa el impulso para empujar la barra sobre la cabeza y bájala.', '["SHOULDER"]'::jsonb, false),
    ('ca8e5fcf-d009-5a2e-bc0f-28124efa3161', 'Push press tras nuca', 'Push Press - Behind the Neck', 'FREE_EXERCISE_DB', 'Push_Press_-_Behind_the_Neck', 'Hombros', '["Pantorrillas", "Cuádriceps", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PUSH', '1. Con la barra sobre la espalda alta y los pies bajo la cadera.
2. Flexiona un poco las rodillas e impulsa con fuerza.
3. Termina empujando la barra sobre la cabeza y bájala.', '["SHOULDER", "NECK"]'::jsonb, false),
    ('918e583a-48ca-583b-8289-016f0f573721', 'Flexión con plancha lateral', 'Push Up to Side Plank', 'FREE_EXERCISE_DB', 'Push_Up_to_Side_Plank', 'Pecho', '["Abdomen", "Hombros", "Tríceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En posición de flexión con las manos algo más abiertas que los hombros.
2. Haz una flexión y al subir gira a plancha lateral con el brazo arriba.
3. Vuelve, haz otra flexión y gira al otro lado.', '["WRIST"]'::jsonb, false),
    ('263128f4-4204-5a5a-8f04-8308b9a76026', 'Flexiones con agarre cerrado y abierto', 'Pushups (Close and Wide Hand Positions)', 'FREE_EXERCISE_DB', 'Pushups_Close_and_Wide_Hand_Positions', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En posición de flexión con las manos abiertas o cerradas.
2. Baja hasta casi tocar el suelo con el pecho.
3. Empuja hasta extender los codos y alterna el agarre.', '["WRIST"]'::jsonb, false),
    ('58e9422d-4465-5a3c-8e06-835653140cbd', 'Pirámide en fitball', 'Pyramid', 'FREE_EXERCISE_DB', 'Pyramid', 'Lumbares', '["Hombros"]'::jsonb, 'OTHER', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Rueda hacia adelante sobre el fitball hasta que la cadera quede arriba.
2. Apoya manos y pies en el suelo.
3. Relaja la espalda y sostén.', '[]'::jsonb, false),
    ('ada7f9e0-e3ce-5395-a1f2-7089e509b4ae', 'Estiramiento de cuádriceps con cinta', 'Quad Stretch', 'FREE_EXERCISE_DB', 'Quad_Stretch', 'Cuádriceps', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. De costado con una cinta en el pie de arriba.
2. Lleva el talón hacia el glúteo sujetando la cinta.
3. Tira suave, sostén 10 a 20 s y cambia de lado.', '[]'::jsonb, false),
    ('7fb619c3-1d39-56e4-92ef-907b27d8f60b', 'Liberación de cuádriceps con rodillo', 'Quadriceps-SMR', 'FREE_EXERCISE_DB', 'Quadriceps-SMR', 'Cuádriceps', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Boca abajo apoyado en los antebrazos con el rodillo bajo un muslo.
2. Rueda desde encima de la rodilla hasta la cadera.
3. Sostén 10 a 30 s en los puntos tensos y cambia de pierna.', '[]'::jsonb, false),
    ('d8c41326-d1a1-530d-8e36-023f7e59a47e', 'Salto rápido al cajón', 'Quick Leap', 'FREE_EXERCISE_DB', 'Quick_Leap', 'Cuádriceps', '["Pantorrillas", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie frente a un cajón.
2. Salta al cajón cayendo con ambos pies planos.
3. Sin pausa, salta hacia arriba con los brazos sobre la cabeza y cae amortiguando.', '[]'::jsonb, false),
    ('20e9c884-d3a8-5d16-801e-f1d21c4a99d4', 'Entrega a posición de rack', 'Rack Delivery', 'FREE_EXERCISE_DB', 'Rack_Delivery', 'Hombros', '["Antebrazos", "Trapecio"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con la barra en posición de espantapájaros, brazos paralelos al suelo.
2. Gira los codos alrededor de la barra llevándola a los hombros.
3. Afloja el agarre y deja la barra apoyada en los hombros.', '[]'::jsonb, false),
    ('8754a449-72b0-5177-a0b4-b4f4ef8b298e', 'Peso muerto parcial desde rack con bandas', 'Rack Pull with Bands', 'FREE_EXERCISE_DB', 'Rack_Pull_with_Bands', 'Lumbares', '["Antebrazos", "Glúteos", "Isquiotibiales", "Cuádriceps", "Trapecio"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PULL', '1. Con la barra en los pines del rack y bandas ancladas abajo.
2. Ponte en posición de peso muerto con la espalda arqueada.
3. Extiende cadera y rodillas hasta quedar erguido y baja a los pines.', '["LOWER_BACK"]'::jsonb, false),
    ('c59a6b4b-19d3-536c-95c9-7509f1de056d', 'Peso muerto parcial desde rack', 'Rack Pulls', 'FREE_EXERCISE_DB', 'Rack_Pulls', 'Lumbares', '["Antebrazos", "Glúteos", "Isquiotibiales", "Trapecio"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PULL', '1. Con la barra en los pines a la altura de las rodillas.
2. Ponte en posición de peso muerto con la espalda firme.
3. Extiende cadera y rodillas hasta quedar erguido y baja a los pines.', '["LOWER_BACK"]'::jsonb, true),
    ('42be1240-8e5d-58d2-b095-1c87a997dc1c', 'Elevación de pierna atrás en cuadrupedia', 'Rear Leg Raises', 'FREE_EXERCISE_DB', 'Rear_Leg_Raises', 'Cuádriceps', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'PUSH', '1. En cuatro apoyos con la cabeza al frente.
2. Extiende una pierna hacia atrás y arriba.
3. Repite 5 a 10 veces y cambia de lado.', '[]'::jsonb, false),
    ('6c6db2c7-4437-52af-9142-9b72b15da7ff', 'Bicicleta reclinada', 'Recumbent Bike', 'FREE_EXERCISE_DB', 'Recumbent_Bike', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'MACHINE', 'BEGINNER', 'CARDIO', 'Cardio', NULL, NULL, '1. Siéntate y ajusta el asiento a tu altura.
2. Elige un programa o el modo manual.
3. Pedalea ajustando la resistencia a tu intensidad.', '[]'::jsonb, false),
    ('b44fb2e8-3299-5c65-9145-4a0b13111649', 'Devolución de pase desde posición atlética', 'Return Push from Stance', 'FREE_EXERCISE_DB', 'Return_Push_from_Stance', 'Hombros', '["Pecho", "Tríceps"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. En posición atlética frente a un compañero.
2. A la señal, recibe el balón medicinal con ambas manos.
3. Devuélvelo de inmediato con un empuje.', '[]'::jsonb, false),
    ('1810b165-0d64-50f8-ae9f-5c3ce8bd6a85', 'Press de banca con bandas invertidas', 'Reverse Band Bench Press', 'FREE_EXERCISE_DB', 'Reverse_Band_Bench_Press', 'Tríceps', '["Pecho", "Antebrazos", "Espalda", "Hombros"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. En el rack con bandas ancladas arriba y sujetas a la barra.
2. Baja la barra al pecho con los omóplatos juntos.
3. Empuja hasta extender los brazos.', '["SHOULDER"]'::jsonb, false),
    ('586c965d-7dd9-5883-8061-79a2c3ce3155', 'Sentadilla al cajón con bandas invertidas', 'Reverse Band Box Squat', 'FREE_EXERCISE_DB', 'Reverse_Band_Box_Squat', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Antebrazos", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. En el rack con bandas ancladas arriba y la barra sobre la espalda.
2. Siéntate en el cajón llevando la cadera atrás.
3. Sube empujando con los talones.', '["KNEE"]'::jsonb, false),
    ('5f5964f9-0bd0-5b7e-8452-6721ba887bc6', 'Peso muerto con bandas invertidas', 'Reverse Band Deadlift', 'FREE_EXERCISE_DB', 'Reverse_Band_Deadlift', 'Lumbares', '["Abductores", "Aductores", "Pantorrillas", "Glúteos", "Isquiotibiales", "Cuádriceps"]'::jsonb, 'BARBELL', 'ADVANCED', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PULL', '1. Con bandas ancladas arriba y sujetas a la barra.
2. Toma la barra con la espalda recta y la cadera baja.
3. Extiende cadera y rodillas hasta quedar erguido.', '["LOWER_BACK"]'::jsonb, false),
    ('d25692c3-13b7-543c-b8ae-64a33aae80fb', 'Sentadilla con bandas invertidas', 'Reverse Band Power Squat', 'FREE_EXERCISE_DB', 'Reverse_Band_Power_Squat', 'Cuádriceps', '["Aductores", "Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'BARBELL', 'ADVANCED', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. En el rack con bandas ancladas arriba y la barra sobre la espalda.
2. Baja con la espalda arqueada hasta la paralela.
3. Sube empujando con los talones.', '["KNEE"]'::jsonb, false),
    ('43ef6287-588b-5a47-9936-899630c71dee', 'Peso muerto sumo con bandas invertidas', 'Reverse Band Sumo Deadlift', 'FREE_EXERCISE_DB', 'Reverse_Band_Sumo_Deadlift', 'Isquiotibiales', '["Abductores", "Aductores", "Pantorrillas", "Antebrazos", "Glúteos", "Lumbares", "Cuádriceps", "Trapecio"]'::jsonb, 'BARBELL', 'ADVANCED', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PULL', '1. Con los pies muy abiertos y bandas ancladas arriba.
2. Toma la barra por dentro de las piernas con la espalda recta.
3. Extiende cadera y rodillas hasta quedar erguido.', '["LOWER_BACK", "HIP"]'::jsonb, false),
    ('700f2242-f1dc-570b-9866-1c021773d094', 'Curl invertido con barra', 'Reverse Barbell Curl', 'FREE_EXERCISE_DB', 'Reverse_Barbell_Curl', 'Bíceps', '["Antebrazos"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie con la barra en agarre prono al ancho de los hombros.
2. Sube la barra flexionando solo los codos.
3. Aprieta y baja despacio.', '["ELBOW", "WRIST"]'::jsonb, true),
    ('2a77b281-0a4a-5616-890b-0b2dd68a9fa1', 'Curl predicador invertido con barra Z', 'Reverse Barbell Preacher Curls', 'FREE_EXERCISE_DB', 'Reverse_Barbell_Preacher_Curls', 'Bíceps', '["Antebrazos"]'::jsonb, 'EZ_BAR', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Con los brazos sobre el banco predicador y la barra Z en agarre prono.
2. Sube la barra hasta la altura de los hombros.
3. Aprieta y baja despacio.', '[]'::jsonb, false),
    ('04aa816e-2983-5d50-8046-99c936925295', 'Curl invertido en polea', 'Reverse Cable Curl', 'FREE_EXERCISE_DB', 'Reverse_Cable_Curl', 'Bíceps', '["Antebrazos"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie frente a la polea baja con la barra en agarre prono.
2. Sube la barra flexionando solo los codos.
3. Aprieta y baja despacio.', '[]'::jsonb, false),
    ('a85de94d-56b8-5e31-ba1f-1ca70436d2ae', 'Aperturas invertidas con rotación externa', 'Reverse Flyes With External Rotation', 'FREE_EXERCISE_DB', 'Reverse_Flyes_With_External_Rotation', 'Hombros', '[]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca abajo en banco inclinado a 30° con las mancuernas al frente.
2. Abre los brazos hacia los costados y gira hasta que las palmas miren al frente.
3. Vuelve despacio.', '[]'::jsonb, false),
    ('95564d2d-8585-5b85-a98f-c1944c243592', 'Remo inclinado con agarre supino', 'Reverse Grip Bent-Over Rows', 'FREE_EXERCISE_DB', 'Reverse_Grip_Bent-Over_Rows', 'Espalda', '["Bíceps", "Hombros"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Inclinado casi paralelo al suelo con la barra en agarre supino.
2. Tira la barra hacia el abdomen con los codos pegados.
3. Aprieta la espalda y baja despacio.', '["LOWER_BACK"]'::jsonb, false),
    ('67327938-f3ca-5f0c-9838-46df19585e7d', 'Extensión de tríceps en polea con agarre supino', 'Reverse Grip Triceps Pushdown', 'FREE_EXERCISE_DB', 'Reverse_Grip_Triceps_Pushdown', 'Tríceps', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Frente a la polea alta con la barra en agarre supino y los codos pegados.
2. Extiende los codos hasta llevar la barra a los muslos.
3. Vuelve despacio sin mover los brazos.', '[]'::jsonb, false),
    ('931a42e6-8757-57ee-a343-96230fc8310c', 'Hiperextensión invertida', 'Reverse Hyperextension', 'FREE_EXERCISE_DB', 'Reverse_Hyperextension', 'Isquiotibiales', '["Pantorrillas", "Glúteos"]'::jsonb, 'MACHINE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', NULL, 'PULL', '1. Boca abajo en la máquina con la cadera en el borde y los pies entre los rodillos.
2. Lleva las piernas hacia atrás extendiendo la cadera, sin pasarte.
3. Vuelve con control.', '["LOWER_BACK"]'::jsonb, true),
    ('68e31e40-dc2b-581b-8257-ff6f8e2fa600', 'Curl invertido con disco', 'Reverse Plate Curls', 'FREE_EXERCISE_DB', 'Reverse_Plate_Curls', 'Bíceps', '["Antebrazos"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie con un disco en ambas manos y agarre prono.
2. Súbelo flexionando los codos sin moverlos.
3. Aprieta y baja despacio.', '[]'::jsonb, false),
    ('715aacad-2f05-58a3-a6e8-ba25ffde3b76', 'Press de banca invertido para tríceps', 'Reverse Triceps Bench Press', 'FREE_EXERCISE_DB', 'Reverse_Triceps_Bench_Press', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado en banco plano con agarre supino cerrado.
2. Baja la barra al pecho medio con los codos pegados.
3. Empuja hasta extender los codos.', '["SHOULDER"]'::jsonb, false),
    ('5d932426-8268-57f8-98ac-d788bc960e28', 'Liberación de romboides con rodillo', 'Rhomboids-SMR', 'FREE_EXERCISE_DB', 'Rhomboids-SMR', 'Espalda', '["Trapecio"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Boca arriba con el rodillo bajo la espalda alta y los brazos cruzados.
2. Sube la cadera y apoya el peso en el rodillo.
3. Rueda por la espalda media y alta, pausando 10 a 30 s en los puntos tensos.', '[]'::jsonb, false),
    ('76cf9f62-c3c8-5fb6-8a97-577c5cbcc2df', 'Caminata con rickshaw', 'Rickshaw Carry', 'FREE_EXERCISE_DB', 'Rickshaw_Carry', 'Antebrazos', '["Abdomen", "Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares", "Cuádriceps", "Trapecio"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRONGMAN', 'Strongman', 'COMPOUND', NULL, '1. De pie dentro del marco, toma los agarres.
2. Súbelo con los talones, pecho arriba y espalda recta.
3. Camina rápido con pasos cortos hasta el final y bájalo.', '[]'::jsonb, false),
    ('6e2ac83e-6611-5603-80f0-1f1a82a08fd1', 'Peso muerto con rickshaw', 'Rickshaw Deadlift', 'FREE_EXERCISE_DB', 'Rickshaw_Deadlift', 'Cuádriceps', '["Antebrazos", "Glúteos", "Isquiotibiales", "Lumbares", "Trapecio"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRONGMAN', 'Strongman', 'COMPOUND', 'PULL', '1. Centrado entre los agarres con los pies al ancho de la cadera.
2. Tómalos con la cadera baja, pecho arriba y espalda arqueada.
3. Empuja con los talones hasta quedar erguido.', '["LOWER_BACK"]'::jsonb, false),
    ('bcbb5e52-18a8-52b5-9ee2-e405bcf9ffee', 'Fondos en anillas', 'Ring Dips', 'FREE_EXERCISE_DB', 'Ring_Dips', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sube a las anillas con los brazos bloqueados.
2. Baja flexionando los codos hasta pasar los 90°, sin balancearte.
3. Empuja hasta extender los codos.', '["SHOULDER", "ELBOW"]'::jsonb, false),
    ('09321cd8-0305-5ba7-994c-2f63b5be3f0e', 'Salto cohete', 'Rocket Jump', 'FREE_EXERCISE_DB', 'Rocket_Jump', 'Cuádriceps', '["Pantorrillas", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie con los pies al ancho de los hombros.
2. Baja a media sentadilla y salta lo más alto posible estirando los brazos arriba.
3. Cae amortiguando con las piernas.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('026ec3a4-e287-5f16-972c-bf712562e132', 'Elevación de talones con balanceo con barra', 'Rocking Standing Calf Raise', 'FREE_EXERCISE_DB', 'Rocking_Standing_Calf_Raise', 'Pantorrillas', '[]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie con la barra sobre la espalda.
2. Sube a puntas de pie y luego bascula sobre los talones subiendo las puntas.
3. Alterna con control.', '["ANKLE"]'::jsonb, false),
    ('41d60c1a-3448-540f-a6f0-be53063e1e09', 'Dominadas y jalones Rocky', 'Rocky Pull-Ups/Pulldowns', 'FREE_EXERCISE_DB', 'Rocky_Pull-Ups_Pulldowns', 'Espalda', '["Bíceps", "Hombros"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Cuelga con agarre ancho, pecho afuera y espalda arqueada.
2. Sube hasta llevar el pecho a la barra.
3. Baja despacio y alterna con jalones tras nuca.', '[]'::jsonb, false),
    ('835a7126-877d-53ff-b201-9dc6aa25cf40', 'Peso muerto rumano con déficit', 'Romanian Deadlift from Deficit', 'FREE_EXERCISE_DB', 'Romanian_Deadlift_from_Deficit', 'Isquiotibiales', '["Antebrazos", "Glúteos", "Lumbares", "Trapecio"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. De pie sobre una plataforma con la barra frente a los muslos.
2. Lleva la cadera atrás con la espalda firme, la barra pegada a las piernas.
3. Baja hasta donde te permita la flexibilidad y vuelve erguido.', '["LOWER_BACK"]'::jsonb, false),
    ('d3119c92-533a-5b40-b424-d1baa81b7ed5', 'Trepa de cuerda', 'Rope Climb', 'FREE_EXERCISE_DB', 'Rope_Climb', 'Espalda', '["Bíceps", "Antebrazos", "Hombros"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Toma la cuerda arriba y envuélvela en una pierna, pinzándola con los pies.
2. Tira con los brazos y sube las rodillas.
3. Fija los pies, ponte de pie y repite; baja con control.', '[]'::jsonb, false),
    ('4283db29-dd9b-51ed-a474-23bb95d5767a', 'Crunch con cuerda en polea', 'Rope Crunch', 'FREE_EXERCISE_DB', 'Rope_Crunch', 'Abdomen', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Arrodillado frente a la polea alta con la cuerda sobre la cabeza.
2. Flexiona la columna llevando las costillas hacia las piernas.
3. Pausa abajo y vuelve despacio.', '[]'::jsonb, true),
    ('f99e7b99-ac3f-5ccc-901a-a99bba55508c', 'Saltar la cuerda', 'Rope Jumping', 'FREE_EXERCISE_DB', 'Rope_Jumping', 'Cuádriceps', '["Pantorrillas", "Isquiotibiales"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'CARDIO', 'Cardio', NULL, NULL, '1. Con un extremo de la cuerda en cada mano y la cuerda detrás.
2. Gírala sobre la cabeza y salta cuando llegue al suelo.
3. Mantén un ritmo constante.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('3ac176be-5be8-5669-9c81-05b1d31087a2', 'Pullover en polea con cuerda', 'Rope Straight-Arm Pulldown', 'FREE_EXERCISE_DB', 'Rope_Straight-Arm_Pulldown', 'Espalda', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Frente a la polea alta, inclinado desde la cadera con la cuerda y brazos extendidos.
2. Baja la cuerda a los muslos sin flexionar los codos.
3. Aprieta los dorsales y vuelve despacio.', '[]'::jsonb, false),
    ('a2968421-99c8-5df8-b524-0380bfa5cd1b', 'Estiramiento de hombros con palo', 'Round The World Shoulder Stretch', 'FREE_EXERCISE_DB', 'Round_The_World_Shoulder_Stretch', 'Hombros', '["Bíceps", "Pecho"]'::jsonb, 'OTHER', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. De pie con un palo detrás de la cadera, agarre ancho y palmas abajo.
2. Sube los brazos por detrás de la cabeza.
3. No fuerces si cuesta seguir.', '[]'::jsonb, false),
    ('c877e763-b730-5bbe-8fde-93b716ac9505', 'Remo en máquina', 'Rowing, Stationary', 'FREE_EXERCISE_DB', 'Rowing_Stationary', 'Cuádriceps', '["Bíceps", "Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares", "Espalda"]'::jsonb, 'MACHINE', 'INTERMEDIATE', 'CARDIO', 'Cardio', NULL, NULL, '1. Siéntate con los pies sujetos y la espalda recta.
2. Empuja con las piernas, inclina el torso un poco atrás y tira del mango al abdomen.
3. Vuelve en orden inverso: brazos, torso y piernas.', '[]'::jsonb, false),
    ('5e395d7a-86bb-5cdd-aa31-cbd423c198f2', 'Estiramiento del corredor', 'Runner''s Stretch', 'FREE_EXERCISE_DB', 'Runners_Stretch', 'Isquiotibiales', '["Pantorrillas"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. En zancada baja con las manos a los lados del pie delantero.
2. Mantén el talón delantero en el suelo.
3. Sube y baja la cadera suave para profundizar.', '[]'::jsonb, false),
    ('45f28bef-6992-5986-8ba3-10e751f4d384', 'Correr en cinta', 'Running, Treadmill', 'FREE_EXERCISE_DB', 'Running_Treadmill', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'MACHINE', 'BEGINNER', 'CARDIO', 'Cardio', NULL, NULL, '1. Sube a la cinta y elige un programa o el modo manual.
2. Corre a un ritmo sostenido.
3. Ajusta velocidad o inclinación para cambiar la intensidad.', '[]'::jsonb, false),
    ('bc5f1122-043a-5b35-801e-e6122f1165b2', 'Giro ruso', 'Russian Twist', 'FREE_EXERCISE_DB', 'Russian_Twist', 'Abdomen', '["Lumbares"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Sentado con las rodillas flexionadas y el torso inclinado formando una V.
2. Con los brazos al frente, gira el torso hacia un lado.
3. Pausa y gira al otro lado.', '[]'::jsonb, true),
    ('27a7a6bc-a0ef-5b21-996f-b99a2dee9d81', 'Carga de saco de arena', 'Sandbag Load', 'FREE_EXERCISE_DB', 'Sandbag_Load', 'Cuádriceps', '["Abdomen", "Bíceps", "Pantorrillas", "Antebrazos", "Glúteos", "Isquiotibiales", "Lumbares", "Espalda", "Hombros", "Trapecio"]'::jsonb, 'OTHER', 'BEGINNER', 'STRONGMAN', 'Strongman', 'COMPOUND', 'PULL', '1. Abraza el saco y súbelo extendiendo cadera y rodillas.
2. Camina rápido hasta la plataforma.
3. Súbelo extendiendo cadera, rodillas y tobillos.', '["LOWER_BACK"]'::jsonb, false),
    ('e62725d5-938e-52aa-acad-5265fda7fcfe', 'Dominada escapular', 'Scapular Pull-Up', 'FREE_EXERCISE_DB', 'Scapular_Pull-Up', 'Trapecio', '["Espalda"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Cuelga de la barra con agarre prono.
2. Sube unos centímetros bajando los hombros, sin flexionar los codos.
3. Pausa y baja despacio.', '[]'::jsonb, false),
    ('a47ee66e-ba82-5aec-a134-cd4bc4e58d91', 'Tijeras', 'Scissor Kick', 'FREE_EXERCISE_DB', 'Scissor_Kick', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'PULL', '1. Boca arriba con los brazos a los costados y los talones a unos 15 cm del suelo.
2. Sube una pierna a 45° mientras bajas la otra.
3. Alterna sin apoyar los pies.', '[]'::jsonb, false),
    ('7a8068fb-54c8-58cb-a0b2-2dcafb4a0af2', 'Salto en tijera', 'Scissors Jump', 'FREE_EXERCISE_DB', 'Scissors_Jump', 'Cuádriceps', '["Glúteos", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. En zancada con la rodilla de atrás cerca del suelo.
2. Salta lo más alto posible y cambia las piernas en el aire.
3. Cae en zancada amortiguando y repite.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('28840335-83d1-5788-a995-291cc534ec24', 'Curl femoral sentado con banda', 'Seated Band Hamstring Curl', 'FREE_EXERCISE_DB', 'Seated_Band_Hamstring_Curl', 'Isquiotibiales', '[]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado en un banco con una banda anclada abajo detrás de los tobillos.
2. Flexiona las rodillas llevando los pies hacia el banco.
3. Pausa y vuelve despacio.', '[]'::jsonb, false),
    ('b1e57c0d-dcd3-5637-ac15-21280225dd8a', 'Press militar sentado con barra', 'Seated Barbell Military Press', 'FREE_EXERCISE_DB', 'Seated_Barbell_Military_Press', 'Hombros', '["Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sentado con respaldo, la barra a la altura de los hombros y agarre algo ancho.
2. Empuja hasta extender los brazos.
3. Baja despacio a la altura de los hombros.', '["SHOULDER"]'::jsonb, false),
    ('f858dc65-5c89-54fc-87da-93d2f6f13882', 'Giro sentado con barra', 'Seated Barbell Twist', 'FREE_EXERCISE_DB', 'Seated_Barbell_Twist', 'Abdomen', '[]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado en el extremo de un banco con la barra sobre los hombros.
2. Gira el torso hacia un lado sin mover pies ni cabeza.
3. Vuelve y gira al otro lado.', '[]'::jsonb, false),
    ('c36d6390-dccb-5b80-902b-68e2f55d69da', 'Patada de tríceps sentado a una mano', 'Seated Bent-Over One-Arm Dumbbell Triceps Extension', 'FREE_EXERCISE_DB', 'Seated_Bent-Over_One-Arm_Dumbbell_Triceps_Extension', 'Tríceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Sentado e inclinado al frente con el brazo pegado al torso y el codo a 90°.
2. Extiende el codo hacia atrás sin mover el brazo.
3. Vuelve despacio y cambia de brazo.', '["ELBOW"]'::jsonb, false),
    ('2efbd857-a3cd-556f-8ee8-25aad9b610b2', 'Elevación de deltoide posterior sentado', 'Seated Bent-Over Rear Delt Raise', 'FREE_EXERCISE_DB', 'Seated_Bent-Over_Rear_Delt_Raise', 'Hombros', '[]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado en el extremo de un banco, inclinado con las mancuernas detrás de las pantorrillas.
2. Sube los brazos a los costados hasta que queden paralelos al suelo.
3. Baja despacio.', '[]'::jsonb, false),
    ('476aa456-5c3a-5074-8315-fba9ce051c4f', 'Patada de tríceps sentado con dos mancuernas', 'Seated Bent-Over Two-Arm Dumbbell Triceps Extension', 'FREE_EXERCISE_DB', 'Seated_Bent-Over_Two-Arm_Dumbbell_Triceps_Extension', 'Tríceps', '[]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Sentado e inclinado al frente con los brazos pegados al torso y los codos a 90°.
2. Extiende los codos hacia atrás sin mover los brazos.
3. Vuelve despacio.', '["ELBOW"]'::jsonb, false),
    ('811145ef-c3b8-5d76-95e5-25ca6c899869', 'Estiramiento de bíceps asistido', 'Seated Biceps', 'FREE_EXERCISE_DB', 'Seated_Biceps', 'Bíceps', '["Pecho", "Hombros"]'::jsonb, 'BODYWEIGHT', 'ADVANCED', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Sentado con los brazos extendidos atrás; un compañero sujeta tus muñecas.
2. Intenta flexionar los codos contra su resistencia 10 a 20 s.
3. Relaja mientras sube suave tus muñecas.', '[]'::jsonb, false),
    ('5cbd5576-9acf-52a7-8b10-93dfb2c6f2d0', 'Press de hombros sentado en polea', 'Seated Cable Shoulder Press', 'FREE_EXERCISE_DB', 'Seated_Cable_Shoulder_Press', 'Hombros', '["Tríceps"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sentado con los agarres a la altura de los hombros y los codos a 90°.
2. Empuja hacia arriba juntando los agarres sobre la cabeza.
3. Vuelve despacio manteniendo la tensión.', '["SHOULDER"]'::jsonb, false),
    ('0e08da79-b39f-58b6-8c3b-9bd81b0819dd', 'Estiramiento de pantorrilla sentado', 'Seated Calf Stretch', 'FREE_EXERCISE_DB', 'Seated_Calf_Stretch', 'Pantorrillas', '["Isquiotibiales", "Lumbares"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado con una pierna flexionada y la otra extendida.
2. Con una banda, toalla o la mano, tira de la punta del pie hacia ti.
3. Sostén 10 a 20 s y cambia de lado.', '[]'::jsonb, false),
    ('4fd6891d-2de8-592f-b56a-b6d2f541c6d1', 'Curl concentrado sentado con barra', 'Seated Close-Grip Concentration Barbell Curl', 'FREE_EXERCISE_DB', 'Seated_Close-Grip_Concentration_Barbell_Curl', 'Bíceps', '[]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado con la parte de atrás de los brazos apoyada en el interior de los muslos y agarre supino cerrado.
2. Sube la barra flexionando los codos.
3. Aprieta y baja despacio.', '["ELBOW", "WRIST"]'::jsonb, false),
    ('8b76919b-50e0-55b9-afc9-d5c09706b3de', 'Curl sentado con mancuernas', 'Seated Dumbbell Curl', 'FREE_EXERCISE_DB', 'Seated_Dumbbell_Curl', 'Bíceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado con las mancuernas a los costados, palmas hacia el cuerpo.
2. Sube girando las palmas hacia arriba al pasar los muslos.
3. Aprieta y baja despacio.', '[]'::jsonb, false),
    ('d7e447ee-f4d0-5e32-b809-41a5ccfbd893', 'Curl abierto sentado con mancuernas', 'Seated Dumbbell Inner Biceps Curl', 'FREE_EXERCISE_DB', 'Seated_Dumbbell_Inner_Biceps_Curl', 'Bíceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado con las mancuernas a los costados en agarre neutro.
2. Súbelas hacia afuera girando las palmas hacia afuera.
3. Aprieta y baja despacio.', '[]'::jsonb, false),
    ('aec23388-52c9-5f72-8e9e-143704ba3056', 'Curl de muñeca sentado en pronación con mancuernas', 'Seated Dumbbell Palms-Down Wrist Curl', 'FREE_EXERCISE_DB', 'Seated_Dumbbell_Palms-Down_Wrist_Curl', 'Antebrazos', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado con los antebrazos sobre los muslos, palmas abajo y las muñecas fuera.
2. Sube las mancuernas solo con las muñecas.
3. Baja despacio.', '["WRIST"]'::jsonb, false),
    ('9040bece-263d-52e3-adfe-d7b6a4e453ab', 'Curl de muñeca sentado con mancuernas', 'Seated Dumbbell Palms-Up Wrist Curl', 'FREE_EXERCISE_DB', 'Seated_Dumbbell_Palms-Up_Wrist_Curl', 'Antebrazos', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado con los antebrazos sobre los muslos, palmas arriba y las muñecas fuera.
2. Sube las mancuernas solo con las muñecas.
3. Baja despacio.', '["WRIST"]'::jsonb, true),
    ('d7c4f828-0dc3-5225-8788-682c8f0b3303', 'Encogimiento de piernas sentado en banco', 'Seated Flat Bench Leg Pull-In', 'FREE_EXERCISE_DB', 'Seated_Flat_Bench_Leg_Pull-In', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Sentado en el banco, sujeto a los lados, torso inclinado atrás y piernas extendidas.
2. Lleva las rodillas hacia el pecho acercando el torso.
3. Pausa y vuelve a extender.', '[]'::jsonb, false),
    ('678621f7-b81e-5ec1-ac33-c0cb4b3e4f09', 'Estiramiento de isquiotibiales sentado', 'Seated Floor Hamstring Stretch', 'FREE_EXERCISE_DB', 'Seated_Floor_Hamstring_Stretch', 'Isquiotibiales', '["Pantorrillas"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado con una pierna extendida y la planta de la otra contra el muslo.
2. Inclínate desde la cadera hacia el tobillo.
3. Sostén 15 s y cambia de lado.', '[]'::jsonb, false),
    ('eee0d76f-8dc0-572b-9cee-6b5e2bfaa013', 'Estiramiento de deltoide frontal asistido', 'Seated Front Deltoid', 'FREE_EXERCISE_DB', 'Seated_Front_Deltoid', 'Hombros', '["Pecho"]'::jsonb, 'BODYWEIGHT', 'ADVANCED', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado con los brazos abiertos atrás; un compañero sujeta tus muñecas.
2. Intenta llevar los brazos al frente contra su resistencia 10 a 20 s.
3. Relaja mientras los lleva suave hacia atrás.', '[]'::jsonb, false),
    ('dc1538d9-772f-5e82-893e-40bf8f1344ba', 'Estiramiento de glúteo sentado asistido', 'Seated Glute', 'FREE_EXERCISE_DB', 'Seated_Glute', 'Glúteos', '["Aductores"]'::jsonb, 'BODYWEIGHT', 'ADVANCED', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado con un tobillo sobre la rodilla contraria, inclinado al frente; un compañero sostiene tus hombros.
2. Empuja el torso atrás contra su resistencia 10 a 20 s.
3. Relaja mientras te empuja suave al frente.', '[]'::jsonb, false),
    ('a1dc1b3c-4ecf-58b2-93db-08c9da28743a', 'Buenos días sentado', 'Seated Good Mornings', 'FREE_EXERCISE_DB', 'Seated_Good_Mornings', 'Lumbares', '["Glúteos"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PULL', '1. Sentado en un cajón dentro del rack con la barra sobre la espalda.
2. Inclínate al frente con la espalda arqueada y firme.
3. Vuelve erguido.', '["LOWER_BACK"]'::jsonb, false),
    ('5c88633f-cca1-50ee-b6eb-8e65df25f79a', 'Estiramiento de isquiotibiales sentado asistido', 'Seated Hamstring', 'FREE_EXERCISE_DB', 'Seated_Hamstring', 'Isquiotibiales', '["Pantorrillas"]'::jsonb, 'BODYWEIGHT', 'ADVANCED', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado con las piernas extendidas; un compañero sostiene tus hombros.
2. Empuja el torso atrás contra su resistencia 10 a 20 s.
3. Relaja mientras te empuja suave al frente.', '[]'::jsonb, false),
    ('1d9e671c-882f-5993-bedd-24eb9f878f2c', 'Estiramiento de isquiotibiales y pantorrilla con cinta', 'Seated Hamstring and Calf Stretch', 'FREE_EXERCISE_DB', 'Seated_Hamstring_and_Calf_Stretch', 'Isquiotibiales', '["Pantorrillas"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado con las piernas extendidas y una cinta en un pie.
2. Inclínate un poco y tira de la punta del pie hacia ti.
3. Sostén 10 a 20 s y cambia de pierna.', '[]'::jsonb, false),
    ('0ac2aecc-36d8-5c94-b80c-baf9513bac8e', 'Extensión de cuello con arnés', 'Seated Head Harness Neck Resistance', 'FREE_EXERCISE_DB', 'Seated_Head_Harness_Neck_Resistance', 'Cuello', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado e inclinado al frente con el arnés en la cabeza y el peso colgando.
2. Sube la cabeza hasta mirar al frente.
3. Baja despacio.', '["NECK"]'::jsonb, false),
    ('869d4725-c5de-5dc9-b6c9-f3d5b6bd95e6', 'Encogimientos de piernas sentado', 'Seated Leg Tucks', 'FREE_EXERCISE_DB', 'Seated_Leg_Tucks', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado en el borde del banco, torso inclinado atrás y piernas extendidas.
2. Lleva las rodillas al pecho acercando el torso.
3. Pausa y vuelve a extender.', '[]'::jsonb, false),
    ('cd7cd2b4-3a9c-5098-b4a9-f0078487e472', 'Curl de muñeca en pronación a una mano', 'Seated One-Arm Dumbbell Palms-Down Wrist Curl', 'FREE_EXERCISE_DB', 'Seated_One-Arm_Dumbbell_Palms-Down_Wrist_Curl', 'Antebrazos', '[]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado con el antebrazo sobre el muslo, palma abajo y la muñeca fuera de la rodilla.
2. Sube la mancuerna solo con la muñeca.
3. Baja despacio y cambia de mano.', '["WRIST"]'::jsonb, false),
    ('0d0ae68e-e1d6-5261-8ae3-0c8e07617e81', 'Curl de muñeca a una mano', 'Seated One-Arm Dumbbell Palms-Up Wrist Curl', 'FREE_EXERCISE_DB', 'Seated_One-Arm_Dumbbell_Palms-Up_Wrist_Curl', 'Antebrazos', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado con el antebrazo sobre el muslo, palma arriba y la muñeca fuera de la rodilla.
2. Sube la mancuerna solo con la muñeca.
3. Baja despacio y cambia de mano.', '["WRIST"]'::jsonb, false),
    ('62c03014-d769-5a8d-af4e-1b279b1dede3', 'Remo sentado a una mano en polea', 'Seated One-arm Cable Pulley Rows', 'FREE_EXERCISE_DB', 'Seated_One-arm_Cable_Pulley_Rows', 'Espalda', '["Bíceps", "Trapecio"]'::jsonb, 'CABLE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Sentado frente a la polea con un agarre en una mano y el torso erguido.
2. Tira hacia el abdomen juntando el omóplato.
3. Vuelve despacio y cambia de mano.', '[]'::jsonb, false),
    ('4d4378e2-6a8c-5772-983b-5510a7c656a1', 'Estiramiento lateral sentado', 'Seated Overhead Stretch', 'FREE_EXERCISE_DB', 'Seated_Overhead_Stretch', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Sentado con las plantas juntas, una mano en el suelo y la otra detrás de la cabeza.
2. Sube el codo e inclina el torso al lado contrario.
3. Sostén 10 a 20 s y cambia de lado.', '[]'::jsonb, false),
    ('5236dfd9-4025-55b6-9b47-219aab1432ac', 'Curl de muñeca sentado con barra', 'Seated Palm-Up Barbell Wrist Curl', 'FREE_EXERCISE_DB', 'Seated_Palm-Up_Barbell_Wrist_Curl', 'Antebrazos', '[]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado con los antebrazos sobre los muslos, palmas arriba y la barra fuera de las rodillas.
2. Sube la barra solo con las muñecas.
3. Baja despacio.', '["WRIST"]'::jsonb, false),
    ('36be4a93-92fc-5291-9517-7da22922313a', 'Curl de muñeca sentado en pronación con barra', 'Seated Palms-Down Barbell Wrist Curl', 'FREE_EXERCISE_DB', 'Seated_Palms-Down_Barbell_Wrist_Curl', 'Antebrazos', '[]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado con los antebrazos sobre los muslos, palmas abajo y la barra fuera de las rodillas.
2. Sube la barra solo con las muñecas.
3. Baja despacio.', '["WRIST"]'::jsonb, false),
    ('22d49d16-c5cc-53a3-854a-afb702972340', 'Elevaciones laterales sentado', 'Seated Side Lateral Raise', 'FREE_EXERCISE_DB', 'Seated_Side_Lateral_Raise', 'Hombros', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Sentado en el borde de un banco con las mancuernas a los costados.
2. Súbelas a los costados hasta que los brazos queden paralelos al suelo.
3. Baja despacio.', '[]'::jsonb, false),
    ('ff31e60b-6fc5-5b82-b49a-fe306d5a4aa8', 'Extensión de tríceps sentado con mancuerna', 'Seated Triceps Press', 'FREE_EXERCISE_DB', 'Seated_Triceps_Press', 'Tríceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Sentado con respaldo, sostén una mancuerna con ambas manos sobre la cabeza.
2. Bájala detrás de la cabeza con los codos cerca.
3. Extiende los codos para volver.', '[]'::jsonb, false),
    ('511d54ed-f9f0-518c-92e0-944fe0a787e3', 'Curl de muñeca sentado en polea baja', 'Seated Two-Arm Palms-Up Low-Pulley Wrist Curl', 'FREE_EXERCISE_DB', 'Seated_Two-Arm_Palms-Up_Low-Pulley_Wrist_Curl', 'Antebrazos', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado frente a la polea baja, antebrazos sobre los muslos, palmas arriba.
2. Sube la barra solo con las muñecas.
3. Baja despacio.', '["WRIST"]'::jsonb, false),
    ('2c278d80-98ae-5a96-8797-ff2f6f364ba6', 'Press alterno con inclinación lateral', 'See-Saw Press (Alternating Side Press)', 'FREE_EXERCISE_DB', 'See-Saw_Press_Alternating_Side_Press', 'Hombros', '["Abdomen", "Tríceps"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie con las mancuernas en los hombros, palmas hacia ti.
2. Empuja una mancuerna arriba inclinando el torso al lado contrario.
3. Bájala mientras subes la otra.', '[]'::jsonb, false),
    ('c0e04142-bc43-559c-9172-c7ce31814ee7', 'Remo shotgun', 'Shotgun Row', 'FREE_EXERCISE_DB', 'Shotgun_Row', 'Espalda', '["Bíceps"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Frente a la polea baja con un agarre en una mano y los pies en tijera.
2. Tira llevando el codo atrás y girando la palma hacia arriba.
3. Pausa y vuelve despacio.', '[]'::jsonb, false),
    ('b84a5a34-9e2f-5026-a1a1-e314a6b5df84', 'Círculos de hombros', 'Shoulder Circles', 'FREE_EXERCISE_DB', 'Shoulder_Circles', 'Hombros', '["Trapecio"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'PULL', '1. Con los brazos relajados a los costados.
2. Rota los hombros adelante, arriba, atrás y abajo.
3. Cambia de dirección.', '[]'::jsonb, false),
    ('22f95201-e3de-56ba-b237-6c761f03cbf0', 'Press de hombros con banda', 'Shoulder Press - With Bands', 'FREE_EXERCISE_DB', 'Shoulder_Press_-_With_Bands', 'Hombros', '["Tríceps"]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie sobre la banda con los agarres a la altura de los hombros.
2. Empuja hasta extender los brazos sobre la cabeza.
3. Baja despacio.', '["SHOULDER"]'::jsonb, true),
    ('cb1bf86c-6aae-5e31-a42e-336433d0971a', 'Elevación de hombros', 'Shoulder Raise', 'FREE_EXERCISE_DB', 'Shoulder_Raise', 'Hombros', '["Espalda"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'PULL', '1. Con los brazos relajados a los costados.
2. Sube los hombros hacia las orejas.
3. Bájalos despacio.', '[]'::jsonb, false),
    ('361a5c7a-738b-5a5c-b6db-21d51f6dc86c', 'Estiramiento de hombro cruzado', 'Shoulder Stretch', 'FREE_EXERCISE_DB', 'Shoulder_Stretch', 'Hombros', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'PULL', '1. Cruza un brazo recto frente al cuerpo.
2. Sujétalo con la otra mano.
3. Sostén y cambia de lado.', '[]'::jsonb, false),
    ('24ac2a94-e0fd-5f2a-934d-882ce6d74477', 'Estiramiento lateral acostado', 'Side-Lying Floor Stretch', 'FREE_EXERCISE_DB', 'Side-Lying_Floor_Stretch', 'Espalda', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. De costado con la rodilla de abajo flexionada al frente.
2. Estira el brazo de arriba sobre la cabeza y tira suave de la muñeca.
3. Sostén y cambia de lado.', '[]'::jsonb, false),
    ('52d49731-4773-5ad2-b3ed-e92b94fd1240', 'Plancha lateral', 'Side Bridge', 'FREE_EXERCISE_DB', 'Side_Bridge', 'Abdomen', '["Hombros"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', NULL, 'STATIC', '1. De costado apoyado en el antebrazo, codo bajo el hombro.
2. Sube la cadera hasta alinear el cuerpo.
3. Sostén y cambia de lado.', '[]'::jsonb, true),
    ('a157f38a-4047-5be0-b925-f1719646c684', 'Saltos laterales y sprint', 'Side Hop-Sprint', 'FREE_EXERCISE_DB', 'Side_Hop-Sprint', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De costado a un cono.
2. Salta de lado sobre el cono y vuelve, lo más rápido posible.
3. Tras el último salto, sal en sprint corto.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('1c4f5616-eda0-5d04-8196-03a133ebc52f', 'Navaja lateral', 'Side Jackknife', 'FREE_EXERCISE_DB', 'Side_Jackknife', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Acostado de costado con una mano detrás de la cabeza.
2. Sube el torso y las piernas a la vez contrayendo el oblicuo.
3. Baja despacio y cambia de lado.', '[]'::jsonb, false),
    ('16a5cefc-6439-57b1-b573-20c128715d1d', 'Elevación frontal a lateral', 'Side Laterals to Front Raise', 'FREE_EXERCISE_DB', 'Side_Laterals_to_Front_Raise', 'Hombros', '["Trapecio"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie con las mancuernas a los costados.
2. Súbelas al frente y ábrelas a los costados a la altura de los hombros.
3. Baja; en la siguiente rep invierte el orden.', '["SHOULDER"]'::jsonb, false),
    ('6ea70278-f84a-5e08-b9de-d79e79634583', 'Balanceo lateral de pierna', 'Side Leg Raises', 'FREE_EXERCISE_DB', 'Side_Leg_Raises', 'Aductores', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'PULL', '1. De pie sobre una pierna, sujeto a una silla.
2. Balancea la otra pierna recta hacia el costado y crúzala por delante.
3. Repite 5 a 10 veces y cambia de pierna.', '[]'::jsonb, false),
    ('873303ff-bf9d-5d07-a08b-5a8a7cb88c6d', 'Estiramiento de ingle de costado', 'Side Lying Groin Stretch', 'FREE_EXERCISE_DB', 'Side_Lying_Groin_Stretch', 'Aductores', '["Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. De costado con la rodilla de abajo flexionada.
2. Sube la pierna de arriba y sujétala por detrás de la rodilla.
3. Llévala hacia el hombro, sostén y cambia de lado.', '[]'::jsonb, false),
    ('de19be6c-ccea-56f8-a49a-b6833b096a56', 'Estiramiento lateral de cuello', 'Side Neck Stretch', 'FREE_EXERCISE_DB', 'Side_Neck_Stretch', 'Cuello', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Con los hombros relajados.
2. Inclina la cabeza hacia un hombro y ayúdate con la mano.
3. Sostén y cambia de lado.', '[]'::jsonb, false),
    ('af838515-54a5-53e7-97a9-071e94ac3e2a', 'Salto largo lateral', 'Side Standing Long Jump', 'FREE_EXERCISE_DB', 'Side_Standing_Long_Jump', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie en posición atlética.
2. Salta lo más lejos posible hacia el costado impulsando con los brazos.
3. Cae amortiguando y repite al otro lado.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('e2167317-900f-52d2-80d3-4174c95f094e', 'Dominadas de lado a lado', 'Side To Side Chins', 'FREE_EXERCISE_DB', 'Side_To_Side_Chins', 'Espalda', '["Bíceps", "Antebrazos", "Hombros"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Cuelga con agarre ancho, pecho afuera.
2. Sube inclinándote hacia una mano.
3. Baja y en la siguiente sube hacia la otra.', '[]'::jsonb, false),
    ('b148469d-e7c5-5e3d-ae0d-4c3bc791691c', 'Estiramiento con tirón de muñeca', 'Side Wrist Pull', 'FREE_EXERCISE_DB', 'Side_Wrist_Pull', 'Hombros', '["Antebrazos", "Espalda"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. De pie, cruza un brazo frente al cuerpo y sujeta la muñeca con la otra mano.
2. Tira y súbelo despacio hasta la altura del hombro.
3. Siente el estiramiento en la espalda y cambia de lado.', '[]'::jsonb, false),
    ('85e7e4bc-eba7-5977-b1c7-ce50bf4584ef', 'Desplazamiento lateral sobre cajón', 'Side to Side Box Shuffle', 'FREE_EXERCISE_DB', 'Side_to_Side_Box_Shuffle', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De costado al cajón con un pie encima.
2. Salta al otro lado cambiando de pie sobre el cajón.
3. Sigue de un lado a otro ayudándote con los brazos.', '[]'::jsonb, false),
    ('ef7edd8c-a7e2-5f44-9308-c8436e29a3c6', 'Cruce en polea a una mano', 'Single-Arm Cable Crossover', 'FREE_EXERCISE_DB', 'Single-Arm_Cable_Crossover', 'Pecho', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Entre dos poleas altas con las manos juntas al frente.
2. Abre un brazo al costado con el codo apenas flexionado.
3. Vuelve a juntarlo y alterna.', '[]'::jsonb, false),
    ('304e4f09-25cc-5ff4-905d-b72aad546730', 'Landmine jammer a una mano', 'Single-Arm Linear Jammer', 'FREE_EXERCISE_DB', 'Single-Arm_Linear_Jammer', 'Hombros', '["Pecho", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra anclada y el extremo a la altura del hombro en una mano.
2. Extiende cadera y rodillas con fuerza empujando la barra.
3. Vuelve al hombro y repite.', '[]'::jsonb, false),
    ('6cca2066-fed5-567f-a72b-b0600e192672', 'Flexión a una mano', 'Single-Arm Push-Up', 'FREE_EXERCISE_DB', 'Single-Arm_Push-Up', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En posición de flexión con un brazo bajo el hombro y los pies abiertos.
2. Baja hasta casi tocar el suelo.
3. Empuja hasta extender el brazo.', '["WRIST"]'::jsonb, false),
    ('dfb2383d-8c2a-5424-b258-94a92761444a', 'Sprint alrededor de un cono', 'Single-Cone Sprint Drill', 'FREE_EXERCISE_DB', 'Single-Cone_Sprint_Drill', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', NULL, 'PUSH', '1. De pie junto a un cono.
2. Rodéalo con pasos muy rápidos y rodillas altas.
3. Descansa tras tres vueltas.', '["ANKLE"]'::jsonb, false),
    ('b34f8c05-d6ec-5d5a-9513-50c4b37802ee', 'Subida a cajón alto a una pierna', 'Single-Leg High Box Squat', 'FREE_EXERCISE_DB', 'Single-Leg_High_Box_Squat', 'Cuádriceps', '["Glúteos", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Frente a un cajón alto, sujeto a una banda para equilibrio.
2. Sube al cajón con una pierna hasta quedar de pie.
3. Baja con control y cambia de pierna al terminar.', '["KNEE"]'::jsonb, false),
    ('777b7800-dd40-5f18-8205-65ca04bd7404', 'Saltos a una pierna sobre conos', 'Single-Leg Hop Progression', 'FREE_EXERCISE_DB', 'Single-Leg_Hop_Progression', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie sobre una pierna frente a una fila de conos.
2. Salta sobre cada cono cayendo con la misma pierna.
3. Vuelve con la otra pierna.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('ce7abb51-2dae-5268-b6fc-54e4246b27f9', 'Salto lateral a una pierna', 'Single-Leg Lateral Hop', 'FREE_EXERCISE_DB', 'Single-Leg_Lateral_Hop', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie sobre una pierna junto a un cono.
2. Salta de lado sobre el cono cayendo con la misma pierna.
3. Rebota de vuelta y sigue.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('76cf3975-604d-540b-81bc-799099981a35', 'Extensión de cuádriceps a una pierna', 'Single-Leg Leg Extension', 'FREE_EXERCISE_DB', 'Single-Leg_Leg_Extension', 'Cuádriceps', '[]'::jsonb, 'MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Sentado en la máquina con el rodillo sobre la parte baja de la tibia.
2. Extiende una pierna completa y pausa arriba.
3. Baja sin soltar la tensión y cambia de pierna.', '["KNEE"]'::jsonb, false),
    ('7ed14ce9-08c5-55e6-865b-a1e929739dc4', 'Salto con pierna en cajón', 'Single-Leg Stride Jump', 'FREE_EXERCISE_DB', 'Single-Leg_Stride_Jump', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De costado al cajón con el pie de adentro encima.
2. Empuja con esa pierna y salta lo más alto posible subiendo la otra rodilla.
3. Cae en la misma posición y repite.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('86b749c8-dd21-5272-89ff-45cbbecca23c', 'Elevación frontal con una mancuerna', 'Single Dumbbell Raise', 'FREE_EXERCISE_DB', 'Single_Dumbbell_Raise', 'Hombros', '["Antebrazos", "Trapecio"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', NULL, '1. De pie con una mancuerna sujeta por los discos con ambas manos.
2. Súbela con los brazos rectos por encima de los hombros.
3. Baja despacio.', '[]'::jsonb, false),
    ('7670a5c0-7959-5b4e-a706-013dd83468c1', 'Salto con talón al glúteo a una pierna', 'Single Leg Butt Kick', 'FREE_EXERCISE_DB', 'Single_Leg_Butt_Kick', 'Cuádriceps', '["Pantorrillas", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie sobre una pierna con la otra rodilla arriba.
2. Salta y lleva el talón de la pierna de apoyo al glúteo.
3. Cae suave y repite.', '[]'::jsonb, false),
    ('93367b14-74be-5fa7-83dc-21036bb4231a', 'Impulso a una pierna desde cajón', 'Single Leg Push-off', 'FREE_EXERCISE_DB', 'Single_Leg_Push-off', 'Cuádriceps', '["Pantorrillas", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. Con un pie sobre un cajón y el otro en el suelo.
2. Empuja con el pie de arriba saltando lo más alto posible.
3. Cae con el mismo pie en el cajón.', '[]'::jsonb, false),
    ('a7064467-c251-5a38-b2e9-f0e54015a00c', 'Abdominal completo', 'Sit-Up', 'FREE_EXERCISE_DB', 'Sit-Up', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca arriba con las rodillas flexionadas, los pies sujetos y las manos detrás de la cabeza.
2. Sube el torso hasta formar una V con los muslos.
3. Baja despacio.', '[]'::jsonb, false),
    ('855ff50a-2367-5741-8e1a-e517ad955ec6', 'Sentadillas parciales', 'Sit Squats', 'FREE_EXERCISE_DB', 'Sit_Squats', 'Cuádriceps', '["Abductores", "Glúteos", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'PUSH', '1. De pie con los pies al ancho de los hombros.
2. Baja llevando la cadera atrás sin llegar a la paralela.
3. Sube rápido y repite 5 a 10 veces.', '[]'::jsonb, false),
    ('c3eb0c90-7d22-532c-9109-bb9ac21fd5b9', 'Patinaje', 'Skating', 'FREE_EXERCISE_DB', 'Skating', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'CARDIO', 'Cardio', NULL, NULL, '1. Usa protecciones y domina giros y frenado.
2. Patina a ritmo cómodo unos 30 minutos.
3. Para más intensidad, alterna 2 min rápido con 3 min suave.', '[]'::jsonb, false),
    ('234b809e-745d-5f82-910f-6a53144ee386', 'Arrastre de trineo con arnés', 'Sled Drag - Harness', 'FREE_EXERCISE_DB', 'Sled_Drag_-_Harness', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'STRONGMAN', 'Strongman', 'COMPOUND', 'PUSH', '1. Carga el trineo y engánchalo al arnés o al cinturón.
2. Inclínate en la dirección de avance.
3. Avanza extendiendo cadera y rodillas.', '[]'::jsonb, false),
    ('85f608eb-5471-5c6b-988d-380fdfebe7ff', 'Caminata hacia atrás con trineo y brazos arriba', 'Sled Overhead Backward Walk', 'FREE_EXERCISE_DB', 'Sled_Overhead_Backward_Walk', 'Hombros', '["Pantorrillas", "Espalda", "Cuádriceps"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De frente al trineo con los agarres y la cuerda tensa.
2. Sube los brazos rectos sobre la cabeza.
3. Camina hacia atrás sin tirones.', '["SHOULDER"]'::jsonb, false),
    ('e0f4eb19-bc64-5c22-a9c8-5375e50eb354', 'Extensión de tríceps con trineo', 'Sled Overhead Triceps Extension', 'FREE_EXERCISE_DB', 'Sled_Overhead_Triceps_Extension', 'Tríceps', '[]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De espaldas al trineo con los agarres detrás de la cabeza y los codos arriba.
2. Extiende los codos sin mover los brazos.
3. Da un paso al frente para tensar la cuerda y repite.', '["SHOULDER", "ELBOW"]'::jsonb, false),
    ('8e71c992-3adc-5fe8-8dfa-4b6acd382dc0', 'Empuje de trineo', 'Sled Push', 'FREE_EXERCISE_DB', 'Sled_Push', 'Cuádriceps', '["Pantorrillas", "Pecho", "Glúteos", "Isquiotibiales", "Tríceps"]'::jsonb, 'OTHER', 'BEGINNER', 'STRONGMAN', 'Strongman', 'COMPOUND', 'PUSH', '1. Inclinado sobre el trineo con los brazos extendidos en los agarres.
2. Empújalo lo más rápido posible.
3. Extiende cadera y rodillas en cada paso.', '["KNEE"]'::jsonb, false),
    ('88ba9834-b92f-5eba-8da5-934c69fe5c74', 'Aperturas invertidas con trineo', 'Sled Reverse Flye', 'FREE_EXERCISE_DB', 'Sled_Reverse_Flye', 'Hombros', '[]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De frente al trineo con los agarres a la cintura y la cuerda tensa.
2. Abre los brazos arriba y afuera sin flexionar los codos.
3. Vuelve y retrocede para tensar la cuerda.', '[]'::jsonb, false),
    ('742a544f-7272-5448-b455-a8e8a1c88027', 'Remo con trineo', 'Sled Row', 'FREE_EXERCISE_DB', 'Sled_Row', 'Espalda', '["Bíceps"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De frente al trineo con los agarres y los brazos extendidos.
2. Tira juntando los omóplatos para acercar el trineo.
3. Retrocede un paso y repite.', '[]'::jsonb, false),
    ('8f4c854b-980e-5256-920d-23227a18689e', 'Golpes con mazo', 'Sledgehammer Swings', 'FREE_EXERCISE_DB', 'Sledgehammer_Swings', 'Abdomen', '["Pantorrillas", "Antebrazos", "Espalda", "Hombros"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PULL', '1. Frente a un neumático con el mazo y los pies escalonados.
2. Súbelo deslizando la mano hacia la cabeza del mazo.
3. Golpea el neumático con fuerza y controla el rebote.', '["LOWER_BACK"]'::jsonb, false),
    ('feb82593-ee73-56df-b8b4-c6076dd5d68b', 'Protracción de hombros inclinado en máquina Smith', 'Smith Incline Shoulder Raise', 'FREE_EXERCISE_DB', 'Smith_Incline_Shoulder_Raise', 'Hombros', '["Pecho"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. En banco inclinado bajo la Smith con la barra arriba y los codos casi rectos.
2. Empuja la barra separando los omóplatos del banco.
3. Baja despacio sin flexionar los codos.', '[]'::jsonb, false),
    ('d6a57cd2-5201-5e31-9fc6-1ea242912252', 'Encogimientos por detrás en máquina Smith', 'Smith Machine Behind the Back Shrug', 'FREE_EXERCISE_DB', 'Smith_Machine_Behind_the_Back_Shrug', 'Trapecio', '["Hombros"]'::jsonb, 'SMITH_MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie con la barra detrás de ti, agarre prono.
2. Encoge los hombros hacia arriba sin flexionar los brazos.
3. Pausa y baja despacio.', '["NECK"]'::jsonb, false),
    ('0685994f-ffc0-5f99-a6f1-a8827a18ef9f', 'Press de banca en máquina Smith', 'Smith Machine Bench Press', 'FREE_EXERCISE_DB', 'Smith_Machine_Bench_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'SMITH_MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En banco plano bajo la Smith con agarre algo ancho.
2. Baja la barra al pecho medio.
3. Pausa y empuja hasta extender los brazos.', '["SHOULDER"]'::jsonb, false),
    ('7c334564-1334-547a-a616-bf8f49b53760', 'Remo inclinado en máquina Smith', 'Smith Machine Bent Over Row', 'FREE_EXERCISE_DB', 'Smith_Machine_Bent_Over_Row', 'Espalda', '["Bíceps", "Hombros"]'::jsonb, 'SMITH_MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Inclinado casi paralelo al suelo con la barra de la Smith en agarre prono.
2. Tira la barra hacia el abdomen con los codos cerca.
3. Aprieta la espalda y baja despacio.', '["LOWER_BACK"]'::jsonb, false),
    ('1fdb46ff-d4ba-5ad6-a7a2-52c6401c6c12', 'Elevación de talones en máquina Smith', 'Smith Machine Calf Raise', 'FREE_EXERCISE_DB', 'Smith_Machine_Calf_Raise', 'Pantorrillas', '[]'::jsonb, 'SMITH_MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Con la barra sobre la espalda y la punta de los pies sobre un disco.
2. Sube los talones lo más alto posible.
3. Pausa y baja despacio.', '["ANKLE"]'::jsonb, true),
    ('f9ff27ab-d431-5024-89e3-90c1d92304c7', 'Press de banca cerrado en máquina Smith', 'Smith Machine Close-Grip Bench Press', 'FREE_EXERCISE_DB', 'Smith_Machine_Close-Grip_Bench_Press', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'SMITH_MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En banco plano bajo la Smith con agarre al ancho de los hombros.
2. Baja la barra al pecho con los codos pegados.
3. Empuja hasta extender los brazos.', '["SHOULDER", "ELBOW", "WRIST"]'::jsonb, false),
    ('765e7947-31d3-5c80-9387-66f163403118', 'Press de pecho declinado en máquina Smith', 'Smith Machine Decline Press', 'FREE_EXERCISE_DB', 'Smith_Machine_Decline_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'SMITH_MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', NULL, '1. En banco declinado bajo la Smith con los brazos extendidos.
2. Baja la barra al pecho.
3. Pausa y empuja hasta extender los brazos.', '[]'::jsonb, false),
    ('7d7ace97-cd82-5f22-8ea7-cc58e9517745', 'Cargada colgante en máquina Smith', 'Smith Machine Hang Power Clean', 'FREE_EXERCISE_DB', 'Smith_Machine_Hang_Power_Clean', 'Isquiotibiales', '["Glúteos", "Lumbares", "Cuádriceps", "Hombros", "Trapecio"]'::jsonb, 'SMITH_MACHINE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la barra a la altura de las rodillas y la cadera atrás.
2. Extiende cadera y rodillas con fuerza tirando con los codos altos.
3. Recibe la barra a la altura de los hombros y bájala.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('887399db-6610-589b-b2ec-14394c9e5a01', 'Elevación de cadera en máquina Smith', 'Smith Machine Hip Raise', 'FREE_EXERCISE_DB', 'Smith_Machine_Hip_Raise', 'Abdomen', '[]'::jsonb, 'SMITH_MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Acostado en banco con las plantas contra la barra y las piernas extendidas.
2. Despega la cadera del banco rotando la pelvis.
3. Pausa y baja despacio.', '[]'::jsonb, false),
    ('2dbaa31b-31c8-51bd-995b-d28af7b1750a', 'Press inclinado en máquina Smith', 'Smith Machine Incline Bench Press', 'FREE_EXERCISE_DB', 'Smith_Machine_Incline_Bench_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'SMITH_MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En banco inclinado bajo la Smith con agarre algo ancho.
2. Baja la barra al pecho alto.
3. Empuja hasta extender los brazos.', '["SHOULDER"]'::jsonb, false),
    ('cfa906de-bfcf-5600-8c51-9771e18cb9fb', 'Prensa de piernas en máquina Smith', 'Smith Machine Leg Press', 'FREE_EXERCISE_DB', 'Smith_Machine_Leg_Press', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'SMITH_MACHINE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado bajo la barra con la mitad de los pies sobre ella y las rodillas al pecho.
2. Empuja la barra hacia arriba sin bloquear las rodillas.
3. Pausa y baja despacio.', '["KNEE"]'::jsonb, false),
    ('6c347285-fd17-50e7-b2b9-ae842b5d5670', 'Remo al mentón a una mano en máquina Smith', 'Smith Machine One-Arm Upright Row', 'FREE_EXERCISE_DB', 'Smith_Machine_One-Arm_Upright_Row', 'Hombros', '["Bíceps", "Trapecio"]'::jsonb, 'SMITH_MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De pie con la barra a la altura de los muslos y una mano en ella.
2. Sube el codo hacia afuera hasta que el brazo quede paralelo al suelo.
3. Pausa y baja; cambia de lado.', '["SHOULDER"]'::jsonb, false),
    ('13ace11f-8465-5df0-8281-63dcb67a56dc', 'Press de hombros en máquina Smith', 'Smith Machine Overhead Shoulder Press', 'FREE_EXERCISE_DB', 'Smith_Machine_Overhead_Shoulder_Press', 'Hombros', '["Tríceps"]'::jsonb, 'SMITH_MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Sentado bajo la Smith con la barra a la altura de los hombros.
2. Empuja hasta extender los brazos.
3. Baja despacio.', '["SHOULDER"]'::jsonb, false),
    ('b1db0705-1785-5bcd-b74b-cd993d50e8f4', 'Sentadilla pistol en máquina Smith', 'Smith Machine Pistol Squat', 'FREE_EXERCISE_DB', 'Smith_Machine_Pistol_Squat', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'SMITH_MACHINE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra sobre la espalda, un pie adelante y la otra pierna elevada al frente.
2. Baja sobre la pierna de apoyo con la espalda recta.
3. Sube empujando con el talón y cambia de pierna.', '["KNEE"]'::jsonb, false),
    ('1da1b34a-a17c-5a10-8046-73cac0bae25d', 'Elevación de puntas en máquina Smith', 'Smith Machine Reverse Calf Raises', 'FREE_EXERCISE_DB', 'Smith_Machine_Reverse_Calf_Raises', 'Pantorrillas', '[]'::jsonb, 'SMITH_MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Con la barra sobre la espalda y los talones sobre una plataforma.
2. Sube las puntas de los pies lo más posible.
3. Baja despacio.', '["ANKLE"]'::jsonb, false),
    ('6e7c4eeb-7b99-51a6-b06d-a9ee0e4474b1', 'Peso muerto piernas rígidas en máquina Smith', 'Smith Machine Stiff-Legged Deadlift', 'FREE_EXERCISE_DB', 'Smith_Machine_Stiff-Legged_Deadlift', 'Isquiotibiales', '["Glúteos", "Lumbares"]'::jsonb, 'SMITH_MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De pie con la barra de la Smith frente a los muslos y las rodillas apenas flexionadas.
2. Lleva la cadera atrás bajando la barra con la espalda recta.
3. Sube apretando glúteos e isquiotibiales.', '["LOWER_BACK"]'::jsonb, false),
    ('9abbe180-8a9e-53ff-abaa-1bf20d134bf3', 'Remo al mentón en máquina Smith', 'Smith Machine Upright Row', 'FREE_EXERCISE_DB', 'Smith_Machine_Upright_Row', 'Trapecio', '["Bíceps", "Espalda", "Hombros"]'::jsonb, 'SMITH_MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De pie con la barra de la Smith frente a los muslos, agarre prono al ancho de los hombros.
2. Sube la barra pegada al cuerpo llevando los codos arriba.
3. Pausa cerca del mentón y baja despacio.', '["SHOULDER"]'::jsonb, true),
    ('f138ce9d-90ac-550d-a508-6fb420dc8d3d', 'Sentadilla búlgara en máquina Smith', 'Smith Single-Leg Split Squat', 'FREE_EXERCISE_DB', 'Smith_Single-Leg_Split_Squat', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'SMITH_MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra sobre la espalda y un pie apoyado atrás en un banco.
2. Baja hasta que el muslo delantero quede paralelo.
3. Sube empujando con el talón y cambia de pierna.', '["KNEE", "HIP", "ANKLE"]'::jsonb, false),
    ('cfa936e4-cba1-52c0-a12b-c6c8d29427ec', 'Arrancada', 'Snatch', 'FREE_EXERCISE_DB', 'Snatch', 'Cuádriceps', '["Bíceps", "Glúteos", "Isquiotibiales", "Lumbares", "Hombros", "Trapecio", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con la barra sobre el empeine, agarre ancho y la cadera baja.
2. Empuja el suelo y extiende cadera, rodillas y tobillos con fuerza.
3. Métete debajo en sentadilla profunda con la barra arriba y ponte de pie.', '["LOWER_BACK", "SHOULDER"]'::jsonb, false),
    ('4c6ca38a-3545-556b-b56b-b302d7e0d69a', 'Equilibrio de arrancada', 'Snatch Balance', 'FREE_EXERCISE_DB', 'Snatch_Balance', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Hombros", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PUSH', '1. Con la barra sobre la espalda y agarre de arrancada.
2. Flexiona un poco las rodillas, impulsa y métete debajo con fuerza.
3. Recibe la barra arriba en sentadilla profunda y ponte de pie.', '["LOWER_BACK", "SHOULDER"]'::jsonb, false),
    ('9b4c867d-ad54-5c19-9345-619da9ab9c92', 'Peso muerto de arrancada', 'Snatch Deadlift', 'FREE_EXERCISE_DB', 'Snatch_Deadlift', 'Isquiotibiales', '["Antebrazos", "Glúteos", "Lumbares", "Cuádriceps", "Trapecio"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con agarre ancho, la barra en el suelo y la espalda firme.
2. Sube empujando con los talones manteniendo el ángulo de la espalda.
3. Al pasar las rodillas, lleva la cadera a la barra hasta quedar erguido.', '["LOWER_BACK", "SHOULDER"]'::jsonb, false),
    ('5da4084b-b41e-5d4e-a565-7f03be9a15c3', 'Tirón de arrancada', 'Snatch Pull', 'FREE_EXERCISE_DB', 'Snatch_Pull', 'Isquiotibiales', '["Pantorrillas", "Glúteos", "Lumbares", "Cuádriceps", "Trapecio"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con agarre ancho, la barra cerca de las tibias y la espalda recta.
2. Extiende las rodillas sin cambiar el ángulo de la espalda.
3. Al pasar las rodillas, extiende cadera, rodillas y tobillos con fuerza y encoge los hombros.', '["LOWER_BACK", "SHOULDER"]'::jsonb, false),
    ('604c2be4-80c8-5029-8c93-10859b85c687', 'Encogimiento de arrancada', 'Snatch Shrug', 'FREE_EXERCISE_DB', 'Snatch_Shrug', 'Trapecio', '["Antebrazos", "Hombros"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con agarre ancho y la barra a mitad del muslo, torso algo inclinado.
2. Encoge los hombros hacia las orejas con fuerza.
3. Baja con control.', '["LOWER_BACK", "SHOULDER", "NECK"]'::jsonb, false),
    ('1bcd71d9-b982-5266-af27-a15fe8e803cd', 'Arrancada desde bloques', 'Snatch from Blocks', 'FREE_EXERCISE_DB', 'Snatch_from_Blocks', 'Cuádriceps', '["Pantorrillas", "Antebrazos", "Glúteos", "Isquiotibiales", "Lumbares", "Hombros", "Trapecio", "Tríceps"]'::jsonb, 'BARBELL', 'ADVANCED', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con la barra sobre bloques y agarre ancho.
2. Extiende cadera, rodillas y tobillos con fuerza.
3. Recibe la barra arriba en sentadilla profunda y ponte de pie.', '["LOWER_BACK", "SHOULDER"]'::jsonb, false),
    ('3e6fec19-4862-516b-9338-c3a9bb4cbdec', 'Extensión de tríceps rápida con banda', 'Speed Band Overhead Triceps', 'FREE_EXERCISE_DB', 'Speed_Band_Overhead_Triceps', 'Tríceps', '[]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Con la banda anclada abajo y sujeta detrás de la cabeza, codos arriba.
2. Extiende los codos rápido sin mover los brazos.
3. Pausa y vuelve.', '["SHOULDER"]'::jsonb, false),
    ('2575319d-51e2-5a9d-b409-64b86e174668', 'Sentadilla al cajón con velocidad', 'Speed Box Squat', 'FREE_EXERCISE_DB', 'Speed_Box_Squat', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. Con bandas en la barra y 50 a 70% de tu máximo sobre la espalda.
2. Siéntate en el cajón con control.
3. Sube lo más rápido posible.', '["KNEE"]'::jsonb, false),
    ('58588cb0-32cd-5d38-ae79-02382ad1c93c', 'Sentadilla con velocidad', 'Speed Squats', 'FREE_EXERCISE_DB', 'Speed_Squats', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'BARBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con una carga ligera sobre la espalda.
2. Baja con control hasta la paralela.
3. Sube lo más rápido posible.', '["KNEE"]'::jsonb, false),
    ('861a515c-12e3-575b-8e5d-82388577b559', 'Lanzador de hechizos con mancuernas', 'Spell Caster', 'FREE_EXERCISE_DB', 'Spell_Caster', 'Abdomen', '["Glúteos", "Hombros"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De pie con los pies abiertos y una mancuerna en cada mano.
2. Lleva las mancuernas junto a una cadera girando el torso.
3. Gira y llévalas al otro lado con los brazos rectos.', '[]'::jsonb, false),
    ('c31b73ca-c4e7-5fc8-9759-20ca5a7208fa', 'Gateo de araña', 'Spider Crawl', 'FREE_EXERCISE_DB', 'Spider_Crawl', 'Abdomen', '["Pecho", "Hombros", "Tríceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. En posición de flexión con los codos a 90°.
2. Lleva una rodilla hacia el codo del mismo lado por fuera.
3. Vuelve y repite del otro lado.', '[]'::jsonb, false),
    ('a1d5d5f7-7646-536f-82d1-b5295e70a805', 'Curl araña con barra Z', 'Spider Curl', 'FREE_EXERCISE_DB', 'Spider_Curl', 'Bíceps', '[]'::jsonb, 'EZ_BAR', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca abajo sobre el lado inclinado del banco predicador, brazos colgando.
2. Sube la barra Z flexionando los codos.
3. Aprieta y baja despacio.', '[]'::jsonb, false),
    ('35c7c62d-5755-59cc-b6c4-fe4493ef155a', 'Estiramiento de columna sentado', 'Spinal Stretch', 'FREE_EXERCISE_DB', 'Spinal_Stretch', 'Espalda', '["Lumbares", "Cuello", "Trapecio"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Sentado con la espalda recta y las manos detrás de la cabeza.
2. Gira el torso hacia un lado varias veces.
3. Inclínate y lleva el codo hacia el interior de la rodilla; cambia de lado.', '[]'::jsonb, false),
    ('5b78b875-9da1-5100-a286-0671c6bf9611', 'Cargada en tijera', 'Split Clean', 'FREE_EXERCISE_DB', 'Split_Clean', 'Cuádriceps', '["Pantorrillas", "Antebrazos", "Glúteos", "Isquiotibiales", "Lumbares", "Hombros", "Trapecio"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con la barra cerca de las tibias y la espalda recta.
2. Tira y extiende cadera, rodillas y tobillos con fuerza.
3. Recibe la barra en los hombros cayendo en tijera y junta los pies.', '["KNEE", "LOWER_BACK", "WRIST"]'::jsonb, false),
    ('588a9681-1de6-5e27-9c5e-a46899634044', 'Envión en tijera', 'Split Jerk', 'FREE_EXERCISE_DB', 'Split_Jerk', 'Cuádriceps', '["Glúteos", "Isquiotibiales", "Hombros", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PUSH', '1. Con la barra al frente de los hombros y los pies bajo la cadera.
2. Flexiona un poco las rodillas e impulsa con fuerza.
3. Recibe la barra arriba cayendo en tijera y junta los pies.', '["KNEE", "SHOULDER"]'::jsonb, false),
    ('8ee5944d-c8e4-5180-969f-13388b1dfec8', 'Salto en zancada', 'Split Jump', 'FREE_EXERCISE_DB', 'Split_Jump', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. En zancada con la rodilla de atrás cerca del suelo.
2. Salta lo más alto posible impulsando con los brazos.
3. Cae en la misma zancada amortiguando y repite.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('027cf944-47e8-508a-baf9-7855e7c9abf0', 'Arrancada en tijera', 'Split Snatch', 'FREE_EXERCISE_DB', 'Split_Snatch', 'Isquiotibiales', '["Pantorrillas", "Antebrazos", "Glúteos", "Lumbares", "Cuádriceps", "Hombros", "Trapecio", "Tríceps"]'::jsonb, 'BARBELL', 'ADVANCED', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con la barra en el suelo y agarre ancho.
2. Tira y extiende cadera, rodillas y tobillos con fuerza.
3. Recibe la barra arriba cayendo en tijera y junta los pies.', '["KNEE", "LOWER_BACK", "SHOULDER"]'::jsonb, false),
    ('43d3451b-22e7-57d6-858e-37e634a2c2f8', 'Saltos en tijera', 'Split Squats', 'FREE_EXERCISE_DB', 'Split_Squats', 'Isquiotibiales', '["Pantorrillas", "Glúteos", "Cuádriceps"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'PUSH', '1. De pie.
2. Salta a una zancada flexionando un poco las rodillas.
3. Salta de nuevo cambiando de pierna; 5 a 10 por lado.', '[]'::jsonb, false),
    ('ddff384f-669f-5127-8798-a070cb3123d1', 'Envión en sentadilla', 'Squat Jerk', 'FREE_EXERCISE_DB', 'Squat_Jerk', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Hombros", "Tríceps"]'::jsonb, 'BARBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra al frente de los hombros.
2. Flexiona un poco las rodillas e impulsa con fuerza.
3. Recibe la barra arriba en sentadilla profunda y ponte de pie.', '["KNEE", "SHOULDER"]'::jsonb, false),
    ('6c2c6aee-6b7f-536e-b3ee-fbc9f3f0ab62', 'Sentadilla con bandas', 'Squat with Bands', 'FREE_EXERCISE_DB', 'Squat_with_Bands', 'Cuádriceps', '["Aductores", "Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. Con bandas en la barra ancladas abajo y la barra sobre la espalda.
2. Baja con la espalda arqueada hasta la paralela.
3. Sube empujando con los talones contra la tensión.', '["KNEE"]'::jsonb, false),
    ('8e8e332e-7a99-5600-8bb2-c6dcdd3fef3a', 'Sentadilla con cadenas', 'Squat with Chains', 'FREE_EXERCISE_DB', 'Squat_with_Chains', 'Cuádriceps', '["Aductores", "Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PUSH', '1. Con cadenas colgando de la barra y la barra sobre la espalda.
2. Baja con la espalda firme hasta la paralela.
3. Sube empujando con los talones; la carga aumenta al subir.', '["KNEE"]'::jsonb, false),
    ('04d876fd-5e0b-5729-b753-9b1ce8d61765', 'Sentadilla con desplazamiento de disco', 'Squat with Plate Movers', 'FREE_EXERCISE_DB', 'Squat_with_Plate_Movers', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra sobre la espalda, postura abierta y un pie sobre un disco.
2. Baja en sentadilla y sube.
3. Mueve el disco con el pie entre series y repite.', '["KNEE"]'::jsonb, false),
    ('692b4c4d-e25c-5850-b5ce-951e4a9b5399', 'Sentadilla con banda', 'Squats - With Bands', 'FREE_EXERCISE_DB', 'Squats_-_With_Bands', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie sobre la banda con los agarres junto a la cara.
2. Baja hasta que los muslos queden paralelos.
3. Sube empujando con los talones.', '["KNEE"]'::jsonb, true),
    ('d6fbae74-4a29-578b-9d5c-e41ff9be4f18', 'Escaladora', 'Stairmaster', 'FREE_EXERCISE_DB', 'Stairmaster', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'MACHINE', 'INTERMEDIATE', 'CARDIO', 'Cardio', NULL, NULL, '1. Sube a la máquina y elige un programa o el modo manual.
2. Pisa con ritmo constante sin llevar los pedales al tope.
3. Sujétate de las barras para no caerte.', '[]'::jsonb, false),
    ('9f0ef612-dad0-5134-a85f-c99ad512fb7e', 'Press alterno de pie con mancuernas', 'Standing Alternating Dumbbell Press', 'FREE_EXERCISE_DB', 'Standing_Alternating_Dumbbell_Press', 'Hombros', '["Tríceps"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie con las mancuernas en los hombros, palmas al frente.
2. Empuja una mancuerna arriba sin inclinarte.
3. Bájala y empuja la otra.', '[]'::jsonb, false),
    ('0be18d5d-f830-57fd-9f9a-8fb07e2ea5c0', 'Elevación de talones de pie con barra', 'Standing Barbell Calf Raise', 'FREE_EXERCISE_DB', 'Standing_Barbell_Calf_Raise', 'Pantorrillas', '[]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Con la barra sobre la espalda y los pies al ancho de los hombros.
2. Sube los talones lo más alto posible.
3. Pausa y baja despacio.', '["ANKLE"]'::jsonb, false),
    ('4af5eda4-8fce-53e1-9c5b-588696896078', 'Press tras nuca de pie con barra', 'Standing Barbell Press Behind Neck', 'FREE_EXERCISE_DB', 'Standing_Barbell_Press_Behind_Neck', 'Hombros', '["Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra sobre la espalda alta y agarre algo ancho.
2. Empuja la barra hasta extender los brazos.
3. Bájala despacio detrás de la cabeza.', '["NECK"]'::jsonb, false),
    ('a957875f-f39f-5292-8c6e-9768b4f4c263', 'Patada de tríceps a una mano', 'Standing Bent-Over One-Arm Dumbbell Triceps Extension', 'FREE_EXERCISE_DB', 'Standing_Bent-Over_One-Arm_Dumbbell_Triceps_Extension', 'Tríceps', '["Hombros"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Inclinado casi paralelo al suelo con el brazo pegado al torso y el codo a 90°.
2. Extiende el codo hacia atrás sin mover el brazo.
3. Vuelve despacio y cambia de brazo.', '["ELBOW"]'::jsonb, false),
    ('352fc7fb-f953-5a62-88a2-37e147ada55f', 'Patada de tríceps con dos mancuernas', 'Standing Bent-Over Two-Arm Dumbbell Triceps Extension', 'FREE_EXERCISE_DB', 'Standing_Bent-Over_Two-Arm_Dumbbell_Triceps_Extension', 'Tríceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Inclinado casi paralelo al suelo con los brazos pegados al torso y los codos a 90°.
2. Extiende los codos hacia atrás sin mover los brazos.
3. Vuelve despacio.', '["ELBOW"]'::jsonb, false),
    ('18b77cef-3d00-568b-88c3-241ec0351fec', 'Estiramiento de bíceps de pie', 'Standing Biceps Stretch', 'FREE_EXERCISE_DB', 'Standing_Biceps_Stretch', 'Bíceps', '["Pecho", "Hombros"]'::jsonb, 'OTHER', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Entrelaza las manos detrás de la espalda con los brazos rectos.
2. Gira las palmas hacia abajo.
3. Sube los brazos hasta sentir el estiramiento y sostén.', '[]'::jsonb, false),
    ('155ef7d8-cca4-57af-aad6-1db12f2aa7fc', 'Press Bradford de pie', 'Standing Bradford Press', 'FREE_EXERCISE_DB', 'Standing_Bradford_Press', 'Hombros', '["Tríceps"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie con la barra al frente de los hombros.
2. Pásala sobre la cabeza sin bloquear los codos y bájala detrás de la cabeza.
3. Vuelve al frente y alterna.', '["SHOULDER"]'::jsonb, false),
    ('21a023ad-30e6-59eb-8825-903ffa5f4c39', 'Elevación diagonal en polea', 'Standing Cable Lift', 'FREE_EXERCISE_DB', 'Standing_Cable_Lift', 'Abdomen', '["Hombros"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De costado a la polea baja, en sentadilla con el agarre en ambas manos.
2. Tira en diagonal hacia arriba y afuera girando el torso al ponerte de pie.
3. Vuelve con control.', '[]'::jsonb, false),
    ('6e99bc60-2707-518d-af1b-90adba0986a0', 'Curl concentrado de pie', 'Standing Concentration Curl', 'FREE_EXERCISE_DB', 'Standing_Concentration_Curl', 'Bíceps', '["Antebrazos"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Inclinado al frente con el brazo colgando y el codo hacia afuera.
2. Sube la mancuerna flexionando el codo sin mover el brazo.
3. Aprieta y baja despacio.', '[]'::jsonb, false),
    ('5d8d538b-cbdd-51d6-8185-d0eb6425eedb', 'Press de hombros de pie con mancuernas', 'Standing Dumbbell Press', 'FREE_EXERCISE_DB', 'Standing_Dumbbell_Press', 'Hombros', '["Tríceps"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie con las mancuernas a la altura de la cabeza, codos a 90°.
2. Empuja hacia arriba sin impulso de piernas ni inclinarte.
3. Pausa y baja despacio.', '[]'::jsonb, false),
    ('da832c29-d55c-5fcf-b58d-0ffeaec40395', 'Curl invertido con mancuernas', 'Standing Dumbbell Reverse Curl', 'FREE_EXERCISE_DB', 'Standing_Dumbbell_Reverse_Curl', 'Bíceps', '["Antebrazos"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie con las mancuernas en agarre prono.
2. Sube flexionando solo los codos.
3. Aprieta y baja despacio.', '[]'::jsonb, false),
    ('577a7eb7-6eca-58d3-8a75-6c4486a70161', 'Elevación frontal hasta arriba con mancuernas', 'Standing Dumbbell Straight-Arm Front Delt Raise Above Head', 'FREE_EXERCISE_DB', 'Standing_Dumbbell_Straight-Arm_Front_Delt_Raise_Above_Head', 'Hombros', '[]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie con las mancuernas frente a los muslos y los brazos rectos.
2. Súbelas en semicírculo hasta sobre la cabeza.
3. Vuelve por el mismo camino.', '[]'::jsonb, false),
    ('41570979-bb4f-5933-82c7-0c0287e9b4ff', 'Extensión de tríceps de pie con mancuerna', 'Standing Dumbbell Triceps Extension', 'FREE_EXERCISE_DB', 'Standing_Dumbbell_Triceps_Extension', 'Tríceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie con una mancuerna sobre la cabeza sostenida con ambas manos.
2. Bájala detrás de la cabeza con los codos cerca.
3. Extiende los codos para volver.', '["ELBOW"]'::jsonb, false),
    ('3e1f1b58-9a1e-5de7-9e6e-83642bd743c3', 'Remo al mentón con mancuernas', 'Standing Dumbbell Upright Row', 'FREE_EXERCISE_DB', 'Standing_Dumbbell_Upright_Row', 'Trapecio', '["Bíceps", "Hombros"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De pie con las mancuernas frente a los muslos, agarre prono.
2. Súbelas pegadas al cuerpo llevando los codos arriba.
3. Pausa cerca del mentón y baja despacio.', '["SHOULDER"]'::jsonb, false),
    ('03d0d9e0-2c74-5749-960d-429349077b9e', 'Estiramiento de cuádriceps con pie elevado', 'Standing Elevated Quad Stretch', 'FREE_EXERCISE_DB', 'Standing_Elevated_Quad_Stretch', 'Cuádriceps', '[]'::jsonb, 'OTHER', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. De espaldas a un banco, apoya el empeine de un pie sobre él.
2. Flexiona un poco la rodilla de apoyo.
3. Sostén y cambia de lado.', '[]'::jsonb, false),
    ('0e5d4ea4-cbdd-5efd-974b-299b594d31bb', 'Elevación frontal hasta arriba con barra', 'Standing Front Barbell Raise Over Head', 'FREE_EXERCISE_DB', 'Standing_Front_Barbell_Raise_Over_Head', 'Hombros', '[]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie con la barra en agarre prono cerrado y los codos apenas flexionados.
2. Súbela hasta quedar sobre la cabeza.
3. Baja despacio.', '[]'::jsonb, false),
    ('72b54a95-2fae-599c-ab50-96191c23415a', 'Estiramiento de gemelo de pie', 'Standing Gastrocnemius Calf Stretch', 'FREE_EXERCISE_DB', 'Standing_Gastrocnemius_Calf_Stretch', 'Pantorrillas', '["Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Apoya el talón en un escalón con la rodilla extendida.
2. Inclínate y tira de la punta del pie hacia ti.
3. Sostén y cambia de lado.', '[]'::jsonb, false),
    ('3c040ae9-8ff6-5ae9-ae5c-4055e82079f9', 'Estiramiento de isquiotibiales y pantorrilla de pie', 'Standing Hamstring and Calf Stretch', 'FREE_EXERCISE_DB', 'Standing_Hamstring_and_Calf_Stretch', 'Isquiotibiales', '[]'::jsonb, 'OTHER', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. De pie con una cinta en el pie adelantado y la pierna de atrás flexionada.
2. Sube la punta del pie e inclínate al frente tirando de la cinta.
3. Sostén 10 a 20 s y cambia de pie.', '[]'::jsonb, false),
    ('e24e20e3-cedd-569f-9630-438ac2b01f01', 'Círculos de cadera de pie', 'Standing Hip Circles', 'FREE_EXERCISE_DB', 'Standing_Hip_Circles', 'Abductores', '["Aductores"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'PULL', '1. De pie sobre una pierna, sujeto a un apoyo, con la otra rodilla a 90°.
2. Abre la cadera dibujando un círculo grande con la rodilla.
3. Repite despacio y cambia de lado.', '[]'::jsonb, false),
    ('f31385dc-8257-5b6a-8530-1312c7dc2ed9', 'Estiramiento de flexores de cadera de pie', 'Standing Hip Flexors', 'FREE_EXERCISE_DB', 'Standing_Hip_Flexors', 'Cuádriceps', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. De pie con un pie un poco adelante.
2. Flexiona ambas rodillas y empuja la cadera de atrás al frente.
3. Sostén y cambia de lado.', '[]'::jsonb, false),
    ('664d990a-4ff0-58a0-85ce-7710745df93a', 'Curl abierto de pie', 'Standing Inner-Biceps Curl', 'FREE_EXERCISE_DB', 'Standing_Inner-Biceps_Curl', 'Bíceps', '[]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie con las mancuernas a los costados en agarre neutro.
2. Súbelas hacia afuera girando las palmas hacia arriba.
3. Aprieta y baja despacio.', '[]'::jsonb, false),
    ('ad62aae0-899c-53b1-839b-992a07182610', 'Estiramiento lateral de pie', 'Standing Lateral Stretch', 'FREE_EXERCISE_DB', 'Standing_Lateral_Stretch', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. De pie, una mano en la cadera y la otra detrás de la cabeza.
2. Inclina el torso hacia el lado de la mano en la cadera.
3. Sostén y cambia de lado.', '[]'::jsonb, false),
    ('05900f95-b555-5350-be15-a2756b54c08c', 'Salto largo sin impulso', 'Standing Long Jump', 'FREE_EXERCISE_DB', 'Standing_Long_Jump', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie en media sentadilla, preferible sobre una superficie blanda.
2. Balancea los brazos y salta lo más lejos posible.
3. Cae con los pies adelante y mide la distancia.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('2a12dd45-b287-58dc-aae5-683075b090a9', 'Elevación lateral en polea baja', 'Standing Low-Pulley Deltoid Raise', 'FREE_EXERCISE_DB', 'Standing_Low-Pulley_Deltoid_Raise', 'Hombros', '["Antebrazos"]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De costado a la polea baja, toma el agarre con la mano más lejana.
2. Sube el brazo cruzando el cuerpo hasta la altura del hombro.
3. Baja despacio y cambia de lado.', '[]'::jsonb, false),
    ('e74edee1-ce9f-53fd-93d4-599e66b5fdca', 'Extensión de tríceps a una mano en polea baja', 'Standing Low-Pulley One-Arm Triceps Extension', 'FREE_EXERCISE_DB', 'Standing_Low-Pulley_One-Arm_Triceps_Extension', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'CABLE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De espaldas a la polea baja con el agarre sobre la cabeza.
2. Baja la mano detrás de la cabeza con el codo fijo.
3. Extiende el codo y cambia de brazo.', '["ELBOW"]'::jsonb, false),
    ('909da695-ac5e-59e4-b383-4d54c6050de7', 'Agarre de discos con los dedos', 'Standing Olympic Plate Hand Squeeze', 'FREE_EXERCISE_DB', 'Standing_Olympic_Plate_Hand_Squeeze', 'Antebrazos', '["Bíceps"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'STATIC', '1. De pie con un disco en cada mano sujeto por el borde.
2. Abre un poco los dedos dejándolo bajar sin soltarlo.
3. Ciérralos para subirlo.', '[]'::jsonb, false),
    ('c71f2734-3019-56b9-a06d-3edfbf1efc36', 'Curl a una mano apoyado en banco inclinado', 'Standing One-Arm Dumbbell Curl Over Incline Bench', 'FREE_EXERCISE_DB', 'Standing_One-Arm_Dumbbell_Curl_Over_Incline_Bench', 'Bíceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie detrás de un banco inclinado con el pecho y el brazo apoyados.
2. Sube la mancuerna flexionando el codo.
3. Aprieta y baja despacio.', '[]'::jsonb, false),
    ('cb4c4d23-4d1d-5927-8d60-b7ab942b381a', 'Extensión de tríceps a una mano de pie', 'Standing One-Arm Dumbbell Triceps Extension', 'FREE_EXERCISE_DB', 'Standing_One-Arm_Dumbbell_Triceps_Extension', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie con una mancuerna arriba en una mano.
2. Bájala detrás de la cabeza con el codo cerca.
3. Extiende el codo y cambia de brazo.', '["ELBOW"]'::jsonb, false),
    ('3e15000d-56ce-5cf0-adff-ba00b39f1ae2', 'Extensión de tríceps sobre la cabeza con barra', 'Standing Overhead Barbell Triceps Extension', 'FREE_EXERCISE_DB', 'Standing_Overhead_Barbell_Triceps_Extension', 'Tríceps', '["Hombros"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie con la barra arriba y agarre cerrado.
2. Bájala detrás de la cabeza con los codos cerca.
3. Extiende los codos para volver.', '["SHOULDER", "ELBOW"]'::jsonb, false),
    ('ab54b757-300d-58b2-a2b9-d30704f8d4ad', 'Press a una mano con agarre neutro', 'Standing Palm-In One-Arm Dumbbell Press', 'FREE_EXERCISE_DB', 'Standing_Palm-In_One-Arm_Dumbbell_Press', 'Hombros', '["Tríceps"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie con una mancuerna a la altura del hombro, palma hacia adentro.
2. Empuja hasta extender el brazo.
3. Baja despacio y cambia de lado.', '[]'::jsonb, false),
    ('2d41f722-5b92-5056-9dfc-16a74bfd4eca', 'Press de hombros con agarre neutro', 'Standing Palms-In Dumbbell Press', 'FREE_EXERCISE_DB', 'Standing_Palms-In_Dumbbell_Press', 'Hombros', '["Tríceps"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie con las mancuernas a la altura de los hombros, palmas enfrentadas.
2. Empuja hasta extender los brazos.
3. Baja despacio.', '[]'::jsonb, false),
    ('28f46dc3-a119-5253-95fc-647c1b40a5a6', 'Curl de muñeca por detrás con barra', 'Standing Palms-Up Barbell Behind The Back Wrist Curl', 'FREE_EXERCISE_DB', 'Standing_Palms-Up_Barbell_Behind_The_Back_Wrist_Curl', 'Antebrazos', '[]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie con la barra detrás de los glúteos y los brazos extendidos.
2. Sube la barra solo con las muñecas.
3. Baja despacio.', '["WRIST"]'::jsonb, false),
    ('73213e19-b3f5-527d-a630-5eafafd15044', 'Báscula pélvica de pie', 'Standing Pelvic Tilt', 'FREE_EXERCISE_DB', 'Standing_Pelvic_Tilt', 'Lumbares', '["Glúteos"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. De pie con los pies al ancho de la cadera y las rodillas suaves.
2. Mueve la pelvis adelante y atrás varias veces.
3. Sostén con el coxis hacia adelante.', '[]'::jsonb, false),
    ('c3a32d0f-313c-5cf8-8122-0fc93c97dfae', 'Crunch de pie con cuerda', 'Standing Rope Crunch', 'FREE_EXERCISE_DB', 'Standing_Rope_Crunch', 'Abdomen', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De espaldas a la polea alta con la cuerda sobre los hombros.
2. Flexiona la columna bajando el torso.
3. Sostén y vuelve despacio.', '[]'::jsonb, false),
    ('ce9b5753-d0c7-5eda-962b-752bd2745df2', 'Estiramiento de sóleo y tendón de Aquiles', 'Standing Soleus And Achilles Stretch', 'FREE_EXERCISE_DB', 'Standing_Soleus_And_Achilles_Stretch', 'Pantorrillas', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. De pie con un pie un poco adelante.
2. Flexiona ambas rodillas sin despegar el talón de atrás.
3. Sostén y cambia de lado.', '[]'::jsonb, false),
    ('ad155c0a-e776-5e4a-a378-a52896a448b2', 'Tocar las puntas de pie', 'Standing Toe Touches', 'FREE_EXERCISE_DB', 'Standing_Toe_Touches', 'Isquiotibiales', '["Pantorrillas"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. De pie con las piernas rectas.
2. Inclínate desde la cintura y deja colgar el torso y los brazos.
3. Sostén 10 a 20 s.', '[]'::jsonb, false),
    ('df236b9d-c478-54be-b65e-423c9799a1d6', 'Extensión de tríceps con toalla', 'Standing Towel Triceps Extension', 'FREE_EXERCISE_DB', 'Standing_Towel_Triceps_Extension', 'Tríceps', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De pie con una toalla sobre la cabeza; un compañero sujeta el otro extremo.
2. Baja las manos detrás de la cabeza contra su resistencia.
3. Extiende los codos para volver.', '["ELBOW"]'::jsonb, false),
    ('1b0ae62b-b5f1-52af-a7f7-714a67867edc', 'Lanzamiento sobre la cabeza con balón medicinal', 'Standing Two-Arm Overhead Throw', 'FREE_EXERCISE_DB', 'Standing_Two-Arm_Overhead_Throw', 'Hombros', '["Pecho", "Espalda"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PULL', '1. De pie con el balón detrás de la cabeza.
2. Lánzalo al frente con fuerza flexionando la cadera.
3. Recíbelo de un compañero o al rebotar en la pared.', '["SHOULDER"]'::jsonb, false),
    ('67d248ad-2b6e-594a-8421-cb453432caa9', 'Salto estrella', 'Star Jump', 'FREE_EXERCISE_DB', 'Star_Jump', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Hombros"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De pie con los brazos junto al cuerpo.
2. Baja a media sentadilla y salta abriendo brazos y piernas.
3. Ciérralos al caer y amortigua.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('7c2a4706-9b5c-5953-bab0-59cd1c8752a4', 'Subida al cajón con rodilla arriba', 'Step-up with Knee Raise', 'FREE_EXERCISE_DB', 'Step-up_with_Knee_Raise', 'Glúteos', '["Isquiotibiales", "Cuádriceps"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie frente a un cajón.
2. Sube con un pie y eleva la otra rodilla lo más alto posible.
3. Baja y repite con la otra pierna.', '["KNEE"]'::jsonb, true),
    ('640ed95a-0f3b-51f4-84a9-43143170b0d4', 'Escaladora de escalones', 'Step Mill', 'FREE_EXERCISE_DB', 'Step_Mill', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'MACHINE', 'INTERMEDIATE', 'CARDIO', 'Cardio', NULL, NULL, '1. Sube a la máquina y elige un programa o el modo manual.
2. Sube los escalones con cuidado de no tropezar.
3. Sujétate de las barras para no caerte.', '[]'::jsonb, false),
    ('4388e5b7-9ccb-5a4a-b758-6554d6324135', 'Buenos días con piernas rígidas', 'Stiff Leg Barbell Good Morning', 'FREE_EXERCISE_DB', 'Stiff_Leg_Barbell_Good_Morning', 'Lumbares', '["Glúteos", "Isquiotibiales"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra sobre la espalda y las rodillas casi rectas.
2. Inclínate desde la cadera con la espalda recta.
3. Vuelve erguido.', '["LOWER_BACK"]'::jsonb, false),
    ('1b732be2-27c5-56e3-9b83-d6075d5a155d', 'Vacío abdominal', 'Stomach Vacuum', 'FREE_EXERCISE_DB', 'Stomach_Vacuum', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. De pie con las manos en la cadera.
2. Suelta todo el aire y mete el abdomen al máximo.
3. Sostén unos 20 s respirando suave.', '[]'::jsonb, false),
    ('65cd4f30-e0d0-5bbe-98a9-3d28882497ba', 'Pullover con mancuerna y brazos rectos', 'Straight-Arm Dumbbell Pullover', 'FREE_EXERCISE_DB', 'Straight-Arm_Dumbbell_Pullover', 'Pecho', '["Espalda", "Hombros", "Tríceps"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con los hombros apoyados de través en un banco y una mancuerna sobre el pecho.
2. Llévala en arco por detrás de la cabeza con los brazos casi rectos.
3. Vuelve por el mismo camino.', '["SHOULDER"]'::jsonb, false),
    ('18024422-e4c4-5ad6-b5f8-222c84569c08', 'Remo con barra de pie sobre banco', 'Straight Bar Bench Mid Rows', 'FREE_EXERCISE_DB', 'Straight_Bar_Bench_Mid_Rows', 'Espalda', '["Bíceps"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De pie sobre un banco con la barra al extremo y agarre prono.
2. Tira hacia el torso juntando los omóplatos.
3. Pausa y baja hasta extender los brazos.', '[]'::jsonb, false),
    ('5afdb52c-e354-5d30-aa95-736d54c3b0ba', 'Elevaciones frontales boca abajo con barra', 'Straight Raises on Incline Bench', 'FREE_EXERCISE_DB', 'Straight_Raises_on_Incline_Bench', 'Hombros', '["Trapecio"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Boca abajo en banco inclinado con la barra colgando.
2. Sube la barra al frente con los brazos rectos.
3. Baja despacio.', '[]'::jsonb, false),
    ('3ff2e178-69ed-5279-a99a-1d4ea4cf3d4a', 'Salto cruzado sobre cajón', 'Stride Jump Crossover', 'FREE_EXERCISE_DB', 'Stride_Jump_Crossover', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. De costado al cajón con el pie de adentro encima.
2. Salta lo más alto posible y cruza al otro lado.
3. Cae con el otro pie sobre el cajón y repite.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('85e93aec-1c7f-521b-ba6c-e3175c17351b', 'Peso muerto sumo con bandas', 'Sumo Deadlift with Bands', 'FREE_EXERCISE_DB', 'Sumo_Deadlift_with_Bands', 'Isquiotibiales', '["Aductores", "Antebrazos", "Glúteos", "Lumbares", "Espalda", "Cuádriceps", "Trapecio"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PULL', '1. Con bandas cortas sobre la barra y bajo los pies, pies muy abiertos.
2. Toma la barra por dentro de las piernas con la espalda recta.
3. Extiende cadera y rodillas hasta quedar erguido.', '["LOWER_BACK", "HIP"]'::jsonb, false),
    ('fbb41a2d-4f60-5e34-a7f5-c8af2033949c', 'Peso muerto sumo con cadenas', 'Sumo Deadlift with Chains', 'FREE_EXERCISE_DB', 'Sumo_Deadlift_with_Chains', 'Isquiotibiales', '["Abductores", "Aductores", "Antebrazos", "Glúteos", "Lumbares", "Espalda", "Cuádriceps", "Trapecio"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'POWERLIFTING', 'Fuerza', 'COMPOUND', 'PULL', '1. Con cadenas sobre la barra y los pies muy abiertos.
2. Toma la barra por dentro de las piernas con la espalda recta.
3. Extiende cadera y rodillas hasta quedar erguido.', '["LOWER_BACK", "HIP"]'::jsonb, false),
    ('32a8b036-ea7a-55ad-9a54-e31f6914756a', 'Superman', 'Superman', 'FREE_EXERCISE_DB', 'Superman', 'Lumbares', '["Glúteos", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'COMPOUND', 'STATIC', '1. Boca abajo con los brazos extendidos al frente.
2. Sube brazos, piernas y pecho a la vez y sostén 2 s.
3. Baja despacio.', '[]'::jsonb, true),
    ('13ea228d-b315-5480-b601-694163e6bc54', 'Lanzamiento de pecho acostado con balón medicinal', 'Supine Chest Throw', 'FREE_EXERCISE_DB', 'Supine_Chest_Throw', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PUSH', '1. Boca arriba con las rodillas flexionadas y el balón en el pecho.
2. Lánzalo hacia arriba lo más alto posible extendiendo los codos.
3. Atrápalo con ambas manos.', '[]'::jsonb, false),
    ('1a5ddf84-50d0-5be7-8f8b-4bdc02278ef9', 'Lanzamiento acostado a una mano con balón medicinal', 'Supine One-Arm Overhead Throw', 'FREE_EXERCISE_DB', 'Supine_One-Arm_Overhead_Throw', 'Abdomen', '["Pecho", "Espalda", "Hombros"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PULL', '1. Boca arriba con el balón en una mano detrás de la cabeza.
2. Siéntate lanzándolo al frente lo más lejos posible.
3. Recíbelo y cambia de mano.', '["SHOULDER"]'::jsonb, false),
    ('f893872b-f40e-5f37-8b72-455f5d989f4a', 'Lanzamiento acostado con balón medicinal', 'Supine Two-Arm Overhead Throw', 'FREE_EXERCISE_DB', 'Supine_Two-Arm_Overhead_Throw', 'Abdomen', '["Pecho", "Espalda", "Hombros"]'::jsonb, 'OTHER', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PULL', '1. Boca arriba con el balón en ambas manos detrás de la cabeza.
2. Siéntate lanzándolo al frente lo más lejos posible.
3. Recíbelo de un compañero o la pared.', '["SHOULDER"]'::jsonb, false),
    ('6248a68f-844a-5529-9dcc-fa27506be61f', 'Rollout en correas de suspensión', 'Suspended Fallout', 'FREE_EXERCISE_DB', 'Suspended_Fallout', 'Abdomen', '["Pecho", "Lumbares", "Hombros"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Inclinado sobre las correas en posición de flexión inclinada.
2. Lleva los brazos rectos sobre la cabeza acercando el cuerpo al suelo.
3. Vuelve con el abdomen firme.', '[]'::jsonb, false),
    ('820c3fc0-aaf8-519c-a273-a51cb21d9030', 'Flexiones en correas de suspensión', 'Suspended Push-Up', 'FREE_EXERCISE_DB', 'Suspended_Push-Up', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Inclinado sobre las correas con un agarre en cada mano, brazos extendidos.
2. Baja con el cuerpo rígido hasta pasar los 90° en los codos.
3. Pausa y empuja hasta extender.', '["WRIST"]'::jsonb, false),
    ('ac2c3189-c289-5fa4-b8e0-3940222328ca', 'Crunch invertido en correas de suspensión', 'Suspended Reverse Crunch', 'FREE_EXERCISE_DB', 'Suspended_Reverse_Crunch', 'Abdomen', '[]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. En plancha con los pies en las correas.
2. Lleva las rodillas al pecho flexionando la columna.
3. Vuelve con control sin hundir la cadera.', '[]'::jsonb, false),
    ('d44d3015-36aa-583c-b415-587b8565c6fb', 'Remo en correas de suspensión', 'Suspended Row', 'FREE_EXERCISE_DB', 'Suspended_Row', 'Espalda', '["Bíceps"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Inclinado hacia atrás con un agarre en cada mano y el cuerpo recto.
2. Tira del pecho hacia las manos juntando los omóplatos.
3. Pausa y vuelve despacio.', '[]'::jsonb, false),
    ('60e82759-69b8-530c-a0a6-e84ade5c51df', 'Sentadilla búlgara en correas de suspensión', 'Suspended Split Squat', 'FREE_EXERCISE_DB', 'Suspended_Split_Squat', 'Cuádriceps', '["Abductores", "Aductores", "Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De espaldas a las correas con el pie de atrás en el agarre.
2. Baja flexionando la rodilla delantera con el peso en el talón.
3. Sube extendiendo cadera y rodilla y cambia de pierna.', '["KNEE", "HIP", "ANKLE"]'::jsonb, false),
    ('1d4276c9-7201-5de6-8f3b-7b4cad00b3f6', 'Press Svend', 'Svend Press', 'FREE_EXERCISE_DB', 'Svend_Press', 'Pecho', '["Antebrazos", "Hombros", "Tríceps"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie, aprieta dos discos ligeros entre las palmas frente al pecho.
2. Extiende los brazos al frente sin dejar de apretar.
3. Pausa y vuelve al pecho.', '[]'::jsonb, false),
    ('48f21909-5d52-582e-987a-b5e6c63aa61a', 'Remo en T con agarre', 'T-Bar Row with Handle', 'FREE_EXERCISE_DB', 'T-Bar_Row_with_Handle', 'Espalda', '["Bíceps"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con la barra anclada y un agarre en V bajo el extremo, inclinado con la cadera atrás.
2. Tira hacia el abdomen alto juntando los omóplatos.
3. Baja despacio sin tirones.', '["LOWER_BACK"]'::jsonb, false),
    ('379d97b4-e9d5-5555-8a41-dfe6fef1a829', 'Press Tate', 'Tate Press', 'FREE_EXERCISE_DB', 'Tate_Press', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Acostado en banco plano con las mancuernas arriba y los codos hacia afuera.
2. Baja las mancuernas hacia el pecho flexionando solo los codos.
3. Extiende los codos para volver.', '["ELBOW"]'::jsonb, false),
    ('9d76dcb6-69b1-56ed-8666-3a6047440030', 'Estiramiento en V sentado', 'The Straddle', 'FREE_EXERCISE_DB', 'The_Straddle', 'Isquiotibiales', '["Aductores", "Pantorrillas"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado con las piernas abiertas en V.
2. Inclínate al frente con las manos en el suelo.
3. Sostén 10 a 20 s.', '[]'::jsonb, false),
    ('846fbe5e-18cd-5d2e-930d-b18d177ac7cb', 'Aductores en máquina', 'Thigh Adductor', 'FREE_EXERCISE_DB', 'Thigh_Adductor', 'Aductores', '["Glúteos", "Isquiotibiales"]'::jsonb, 'MACHINE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Sentado en la máquina con las piernas contra los apoyos.
2. Junta las piernas apretando los aductores.
3. Pausa y abre despacio.', '["HIP"]'::jsonb, true),
    ('e4717b15-95d7-5f6f-a5c9-cbe83f5c56a6', 'Volteo de neumático', 'Tire Flip', 'FREE_EXERCISE_DB', 'Tire_Flip', 'Cuádriceps', '["Pantorrillas", "Pecho", "Antebrazos", "Glúteos", "Isquiotibiales", "Lumbares", "Hombros", "Trapecio", "Tríceps"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRONGMAN', 'Strongman', 'COMPOUND', 'PULL', '1. Toma el neumático por abajo con el pecho contra él.
2. Extiende cadera, rodillas y tobillos levantándolo.
3. A 45°, empuja con la rodilla, cambia el agarre y vuélcalo.', '["LOWER_BACK"]'::jsonb, false),
    ('b70a49fb-2d09-5b25-a538-d2f2771ef12e', 'Toques de puntas acostado', 'Toe Touchers', 'FREE_EXERCISE_DB', 'Toe_Touchers', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'PULL', '1. Boca arriba con las piernas arriba casi verticales.
2. Sube el torso intentando tocar las puntas con las manos.
3. Baja despacio sin despegar la zona lumbar.', '[]'::jsonb, false),
    ('d4f168c9-30aa-584c-be9a-4effe7599231', 'Rotación de torso con fitball', 'Torso Rotation', 'FREE_EXERCISE_DB', 'Torso_Rotation', 'Abdomen', '[]'::jsonb, 'OTHER', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'PULL', '1. De pie con un fitball al frente y los brazos extendidos.
2. Gira el torso a un lado siguiendo el balón con la vista.
3. Gira al otro lado; 10 a 20 veces.', '[]'::jsonb, false),
    ('c1fc0765-0ebb-5976-b551-6200320e1152', 'Trail running o senderismo', 'Trail Running/Walking', 'FREE_EXERCISE_DB', 'Trail_Running_Walking', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'CARDIO', 'Cardio', NULL, NULL, '1. Usa calzado adecuado para el terreno.
2. Sube con pasos firmes y cortos.
3. En bajada acorta el paso y flexiona las rodillas para amortiguar.', '[]'::jsonb, false),
    ('723484d7-4ec3-5566-979d-90a89528b95f', 'Peso muerto con barra hexagonal', 'Trap Bar Deadlift', 'FREE_EXERCISE_DB', 'Trap_Bar_Deadlift', 'Cuádriceps', '["Glúteos", "Isquiotibiales"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De pie dentro de la barra, toma los agarres con la cadera baja y el pecho arriba.
2. Empuja con los talones extendiendo cadera y rodillas sin redondear la espalda.
3. Bájala con control.', '["LOWER_BACK"]'::jsonb, true),
    ('476b0864-1c4a-56ee-8c37-49308ec0710c', 'Patada de tríceps con mancuernas', 'Tricep Dumbbell Kickback', 'FREE_EXERCISE_DB', 'Tricep_Dumbbell_Kickback', 'Tríceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Inclinado casi paralelo al suelo con los brazos pegados al torso y los codos a 90°.
2. Extiende los codos hacia atrás sin mover los brazos.
3. Vuelve despacio.', '[]'::jsonb, false),
    ('d00101a4-0595-5a09-8191-41969a9d3bfa', 'Estiramiento de tríceps cruzado', 'Tricep Side Stretch', 'FREE_EXERCISE_DB', 'Tricep_Side_Stretch', 'Tríceps', '["Hombros"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Lleva un brazo por encima del hombro contrario.
2. Sujeta el codo con la otra mano.
3. Sostén y cambia de brazo.', '[]'::jsonb, false),
    ('d71f71ad-1017-582d-94e7-3f411f2bd944', 'Extensión de tríceps sobre la cabeza con cuerda', 'Triceps Overhead Extension with Rope', 'FREE_EXERCISE_DB', 'Triceps_Overhead_Extension_with_Rope', 'Tríceps', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. De espaldas a la polea baja con la cuerda detrás de la cabeza y los codos arriba.
2. Extiende los codos sin mover los brazos.
3. Aprieta y vuelve despacio.', '["SHOULDER"]'::jsonb, false),
    ('45f499ae-472a-5ca5-a19c-756b190d5de0', 'Extensión de tríceps en polea con barra en V', 'Triceps Pushdown - V-Bar Attachment', 'FREE_EXERCISE_DB', 'Triceps_Pushdown_-_V-Bar_Attachment', 'Tríceps', '[]'::jsonb, 'CABLE', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PUSH', '1. Frente a la polea alta con la barra en V y los codos pegados al cuerpo.
2. Baja la barra hasta extender los brazos.
3. Vuelve despacio sin mover los brazos.', '[]'::jsonb, false),
    ('e60bd2de-bd7b-53ea-beab-d26948c42069', 'Estiramiento de tríceps', 'Triceps Stretch', 'FREE_EXERCISE_DB', 'Triceps_Stretch', 'Tríceps', '["Espalda"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'STATIC', '1. Lleva una mano detrás de la cabeza.
2. Sujeta el codo con la otra mano y tira suave.
3. Sostén 10 a 20 s y cambia de lado.', '[]'::jsonb, false),
    ('97281fb6-796b-5071-9e9c-2822bc94e688', 'Crunch con piernas recogidas', 'Tuck Crunch', 'FREE_EXERCISE_DB', 'Tuck_Crunch', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca arriba con los muslos verticales, los tobillos cruzados y los brazos sobre el pecho.
2. Sube los hombros contrayendo el abdomen.
3. Baja despacio.', '[]'::jsonb, false),
    ('975af653-621b-5131-aaee-f7c1b0f6ac62', 'Curl predicador con mancuernas', 'Two-Arm Dumbbell Preacher Curl', 'FREE_EXERCISE_DB', 'Two-Arm_Dumbbell_Preacher_Curl', 'Bíceps', '[]'::jsonb, 'DUMBBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Con los brazos sobre el banco predicador y una mancuerna en cada mano.
2. Baja hasta extender los brazos.
3. Sube apretando el bíceps.', '[]'::jsonb, false),
    ('359cc79d-e382-565e-8dbd-31f382230657', 'Cargada con dos kettlebells', 'Two-Arm Kettlebell Clean', 'FREE_EXERCISE_DB', 'Two-Arm_Kettlebell_Clean', 'Hombros', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares", "Trapecio"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Con las kettlebells entre los pies y la cadera atrás.
2. Extiende piernas y cadera llevándolas a los hombros, girando las muñecas.
3. Bájalas y repite.', '["LOWER_BACK", "WRIST"]'::jsonb, false),
    ('34e8fef5-115c-52b5-a4b6-666642a6b6c1', 'Envión doble con kettlebells', 'Two-Arm Kettlebell Jerk', 'FREE_EXERCISE_DB', 'Two-Arm_Kettlebell_Jerk', 'Hombros', '["Pantorrillas", "Cuádriceps", "Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con las kettlebells en los hombros, palmas al frente.
2. Flexiona un poco e impúlsalas sobre la cabeza.
3. Métete debajo y ponte de pie con los brazos bloqueados.', '["SHOULDER"]'::jsonb, false),
    ('da3e6403-d239-5426-ae5e-b67f1cd66ff9', 'Press militar con dos kettlebells', 'Two-Arm Kettlebell Military Press', 'FREE_EXERCISE_DB', 'Two-Arm_Kettlebell_Military_Press', 'Hombros', '["Tríceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con las kettlebells en los hombros, palmas al frente.
2. Empújalas arriba y un poco afuera hasta bloquear los codos.
3. Bájalas con control apretando glúteos y abdomen.', '["SHOULDER"]'::jsonb, true),
    ('58e96b95-c18f-51aa-b97e-748d325f5eba', 'Remo con dos kettlebells', 'Two-Arm Kettlebell Row', 'FREE_EXERCISE_DB', 'Two-Arm_Kettlebell_Row', 'Espalda', '["Bíceps"]'::jsonb, 'KETTLEBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Inclinado con la cadera atrás y las kettlebells frente a los pies.
2. Tira de ambas hacia el abdomen juntando los omóplatos.
3. Bájalas con la espalda recta.', '[]'::jsonb, true),
    ('77c570c8-ee14-5411-9053-f69367520b59', 'Estiramiento abrazando las piernas', 'Upper Back-Leg Grab', 'FREE_EXERCISE_DB', 'Upper_Back-Leg_Grab', 'Isquiotibiales', '["Lumbares", "Espalda"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Sentado con las piernas extendidas y juntas.
2. Inclínate y abraza los muslos por debajo llevando el pecho a las rodillas.
3. Tira la espalda hacia atrás para estirar la espalda media.', '[]'::jsonb, false),
    ('daa5176a-96bd-5a20-8d86-2017e43d79ab', 'Estiramiento de espalda alta', 'Upper Back Stretch', 'FREE_EXERCISE_DB', 'Upper_Back_Stretch', 'Espalda', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Entrelaza los dedos con los pulgares hacia abajo.
2. Redondea los hombros llevando las manos al frente.
3. Sostén.', '[]'::jsonb, false),
    ('194a1274-9a1f-5032-9f90-6d52c9f846d4', 'Remo al mentón con barra', 'Upright Barbell Row', 'FREE_EXERCISE_DB', 'Upright_Barbell_Row', 'Hombros', '["Trapecio"]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De pie con la barra frente a los muslos, agarre prono algo cerrado.
2. Súbela pegada al cuerpo llevando los codos arriba.
3. Pausa cerca del mentón y baja despacio.', '["LOWER_BACK", "SHOULDER"]'::jsonb, true),
    ('d798e672-4773-5428-b224-19ccb3f7b862', 'Remo al mentón en polea', 'Upright Cable Row', 'FREE_EXERCISE_DB', 'Upright_Cable_Row', 'Trapecio', '["Hombros"]'::jsonb, 'CABLE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Frente a la polea baja con la barra en agarre prono algo cerrado.
2. Súbela pegada al cuerpo llevando los codos arriba.
3. Pausa cerca del mentón y baja despacio.', '["SHOULDER"]'::jsonb, false),
    ('d7c20f93-ffcb-5e6d-8d90-d97a945d5389', 'Remo al mentón con banda', 'Upright Row - With Bands', 'FREE_EXERCISE_DB', 'Upright_Row_-_With_Bands', 'Trapecio', '["Hombros"]'::jsonb, 'BAND', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. De pie sobre la banda con los agarres frente a los muslos.
2. Súbelos pegados al cuerpo llevando los codos arriba.
3. Pausa cerca del mentón y baja despacio.', '["SHOULDER"]'::jsonb, true),
    ('c202d92d-c876-5568-a99d-0dee9f65d6a6', 'Estiramiento hacia arriba', 'Upward Stretch', 'FREE_EXERCISE_DB', 'Upward_Stretch', 'Hombros', '["Pecho", "Espalda"]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. Estira los brazos sobre la cabeza con las palmas juntas.
2. Empuja las manos arriba y un poco atrás.
3. Mantén la espalda recta y sostén.', '[]'::jsonb, false),
    ('dd6e27ec-c002-5a45-a182-105b205d2f94', 'Jalón con barra en V', 'V-Bar Pulldown', 'FREE_EXERCISE_DB', 'V-Bar_Pulldown', 'Espalda', '["Bíceps", "Hombros"]'::jsonb, 'CABLE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Sentado en el jalón con la barra en V y agarre neutro, pecho afuera.
2. Tira hacia el pecho juntando los omóplatos.
3. Vuelve despacio.', '[]'::jsonb, false),
    ('25a55938-2309-514d-ab0e-c06367895367', 'Swing vertical con mancuerna', 'Vertical Swing', 'FREE_EXERCISE_DB', 'Vertical_Swing', 'Isquiotibiales', '["Glúteos", "Cuádriceps", "Hombros"]'::jsonb, 'DUMBBELL', 'BEGINNER', 'PLYOMETRICS', 'Pliometría', 'COMPOUND', 'PULL', '1. Con una mancuerna en ambas manos colgando entre las piernas.
2. Balancéala atrás y extiende cadera, rodillas y tobillos saltando.
3. Llévala sobre la cabeza, cae amortiguando y repite.', '["LOWER_BACK"]'::jsonb, false),
    ('e872555a-fa97-5032-a96e-d2d95934a9e8', 'Caminar en cinta', 'Walking, Treadmill', 'FREE_EXERCISE_DB', 'Walking_Treadmill', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'MACHINE', 'BEGINNER', 'CARDIO', 'Cardio', NULL, NULL, '1. Sube a la cinta y elige un programa o el modo manual.
2. Camina a paso moderado o rápido.
3. Ajusta la inclinación para cambiar la intensidad.', '[]'::jsonb, false),
    ('7151a00f-5e15-51a2-8e89-d6d6498870c8', 'Hiperextensión en fitball con peso', 'Weighted Ball Hyperextension', 'FREE_EXERCISE_DB', 'Weighted_Ball_Hyperextension', 'Lumbares', '["Glúteos", "Isquiotibiales", "Espalda"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Boca abajo sobre el fitball con un disco en el pecho o la nuca.
2. Sube el torso usando la zona lumbar.
3. Sostén un segundo y baja despacio.', '["LOWER_BACK"]'::jsonb, false),
    ('7456372b-d9c5-53fa-a4ea-83343ea3b564', 'Inclinación lateral en fitball con peso', 'Weighted Ball Side Bend', 'FREE_EXERCISE_DB', 'Weighted_Ball_Side_Bend', 'Abdomen', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De costado sobre el fitball con un disco junto a la cabeza.
2. Sube el torso de lado contrayendo el oblicuo.
3. Baja despacio y cambia de lado.', '[]'::jsonb, false),
    ('17497b83-7891-56c3-9e54-de2b9c366e1e', 'Fondos entre bancos con peso', 'Weighted Bench Dip', 'FREE_EXERCISE_DB', 'Weighted_Bench_Dip', 'Tríceps', '["Pecho", "Hombros"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con las manos en un banco detrás, los pies en otro y un disco sobre los muslos.
2. Baja flexionando los codos.
3. Empuja hasta extender los brazos.', '["SHOULDER", "ELBOW"]'::jsonb, false),
    ('fc17b5ea-916e-5190-9ef1-a17f41836556', 'Crunch con peso', 'Weighted Crunches', 'FREE_EXERCISE_DB', 'Weighted_Crunches', 'Abdomen', '[]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Boca arriba con las rodillas flexionadas y un balón medicinal en el pecho.
2. Despega los hombros del suelo contrayendo el abdomen.
3. Pausa y baja despacio.', '[]'::jsonb, false),
    ('e63d311d-8d0a-59aa-96d5-e8d6d300f928', 'Sentadilla con salto con peso', 'Weighted Jump Squat', 'FREE_EXERCISE_DB', 'Weighted_Jump_Squat', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con una barra ligera sobre la espalda.
2. Baja en media sentadilla y salta con fuerza.
3. Cae amortiguando y repite.', '["KNEE", "ANKLE"]'::jsonb, false),
    ('6b4a09ac-eafa-5d15-959c-074fa166ed6c', 'Sentadilla sissy con disco', 'Weighted Sissy Squat', 'FREE_EXERCISE_DB', 'Weighted_Sissy_Squat', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BARBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie en puntas, sujeto a un rack con una mano y un disco en el pecho.
2. Lleva rodillas y pelvis al frente inclinando el torso atrás.
3. Baja hasta casi 90° en las rodillas y sube.', '["KNEE"]'::jsonb, false),
    ('fafb1011-02b4-5af8-8ea5-6e2b92ac8c32', 'Abdominal declinado con bandas', 'Weighted Sit-Ups - With Bands', 'FREE_EXERCISE_DB', 'Weighted_Sit-Ups_-_With_Bands', 'Abdomen', '[]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. En banco declinado con bandas ancladas a la base y los agarres junto a la clavícula.
2. Sube el torso contra la tensión.
3. Baja despacio.', '[]'::jsonb, false),
    ('b13d2317-7c37-5127-9d6e-372a0683f3a6', 'Sentadilla con cinturón de peso', 'Weighted Squat', 'FREE_EXERCISE_DB', 'Weighted_Squat', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. De pie sobre dos bancos con un peso colgando del cinturón.
2. Baja con la espalda recta hasta la paralela.
3. Sube empujando con los talones.', '["KNEE"]'::jsonb, false),
    ('fd3b8b10-dd8a-57f6-bf66-133efa78e845', 'Press de banca agarre ancho', 'Wide-Grip Barbell Bench Press', 'FREE_EXERCISE_DB', 'Wide-Grip_Barbell_Bench_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Acostado en banco plano con agarre más ancho que los hombros.
2. Baja la barra al pecho medio.
3. Pausa y empuja hasta extender los brazos.', '["SHOULDER"]'::jsonb, false),
    ('72e50f8f-41c7-5738-8da3-ca902864b7a3', 'Press declinado agarre ancho', 'Wide-Grip Decline Barbell Bench Press', 'FREE_EXERCISE_DB', 'Wide-Grip_Decline_Barbell_Bench_Press', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. En banco declinado con los pies sujetos y agarre ancho.
2. Baja la barra al pecho bajo.
3. Pausa y empuja hasta extender los brazos.', '["SHOULDER"]'::jsonb, false),
    ('3acd8c1a-7d46-543c-b1ec-02aa27d2b975', 'Pullover declinado con barra', 'Wide-Grip Decline Barbell Pullover', 'FREE_EXERCISE_DB', 'Wide-Grip_Decline_Barbell_Pullover', 'Pecho', '["Hombros", "Tríceps"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. En banco declinado con la barra arriba y agarre ancho.
2. Llévala en arco por detrás de la cabeza.
3. Vuelve por el mismo camino.', '["SHOULDER"]'::jsonb, false),
    ('a740d376-b236-5015-900b-14f05acb055a', 'Jalón tras nuca agarre ancho', 'Wide-Grip Pulldown Behind The Neck', 'FREE_EXERCISE_DB', 'Wide-Grip_Pulldown_Behind_The_Neck', 'Espalda', '["Bíceps", "Hombros"]'::jsonb, 'CABLE', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Sentado en el jalón con agarre ancho.
2. Tira la barra hasta detrás de la nuca.
3. Vuelve despacio.', '["SHOULDER", "NECK"]'::jsonb, false),
    ('51fda5ab-ce14-5009-b1fb-4c1ec6e80616', 'Dominadas tras nuca agarre ancho', 'Wide-Grip Rear Pull-Up', 'FREE_EXERCISE_DB', 'Wide-Grip_Rear_Pull-Up', 'Espalda', '["Bíceps", "Hombros"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Cuelga con agarre ancho y la cabeza algo adelantada.
2. Sube hasta que la barra quede cerca de la nuca.
3. Baja despacio.', '[]'::jsonb, false),
    ('ba641524-52ed-5a15-a6b1-a6ef749a992f', 'Curl con barra agarre ancho', 'Wide-Grip Standing Barbell Curl', 'FREE_EXERCISE_DB', 'Wide-Grip_Standing_Barbell_Curl', 'Bíceps', '[]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie con la barra en agarre supino más ancho que los hombros.
2. Sube la barra flexionando solo los codos.
3. Aprieta y baja despacio.', '["ELBOW", "WRIST"]'::jsonb, false),
    ('c00a6d59-676a-52d7-bf91-ab6bdb535c8b', 'Sentadilla con barra pies abiertos', 'Wide Stance Barbell Squat', 'FREE_EXERCISE_DB', 'Wide_Stance_Barbell_Squat', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra sobre la espalda y los pies más abiertos que los hombros.
2. Baja hasta que los muslos queden paralelos.
3. Sube empujando con los talones.', '["KNEE", "LOWER_BACK"]'::jsonb, false),
    ('3ddedb69-19d0-5336-91c8-d1904a473821', 'Peso muerto piernas rígidas pies abiertos', 'Wide Stance Stiff Legs', 'FREE_EXERCISE_DB', 'Wide_Stance_Stiff_Legs', 'Isquiotibiales', '["Aductores", "Glúteos", "Lumbares"]'::jsonb, 'BARBELL', 'INTERMEDIATE', 'OLYMPIC', 'Halterofilia', 'COMPOUND', 'PULL', '1. Con postura amplia, toma la barra con la cadera atrás y las piernas casi rectas.
2. Lleva la cadera al frente hasta quedar erguido.
3. Baja despacio.', '["LOWER_BACK"]'::jsonb, false),
    ('41cc5c43-9ed1-5803-9c4e-4422587102e3', 'Rodillas alternas colgado', 'Wind Sprints', 'FREE_EXERCISE_DB', 'Wind_Sprints', 'Abdomen', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PULL', '1. Cuelga de la barra con brazos y piernas extendidos.
2. Sube una rodilla lo más alto posible sin balancearte.
3. Bájala mientras subes la otra; alterna.', '["ANKLE"]'::jsonb, false),
    ('73b2ccbd-5286-5973-a80a-7714b5acc882', 'Molinos acostado', 'Windmills', 'FREE_EXERCISE_DB', 'Windmills', 'Abductores', '["Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'PULL', '1. Boca arriba con los brazos abiertos y las piernas rectas.
2. Cruza una pierna sobre el cuerpo hacia la mano contraria.
3. Vuelve y alterna 10 a 20 veces.', '[]'::jsonb, false),
    ('0e67392b-80f7-55b9-94c5-d1ca02ada1da', 'El mejor estiramiento del mundo', 'World''s Greatest Stretch', 'FREE_EXERCISE_DB', 'Worlds_Greatest_Stretch', 'Isquiotibiales', '["Pantorrillas", "Glúteos", "Cuádriceps"]'::jsonb, 'BODYWEIGHT', 'INTERMEDIATE', 'STRETCHING', 'Movilidad', NULL, 'STATIC', '1. En zancada profunda con el torso erguido; sostén 10 a 20 s.
2. Apoya el codo junto al pie delantero y la otra mano en el suelo.
3. Gira abriendo el brazo hacia el techo y cambia de lado.', '[]'::jsonb, false),
    ('a7f955ee-802c-5dbe-bd89-e22d928ce5a4', 'Círculos de muñecas', 'Wrist Circles', 'FREE_EXERCISE_DB', 'Wrist_Circles', 'Antebrazos', '[]'::jsonb, 'BODYWEIGHT', 'BEGINNER', 'STRETCHING', 'Movilidad', 'ISOLATION', 'PULL', '1. De pie con los brazos abiertos en T, palmas abajo.
2. Gira las muñecas en círculos hacia adelante.
3. Cambia de dirección.', '[]'::jsonb, false),
    ('c8511621-7df2-56ca-9a2b-278441446bfc', 'Enrollador de muñecas', 'Wrist Roller', 'FREE_EXERCISE_DB', 'Wrist_Roller', 'Antebrazos', '["Hombros"]'::jsonb, 'OTHER', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie con el rodillo en agarre prono y los brazos extendidos al frente.
2. Gira una muñeca a la vez para enrollar la cuerda y subir el peso.
3. Desenróllala despacio para bajarlo.', '["WRIST"]'::jsonb, true),
    ('3593b2f6-0947-59c2-a2af-40a6175a6031', 'Rotaciones de muñeca con barra', 'Wrist Rotations with Straight Bar', 'FREE_EXERCISE_DB', 'Wrist_Rotations_with_Straight_Bar', 'Antebrazos', '[]'::jsonb, 'BARBELL', 'BEGINNER', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Con la barra en agarre prono al ancho de los hombros.
2. Extiende las muñecas alternando, como enrollando un periódico.
3. Invierte el sentido hasta el fallo.', '[]'::jsonb, false),
    ('be6359fd-f29d-5516-b842-ee8bdc234157', 'Caminata con yugo', 'Yoke Walk', 'FREE_EXERCISE_DB', 'Yoke_Walk', 'Cuádriceps', '["Abdomen", "Abductores", "Aductores", "Pantorrillas", "Glúteos", "Isquiotibiales", "Lumbares"]'::jsonb, 'OTHER', 'INTERMEDIATE', 'STRONGMAN', 'Strongman', 'COMPOUND', NULL, '1. Con el yugo apoyado sobre la espalda.
2. Levántalo empujando con los talones, espalda arqueada.
3. Camina lo más rápido posible con pasos cortos.', '["LOWER_BACK"]'::jsonb, false),
    ('da8f1f7a-a715-5111-9d56-c94b9497c9e6', 'Sentadilla Zercher', 'Zercher Squats', 'FREE_EXERCISE_DB', 'Zercher_Squats', 'Cuádriceps', '["Pantorrillas", "Glúteos", "Isquiotibiales"]'::jsonb, 'BARBELL', 'ADVANCED', 'STRENGTH', 'Hipertrofia', 'COMPOUND', 'PUSH', '1. Con la barra apoyada en el pliegue de los codos y las manos juntas.
2. Baja en sentadilla con el torso erguido.
3. Sube empujando con los talones.', '["KNEE", "LOWER_BACK"]'::jsonb, false),
    ('8992c00f-c9ea-5473-a146-9b998cf85bb6', 'Curl Zottman', 'Zottman Curl', 'FREE_EXERCISE_DB', 'Zottman_Curl', 'Bíceps', '["Antebrazos"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. De pie con las mancuernas a los costados, palmas enfrentadas.
2. Sube girando las palmas hacia arriba.
3. Arriba gira las palmas hacia abajo y baja despacio.', '[]'::jsonb, true),
    ('f4b78cfa-fafc-56af-a2ee-f551ac45fac5', 'Curl Zottman en banco predicador', 'Zottman Preacher Curl', 'FREE_EXERCISE_DB', 'Zottman_Preacher_Curl', 'Bíceps', '["Antebrazos"]'::jsonb, 'DUMBBELL', 'INTERMEDIATE', 'STRENGTH', 'Hipertrofia', 'ISOLATION', 'PULL', '1. Con los brazos sobre el banco predicador y las mancuernas arriba, palmas abajo.
2. Baja despacio con las palmas abajo.
3. Abajo gira las palmas hacia arriba y sube.', '[]'::jsonb, false)
ON CONFLICT DO NOTHING;
