-- Fase 5: coach IA con Claude.
-- Modelo elegido por el usuario para cada tarea (NULL = el modelo por defecto del servidor).
-- Tabla propia: user_preferences se borra y recrea en el onboarding.
CREATE TABLE user_ai_settings (
    user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    coach_model varchar(80),
    routine_model varchar(80),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- Una fila por llamada al modelo: sirve para limites diarios y para medir costo.
CREATE TABLE ai_usage (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feature varchar(20) NOT NULL,
    model varchar(80) NOT NULL,
    input_tokens integer NOT NULL DEFAULT 0,
    output_tokens integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ai_usage_feature_check CHECK (feature IN ('COACH', 'ROUTINE'))
);

CREATE INDEX idx_ai_usage_user_feature_created ON ai_usage(user_id, feature, created_at DESC);
