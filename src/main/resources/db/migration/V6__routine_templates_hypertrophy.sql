ALTER TABLE routines
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE routines
    ADD COLUMN IF NOT EXISTS template_key varchar(120),
    ADD COLUMN IF NOT EXISTS routine_split varchar(80);

CREATE UNIQUE INDEX IF NOT EXISTS idx_routines_template_key
    ON routines(template_key)
    WHERE template_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_routines_source_objective_split
    ON routines(source, objective, routine_split, difficulty, days_per_week);

CREATE TEMP TABLE tmp_template_catalog_exercises (
    name varchar(180) NOT NULL,
    category varchar(80) NOT NULL,
    primary_muscle_group varchar(120) NOT NULL,
    equipment varchar(120) NOT NULL,
    difficulty varchar(80) NOT NULL,
    instructions text NOT NULL
) ON COMMIT DROP;

INSERT INTO tmp_template_catalog_exercises (name, category, primary_muscle_group, equipment, difficulty, instructions)
VALUES
    ('Peso muerto rumano con mancuernas', 'Hipertrofia', 'Isquiotibiales', 'DUMBBELL', 'BEGINNER', 'Desplaza la cadera hacia atras, baja las mancuernas cerca de las piernas y vuelve apretando gluteos e isquiotibiales.'),
    ('Sentadilla guiada', 'Hipertrofia', 'Cuadriceps', 'SMITH_MACHINE', 'BEGINNER', 'Mantiene el torso estable bajo la guia, baja con control y empuja desde el pie completo sin perder alineacion.'),
    ('Encogimientos con mancuernas', 'Hipertrofia', 'Hombros', 'DUMBBELL', 'BEGINNER', 'Eleva los hombros hacia arriba con recorrido corto y pausa un instante en la contraccion antes de bajar.'),
    ('Press de banca cerrado', 'Hipertrofia', 'Triceps', 'BARBELL', 'INTERMEDIATE', 'Usa un agarre mas cerrado que el press plano, baja controlando al pecho y extiende enfocando el triceps.');

UPDATE exercises AS target
SET category = src.category,
    primary_muscle_group = src.primary_muscle_group,
    equipment = src.equipment,
    difficulty = src.difficulty,
    instructions = src.instructions,
    is_active = true,
    updated_at = now()
FROM tmp_template_catalog_exercises AS src
WHERE lower(target.name) = lower(src.name)
  AND (
      target.category IS DISTINCT FROM src.category
      OR target.primary_muscle_group IS DISTINCT FROM src.primary_muscle_group
      OR target.equipment IS DISTINCT FROM src.equipment
      OR target.difficulty IS DISTINCT FROM src.difficulty
      OR target.instructions IS DISTINCT FROM src.instructions
      OR target.is_active IS DISTINCT FROM true
  );

INSERT INTO exercises (
    name,
    description,
    category,
    primary_muscle_group,
    secondary_muscle_groups,
    equipment,
    difficulty,
    instructions,
    is_active
)
SELECT
    src.name,
    NULL,
    src.category,
    src.primary_muscle_group,
    '[]'::jsonb,
    src.equipment,
    src.difficulty,
    src.instructions,
    true
FROM tmp_template_catalog_exercises AS src
WHERE NOT EXISTS (
    SELECT 1
    FROM exercises e
    WHERE lower(e.name) = lower(src.name)
);

CREATE TEMP TABLE tmp_routine_templates (
    template_key varchar(120) NOT NULL,
    title varchar(180) NOT NULL,
    description text NOT NULL,
    objective varchar(80) NOT NULL,
    difficulty varchar(40) NOT NULL,
    estimated_duration_minutes smallint NOT NULL,
    days_per_week smallint NOT NULL,
    routine_split varchar(80) NOT NULL
) ON COMMIT DROP;

