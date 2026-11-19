-- Videos del catalogo (V1 del plan "Videos del catalogo"): llenado diario, alternativas y reemplazo cuando
-- un video deja de cargar.

-- 1) Uso de IA del sistema: el llenado diario no pertenece a un usuario (user_id NULL, feature CATALOG) y
--    las busquedas de reemplazo de un usuario se cuentan aparte (VIDEO_SEARCH) para su limite diario.
ALTER TABLE ai_usage ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE ai_usage DROP CONSTRAINT ai_usage_feature_check;
ALTER TABLE ai_usage ADD CONSTRAINT ai_usage_feature_check
    CHECK (feature IN ('COACH', 'ROUTINE', 'CATALOG', 'VIDEO_SEARCH'));

-- 2) Video de cada ejercicio: alternativas ya elegidas (pasan a ser el video si el principal se cae),
--    videos que fallaron (no se vuelven a elegir), de donde salio y cuando se verifico por ultima vez.
--    Titulo, canal y duracion son datos de la API de YouTube: el refresco los actualiza cada 7 dias.
ALTER TABLE exercise_guides
    ADD COLUMN video_alternatives jsonb NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN video_failed_ids   jsonb NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN video_source       varchar(20),
    ADD COLUMN video_refreshed_at timestamptz,
    ADD CONSTRAINT exercise_guides_alternatives_check CHECK (jsonb_typeof(video_alternatives) = 'array'),
    ADD CONSTRAINT exercise_guides_failed_check CHECK (jsonb_typeof(video_failed_ids) = 'array'),
    ADD CONSTRAINT exercise_guides_source_check
        CHECK (video_source IS NULL OR video_source IN ('FILL', 'ON_DEMAND', 'REPLACEMENT', 'MIGRATION'));

-- 3) Cuota de la YouTube Data API usada por dia (el dia de YouTube: se renueva a medianoche del Pacifico).
--    La comparten el llenado, los reemplazos y el refresco.
CREATE TABLE youtube_quota_usage (
    day      date PRIMARY KEY,
    units    integer NOT NULL DEFAULT 0,
    searches integer NOT NULL DEFAULT 0
);
