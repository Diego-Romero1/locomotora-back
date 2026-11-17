-- R2: rutina actual elegida por el usuario ("Usar como rutina actual").
-- NULL = la app la deduce como antes (la mas reciente entre la ultima entrenada y la ultima editada).
-- Borrar una rutina es un soft delete: el servicio limpia la columna; ON DELETE cubre el borrado real.
ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS current_routine_id uuid REFERENCES routines(id) ON DELETE SET NULL;