INSERT INTO tmp_routine_templates (template_key, title, description, objective, difficulty, estimated_duration_minutes, days_per_week, routine_split)
VALUES
    ('FULL_BODY_BEGINNER_3_DAYS', 'Full Body Beginner - 3 dias', 'Plantilla global de hipertrofia para gimnasio con tres sesiones full body de bajo volumen y tecnica prioritaria.', 'HYPERTROPHY', 'BEGINNER', 55, 3, 'FULL_BODY'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 'Upper Lower Beginner - 4 dias', 'Plantilla global de hipertrofia upper/lower para principiantes con dos dias de tren superior y dos de tren inferior.', 'HYPERTROPHY', 'BEGINNER', 60, 4, 'UPPER_LOWER'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 'Upper Lower Intermediate - 4 dias', 'Plantilla global de hipertrofia upper/lower con mayor volumen en compuestos y accesorios intermedios.', 'HYPERTROPHY', 'INTERMEDIATE', 70, 4, 'UPPER_LOWER'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 'Push Pull Legs Intermediate - 3 dias', 'Plantilla global de hipertrofia PPL compacta para tres dias semanales.', 'HYPERTROPHY', 'INTERMEDIATE', 65, 3, 'PUSH_PULL_LEGS'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 'Push Pull Legs Intermediate - 6 dias', 'Plantilla global de hipertrofia PPL con alta frecuencia semanal y separacion A/B.', 'HYPERTROPHY', 'INTERMEDIATE', 75, 6, 'PUSH_PULL_LEGS'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 'PPL Upper Lower Intermediate - 5 dias', 'Plantilla global de hipertrofia que combina PPL con dos dias adicionales upper/lower.', 'HYPERTROPHY', 'INTERMEDIATE', 70, 5, 'PPL_UPPER_LOWER'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 'Bro Split Advanced - 5 dias', 'Plantilla global de hipertrofia avanzada por grupos musculares para gimnasio.', 'HYPERTROPHY', 'ADVANCED', 80, 5, 'BRO_SPLIT');

CREATE TEMP TABLE tmp_routine_template_days (
    template_key varchar(120) NOT NULL,
    day_index smallint NOT NULL,
    title varchar(160) NOT NULL,
    focus varchar(120)
) ON COMMIT DROP;

