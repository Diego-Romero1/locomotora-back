# Locomotora API

Actualizado: 2026-10-09

## Ejecucion local

Backend (API + Postgres en Docker):

```bash
cd locomotora-backend
docker compose up --build -d
```

Frontend:

```bash
cd locomotora-frontend
npm run start
```

Si no se define `EXPO_PUBLIC_API_URL`, el cliente usa `http://localhost:8080` en web y, en Expo Go, intenta detectar la IP LAN del host de Metro (`src/api/client.ts`). Para forzar una URL:

```bash
EXPO_PUBLIC_API_URL=http://TU_IP_LAN:8080 npm run start
# Android emulator
EXPO_PUBLIC_API_URL=http://10.0.2.2:8080 npm run android
```

Tambien se aceptan URLs de ngrok (`https://*.ngrok-free.app`).

## Convenciones

- Endpoints privados: `Authorization: Bearer <token>`.
- Publicos: `POST /login`, `POST /auth/register`, `GET /actuator/health`.
- Errores: `{ "timestamp": "...", "status": 400, "message": "..." }`.
- IDs: UUID en formato string.
- Fechas: ISO-8601 (`2026-10-09T18:02:00Z`).

## Indice de endpoints

| Metodo   | Ruta                                | Descripcion                                                     |
| -------- | ----------------------------------- | --------------------------------------------------------------- |
| `POST`   | `/auth/register`                    | Crea usuario + perfil vacio y devuelve token                    |
| `POST`   | `/login`                            | Login                                                           |
| `GET`    | `/me`                               | Usuario + perfil                                                |
| `GET`    | `/profile`                          | Perfil fisico                                                   |
| `PUT`    | `/profile`                          | Actualiza perfil                                                |
| `PUT`    | `/profile/photo`                    | Guarda foto de perfil: `{ profilePhotoUrl }`                    |
| `DELETE` | `/profile/photo`                    | Elimina foto de perfil                                          |
| `GET`    | `/profile/onboarding`               | Respuestas del onboarding                                       |
| `PUT`    | `/profile/onboarding`               | Guarda onboarding (objetivo, nivel, dias, equipo, limitaciones) |
| `GET`    | `/goals`                            | Objetivos                                                       |
| `PUT`    | `/goals`                            | Reemplazo total de objetivos                                    |
| `GET`    | `/routine`                          | "Rutina de hoy": ultima rutina activa modificada                |
| `GET`    | `/routines?categoryId=`             | Rutinas del usuario (filtro opcional por categoria)             |
| `GET`    | `/routines/{id}`                    | Rutina del usuario                                              |
| `POST`   | `/routines`                         | Crea rutina (multi-dia)                                         |
| `PUT`    | `/routines/{id}`                    | Actualiza rutina (sincroniza dias y ejercicios por id)          |
| `DELETE` | `/routines/{id}`                    | Soft delete (`is_active = false`)                               |
| `GET`    | `/routine-categories`               | Categorias (Fuerza, Hipertrofia, Cardio, Movilidad, Recovery)   |
| `GET`    | `/routines/templates?goal=`         | Plantillas globales                                             |
| `GET`    | `/routines/templates/{id}`          | Detalle de plantilla                                            |
| `GET`    | `/routines/recommendation`          | Plantilla recomendada segun perfil                              |
| `POST`   | `/routines/templates/{id}/use`      | Clona una plantilla a la cuenta del usuario                     |
| `POST`   | `/routines/recommendation/use`      | Clona la plantilla recomendada                                  |
| `GET`    | `/exercises`                        | Catalogo de ejercicios activos                                  |
| `POST`   | `/exercises/{id}/logs`              | Registro suelto de un ejercicio (fuera de rutina)               |
| `POST`   | `/workout-log`                      | Registra un entrenamiento completado                            |
| `GET`    | `/progress/summary`                 | Resumen de progreso                                             |
| `GET`    | `/progress/activity`                | Ultimas 10 sesiones con sus ejercicios                          |
| `GET`    | `/metrics`                          | Ultimas 100 metricas corporales                                 |
| `POST`   | `/metrics`                          | Agrega metrica corporal                                         |
| `GET`    | `/nutrition?date=`                  | Entradas de nutricion del dia                                   |
| `POST`   | `/nutrition`                        | Agrega entrada                                                  |
| `GET`    | `/nutrition/summary?date=`          | Macros del dia                                                  |
| `POST`   | `/ai/chat`                          | Mensaje al coach (motor local por palabras clave)               |
| `GET`    | `/ai/conversations`                 | Conversaciones                                                  |
| `GET`    | `/ai/conversations/{id}/messages`   | Mensajes de una conversacion                                    |
| `GET`    | `/ai/recommendations`               | Ultimas 50 recomendaciones                                      |

