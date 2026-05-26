CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email varchar(320) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    name varchar(160) NOT NULL,
    role varchar(40) NOT NULL DEFAULT 'USER',
    status varchar(40) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT users_role_check CHECK (role IN ('USER', 'ADMIN', 'COACH')),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED'))
);

CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash varchar(255) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE user_profiles (
    user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    birth_date date,
    sex varchar(40),
    height_cm numeric(5,2),
    activity_level varchar(40),
    experience_level varchar(40),
    training_days_per_week smallint,
    session_duration_minutes smallint,
    medical_notes text,
    injuries jsonb NOT NULL DEFAULT '[]'::jsonb,
    dietary_preferences jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT user_profiles_sex_check CHECK (sex IS NULL OR sex IN ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY')),
    CONSTRAINT user_profiles_activity_check CHECK (activity_level IS NULL OR activity_level IN ('SEDENTARY', 'LIGHT', 'MODERATE', 'ACTIVE', 'VERY_ACTIVE')),
    CONSTRAINT user_profiles_experience_check CHECK (experience_level IS NULL OR experience_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
);

CREATE TABLE user_goals (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_type varchar(80) NOT NULL,
    target_value numeric(10,2),
    target_unit varchar(40),
    target_date date,
    priority smallint NOT NULL DEFAULT 1,
    status varchar(40) NOT NULL DEFAULT 'ACTIVE',
    notes text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT user_goals_status_check CHECK (status IN ('ACTIVE', 'COMPLETED', 'PAUSED', 'CANCELLED'))
);

CREATE TABLE body_metrics (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    measured_at timestamptz NOT NULL DEFAULT now(),
    weight_kg numeric(6,2),
    body_fat_percentage numeric(5,2),
    waist_cm numeric(6,2),
    chest_cm numeric(6,2),
    hip_cm numeric(6,2),
    arm_cm numeric(6,2),
    leg_cm numeric(6,2),
    notes text,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE exercises (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(180) NOT NULL,
    description text,
    primary_muscle_group varchar(80),
    secondary_muscle_groups jsonb NOT NULL DEFAULT '[]'::jsonb,
    equipment varchar(100),
    difficulty varchar(40),
    movement_pattern varchar(80),
    instructions text,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT exercises_difficulty_check CHECK (difficulty IS NULL OR difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
);

CREATE TABLE routines (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title varchar(180) NOT NULL,
    description text,
    objective varchar(80),
    source varchar(40) NOT NULL DEFAULT 'MANUAL',
    difficulty varchar(40),
    estimated_duration_minutes smallint,
    days_per_week smallint,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT routines_source_check CHECK (source IN ('MANUAL', 'AI', 'TEMPLATE')),
    CONSTRAINT routines_difficulty_check CHECK (difficulty IS NULL OR difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
);

CREATE TABLE routine_days (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    routine_id uuid NOT NULL REFERENCES routines(id) ON DELETE CASCADE,
    day_index smallint NOT NULL,
    title varchar(160) NOT NULL,
    focus varchar(120),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT routine_days_unique_day UNIQUE (routine_id, day_index)
);

CREATE TABLE routine_exercises (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    routine_day_id uuid NOT NULL REFERENCES routine_days(id) ON DELETE CASCADE,
    exercise_id uuid NOT NULL REFERENCES exercises(id),
    position smallint NOT NULL,
    sets smallint NOT NULL,
    reps_min smallint,
    reps_max smallint,
    target_reps smallint,
    target_weight_kg numeric(6,2),
    rest_seconds integer,
    tempo varchar(40),
    notes text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT routine_exercises_unique_position UNIQUE (routine_day_id, position)
);

CREATE TABLE workout_sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    routine_id uuid REFERENCES routines(id) ON DELETE SET NULL,
    routine_day_id uuid REFERENCES routine_days(id) ON DELETE SET NULL,
    started_at timestamptz,
    completed_at timestamptz,
    status varchar(40) NOT NULL DEFAULT 'COMPLETED',
    perceived_exertion smallint,
    notes text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT workout_sessions_status_check CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED', 'CANCELLED')),
    CONSTRAINT workout_sessions_rpe_check CHECK (perceived_exertion IS NULL OR perceived_exertion BETWEEN 1 AND 10)
);

CREATE TABLE workout_exercise_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_session_id uuid NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
    routine_exercise_id uuid REFERENCES routine_exercises(id) ON DELETE SET NULL,
    exercise_id uuid NOT NULL REFERENCES exercises(id),
    set_number smallint,
    reps smallint,
    weight_kg numeric(6,2),
    duration_seconds integer,
    distance_meters numeric(8,2),
    completed boolean NOT NULL DEFAULT true,
    notes text,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE nutrition_entries (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entry_date date NOT NULL DEFAULT CURRENT_DATE,
    meal_type varchar(40),
    description text NOT NULL,
    calories numeric(8,2),
    protein_g numeric(8,2),
    carbs_g numeric(8,2),
    fat_g numeric(8,2),
    fiber_g numeric(8,2),
    source varchar(40) NOT NULL DEFAULT 'USER',
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT nutrition_entries_source_check CHECK (source IN ('USER', 'AI', 'IMPORT'))
);

CREATE TABLE ai_conversations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title varchar(180),
    context_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE ai_messages (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id uuid NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role varchar(40) NOT NULL,
    content text NOT NULL,
    model varchar(120),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ai_messages_role_check CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL'))
);

CREATE TABLE ai_recommendations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id uuid REFERENCES ai_conversations(id) ON DELETE SET NULL,
    type varchar(60) NOT NULL,
    title varchar(180) NOT NULL,
    content text NOT NULL,
    status varchar(40) NOT NULL DEFAULT 'PROPOSED',
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ai_recommendations_status_check CHECK (status IN ('PROPOSED', 'ACCEPTED', 'DISMISSED', 'APPLIED'))
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_user_goals_user_status ON user_goals(user_id, status);
CREATE INDEX idx_body_metrics_user_measured_at ON body_metrics(user_id, measured_at DESC);
CREATE INDEX idx_exercises_muscle_active ON exercises(primary_muscle_group, is_active);
CREATE INDEX idx_routines_user_active ON routines(user_id, is_active);
CREATE INDEX idx_routine_days_routine_day_index ON routine_days(routine_id, day_index);
CREATE INDEX idx_routine_exercises_day_position ON routine_exercises(routine_day_id, position);
CREATE INDEX idx_workout_sessions_user_completed_at ON workout_sessions(user_id, completed_at DESC);
CREATE INDEX idx_workout_exercise_logs_session ON workout_exercise_logs(workout_session_id);
CREATE INDEX idx_nutrition_entries_user_entry_date ON nutrition_entries(user_id, entry_date DESC);
CREATE INDEX idx_ai_conversations_user_updated_at ON ai_conversations(user_id, updated_at DESC);
CREATE INDEX idx_ai_messages_conversation_created_at ON ai_messages(conversation_id, created_at);
CREATE INDEX idx_ai_recommendations_user_status ON ai_recommendations(user_id, status);