INSERT INTO tmp_routine_template_days (template_key, day_index, title, focus)
VALUES
    ('FULL_BODY_BEGINNER_3_DAYS', 1, 'Full Body A', 'Base tecnica de empuje, traccion y pierna.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 2, 'Full Body B', 'Variacion full body con estabilidad y gluteo.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 3, 'Full Body C', 'Sesión full body con enfasis en unilateral y cadenas posteriores.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 1, 'Upper A', 'Empuje y traccion basica de torso.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 2, 'Lower A', 'Cuadriceps, femorales y core con bajo volumen.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 3, 'Upper B', 'Segundo estimulo de torso con accesorios seguros.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 4, 'Lower B', 'Tren inferior con gluteo, unilateral y gemelos.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 1, 'Upper A', 'Compuestos base de pecho y espalda.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 2, 'Lower A', 'Patron dominante de sentadilla y bisagra.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 3, 'Upper B', 'Segundo estimulo upper con hombro y traccion vertical.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 4, 'Lower B', 'Gluteo, unilateral y accesorios de pierna.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 1, 'Push', 'Pecho, hombros y triceps.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 2, 'Pull', 'Espalda, deltoide posterior y biceps.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 3, 'Legs', 'Piernas completas y core.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 1, 'Push A', 'Empuje principal de la semana.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 2, 'Pull A', 'Traccion principal con espalda media y biceps.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 3, 'Legs A', 'Piernas A con enfasis en cuadriceps.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 4, 'Push B', 'Segundo empuje con variantes inclinadas.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 5, 'Pull B', 'Segundo pull con maquina y trabajo unilateral.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 6, 'Legs B', 'Piernas B con femorales y gluteos.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 1, 'Push', 'Pecho, hombros y triceps.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 2, 'Pull', 'Espalda, biceps y deltoide posterior.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 3, 'Legs', 'Piernas con base de sentadilla.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 4, 'Upper', 'Upper mixto de refuerzo.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 5, 'Lower', 'Lower mixto de posterior y gluteo.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 1, 'Pecho', 'Volumen concentrado de pecho y apoyo tricipital.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 2, 'Espalda', 'Volumen concentrado de espalda y deltoide posterior.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 3, 'Piernas', 'Piernas completas con sentadilla, bisagra y gemelos.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 4, 'Hombros', 'Deltoides completos con accesorios.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 5, 'Brazos', 'Biceps y triceps con aislamientos y un compuesto cerrado.');

CREATE TEMP TABLE tmp_routine_template_exercises (
    template_key varchar(120) NOT NULL,
    day_index smallint NOT NULL,
    position smallint NOT NULL,
    exercise_name varchar(180) NOT NULL,
    sets smallint NOT NULL,
    reps_min smallint,
    reps_max smallint,
    rest_seconds integer,
    notes text
) ON COMMIT DROP;

INSERT INTO tmp_routine_template_exercises (template_key, day_index, position, exercise_name, sets, reps_min, reps_max, rest_seconds, notes)
VALUES
    -- FULL_BODY_BEGINNER_3_DAYS
    ('FULL_BODY_BEGINNER_3_DAYS', 1, 1, 'Prensa 45 grados', 3, 8, 12, 90, 'Compuesto principal de pierna.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 1, 2, 'Chest Press Machine', 3, 8, 12, 90, 'Empuje estable para aprender tecnica.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 1, 3, 'Jalón al pecho', 3, 8, 12, 90, 'Traccion vertical controlada.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 1, 4, 'Peso muerto rumano con mancuernas', 3, 8, 12, 90, 'Bisagra de cadera con rango comodo.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 1, 5, 'Elevaciones laterales', 2, 12, 15, 60, 'Aislamiento de hombro medio.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 1, 6, 'Plancha', 2, 30, 45, 45, 'Mantener abdomen firme en cada serie.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 2, 1, 'Sentadilla goblet', 3, 8, 12, 90, 'Sentadilla guiada por la carga frontal.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 2, 2, 'Press con mancuernas sentado', 3, 8, 12, 90, 'Empuje vertical estable.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 2, 3, 'Remo sentado en polea', 3, 8, 12, 90, 'Traccion horizontal basica.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 2, 4, 'Hip Thrust', 3, 8, 12, 90, 'Prioriza extension completa de cadera.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 2, 5, 'Curl alternado con mancuernas', 2, 10, 15, 60, 'Trabajo unilateral de biceps.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 2, 6, 'Crunch', 2, 12, 15, 45, 'Controla la flexion del tronco.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 3, 1, 'Zancadas con mancuernas', 3, 10, 12, 90, 'Paso controlado y postura estable.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 3, 2, 'Press con mancuernas inclinado', 3, 8, 12, 90, 'Empuje para pecho superior.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 3, 3, 'Remo Hammer', 3, 8, 12, 90, 'Traccion en maquina con apoyo.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 3, 4, 'Curl femoral acostado', 2, 10, 15, 60, 'Aislamiento sencillo de femorales.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 3, 5, 'Pushdown con cuerda', 2, 10, 15, 60, 'Extension de triceps al final del dia.'),
    ('FULL_BODY_BEGINNER_3_DAYS', 3, 6, 'Standing Calf Raise', 2, 12, 15, 60, 'Pausa breve arriba en cada repeticion.'),

    -- UPPER_LOWER_BEGINNER_4_DAYS
    ('UPPER_LOWER_BEGINNER_4_DAYS', 1, 1, 'Chest Press Machine', 3, 8, 12, 90, 'Compuesto de pecho guiado.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 1, 2, 'Jalón al pecho', 3, 8, 12, 90, 'Traccion vertical de base.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 1, 3, 'Press con mancuernas inclinado', 3, 8, 12, 90, 'Segundo empuje de pecho.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 1, 4, 'Remo sentado en polea', 3, 8, 12, 90, 'Traccion horizontal controlada.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 1, 5, 'Elevaciones laterales', 2, 12, 15, 60, 'Aislamiento de deltoide medio.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 1, 6, 'Curl alternado con mancuernas', 2, 10, 15, 60, 'Biceps sin exceso de volumen.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 1, 7, 'Pushdown con cuerda', 2, 10, 15, 60, 'Triceps en recorrido completo.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 2, 1, 'Prensa 45 grados', 3, 8, 12, 90, 'Compuesto dominante de cuadriceps.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 2, 2, 'Peso muerto rumano con mancuernas', 3, 8, 12, 90, 'Bisagra amigable para principiantes.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 2, 3, 'Curl femoral acostado', 2, 10, 15, 60, 'Aislamiento posterior.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 2, 4, 'Extensión de piernas', 2, 10, 15, 60, 'Cuadriceps con control total.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 2, 5, 'Seated Calf Raise', 2, 12, 15, 60, 'Gemelos sentado para soleo.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 2, 6, 'Plancha', 2, 30, 45, 45, 'Core estable.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 3, 1, 'Press con mancuernas sentado', 3, 8, 12, 90, 'Compuesto de hombro con respaldo.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 3, 2, 'Remo Hammer', 3, 8, 12, 90, 'Remo estable en maquina.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 3, 3, 'Pec Deck', 2, 10, 15, 60, 'Apertura controlada de pecho.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 3, 4, 'Jalón cerrado', 3, 8, 12, 90, 'Variante de traccion vertical.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 3, 5, 'Face Pull', 2, 12, 15, 60, 'Deltoide posterior y salud escapular.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 3, 6, 'Curl martillo', 2, 10, 15, 60, 'Trabajo de biceps y braquial.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 3, 7, 'Fondos asistidos', 2, 10, 12, 60, 'Empuje accesorio seguro.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 4, 1, 'Sentadilla guiada', 3, 8, 12, 90, 'Compuesto estable para tren inferior.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 4, 2, 'Hip Thrust', 3, 8, 12, 90, 'Gluteo con pausa arriba.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 4, 3, 'Bulgarian Split Squat', 2, 10, 12, 75, 'Unilateral con control del balance.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 4, 4, 'Curl femoral sentado', 2, 10, 15, 60, 'Femorales en rango medio.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 4, 5, 'Abductor Machine', 2, 12, 15, 60, 'Trabajo lateral de gluteo.'),
    ('UPPER_LOWER_BEGINNER_4_DAYS', 4, 6, 'Standing Calf Raise', 2, 12, 15, 60, 'Gemelos de pie.'),

    -- UPPER_LOWER_INTERMEDIATE_4_DAYS
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 1, 1, 'Press de banca plano', 4, 6, 10, 120, 'Principal de pecho y fuerza base.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 1, 2, 'Remo con barra', 4, 6, 10, 120, 'Principal de espalda media.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 1, 3, 'Press con mancuernas inclinado', 3, 8, 12, 90, 'Segundo empuje para pecho superior.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 1, 4, 'Jalón al pecho', 3, 8, 12, 90, 'Complemento de traccion vertical.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 1, 5, 'Elevaciones laterales', 3, 12, 15, 75, 'Aislamiento deltoide medio.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 1, 6, 'Curl con barra', 3, 10, 15, 75, 'Biceps con barra recta.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 1, 7, 'Pushdown con cuerda', 3, 10, 15, 75, 'Triceps con tension constante.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 2, 1, 'Sentadilla trasera', 4, 6, 10, 120, 'Compuesto principal de piernas.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 2, 2, 'Peso muerto rumano', 4, 6, 10, 120, 'Bisagra pesada con control.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 2, 3, 'Prensa 45 grados', 3, 8, 12, 90, 'Volumen adicional de cuadriceps.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 2, 4, 'Curl femoral acostado', 3, 10, 15, 75, 'Aislamiento de femorales.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 2, 5, 'Standing Calf Raise', 3, 12, 15, 60, 'Gemelos con pausa arriba.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 2, 6, 'Crunch en polea', 3, 12, 15, 60, 'Core con resistencia externa.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 3, 1, 'Press militar', 4, 6, 10, 120, 'Compuesto principal de hombros.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 3, 2, 'Dominadas neutras', 3, 8, 10, 120, 'Traccion vertical intermedia.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 3, 3, 'Chest Press Machine', 3, 8, 12, 90, 'Empuje guiado para acumular volumen.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 3, 4, 'Remo sentado en polea', 3, 8, 12, 90, 'Control escapular y dorsales.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 3, 5, 'Face Pull', 3, 12, 15, 75, 'Trabajo posterior y salud del hombro.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 3, 6, 'Curl martillo', 3, 10, 15, 75, 'Agarre neutro para brazo completo.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 3, 7, 'Fondos en paralelas', 3, 8, 12, 90, 'Compuesto accesorio para pecho y triceps.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 4, 1, 'Hip Thrust', 4, 8, 12, 120, 'Principal de gluteo.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 4, 2, 'Bulgarian Split Squat', 3, 8, 12, 90, 'Unilateral demandante.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 4, 3, 'Extensión de piernas', 3, 10, 15, 75, 'Aislamiento de cuadriceps.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 4, 4, 'Curl femoral sentado', 3, 10, 15, 75, 'Femorales en acortamiento.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 4, 5, 'Abductor Machine', 3, 12, 15, 75, 'Gluteo medio y estabilidad.'),
    ('UPPER_LOWER_INTERMEDIATE_4_DAYS', 4, 6, 'Seated Calf Raise', 3, 12, 15, 60, 'Gemelos sentado.'),

    -- PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 1, 1, 'Press de banca plano', 4, 6, 10, 120, 'Compuesto principal de empuje.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 1, 2, 'Press con mancuernas inclinado', 3, 8, 12, 90, 'Refuerzo de pecho superior.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 1, 3, 'Press militar', 3, 6, 10, 120, 'Empuje vertical principal.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 1, 4, 'Pec Deck', 3, 10, 15, 75, 'Aislamiento de pecho.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 1, 5, 'Elevaciones laterales', 3, 12, 15, 75, 'Deltoide medio.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 1, 6, 'Pushdown con cuerda', 3, 10, 15, 75, 'Triceps en polea.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 1, 7, 'Fondos asistidos', 3, 8, 12, 90, 'Compuesto adicional sin fatigar en exceso.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 2, 1, 'Jalón al pecho', 4, 8, 12, 120, 'Traccion vertical base.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 2, 2, 'Remo con barra', 4, 6, 10, 120, 'Compuesto de espalda media.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 2, 3, 'Remo sentado en polea', 3, 8, 12, 90, 'Refuerzo horizontal.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 2, 4, 'Pullover en polea', 3, 10, 15, 75, 'Enfasis en dorsales.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 2, 5, 'Face Pull', 3, 12, 15, 75, 'Deltoide posterior.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 2, 6, 'Curl con barra EZ', 3, 10, 15, 75, 'Biceps principal.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 2, 7, 'Curl martillo', 3, 10, 15, 75, 'Trabajo braquial.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 3, 1, 'Sentadilla trasera', 4, 6, 10, 120, 'Principal de piernas.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 3, 2, 'Peso muerto rumano enfocado en isquiotibiales', 4, 6, 10, 120, 'Bisagra orientada a femorales.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 3, 3, 'Curl femoral acostado', 3, 10, 15, 75, 'Femorales accesorios.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 3, 4, 'Extensión de piernas', 3, 10, 15, 75, 'Cuadriceps accesorios.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 3, 5, 'Hip Thrust', 3, 8, 12, 90, 'Gluteo y extension de cadera.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 3, 6, 'Standing Calf Raise', 3, 12, 15, 60, 'Gemelos de pie.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS', 3, 7, 'Crunch en polea', 3, 12, 15, 60, 'Core con carga externa.'),

    -- PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 1, 1, 'Press de banca plano', 4, 6, 10, 120, 'Compuesto principal del push A.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 1, 2, 'Press con mancuernas inclinado', 3, 8, 12, 90, 'Refuerzo para pecho superior.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 1, 3, 'Press militar', 3, 6, 10, 120, 'Empuje vertical.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 1, 4, 'Pec Deck', 3, 10, 15, 75, 'Aislamiento de pecho.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 1, 5, 'Elevaciones laterales', 3, 12, 15, 75, 'Deltoide medio.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 1, 6, 'Pushdown con cuerda', 3, 10, 15, 75, 'Triceps en polea.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 2, 1, 'Jalón al pecho', 4, 8, 12, 120, 'Traccion vertical principal.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 2, 2, 'Remo con barra', 4, 6, 10, 120, 'Compuesto de espalda.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 2, 3, 'Remo sentado en polea', 3, 8, 12, 90, 'Refuerzo de dorsales.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 2, 4, 'Face Pull', 3, 12, 15, 75, 'Posterior y trapecio medio.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 2, 5, 'Curl con barra', 3, 10, 15, 75, 'Biceps principal.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 2, 6, 'Curl martillo', 3, 10, 15, 75, 'Braquial y antebrazo.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 3, 1, 'Sentadilla trasera', 4, 6, 10, 120, 'Dominante de rodilla.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 3, 2, 'Prensa 45 grados', 3, 8, 12, 90, 'Volumen extra de pierna.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 3, 3, 'Curl femoral acostado', 3, 10, 15, 75, 'Femorales.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 3, 4, 'Extensión de piernas', 3, 10, 15, 75, 'Cuadriceps.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 3, 5, 'Standing Calf Raise', 3, 12, 15, 60, 'Gemelos.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 3, 6, 'Plancha', 3, 30, 45, 45, 'Core anti-extension.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 4, 1, 'Press de banca inclinado', 4, 6, 10, 120, 'Compuesto inclinado del push B.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 4, 2, 'Chest Press Machine', 3, 8, 12, 90, 'Volumen guiado de pecho.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 4, 3, 'Press con mancuernas sentado', 3, 8, 12, 90, 'Hombro con estabilidad.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 4, 4, 'Aperturas en polea alta', 3, 10, 15, 75, 'Aislamiento de pecho.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 4, 5, 'Elevaciones laterales', 3, 12, 15, 75, 'Deltoide medio.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 4, 6, 'Extensión overhead en polea', 3, 10, 15, 75, 'Triceps en estiramiento.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 5, 1, 'Jalón cerrado', 4, 8, 12, 120, 'Traccion vertical cerrada.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 5, 2, 'Remo Hammer', 3, 8, 12, 90, 'Remo guiado para espalda media.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 5, 3, 'Remo con mancuerna', 3, 8, 12, 90, 'Trabajo unilateral de dorsales.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 5, 4, 'Pullover en polea', 3, 10, 15, 75, 'Dorsales en recorrido largo.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 5, 5, 'Pájaros con mancuernas', 3, 12, 15, 75, 'Deltoide posterior.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 5, 6, 'Preacher Curl Machine', 3, 10, 15, 75, 'Biceps con apoyo.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 6, 1, 'Peso muerto rumano enfocado en isquiotibiales', 4, 6, 10, 120, 'Bisagra del legs B.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 6, 2, 'Hip Thrust', 4, 8, 12, 120, 'Gluteo y extension de cadera.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 6, 3, 'Bulgarian Split Squat', 3, 8, 12, 90, 'Unilateral pesado.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 6, 4, 'Curl femoral sentado', 3, 10, 15, 75, 'Femorales.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 6, 5, 'Abductor Machine', 3, 12, 15, 75, 'Gluteo medio.'),
    ('PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS', 6, 6, 'Seated Calf Raise', 3, 12, 15, 60, 'Gemelos sentado.'),

    -- PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 1, 1, 'Press de banca plano', 4, 6, 10, 120, 'Empuje horizontal principal.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 1, 2, 'Press de banca inclinado', 3, 8, 12, 90, 'Segundo empuje de pecho.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 1, 3, 'Press militar', 3, 6, 10, 120, 'Empuje vertical.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 1, 4, 'Pec Deck', 3, 10, 15, 75, 'Aislamiento de pecho.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 1, 5, 'Elevaciones laterales', 3, 12, 15, 75, 'Deltoide medio.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 1, 6, 'Pushdown con cuerda', 3, 10, 15, 75, 'Triceps en polea.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 2, 1, 'Jalón al pecho', 4, 8, 12, 120, 'Traccion vertical base.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 2, 2, 'Remo con barra', 4, 6, 10, 120, 'Principal de espalda.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 2, 3, 'Remo con mancuerna', 3, 8, 12, 90, 'Trabajo unilateral.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 2, 4, 'Face Pull', 3, 12, 15, 75, 'Posterior del hombro.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 2, 5, 'Curl con barra', 3, 10, 15, 75, 'Biceps principal.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 2, 6, 'Curl martillo', 3, 10, 15, 75, 'Braquial y antebrazo.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 3, 1, 'Sentadilla trasera', 4, 6, 10, 120, 'Base del dia de piernas.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 3, 2, 'Prensa 45 grados', 3, 8, 12, 90, 'Volumen de cuadriceps.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 3, 3, 'Curl femoral acostado', 3, 10, 15, 75, 'Femorales.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 3, 4, 'Extensión de piernas', 3, 10, 15, 75, 'Cuadriceps accesorios.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 3, 5, 'Standing Calf Raise', 3, 12, 15, 60, 'Gemelos.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 4, 1, 'Press con mancuernas inclinado', 3, 8, 12, 90, 'Empuje upper de refuerzo.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 4, 2, 'Remo sentado en polea', 3, 8, 12, 90, 'Traccion controlada.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 4, 3, 'Chest Press Machine', 3, 8, 12, 90, 'Volumen guiado.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 4, 4, 'Jalón cerrado', 3, 8, 12, 90, 'Traccion vertical complementaria.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 4, 5, 'Elevaciones laterales', 3, 12, 15, 75, 'Deltoide medio.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 4, 6, 'Curl con barra EZ', 3, 10, 15, 75, 'Biceps.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 4, 7, 'Pushdown con cuerda', 3, 10, 15, 75, 'Triceps en polea.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 5, 1, 'Peso muerto rumano enfocado en isquiotibiales', 4, 6, 10, 120, 'Bisagra del lower de refuerzo.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 5, 2, 'Hip Thrust', 4, 8, 12, 120, 'Gluteo principal.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 5, 3, 'Bulgarian Split Squat', 3, 8, 12, 90, 'Unilateral.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 5, 4, 'Curl femoral sentado', 3, 10, 15, 75, 'Femorales.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 5, 5, 'Abductor Machine', 3, 12, 15, 75, 'Gluteo medio.'),
    ('PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS', 5, 6, 'Seated Calf Raise', 3, 12, 15, 60, 'Gemelos sentado.'),

    -- BRO_SPLIT_ADVANCED_5_DAYS
    ('BRO_SPLIT_ADVANCED_5_DAYS', 1, 1, 'Press de banca plano', 4, 6, 10, 150, 'Principal pesado del dia de pecho.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 1, 2, 'Press de banca inclinado', 4, 6, 10, 150, 'Segundo compuesto de pecho.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 1, 3, 'Chest Press Machine', 4, 8, 12, 120, 'Volumen guiado adicional.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 1, 4, 'Crossover en polea', 4, 10, 15, 75, 'Aislamiento largo de pecho.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 1, 5, 'Fondos en paralelas', 4, 8, 12, 120, 'Compuesto final con triceps.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 1, 6, 'Pullover con mancuerna', 3, 10, 15, 75, 'Estiramiento y control del pecho.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 2, 1, 'Dominadas pronas', 4, 6, 10, 150, 'Traccion vertical avanzada.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 2, 2, 'Remo con barra', 4, 6, 10, 150, 'Compuesto principal de espalda.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 2, 3, 'Remo sentado en polea', 4, 8, 12, 120, 'Volumen horizontal.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 2, 4, 'Jalón cerrado', 3, 8, 12, 120, 'Complemento vertical.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 2, 5, 'Pullover en polea', 3, 10, 15, 75, 'Dorsales en recorrido largo.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 2, 6, 'Face Pull', 3, 12, 20, 75, 'Deltoide posterior y estabilidad escapular.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 3, 1, 'Sentadilla trasera', 4, 6, 10, 150, 'Principal de piernas.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 3, 2, 'Prensa 45 grados', 4, 8, 12, 120, 'Volumen de cuadriceps.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 3, 3, 'Peso muerto rumano enfocado en isquiotibiales', 4, 6, 10, 150, 'Bisagra pesada.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 3, 4, 'Curl femoral acostado', 4, 10, 15, 75, 'Femorales accesorios.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 3, 5, 'Extensión de piernas', 4, 10, 15, 75, 'Cuadriceps accesorios.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 3, 6, 'Standing Calf Raise', 4, 12, 20, 60, 'Gemelos con alto volumen.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 4, 1, 'Press militar', 4, 6, 10, 150, 'Principal de hombros.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 4, 2, 'Elevaciones laterales', 4, 12, 20, 75, 'Deltoide medio.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 4, 3, 'Pájaros con mancuernas', 4, 12, 20, 75, 'Posterior del hombro.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 4, 4, 'Press Arnold', 4, 8, 12, 120, 'Compuesto secundario del hombro.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 4, 5, 'Face Pull', 3, 12, 20, 75, 'Trabajo escapular y posterior.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 4, 6, 'Encogimientos con mancuernas', 3, 12, 20, 75, 'Trapecio superior al final.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 5, 1, 'Curl con barra', 4, 8, 12, 75, 'Biceps pesado.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 5, 2, 'Curl inclinado con mancuernas', 4, 10, 15, 75, 'Estiramiento largo del biceps.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 5, 3, 'Curl martillo', 4, 10, 15, 75, 'Braquial y antebrazo.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 5, 4, 'Press de banca cerrado', 4, 6, 10, 120, 'Compuesto cerrado para triceps.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 5, 5, 'Pushdown con cuerda', 4, 10, 15, 75, 'Triceps en polea.'),
    ('BRO_SPLIT_ADVANCED_5_DAYS', 5, 6, 'Extensión de tríceps con mancuerna sobre la cabeza', 4, 10, 15, 75, 'Trabajo en estiramiento del triceps.');

