ALTER TABLE exercises
    ADD COLUMN IF NOT EXISTS category varchar(80),
    ADD COLUMN IF NOT EXISTS primary_muscle_group varchar(120),
    ADD COLUMN IF NOT EXISTS equipment varchar(120),
    ADD COLUMN IF NOT EXISTS difficulty varchar(80),
    ADD COLUMN IF NOT EXISTS instructions text,
    ADD COLUMN IF NOT EXISTS is_active boolean DEFAULT true;

CREATE INDEX IF NOT EXISTS idx_exercises_category ON exercises(category);
CREATE INDEX IF NOT EXISTS idx_exercises_primary_muscle_group ON exercises(primary_muscle_group);
CREATE INDEX IF NOT EXISTS idx_exercises_equipment ON exercises(equipment);
CREATE INDEX IF NOT EXISTS idx_exercises_difficulty ON exercises(difficulty);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = current_schema()
          AND indexname = 'idx_exercises_unique_name'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM exercises
        GROUP BY lower(name)
        HAVING count(*) > 1
    ) THEN
        EXECUTE 'CREATE UNIQUE INDEX idx_exercises_unique_name ON exercises (LOWER(name))';
    END IF;
END $$;

CREATE TEMP TABLE tmp_hypertrophy_exercises (
    name varchar(180) NOT NULL,
    primary_muscle_group varchar(120) NOT NULL,
    equipment varchar(120) NOT NULL,
    difficulty varchar(80) NOT NULL,
    instructions text NOT NULL
) ON COMMIT DROP;