## Autenticacion

### `POST /auth/register`

```json
{ "name": "Diego", "email": "diego@example.com", "password": "password123" }
```

### `POST /login`

```json
{ "email": "diego@example.com", "password": "password123" }
```

Respuesta de ambos:

```json
{ "token": "...", "user": { "id": "...", "name": "Diego", "email": "diego@example.com" } }
```

## Rutinas

### Crear / actualizar: `POST /routines`, `PUT /routines/{id}`

```json
{
  "title": "Torso / Pierna",
  "categoryId": "uuid opcional",
  "description": "Rutina principal",
  "objective": "BUILD_MUSCLE",
  "difficulty": "INTERMEDIATE",
  "estimatedDurationMinutes": 60,
  "daysPerWeek": 2,
  "days": [
    {
      "dayIndex": 1,
      "title": "Torso",
      "focus": "Pecho y espalda",
      "exercises": [
        {
          "exerciseId": "uuid del catalogo",
          "name": "Press de banca plano",
          "sets": 4,
          "reps": 8,
          "repsMin": 6,
          "repsMax": 8,
          "restSeconds": 120,
          "primaryMuscleGroup": "Pecho",
          "notes": null
        }
      ]
    }
  ]
}
```

Reglas (`RoutineService`):

- `title` obligatorio. `difficulty`: `BEGINNER`, `INTERMEDIATE` o `ADVANCED`.
- Se requiere al menos un dia con al menos un ejercicio. Si no se envia `days`, se acepta `exercises` (formato legado) y se crea un unico "Dia 1".
- Cada ejercicio: `name` obligatorio, `sets >= 1`, `restSeconds >= 0`, al menos uno de `reps`/`repsMin`/`repsMax`, y `repsMin <= repsMax`.
- Si `exerciseId` existe se usa; si no, se busca por nombre y, si no hay coincidencia, **se crea un ejercicio global** (ver H10 en `SYSTEM_OVERVIEW.md`).
- `PUT` sincroniza por `id`: un dia o ejercicio con `id` existente de esta rutina se actualiza (conserva la fila y su historial); sin `id`, con un `id` ajeno o repetido, se crea uno nuevo; lo que no viene en el payload se borra. Un ejercicio puede cambiar de dia conservando su `id`. `target_weight_kg` y `tempo` no viajan en el request y se conservan.
- Las plantillas globales no se pueden editar (400).

### Respuesta de rutina

```json
{
  "id": "...",
  "title": "Upper Lower Intermediate - 4 dias",
  "description": "...",
  "objective": "HYPERTROPHY",
  "difficulty": "INTERMEDIATE",
  "estimatedDurationMinutes": 70,
  "daysPerWeek": 4,
  "categoryId": "...",
  "categoryName": "Hipertrofia",
  "routineSplit": "UPPER_LOWER",
  "source": "TEMPLATE",
  "isTemplate": true,
  "templateKey": "UPPER_LOWER_INTERMEDIATE_4_DAYS",
  "days": [
    {
      "id": "...",
      "dayIndex": 1,
      "title": "Upper A",
      "focus": "Compuestos base de pecho y espalda.",
      "exercises": [
        {
          "id": "id de routine_exercises",
          "exerciseId": "id del catalogo",
          "name": "Press de banca plano",
          "sets": 4,
          "reps": 8,
          "repsMin": 6,
          "repsMax": 8,
          "restSeconds": 120,
          "primaryMuscleGroup": "Pecho",
          "notes": null
        }
      ]
    }
  ],
  "exercises": []
}
```