WITH hypertrophy_category AS (
    SELECT id
    FROM routine_categories
    WHERE lower(name) = 'hipertrofia'
    LIMIT 1
)
UPDATE routines AS target
SET user_id = NULL,
    title = src.title,
    description = src.description,
    objective = src.objective,
    source = 'TEMPLATE',
    difficulty = src.difficulty,
    estimated_duration_minutes = src.estimated_duration_minutes,
    days_per_week = src.days_per_week,
    routine_split = src.routine_split,
    category_id = (SELECT id FROM hypertrophy_category),
    is_active = true,
    updated_at = now()
FROM tmp_routine_templates AS src
WHERE target.template_key = src.template_key;

WITH hypertrophy_category AS (
    SELECT id
    FROM routine_categories
    WHERE lower(name) = 'hipertrofia'
    LIMIT 1
)
INSERT INTO routines (
    user_id,
    title,
    description,
    objective,
    source,
    difficulty,
    estimated_duration_minutes,
    days_per_week,
    is_active,
    category_id,
    template_key,
    routine_split
)
SELECT
    NULL,
    src.title,
    src.description,
    src.objective,
    'TEMPLATE',
    src.difficulty,
    src.estimated_duration_minutes,
    src.days_per_week,
    true,
    (SELECT id FROM hypertrophy_category),
    src.template_key,
    src.routine_split
FROM tmp_routine_templates AS src
WHERE NOT EXISTS (
    SELECT 1
    FROM routines r
    WHERE r.template_key = src.template_key
);

DELETE FROM routine_days
WHERE routine_id IN (
    SELECT r.id
    FROM routines r
    JOIN tmp_routine_templates src ON src.template_key = r.template_key
);

INSERT INTO routine_days (routine_id, day_index, title, focus)
SELECT
    r.id,
    src.day_index,
    src.title,
    src.focus
FROM tmp_routine_template_days AS src
JOIN routines r ON r.template_key = src.template_key;

INSERT INTO routine_exercises (
    routine_day_id,
    exercise_id,
    position,
    sets,
    reps_min,
    reps_max,
    rest_seconds,
    notes
)
SELECT
    rd.id,
    e.id,
    src.position,
    src.sets,
    src.reps_min,
    src.reps_max,
    src.rest_seconds,
    src.notes
FROM tmp_routine_template_exercises AS src
JOIN routines r ON r.template_key = src.template_key
JOIN routine_days rd ON rd.routine_id = r.id AND rd.day_index = src.day_index
JOIN exercises e ON lower(e.name) = lower(src.exercise_name);