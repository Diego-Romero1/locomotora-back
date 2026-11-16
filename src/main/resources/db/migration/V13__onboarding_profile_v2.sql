-- Onboarding v2: datos de perfil que usan el recomendador y la IA, zonas que carga cada
-- ejercicio (para cuidar lesiones) y plantillas con nombres en espanol.

-- 1) Perfil: donde entrena y chequeo de salud.
--    health_flags: NULL = no respondio; [] = respondio que nada aplica.
ALTER TABLE user_profiles
    ADD COLUMN training_location varchar(40),
    ADD COLUMN health_flags jsonb,
    ADD CONSTRAINT user_profiles_training_location_check CHECK (
        training_location IS NULL
        OR training_location IN ('FULL_GYM', 'BASIC_GYM', 'HOME_EQUIPPED', 'HOME_NONE')
    ),
    ADD CONSTRAINT user_profiles_health_flags_check CHECK (
        health_flags IS NULL OR jsonb_typeof(health_flags) = 'array'
    );

-- 2) Zonas del cuerpo que un ejercicio carga de forma importante. Mismos codigos que
--    user_limitations.limitation_type. Es una guia conservadora para el catalogo global; los
--    ejercicios propios empiezan sin zonas.
ALTER TABLE exercises
    ADD COLUMN stress_zones jsonb NOT NULL DEFAULT '[]'::jsonb,
    ADD CONSTRAINT exercises_stress_zones_check CHECK (jsonb_typeof(stress_zones) = 'array');

CREATE FUNCTION pg_temp.add_zone(zone text, patterns text[]) RETURNS void AS $$
    UPDATE exercises
    SET stress_zones = stress_zones || to_jsonb(zone), updated_at = now()
    WHERE created_by_user_id IS NULL
      AND NOT stress_zones ? zone
      AND lower(name) LIKE ANY (patterns);
$$ LANGUAGE sql;

SELECT pg_temp.add_zone('KNEE', ARRAY[
    'sentadilla%', '%squat%', 'zancada%', 'extensión de piernas', 'prensa%']);
SELECT pg_temp.add_zone('LOWER_BACK', ARRAY[
    'peso muerto%', 'buenos días', 'remo con barra', 'remo pendlay', 'sentadilla trasera', 'sentadilla frontal']);
SELECT pg_temp.add_zone('SHOULDER', ARRAY[
    'press militar', 'press arnold', 'press con mancuernas sentado', 'shoulder press machine', 'fondos%',
    'press de banca%', 'pullover%', 'dominadas lastradas', 'elevaciones frontales', 'elevación frontal en polea',
    'flexiones declinadas']);
SELECT pg_temp.add_zone('ELBOW', ARRAY[
    'press francés', 'extensión de tríceps con mancuerna sobre la cabeza', 'extensión overhead en polea', 'fondos%',
    'press de banca cerrado', 'curl con barra']);
SELECT pg_temp.add_zone('WRIST', ARRAY[
    'flexiones%', 'curl con barra', 'sentadilla frontal', 'press de banca cerrado', 'fondos en paralelas']);
SELECT pg_temp.add_zone('NECK', ARRAY['encogimientos%', 'crunch']);
SELECT pg_temp.add_zone('HIP', ARRAY[
    'sentadilla sumo', 'peso muerto sumo', 'abductor machine', 'bulgarian split squat', 'zancada%',
    'elevaciones de piernas']);
SELECT pg_temp.add_zone('ANKLE', ARRAY[
    'zancada%', 'bulgarian split squat', '%calf raise%', 'elevación de talones%']);

-- 3) Plantillas. Algunas bases tienen copias sin template_key de plantillas que si lo tienen:
--    se desactivan para que el recomendador y la lista no muestren duplicados.
UPDATE routines dup
SET is_active = false, updated_at = now()
WHERE dup.source = 'TEMPLATE' AND dup.user_id IS NULL AND dup.template_key IS NULL AND dup.is_active
  AND EXISTS (
      SELECT 1 FROM routines keyed
      WHERE keyed.source = 'TEMPLATE' AND keyed.user_id IS NULL AND keyed.template_key IS NOT NULL
        AND keyed.title = dup.title
  );