INSERT INTO tmp_hypertrophy_exercises (name, primary_muscle_group, equipment, difficulty, instructions)
VALUES
    -- Pecho
    ('Press de banca plano', 'Pecho', 'BARBELL', 'INTERMEDIATE', 'Apoya los pies firmes, baja la barra al pecho con control y empuja sin perder estabilidad escapular.'),
    ('Press de banca inclinado', 'Pecho', 'BARBELL', 'INTERMEDIATE', 'Usa un banco inclinado, lleva la barra al pecho alto y presiona manteniendo los hombros estables.'),
    ('Press de banca declinado', 'Pecho', 'BARBELL', 'INTERMEDIATE', 'Fija bien el torso, desciende la barra de forma controlada y empuja enfocando el pecho inferior.'),
    ('Press con mancuernas plano', 'Pecho', 'DUMBBELL', 'BEGINNER', 'Baja las mancuernas a cada lado del pecho y presiona arriba sin chocar los implementos.'),
    ('Press con mancuernas inclinado', 'Pecho', 'DUMBBELL', 'BEGINNER', 'Mantén el pecho elevado, baja con control y empuja las mancuernas sobre la línea clavicular.'),
    ('Press con mancuernas declinado', 'Pecho', 'DUMBBELL', 'INTERMEDIATE', 'Desciende las mancuernas a la parte baja del pecho y presiona manteniendo control del recorrido.'),
    ('Aperturas con mancuernas', 'Pecho', 'DUMBBELL', 'BEGINNER', 'Abre los brazos con ligera flexión de codos y cierra en arco sintiendo el estiramiento del pecho.'),
    ('Pullover con mancuerna', 'Pecho', 'DUMBBELL', 'INTERMEDIATE', 'Lleva la mancuerna detrás de la cabeza con codos semirrígidos y vuelve al centro sin arquear la espalda.'),
    ('Chest Press Machine', 'Pecho', 'MACHINE', 'BEGINNER', 'Ajusta el asiento, empuja los agarres al frente y regresa lento sin despegar la espalda del respaldo.'),
    ('Press inclinado en máquina', 'Pecho', 'MACHINE', 'BEGINNER', 'Empuja en diagonal hacia arriba con el pecho activo y controla la fase de retorno.'),
    ('Pec Deck', 'Pecho', 'MACHINE', 'BEGINNER', 'Mantén los codos alineados con el pecho, junta los brazos al frente y vuelve de forma controlada.'),
    ('Contractor de pecho', 'Pecho', 'MACHINE', 'BEGINNER', 'Contrae el pecho al cerrar los brazos y evita impulsarte con la espalda o el cuello.'),
    ('Aperturas en polea alta', 'Pecho', 'CABLE', 'INTERMEDIATE', 'Cruza las manos hacia abajo y al frente manteniendo tensión continua durante todo el recorrido.'),
    ('Aperturas en polea baja', 'Pecho', 'CABLE', 'INTERMEDIATE', 'Eleva los brazos desde abajo en arco hasta la línea del pecho sin flexionar demasiado los codos.'),
    ('Crossover en polea', 'Pecho', 'CABLE', 'INTERMEDIATE', 'Cruza las poleas al frente del torso, aprieta el pecho y vuelve lento sin perder postura.'),
    ('Press unilateral en polea', 'Pecho', 'CABLE', 'INTERMEDIATE', 'Empuja una polea al frente con el tronco firme y controla la rotación del torso.'),
    ('Flexiones clásicas', 'Pecho', 'BODYWEIGHT', 'BEGINNER', 'Desciende el pecho cerca del suelo con el cuerpo alineado y empuja extendiendo los brazos.'),
    ('Flexiones inclinadas', 'Pecho', 'BODYWEIGHT', 'BEGINNER', 'Apoya las manos en una superficie elevada y realiza la flexión manteniendo la línea corporal.'),
    ('Flexiones declinadas', 'Pecho', 'BODYWEIGHT', 'INTERMEDIATE', 'Coloca los pies elevados, baja con control y empuja priorizando el pecho superior.'),
    ('Flexiones lastradas', 'Pecho', 'BODYWEIGHT', 'ADVANCED', 'Añade carga externa, mantén el tronco estable y completa cada repetición con rango completo.'),

    -- Espalda
    ('Remo con barra', 'Espalda', 'BARBELL', 'INTERMEDIATE', 'Inclina el torso, lleva la barra al abdomen y baja controlando sin redondear la espalda.'),
    ('Remo Pendlay', 'Espalda', 'BARBELL', 'ADVANCED', 'Parte desde el suelo en cada repetición y rema explosivo hacia el torso con espalda neutra.'),
    ('Remo con mancuerna', 'Espalda', 'DUMBBELL', 'BEGINNER', 'Apoya una mano en el banco, lleva la mancuerna a la cadera y desciende sin girar el tronco.'),
    ('Peso muerto', 'Espalda', 'BARBELL', 'ADVANCED', 'Empuja el suelo con los pies, extiende cadera y rodillas a la vez y mantén la barra cerca del cuerpo.'),
    ('Peso muerto rumano', 'Espalda', 'BARBELL', 'INTERMEDIATE', 'Desplaza la cadera atrás con rodillas semiflexionadas y sube contrayendo glúteos y espalda posterior.'),
    ('Jalón al pecho', 'Espalda', 'CABLE', 'BEGINNER', 'Lleva la barra hacia la parte alta del pecho con codos abajo y controla la subida.'),
    ('Jalón supino', 'Espalda', 'CABLE', 'BEGINNER', 'Usa agarre supino, baja al pecho con el torso estable y evita balancearte.'),
    ('Jalón cerrado', 'Espalda', 'CABLE', 'BEGINNER', 'Tira del agarre hacia el pecho manteniendo hombros abajo y tensión en dorsales.'),
    ('Remo sentado en polea', 'Espalda', 'CABLE', 'BEGINNER', 'Lleva el agarre al abdomen con la espalda neutra y regresa sin encorvarte.'),
    ('Pullover en polea', 'Espalda', 'CABLE', 'INTERMEDIATE', 'Baja la barra con brazos casi extendidos hasta las caderas activando dorsales todo el tiempo.'),
    ('Remo Hammer', 'Espalda', 'MACHINE', 'BEGINNER', 'Empuja el pecho contra el soporte, rema hacia el torso y controla el retorno.'),
    ('Remo convergente', 'Espalda', 'MACHINE', 'BEGINNER', 'Tira de los agarres hacia el cuerpo siguiendo la trayectoria de la máquina y sin encoger hombros.'),
    ('High Row Machine', 'Espalda', 'MACHINE', 'BEGINNER', 'Jala los agarres desde arriba hacia el torso con el pecho apoyado y recorrido completo.'),
    ('Lat Pulldown Machine', 'Espalda', 'MACHINE', 'BEGINNER', 'Desciende los agarres al pecho manteniendo el torso fijo y sube controlando la carga.'),
    ('Dominadas pronas', 'Espalda', 'BODYWEIGHT', 'ADVANCED', 'Tira del cuerpo hasta pasar la barbilla por encima de la barra sin balanceo.'),
    ('Dominadas supinas', 'Espalda', 'BODYWEIGHT', 'ADVANCED', 'Usa agarre supino y sube con el pecho hacia la barra manteniendo control en la bajada.'),
    ('Dominadas neutras', 'Espalda', 'BODYWEIGHT', 'INTERMEDIATE', 'Eleva el cuerpo con agarre neutro, hombros estables y extensión completa abajo.'),
    ('Dominadas lastradas', 'Espalda', 'BODYWEIGHT', 'ADVANCED', 'Añade carga externa y mantén técnica estricta durante todo el rango de movimiento.'),

    -- Hombros
    ('Press militar', 'Hombros', 'BARBELL', 'INTERMEDIATE', 'Presiona la barra sobre la cabeza con abdomen firme y sin extender en exceso la zona lumbar.'),
    ('Press Arnold', 'Hombros', 'DUMBBELL', 'INTERMEDIATE', 'Gira las mancuernas mientras suben y baja controlando para mantener tensión en deltoides.'),
    ('Press con mancuernas sentado', 'Hombros', 'DUMBBELL', 'BEGINNER', 'Empuja las mancuernas arriba desde el nivel de los hombros con espalda apoyada.'),
    ('Elevaciones laterales', 'Hombros', 'DUMBBELL', 'BEGINNER', 'Eleva los brazos a los lados hasta la altura del hombro sin usar impulso.'),
    ('Elevaciones frontales', 'Hombros', 'DUMBBELL', 'BEGINNER', 'Sube las mancuernas al frente hasta el nivel de los hombros y baja lento.'),
    ('Pájaros con mancuernas', 'Hombros', 'DUMBBELL', 'BEGINNER', 'Inclina el torso, abre los brazos hacia los lados y aprieta la parte posterior del hombro.'),
    ('Elevaciones laterales en polea', 'Hombros', 'CABLE', 'INTERMEDIATE', 'Eleva el brazo lateralmente con tensión constante y evita inclinar el tronco.'),
    ('Face Pull', 'Hombros', 'CABLE', 'BEGINNER', 'Tira de la cuerda hacia la cara con codos altos y rota externamente al final.'),
    ('Elevación frontal en polea', 'Hombros', 'CABLE', 'BEGINNER', 'Eleva el agarre al frente con el torso estable y baja controlando la resistencia.'),
    ('Shoulder Press Machine', 'Hombros', 'MACHINE', 'BEGINNER', 'Empuja los agarres por encima de la cabeza y vuelve lento sin perder apoyo lumbar.'),
    ('Lateral Raise Machine', 'Hombros', 'MACHINE', 'BEGINNER', 'Eleva los brazos siguiendo la trayectoria de la máquina y controla la bajada.'),
    ('Rear Delt Machine', 'Hombros', 'MACHINE', 'BEGINNER', 'Abre los brazos hacia atrás enfocando la contracción del deltoide posterior.'),

    -- Bíceps
    ('Curl con barra', 'Bíceps', 'BARBELL', 'BEGINNER', 'Flexiona los codos sin balancear el torso y baja la barra de forma controlada.'),
    ('Curl con barra EZ', 'Bíceps', 'BARBELL', 'BEGINNER', 'Usa un agarre cómodo en la barra EZ, sube con control y evita despegar los codos.'),
    ('Curl alternado con mancuernas', 'Bíceps', 'DUMBBELL', 'BEGINNER', 'Alterna cada brazo, gira la palma al subir y baja sin perder tensión.'),
    ('Curl martillo', 'Bíceps', 'DUMBBELL', 'BEGINNER', 'Mantén agarre neutro durante todo el recorrido y evita usar impulso de cadera.'),
    ('Curl inclinado con mancuernas', 'Bíceps', 'DUMBBELL', 'INTERMEDIATE', 'Deja caer los brazos desde un banco inclinado y flexiona manteniendo el hombro estable.'),
    ('Curl en polea baja', 'Bíceps', 'CABLE', 'BEGINNER', 'Tira del agarre desde abajo hasta los hombros con codos pegados al cuerpo.'),
    ('Curl unilateral en polea', 'Bíceps', 'CABLE', 'BEGINNER', 'Trabaja un brazo por vez, mantén el codo fijo y controla la fase excéntrica.'),
    ('Preacher Curl Machine', 'Bíceps', 'MACHINE', 'BEGINNER', 'Apoya bien los brazos, flexiona sin despegar los codos y baja lento.'),
    ('Curl Machine', 'Bíceps', 'MACHINE', 'BEGINNER', 'Ajusta el eje a tu codo, flexiona completo y regresa manteniendo tensión constante.'),

    -- Tríceps
    ('Press francés', 'Tríceps', 'BARBELL', 'INTERMEDIATE', 'Baja la barra hacia la frente con codos fijos y extiende sin abrir demasiado los brazos.'),
    ('Extensión de tríceps con mancuerna sobre la cabeza', 'Tríceps', 'DUMBBELL', 'BEGINNER', 'Desciende la mancuerna detrás de la cabeza y extiende los codos manteniéndolos cerca.'),
    ('Fondos en paralelas', 'Tríceps', 'BODYWEIGHT', 'INTERMEDIATE', 'Baja el cuerpo controladamente entre barras y empuja hasta extender los brazos.'),
    ('Pushdown con barra', 'Tríceps', 'CABLE', 'BEGINNER', 'Empuja la barra hacia abajo con codos pegados al torso y sube sin perder postura.'),
    ('Pushdown con cuerda', 'Tríceps', 'CABLE', 'BEGINNER', 'Separa las puntas de la cuerda al final para completar la extensión del tríceps.'),
    ('Pushdown unilateral', 'Tríceps', 'CABLE', 'BEGINNER', 'Extiende un brazo por vez manteniendo el codo estable y el hombro quieto.'),
    ('Extensión overhead en polea', 'Tríceps', 'CABLE', 'BEGINNER', 'Trabaja por encima de la cabeza para estirar el tríceps y extiende con control.'),
    ('Triceps Extension Machine', 'Tríceps', 'MACHINE', 'BEGINNER', 'Ajusta la máquina al codo, extiende fuerte y vuelve lento sin despegar los brazos.'),
    ('Fondos asistidos', 'Tríceps', 'MACHINE', 'BEGINNER', 'Usa la asistencia necesaria para bajar con control y empujar sin perder alineación.'),

    -- Cuádriceps
    ('Sentadilla trasera', 'Cuádriceps', 'BARBELL', 'INTERMEDIATE', 'Desciende hasta una profundidad segura con pecho arriba y empuja el suelo al subir.'),
    ('Sentadilla frontal', 'Cuádriceps', 'BARBELL', 'ADVANCED', 'Mantén codos altos, torso vertical y desciende controlando la posición de la barra.'),
    ('Sentadilla goblet', 'Cuádriceps', 'DUMBBELL', 'BEGINNER', 'Sostén la mancuerna frente al pecho y baja con rodillas siguiendo la línea de los pies.'),
    ('Bulgarian Split Squat', 'Cuádriceps', 'DUMBBELL', 'INTERMEDIATE', 'Apoya el pie trasero en un banco y desciende verticalmente sobre la pierna delantera.'),
    ('Zancadas con mancuernas', 'Cuádriceps', 'DUMBBELL', 'BEGINNER', 'Da un paso controlado, baja ambas rodillas y empuja con la pierna adelantada.'),
    ('Prensa 45 grados', 'Cuádriceps', 'MACHINE', 'BEGINNER', 'Baja la plataforma hasta un rango cómodo y empuja sin bloquear completamente las rodillas.'),
    ('Hack Squat', 'Cuádriceps', 'MACHINE', 'INTERMEDIATE', 'Mantén la espalda apoyada, desciende profundo y sube controlando la trayectoria fija.'),
    ('Pendulum Squat', 'Cuádriceps', 'MACHINE', 'INTERMEDIATE', 'Usa un rango amplio, mantén el tronco estable y empuja con fuerza desde el pie completo.'),
    ('Extensión de piernas', 'Cuádriceps', 'MACHINE', 'BEGINNER', 'Extiende las rodillas hasta contraer el cuádriceps y baja de forma lenta.'),
    ('Belt Squat', 'Cuádriceps', 'MACHINE', 'INTERMEDIATE', 'Carga la cadera con el cinturón, desciende con torso estable y empuja hasta extender.'),

    -- Isquiotibiales
    ('Peso muerto rumano enfocado en isquiotibiales', 'Isquiotibiales', 'BARBELL', 'INTERMEDIATE', 'Lleva la cadera atrás, siente el estiramiento en femorales y sube apretando glúteos.'),
    ('Peso muerto piernas rígidas', 'Isquiotibiales', 'BARBELL', 'INTERMEDIATE', 'Mantén las piernas casi extendidas, baja la barra cerca del cuerpo y sube con control.'),
    ('Buenos días', 'Isquiotibiales', 'BARBELL', 'ADVANCED', 'Apoya la barra en la espalda alta, inclina el torso y vuelve extendiendo la cadera.'),
    ('Curl femoral acostado', 'Isquiotibiales', 'MACHINE', 'BEGINNER', 'Flexiona las rodillas llevando los talones hacia los glúteos y baja lentamente.'),
    ('Curl femoral sentado', 'Isquiotibiales', 'MACHINE', 'BEGINNER', 'Ajusta la máquina, flexiona con control y mantén la cadera pegada al asiento.'),
    ('Curl femoral de pie', 'Isquiotibiales', 'MACHINE', 'BEGINNER', 'Trabaja una pierna por vez, flexiona la rodilla y evita mover la cadera.'),

    -- Glúteos
    ('Hip Thrust', 'Glúteos', 'BARBELL', 'INTERMEDIATE', 'Eleva la cadera hasta alinear rodillas, cadera y hombros apretando glúteos arriba.'),
    ('Hip Thrust unilateral', 'Glúteos', 'BODYWEIGHT', 'INTERMEDIATE', 'Apoya una pierna, eleva la cadera y mantén la pelvis estable durante cada repetición.'),
    ('Sentadilla sumo', 'Glúteos', 'BARBELL', 'INTERMEDIATE', 'Abre la postura, baja con rodillas hacia afuera y sube empujando desde los talones.'),
    ('Peso muerto sumo', 'Glúteos', 'BARBELL', 'ADVANCED', 'Coloca una base amplia, extiende cadera y rodillas manteniendo la barra cerca del cuerpo.'),
    ('Patada de glúteo en polea', 'Glúteos', 'CABLE', 'BEGINNER', 'Empuja la pierna hacia atrás sin arquear la espalda y vuelve controlando la tensión.'),
    ('Pull Through en polea', 'Glúteos', 'CABLE', 'BEGINNER', 'Lleva la cuerda entre las piernas, empuja la cadera al frente y aprieta glúteos.'),
    ('Glute Drive Machine', 'Glúteos', 'MACHINE', 'BEGINNER', 'Eleva la cadera contra la resistencia y pausa un instante en la contracción máxima.'),
    ('Abductor Machine', 'Glúteos', 'MACHINE', 'BEGINNER', 'Abre las piernas contra la máquina manteniendo la pelvis estable y controla el retorno.'),

    -- Pantorrillas
    ('Elevación de talones de pie', 'Pantorrillas', 'FREE_WEIGHT', 'BEGINNER', 'Eleva los talones al máximo, pausa arriba y desciende sintiendo el estiramiento.'),
    ('Elevación de talones sentado', 'Pantorrillas', 'FREE_WEIGHT', 'BEGINNER', 'Eleva los talones desde la posición sentada y controla la bajada sin rebotes.'),
    ('Standing Calf Raise', 'Pantorrillas', 'MACHINE', 'BEGINNER', 'Empuja con la punta de los pies, mantén la extensión arriba y baja lento.'),
    ('Seated Calf Raise', 'Pantorrillas', 'MACHINE', 'BEGINNER', 'Trabaja el rango completo desde sentado y evita impulsarte con el rebote.'),
    ('Donkey Calf Raise', 'Pantorrillas', 'MACHINE', 'INTERMEDIATE', 'Flexiona la cadera según la máquina, eleva los talones alto y controla la excéntrica.'),

    -- Abdomen
    ('Crunch', 'Abdomen', 'BODYWEIGHT', 'BEGINNER', 'Flexiona el tronco elevando los hombros del suelo y vuelve sin tirar del cuello.'),
    ('Crunch inverso', 'Abdomen', 'BODYWEIGHT', 'BEGINNER', 'Lleva las rodillas al pecho elevando la pelvis y baja controladamente.'),
    ('Elevaciones de piernas', 'Abdomen', 'BODYWEIGHT', 'INTERMEDIATE', 'Eleva las piernas con abdomen firme y evita que la zona lumbar se despegue demasiado.'),
    ('Plancha', 'Abdomen', 'BODYWEIGHT', 'BEGINNER', 'Mantén el cuerpo alineado, abdomen apretado y glúteos activos durante el tiempo indicado.'),
    ('Hollow Hold', 'Abdomen', 'BODYWEIGHT', 'INTERMEDIATE', 'Sostén la posición cóncava con zona lumbar pegada al suelo y brazos extendidos.'),
    ('Crunch en polea', 'Abdomen', 'CABLE', 'BEGINNER', 'Flexiona el tronco contra la polea usando el abdomen y sin tirar con los brazos.'),
    ('Woodchopper en polea', 'Abdomen', 'CABLE', 'INTERMEDIATE', 'Rota el torso en diagonal con control y mantén la cadera estable.'),
    ('Abdominal Crunch Machine', 'Abdomen', 'MACHINE', 'BEGINNER', 'Flexiona el torso siguiendo la máquina y vuelve lento manteniendo tensión abdominal.');

