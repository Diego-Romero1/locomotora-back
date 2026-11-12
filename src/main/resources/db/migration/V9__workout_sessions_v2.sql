-- Fase 1 · Registro fiel.
-- Sesiones idempotentes (client_session_id), tipo de sesion, copia de titulos
-- y del plan del momento en cada serie registrada.

ALTER TABLE workout_sessions
    ADD COLUMN client_session_id uuid,
    ADD COLUMN kind varchar(20) NOT NULL DEFAULT 'ROUTINE',
    ADD COLUMN routine_title varchar(180),
    ADD COLUMN day_title varchar(160);

ALTER TABLE workout_sessions
    ADD CONSTRAINT workout_sessions_kind_check CHECK (kind IN ('ROUTINE', 'FREE', 'QUICK_LOG'));

-- Un mismo id generado en el telefono identifica una sola sesion por usuario.
-- Los NULL no chocan entre si, asi que las sesiones viejas no se ven afectadas.
ALTER TABLE workout_sessions
    ADD CONSTRAINT workout_sessions_user_client_unique UNIQUE (user_id, client_session_id);

ALTER TABLE workout_exercise_logs
    ADD COLUMN exercise_position smallint,
    ADD COLUMN target_reps_min smallint,
    ADD COLUMN target_reps_max smallint,
    ADD COLUMN target_weight_kg numeric(6,2),
    ADD COLUMN rest_seconds integer;

-- "La vez pasada" e historial por ejercicio.
CREATE INDEX idx_workout_exercise_logs_exercise_created
    ON workout_exercise_logs (exercise_id, created_at DESC);

-- Registros sueltos de POST /exercises/{id}/logs: sesion sin rutina ni dia,
-- con logs que no vienen de una rutina. Dejan de contar como entrenamientos.
UPDATE workout_sessions ws
SET kind = 'QUICK_LOG'
WHERE ws.routine_id IS NULL
  AND ws.routine_day_id IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM workout_exercise_logs wel
      WHERE wel.workout_session_id = ws.id
        AND wel.routine_exercise_id IS NOT NULL
  );

-- Copia de titulos para las sesiones existentes.
UPDATE workout_sessions ws
SET routine_title = r.title
FROM routines r
WHERE r.id = ws.routine_id
  AND ws.routine_title IS NULL;

UPDATE workout_sessions ws
SET day_title = rd.title
FROM routine_days rd
WHERE rd.id = ws.routine_day_id
  AND ws.day_title IS NULL;
