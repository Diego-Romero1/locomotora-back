-- Panel de admin ("Actividad de usuarios" en Perfil).
-- El rol ADMIN ya existe en users.role (V1) y se asigna a mano en la base; ninguna migracion lo asigna.

-- Ultima vez que la app estuvo abierta (ping de POST /activity/ping). NULL = no la abrio desde esta version.
ALTER TABLE users
    ADD COLUMN last_seen_at timestamptz;

-- Tiempo de uso de la app por usuario y dia local del usuario. Lo suma el servidor con los pings.
CREATE TABLE user_daily_activity (
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_date date NOT NULL,
    active_seconds integer NOT NULL DEFAULT 0,
    sessions integer NOT NULL DEFAULT 0,
    last_seen_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, activity_date),
    CONSTRAINT user_daily_activity_seconds_check CHECK (active_seconds BETWEEN 0 AND 86400),
    CONSTRAINT user_daily_activity_sessions_check CHECK (sessions >= 0)
);

CREATE INDEX idx_user_daily_activity_date ON user_daily_activity(activity_date);

-- Registro de lo que hace el admin (habilitar / inhabilitar cuentas).
CREATE TABLE admin_audit_log (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    target_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    action varchar(40) NOT NULL,
    reason varchar(300),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT admin_audit_log_action_check CHECK (action IN ('DISABLE_USER', 'ENABLE_USER'))
);

CREATE INDEX idx_admin_audit_log_target_created ON admin_audit_log(target_user_id, created_at DESC);
