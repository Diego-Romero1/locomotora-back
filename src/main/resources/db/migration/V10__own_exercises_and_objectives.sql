-- Fase 4 · Crear facil.
-- Ejercicios propios por usuario (H10) y vocabulario unico de objetivo (H9).

-- NULL = ejercicio global del catalogo; con valor = ejercicio propio que solo ve ese usuario.
ALTER TABLE exercises
    ADD COLUMN created_by_user_id uuid REFERENCES users(id) ON DELETE CASCADE;

-- El nombre era unico en todo el catalogo: dos usuarios no podian crear "Hip thrust con banda".
-- Ahora es unico entre los globales y, por separado, dentro de los propios de cada usuario.
DROP INDEX IF EXISTS idx_exercises_unique_name;
CREATE UNIQUE INDEX idx_exercises_unique_global_name
    ON exercises (lower(name)) WHERE created_by_user_id IS NULL;
CREATE UNIQUE INDEX idx_exercises_unique_own_name
    ON exercises (created_by_user_id, lower(name)) WHERE created_by_user_id IS NOT NULL;

-- Objetivo: las plantillas usaban HYPERTROPHY y la app BUILD_MUSCLE. Queda el de la app.
UPDATE routines
SET objective = 'BUILD_MUSCLE'
WHERE upper(trim(objective)) IN ('HYPERTROPHY', 'HIPERTROFIA', 'GAIN_MUSCLE');