CREATE TEMP TABLE template_names (old_title text PRIMARY KEY, new_title text, new_description text);
INSERT INTO template_names VALUES
    ('Push Pull Legs Intermediate - 3 dias', 'Empuje / Tirón / Pierna · Intermedio · 3 días',
     'Hipertrofia con empuje, tirón y pierna en tres días por semana.'),
    ('Full Body Beginner - 3 dias', 'Cuerpo completo · Principiante · 3 días',
     'Hipertrofia en gimnasio con tres sesiones de cuerpo completo, poco volumen y foco en la técnica.'),
    ('Upper Lower Beginner - 4 dias', 'Torso / Pierna · Principiante · 4 días',
     'Hipertrofia torso/pierna para principiantes: dos días de tren superior y dos de tren inferior.'),
    ('Upper Lower Intermediate - 4 dias', 'Torso / Pierna · Intermedio · 4 días',
     'Hipertrofia torso/pierna con más volumen en ejercicios compuestos y accesorios.'),
    ('Bro Split Advanced - 5 dias', 'Un grupo por día · Avanzado · 5 días',
     'Hipertrofia avanzada en gimnasio, un grupo muscular por día.'),
    ('PPL Upper Lower Intermediate - 5 dias', 'Empuje / Tirón / Pierna + Torso / Pierna · Intermedio · 5 días',
     'Hipertrofia que combina empuje, tirón y pierna con dos días de torso y pierna.'),
    ('Push Pull Legs Intermediate - 6 dias', 'Empuje / Tirón / Pierna · Intermedio · 6 días',
     'Hipertrofia de alta frecuencia: empuje, tirón y pierna dos veces por semana.');

UPDATE routine_days d
SET title = CASE
        WHEN d.title ~ '^Full Body' THEN regexp_replace(d.title, '^Full Body', 'Cuerpo completo')
        WHEN d.title ~ '^Upper' THEN regexp_replace(d.title, '^Upper', 'Torso')
        WHEN d.title ~ '^Lower' THEN regexp_replace(d.title, '^Lower', 'Pierna')
        WHEN d.title ~ '^Push' THEN regexp_replace(d.title, '^Push', 'Empuje')
        WHEN d.title ~ '^Pull' THEN regexp_replace(d.title, '^Pull', 'Tirón')
        WHEN d.title ~ '^Legs' THEN regexp_replace(d.title, '^Legs', 'Piernas')
        ELSE d.title
    END,
    focus = replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(
            replace(replace(replace(replace(replace(replace(d.focus,
            'tecnica', 'técnica'), 'traccion', 'tracción'), 'Traccion', 'Tracción'), 'Patron', 'Patrón'),
            'gluteo', 'glúteo'), 'cuadriceps', 'cuádriceps'), 'biceps', 'bíceps'), 'Biceps', 'Bíceps'),
            'triceps', 'tríceps'), 'Variacion', 'Variación'), 'maquina', 'máquina'), 'enfasis', 'énfasis'),
            'basica', 'básica'), 'estimulo upper', 'estímulo de torso'), 'estimulo', 'estímulo'),
            'Upper mixto', 'Torso mixto'), 'Lower mixto', 'Tren inferior mixto'), 'Segundo pull', 'Segundo tirón'), 'Gluteo', 'Glúteo')
FROM routines r
WHERE d.routine_id = r.id AND r.source = 'TEMPLATE' AND r.user_id IS NULL;

UPDATE routines r
SET title = n.new_title, description = n.new_description, updated_at = now()
FROM template_names n
WHERE r.source = 'TEMPLATE' AND r.user_id IS NULL AND r.title = n.old_title;
