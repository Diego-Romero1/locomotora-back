-- Borradores de rutinas (crear desde cero, personalizar una plantilla o de la IA, o editar una rutina).
-- La app los guarda primero en el telefono y los sube cuando hay conexion; el servidor guarda el
-- borrador tal cual lo arma la app (payload) mas un resumen para listarlos.
-- id lo genera el telefono: asi reintentar no duplica. Gana el borrador mas nuevo (client_updated_at).
CREATE TABLE routine_drafts (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind varchar(10) NOT NULL,
    -- EDIT: la rutina que se esta editando. Si se borra la rutina, el borrador ya no sirve.
    routine_id uuid REFERENCES routines(id) ON DELETE CASCADE,
    title varchar(180),
    step smallint NOT NULL DEFAULT 1,
    day_count smallint NOT NULL DEFAULT 0,
    exercise_count smallint NOT NULL DEFAULT 0,
    payload jsonb NOT NULL,
    client_updated_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT routine_drafts_kind_check CHECK (kind IN ('NEW', 'EDIT')),
    CONSTRAINT routine_drafts_edit_routine_check CHECK (kind = 'NEW' OR routine_id IS NOT NULL)
);

CREATE INDEX idx_routine_drafts_user_updated ON routine_drafts (user_id, client_updated_at DESC);
