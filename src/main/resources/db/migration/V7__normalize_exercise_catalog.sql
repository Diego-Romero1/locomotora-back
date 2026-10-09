-- V6 inserto ejercicios con grupos musculares sin tilde ('Cuadriceps', 'Triceps'),
-- distintos de los que usa V5 ('Cuádriceps', 'Tríceps'). Se unifican con la forma de V5.
UPDATE exercises AS target
SET primary_muscle_group = src.canonical,
    updated_at = now()
FROM (
    VALUES
        ('Cuadriceps', 'Cuádriceps'),
        ('Triceps', 'Tríceps'),
        ('Biceps', 'Bíceps'),
        ('Gluteos', 'Glúteos')
) AS src(variant, canonical)
WHERE target.primary_muscle_group = src.variant;
