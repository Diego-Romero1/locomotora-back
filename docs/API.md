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
| `GET`    | `/today?tz=`                        | Pantalla Hoy: rutina actual, dia que toca, semana, racha        |
| `GET`    | `/routine`                          | Legado: ultima rutina activa modificada. La app usa `/today`    |
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
| `GET`    | `/exercises/last-performance?ids=`  | La vez pasada y la mejor serie de cada ejercicio                |
| `POST`   | `/workout-sessions`                 | Guarda una sesion completa, una fila por serie (idempotente)    |
| `POST`   | `/workout-log`                      | Legado: un registro por ejercicio. La app ya no lo usa          |
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

Cada ejercicio incluye `equipment` (codigo del catalogo, por ejemplo `BARBELL` o `DUMBBELL`) desde la Fase 2.

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

### `POST /workout-sessions`

Guarda un entrenamiento terminado en un solo envio. Lo usa la app desde la Fase 1.

```json
{
  "clientSessionId": "uuid generado en el telefono",
  "routineId": "... (opcional: sin rutina = sesion libre)",
  "routineDayId": "... (opcional: si falta, primer dia)",
  "startedAt": "2026-11-11T18:02:00Z",
  "completedAt": "2026-11-11T18:56:00Z",
  "perceivedExertion": 8,
  "notes": "Buena sesion",
  "exercises": [
    {
      "routineExerciseId": "... (opcional)",
      "exerciseId": "...",
      "position": 1,
      "notes": null,
      "sets": [
        { "setNumber": 1, "reps": 10, "weightKg": 80, "completed": true },
        { "setNumber": 2, "reps": 10, "weightKg": 82.5, "completed": true }
      ]
    }
  ]
}
```

Respuesta (`201` si se creo, `200` si ya existia):

```json
{
  "id": "...", "clientSessionId": "...", "kind": "ROUTINE",
  "routineId": "...", "routineDayId": "...", "routineTitle": "...", "dayTitle": "Lower A",
  "startedAt": "...", "completedAt": "...", "durationSeconds": 3240,
  "completedSets": 13, "totalVolumeKg": 12797.50
}
```

Comportamiento:

- **Idempotente** por `(usuario, clientSessionId)`: reenviar devuelve la sesion ya guardada (`200`) sin duplicar. Dos envios simultaneos tambien terminan en una sola sesion (`ON CONFLICT DO NOTHING`).
- Inserta **una fila por serie** en `workout_exercise_logs`, con `exercise_position` y una copia del plan del momento (`target_reps_min`, `target_reps_max`, `target_weight_kg`, `rest_seconds`).
- La rutina debe ser del usuario; puede estar borrada (soft delete), para que una sesion que esperaba en la cola del telefono se pueda guardar igual. Si no es del usuario: 404.
- El dia debe pertenecer a la rutina (si no, 400). `routineDayId` sin `routineId`: 400.
- Si `routineExerciseId` ya no esta en la rutina (se edito mientras la sesion esperaba) o no coincide con `exerciseId`, la serie se guarda igual sin vinculo al plan. `exerciseId` desconocido: 400.
- 400 tambien si: no hay series con `completed = true`, hay `setNumber` repetido en un ejercicio, `completedAt < startedAt`, la sesion dura mas de 12 h o `completedAt` esta mas de 10 min en el futuro.
- Guarda `kind` (`ROUTINE` con rutina, `FREE` sin rutina) y copia `routine_title` y `day_title`.
- La duracion es `completedAt − startedAt`. La app corre `startedAt` para descontar las pausas.

### `GET /today?tz=America/Argentina/Buenos_Aires`

```json
{
  "routine": { "...": "RoutineResponse" },
  "nextDayId": "id de routine_days",
  "week": { "weekStart": "2026-11-09", "trainedDates": ["2026-11-09", "2026-11-11"], "targetDays": 4 },
  "weekStreak": 5,
  "lastSession": { "...": "WorkoutSessionResponse" }
}
```

- **Rutina actual:** la mas reciente entre la ultima entrenada (si sigue activa) y la ultima creada o editada. Armar o editar una rutina la vuelve la de Hoy; si no, sigue la que se viene entrenando. `null` si no hay rutinas.
- **Dia que toca:** el siguiente, por `day_index`, al ultimo dia entrenado de esa rutina. Despues del ultimo vuelve al primero. Si nunca se entreno o el dia se borro, el primero.
- **Semana:** lunes a domingo en la zona horaria `tz` (IANA; por defecto UTC; desconocida: 400). `trainedDates` son los dias con al menos un entrenamiento; los `QUICK_LOG` no cuentan.
- **`targetDays`:** `days_per_week` de la rutina; si no tiene, la cantidad de dias; si no hay rutina, `training_days_per_week` del perfil; si no, 3.
- **`weekStreak`:** semanas seguidas con al menos `targetDays` dias entrenados. La semana actual suma si ya se cumplio y no corta la racha mientras esta en curso. Usa el objetivo actual para todas las semanas.
- **`lastSession`:** el ultimo entrenamiento (sin `QUICK_LOG`), con el mismo formato que la respuesta de `POST /workout-sessions`.

### `GET /exercises/last-performance?ids=a,b,c`

Hasta 100 ids (si no, 400; id mal formado: 400). Devuelve uno por id, en el orden pedido y sin repetidos:

```json
[
  {
    "exerciseId": "...",
    "lastSession": {
      "sessionId": "...", "completedAt": "2026-11-11T19:00:00Z",
      "sets": [ { "setNumber": 1, "reps": 5, "weightKg": 70.00 } ]
    },
    "bestSet": { "reps": 5, "weightKg": 70.00, "estimatedOneRepMaxKg": 81.7, "completedAt": "..." }
  },
  { "exerciseId": "...", "lastSession": null, "bestSet": null }
]
```

- `lastSession`: las series de la ultima sesion en la que aparece el ejercicio (incluye registros sueltos).
- `bestSet`: la serie con mayor fuerza estimada de todo el historial. Epley: `peso × (1 + reps / 30)`; con 1 rep, el peso levantado.

### `POST /workout-log` (legado)

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

Se mantiene para versiones viejas de la app. Se borra cuando ninguna lo use.

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
