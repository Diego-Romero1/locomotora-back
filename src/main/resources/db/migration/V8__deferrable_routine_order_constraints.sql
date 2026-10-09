-- Al actualizar una rutina se conservan los ids de dias y ejercicios y solo se
-- cambian day_index/position. Para poder reordenar (por ejemplo intercambiar dos
-- dias) dentro de una transaccion, la unicidad se valida al hacer commit.
ALTER TABLE routine_days
    DROP CONSTRAINT routine_days_unique_day,
    ADD CONSTRAINT routine_days_unique_day UNIQUE (routine_id, day_index) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE routine_exercises
    DROP CONSTRAINT routine_exercises_unique_position,
    ADD CONSTRAINT routine_exercises_unique_position UNIQUE (routine_day_id, position) DEFERRABLE INITIALLY DEFERRED;
