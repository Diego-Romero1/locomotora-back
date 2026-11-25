-- K2 · Energia del entrenamiento (kcal estimadas por serie y por sesion).
-- V23 y V24 quedan reservadas para la exportacion de videos.

-- Hora en que se marco cada serie en el telefono (null en sesiones anteriores) y su gasto estimado.
ALTER TABLE workout_exercise_logs
    ADD COLUMN completed_at timestamptz,
    ADD COLUMN energy_kcal numeric(7,2);

-- planned_sets: series que tenia el plan de hoy (null en sesiones anteriores), para mostrar "5 de 15".
-- body_weight_kg: peso corporal usado en el calculo; se conserva al recalcular para que bajar de peso
-- no cambie las sesiones viejas. energy_model_version null = falta calcular.
ALTER TABLE workout_sessions
    ADD COLUMN planned_sets smallint,
    ADD COLUMN energy_kcal numeric(7,2),
    ADD COLUMN energy_work_kcal numeric(7,2),
    ADD COLUMN body_weight_kg numeric(6,2),
    ADD COLUMN body_weight_estimated boolean,
    ADD COLUMN energy_personalized boolean,
    ADD COLUMN energy_model_version smallint,
    ADD CONSTRAINT workout_sessions_planned_sets_check CHECK (planned_sets IS NULL OR planned_sets BETWEEN 0 AND 3000);
