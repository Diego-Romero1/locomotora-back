-- Con los ejercicios propios (V10), borrar un usuario borra en cascada sus ejercicios y tambien
-- sus rutinas y sesiones, que los referencian. Si la verificacion de la FK corre antes de que la
-- cascada borre esas filas, el borrado falla. Diferida, se verifica al final de la transaccion,
-- cuando ya no queda ninguna referencia.
ALTER TABLE routine_exercises
    DROP CONSTRAINT routine_exercises_exercise_id_fkey,
    ADD CONSTRAINT routine_exercises_exercise_id_fkey
        FOREIGN KEY (exercise_id) REFERENCES exercises(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE workout_exercise_logs
    DROP CONSTRAINT workout_exercise_logs_exercise_id_fkey,
    ADD CONSTRAINT workout_exercise_logs_exercise_id_fkey
        FOREIGN KEY (exercise_id) REFERENCES exercises(id) DEFERRABLE INITIALLY DEFERRED;
