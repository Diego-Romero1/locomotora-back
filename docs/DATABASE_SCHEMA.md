# Estructura de base de datos Postgres

Actualizado: 2026-10-09

## Principios

- Postgres es la fuente transaccional principal.
- Flyway debe versionar todos los cambios de esquema.
- Las tablas usan `uuid` como identificador publico.
- Las fechas de negocio usan `timestamptz` o `date` segun corresponda.
- Los datos flexibles de IA y nutricion usan `jsonb` cuando todavia no conviene congelar una estructura.
- Las eliminaciones de datos de usuario deberian ser logicas cuando afecten historiales; para rutinas se usa `is_active`.

## Modulos cubiertos

- Usuarios y autenticacion.
- Perfil fisico y objetivos.
- Catalogo de ejercicios.
- Rutinas personalizadas.
- Plantillas globales de rutinas y recomendaciones de hipertrofia.
- Sesiones de entrenamiento y logs por ejercicio.
- Metricas corporales y progreso.
- Nutricion.
- Conversaciones de IA y recomendaciones.

## Tablas principales

### `users`

Cuenta base del usuario.

Campos principales:

- `id`
- `email`
- `password_hash`
- `name`
- `role`
- `status`
- `created_at`
- `updated_at`

### `refresh_tokens`

Tokens renovables para sesiones moviles.

Campos principales:

- `id`
- `user_id`
- `token_hash`
- `expires_at`
- `revoked_at`
- `created_at`

### `user_profiles`

Perfil fisico actual del usuario.

Campos principales:

- `user_id`
- `birth_date`
- `sex`
- `height_cm`
- `activity_level`
- `experience_level`
- `training_days_per_week`
- `session_duration_minutes`
- `medical_notes`
- `injuries`
- `dietary_preferences`

### `user_goals`

Objetivos historicos y activos.

Ejemplos de objetivo:

- `lose_weight`
- `gain_muscle`
- `improve_strength`
- `improve_endurance`
- `general_fitness`

Campos principales:

- `id`
- `user_id`
- `goal_type`
- `target_value`
- `target_unit`
- `target_date`
- `priority`
- `status`

### `body_metrics`

Historial de metricas corporales.

Campos principales:

- `id`
- `user_id`
- `measured_at`
- `weight_kg`
- `body_fat_percentage`
- `waist_cm`
- `chest_cm`
- `hip_cm`
- `arm_cm`
- `leg_cm`
- `notes`

### `exercises`

Catalogo de ejercicios.

Campos principales:

- `id`
- `name`
- `description`
- `primary_muscle_group`
- `secondary_muscle_groups`
- `equipment`
- `difficulty`
- `movement_pattern`
- `instructions`
- `is_active`

### `routines`

Rutinas creadas manualmente, generadas por IA o publicadas como plantillas globales.

Campos principales:

- `id`
- `user_id`
- `title`
- `description`
- `objective`
- `source`
- `difficulty`
- `estimated_duration_minutes`
- `days_per_week`
- `template_key`
- `routine_split`
- `is_active`

Convenciones actuales:

- Rutina de usuario: `user_id` con UUID del usuario.
- Plantilla global: `user_id = NULL`, `source = 'TEMPLATE'`.
- Clon de plantilla: `user_id` del usuario, `source = 'TEMPLATE'`, `isTemplate = false` en la API.
- La unicidad funcional de plantillas la da `template_key`.

### `routine_days`

Dias o bloques dentro de una rutina.

Campos principales:

- `id`
- `routine_id`
- `day_index`
- `title`
- `focus`

### `routine_exercises`

Ejercicios asignados a cada dia de rutina.

Campos principales:

- `id`
- `routine_day_id`
- `exercise_id`
- `position`
- `sets`
- `reps_min`
- `reps_max`
- `target_reps`
- `target_weight_kg`
- `rest_seconds`
- `tempo`
- `notes`

## Migraciones aplicadas

| Version | Archivo                                  | Contenido                                                                                  |
| ------- | ---------------------------------------- | ------------------------------------------------------------------------------------------ |
| V1      | `V1__initial_schema.sql`                 | Esquema base (todas las tablas de este documento salvo las de V2–V3).                      |
| V2      | `V2__routine_categories.sql`             | `routine_categories` + `routines.category_id`. Seed: Fuerza, Hipertrofia, Cardio, Movilidad, Recovery. |
| V3      | `V3__user_onboarding.sql`                | `user_profiles.weight_kg`, `user_profiles.onboarding_completed`, `user_preferences`, `user_limitations`, `user_equipment`. |
| V4      | `V4__user_profile_photo.sql`             | `users.profile_photo_url`.                                                                 |
| V5      | `V5__seed_hypertrophy_exercises.sql`     | Columnas extra en `exercises` (`category`, `equipment`…), indice unico por `lower(name)`, 105 ejercicios. |
| V6      | `V6__routine_templates_hypertrophy.sql`  | `routines.user_id` opcional, `template_key`, `routine_split`. 4 ejercicios mas y 7 plantillas multi-dia. |
| V7      | `V7__normalize_exercise_catalog.sql`     | Unifica grupos musculares sin tilde (`Cuadriceps`, `Triceps`, `Biceps`, `Gluteos`) con la forma con tilde. |
| V8      | `V8__deferrable_routine_order_constraints.sql` | `UNIQUE (routine_id, day_index)` y `UNIQUE (routine_day_id, position)` pasan a `DEFERRABLE INITIALLY DEFERRED`, para reordenar dentro de una transaccion. |