- `exercises` (raiz) es la lista del primer dia, por compatibilidad.
- `isTemplate` es `true` solo para plantillas globales. Una plantilla clonada conserva `source = TEMPLATE` pero `isTemplate = false` y es editable.
- `reps` se calcula como `target_reps`, o `repsMax`, o `repsMin`.

### Recomendacion

`GET /routines/recommendation` usa objetivo principal (`user_goals`), `experience_level`, dias por semana y duracion del perfil. Normaliza `BUILD_MUSCLE` a `HYPERTROPHY` y usa `user_preferences.preferred_training_style` como desempate.

## Entrenamientos

### `POST /workout-log`

```json
{
  "routineId": "...",
  "routineDayId": "id de routine_days (opcional)",
  "completedExerciseIds": ["id de routine_exercises"],
  "completedExercises": [
    { "routineExerciseId": "...", "reps": 10, "weightKg": 60 }
  ],
  "completedAt": "2026-10-09T19:10:00Z"
}
```

Respuesta: `{ "sessionId": "...", "completedExercises": 3, "completedAt": "..." }`

Comportamiento actual:

- La rutina debe ser del usuario y estar activa (no se puede registrar sobre una plantilla global).
- Crea una `workout_session` `COMPLETED` con `started_at = completed_at`.
- La sesion se asocia al dia de `routineDayId`. Si no viene, se usa el primer dia de la rutina. Si el dia no pertenece a la rutina o el id es invalido: 400.
- Si viene `completedExercises`, se usa; si no, `completedExerciseIds` con las reps/peso objetivo.
- Inserta **una fila por ejercicio** con `set_number = 1` (H3).

El reemplazo propuesto (`POST /workout-sessions`, con series e idempotencia) esta en `ROADMAP_RUTINAS.md`, Fase 2.

### `POST /exercises/{id}/logs`

Registro suelto de un ejercicio del catalogo, sin rutina:

```json
{ "weightKg": 40, "reps": 12, "completedAt": "2026-10-09T19:10:00Z" }
```

Crea una sesion sin rutina y una fila de log.

## Progreso

- `GET /progress/summary` → `{ completedWorkouts, activeRoutines, activeDaysLast30, latestMetric }`
- `GET /progress/activity` → ultimas 10 sesiones: `{ id, routineId, routineTitle, routineDayId, routineDayTitle, completedAt, exercises: [{ id, exerciseId, exerciseName, primaryMuscleGroup, reps, weightKg, completedAt }] }`
- `GET /metrics`, `POST /metrics` → `{ id, measuredAt, weightKg, bodyFatPercentage, waistCm, chestCm, hipCm, armCm, legCm, notes }`

## Nutricion

- `GET /nutrition?date=YYYY-MM-DD`
- `POST /nutrition` → `{ entryDate, mealType, description*, calories, proteinG, carbsG, fatG, fiberG, source, metadataJson }`
- `GET /nutrition/summary?date=YYYY-MM-DD`

## IA

- `POST /ai/chat` → `{ conversationId?, message }` → `{ conversationId, answer, recommendationId }`
- `GET /ai/conversations`, `GET /ai/conversations/{id}/messages`, `GET /ai/recommendations`

La respuesta la genera un motor local por palabras clave (`model = local-context-coach-v1`). No hay proveedor externo conectado. Conversaciones, mensajes y recomendaciones si se persisten.
