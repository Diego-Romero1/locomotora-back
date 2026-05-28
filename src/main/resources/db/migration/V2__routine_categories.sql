CREATE TABLE routine_categories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(120) NOT NULL,
    description text,
    icon varchar(80),
    color varchar(40),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE routines
    ADD COLUMN category_id uuid NULL REFERENCES routine_categories(id);

CREATE INDEX idx_routines_category_id ON routines(category_id);

INSERT INTO routine_categories (name, description, icon, color)
VALUES
    ('Fuerza', 'Rutinas enfocadas en fuerza maxima y progresion de carga.', 'barbell', '#22C55E'),
    ('Hipertrofia', 'Rutinas orientadas a volumen y crecimiento muscular.', 'layers', '#F97316'),
    ('Cardio', 'Rutinas para mejorar la capacidad cardiovascular.', 'heart', '#EF4444'),
    ('Movilidad', 'Rutinas de movilidad articular y rango de movimiento.', 'move', '#38BDF8'),
    ('Recovery', 'Rutinas suaves para recuperacion y descarga.', 'moon', '#94A3B8');