### Tablas agregadas en V2 y V3

- `routine_categories`: `id`, `name`, `description`, `icon`, `color`.
- `user_preferences`: `user_id` (unico), `preferred_training_style`. Se usa como desempate en la recomendacion de plantillas.
- `user_equipment`: `user_id`, `equipment_name`. Se carga en el onboarding; todavia no filtra ejercicios.
- `user_limitations`: `user_id`, `limitation_type`, `notes`. Se carga en el onboarding; todavia no filtra ejercicios.

### Detalles del catalogo (V5 + V6)

- 109 ejercicios globales, agrupados por `primary_muscle_group` en espanol: Pecho, Espalda, Hombros, Biceps, Triceps, Cuadriceps, Isquiotibiales, Gluteos, Pantorrillas, Abdomen.
- V6 insertaba ejercicios sin tilde (`Cuadriceps`, `Triceps`); V7 los unifica con la forma de V5 (H8).
- `equipment` usa codigos: `BARBELL`, `DUMBBELL`, `MACHINE`, `CABLE`, `BODYWEIGHT`, `FREE_WEIGHT`, `SMITH_MACHINE`.
- Las plantillas usan `template_key` como `UPPER_LOWER_INTERMEDIATE_4_DAYS` y `objective = 'HYPERTROPHY'`.

## Comportamiento de FKs que afecta al historial

| FK                                              | Al borrar el padre |
| ----------------------------------------------- | ------------------ |
| `routine_days.routine_id → routines`            | `CASCADE`          |
| `routine_exercises.routine_day_id → routine_days` | `CASCADE`        |
| `workout_sessions.routine_id → routines`        | `SET NULL`         |
| `workout_sessions.routine_day_id → routine_days`| `SET NULL`         |
| `workout_exercise_logs.routine_exercise_id → routine_exercises` | `SET NULL` |
| `workout_exercise_logs.exercise_id → exercises` | sin accion (bloquea) |

Consecuencia: `PUT /routines/{id}` actualiza por id (tarea 1.B1), asi que editar conserva el vinculo de las sesiones con su dia y ejercicio. Solo se pierde (queda en `NULL`) para los dias o ejercicios que se quitan de la rutina; el `exercise_id` del log se conserva siempre.

Las restricciones `UNIQUE (routine_id, day_index)` y `UNIQUE (routine_day_id, position)` son diferidas (V8): se validan al commit, lo que permite intercambiar dias o posiciones. No se pueden usar como arbitro de `ON CONFLICT`.

### `workout_sessions`

Entrenamientos realizados.

Campos principales:

- `id`
- `user_id`
- `routine_id`
- `routine_day_id`
- `started_at`
- `completed_at`
- `status`
- `perceived_exertion`
- `notes`

### `workout_exercise_logs`

Detalle por ejercicio dentro de una sesion.

Campos principales:

- `id`
- `workout_session_id`
- `routine_exercise_id`
- `exercise_id`
- `set_number`
- `reps`
- `weight_kg`
- `duration_seconds`
- `distance_meters`
- `completed`
- `notes`

### `nutrition_entries`

Registros o consultas nutricionales.

Campos principales:

- `id`
- `user_id`
- `entry_date`
- `meal_type`
- `description`
- `calories`
- `protein_g`
- `carbs_g`
- `fat_g`
- `fiber_g`
- `source`
- `metadata`

### `ai_conversations`

Hilos de conversacion con IA.

Campos principales:

- `id`
- `user_id`
- `title`
- `context_snapshot`
- `created_at`
- `updated_at`

### `ai_messages`

Mensajes de usuario/asistente.

Campos principales:

- `id`
- `conversation_id`
- `role`
- `content`
- `model`
- `metadata`
- `created_at`

### `ai_recommendations`

Recomendaciones persistidas a partir de IA.

Campos principales:

- `id`
- `user_id`
- `conversation_id`
- `type`
- `title`
- `content`
- `status`
- `metadata`

## Indices recomendados

- `users.email` unico.
- `refresh_tokens.user_id`.
- `user_goals.user_id, status`.
- `body_metrics.user_id, measured_at desc`.
- `routines.user_id, is_active`.
- `routine_days.routine_id, day_index`.
- `routine_exercises.routine_day_id, position`.
- `workout_sessions.user_id, completed_at desc`.
- `workout_exercise_logs.workout_session_id`.
- `nutrition_entries.user_id, entry_date desc`.
- `ai_conversations.user_id, updated_at desc`.
- `ai_messages.conversation_id, created_at`.
- `ai_recommendations.user_id, status`.

## Pendiente para JPA

Crear entidades y relaciones para:

- `User`
- `RefreshToken`
- `UserProfile`
- `UserGoal`
- `BodyMetric`
- `Exercise`
- `Routine`
- `RoutineDay`
- `RoutineExercise`
- `WorkoutSession`
- `WorkoutExerciseLog`
- `NutritionEntry`
- `AiConversation`
- `AiMessage`
- `AiRecommendation`

Los enums pueden modelarse inicialmente como `String` con validacion de aplicacion o como enums JPA con `@Enumerated(EnumType.STRING)`.

## Migracion inicial

La migracion propuesta esta en:

`locomotora-backend/src/main/resources/db/migration/V1__initial_schema.sql`
