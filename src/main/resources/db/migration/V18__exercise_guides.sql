-- "Detalles con video" (guia de tecnica): consejos generados con IA y el video corto de YouTube elegido
-- para cada ejercicio. Es contenido general del ejercicio (no del usuario), asi que se genera una vez y
-- lo reutilizan todos: ahorra cuota de la IA y de la YouTube Data API.
CREATE TABLE exercise_guides (
    exercise_id            uuid PRIMARY KEY REFERENCES exercises(id) ON DELETE CASCADE,
    language               varchar(8)  NOT NULL DEFAULT 'es',
    tips                   jsonb,
    tips_model             varchar(80),
    tips_generated_at      timestamptz,
    -- PENDING: todavia no se busco (o no se pudo); FOUND: hay video; NOT_FOUND: ninguno era este ejercicio.
    video_status           varchar(20) NOT NULL DEFAULT 'PENDING',
    video_youtube_id       varchar(20),
    video_title            text,
    video_channel          text,
    video_duration_seconds integer,
    video_checked_at       timestamptz,
    updated_at             timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT exercise_guides_video_status_check CHECK (video_status IN ('PENDING', 'FOUND', 'NOT_FOUND')),
    CONSTRAINT exercise_guides_tips_check CHECK (tips IS NULL OR jsonb_typeof(tips) = 'object')
);