UPDATE exercises AS exercises_target
SET category = 'Hipertrofia',
    primary_muscle_group = tmp.primary_muscle_group,
    equipment = tmp.equipment,
    difficulty = tmp.difficulty,
    instructions = tmp.instructions,
    is_active = true,
    updated_at = now()
FROM tmp_hypertrophy_exercises AS tmp
WHERE lower(exercises_target.name) = lower(tmp.name)
  AND (
      exercises_target.category IS DISTINCT FROM 'Hipertrofia'
      OR exercises_target.primary_muscle_group IS DISTINCT FROM tmp.primary_muscle_group
      OR exercises_target.equipment IS DISTINCT FROM tmp.equipment
      OR exercises_target.difficulty IS DISTINCT FROM tmp.difficulty
      OR exercises_target.instructions IS DISTINCT FROM tmp.instructions
      OR exercises_target.is_active IS DISTINCT FROM true
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
    tmp.name,
    NULL,
    'Hipertrofia',
    tmp.primary_muscle_group,
    '[]'::jsonb,
    tmp.equipment,
    tmp.difficulty,
    tmp.instructions,
    true
FROM tmp_hypertrophy_exercises AS tmp
WHERE NOT EXISTS (
    SELECT 1
    FROM exercises
    WHERE lower(name) = lower(tmp.name)
);