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
- Publicos: `POST /login`, `POST /auth/register`, `POST /auth/google`, `GET /actuator/health`.
- Errores: `{ "timestamp": "...", "status": 400, "message": "..." }`.
- IDs: UUID en formato string.
- Fechas: ISO-8601 (`2026-10-09T18:02:00Z`).

## Indice de endpoints

| Metodo   | Ruta                                | Descripcion                                                     |
| -------- | ----------------------------------- | --------------------------------------------------------------- |
| `POST`   | `/auth/register`                    | Crea usuario + perfil vacio y devuelve token                    |
| `POST`   | `/login`                            | Login                                                           |
| `POST`   | `/auth/google`                      | Entra o crea la cuenta con un ID token de Google                |
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
| `POST`   | `/routines`                         | Crea rutina (multi-dia). `source`: MANUAL, AI o TEMPLATE        |
| `POST`   | `/routines/{id}/duplicate?suffix=`  | Copia una rutina propia con todos sus dias                      |
| `PUT`    | `/routines/{id}`                    | Actualiza rutina (sincroniza dias y ejercicios por id)          |
| `DELETE` | `/routines/{id}`                    | Soft delete (`is_active = false`)                               |
| `PUT`    | `/routines/{id}/current`            | R2: elige la rutina actual (la de Hoy). Devuelve la rutina      |
| `GET`    | `/routine-categories`               | Categorias (Fuerza, Hipertrofia, Cardio, Movilidad, Recovery)   |
| `GET`    | `/routines/templates?goal=`         | Plantillas globales                                             |
| `GET`    | `/routines/templates/{id}`          | Detalle de plantilla                                            |
| `GET`    | `/routines/recommendation`          | Plantilla recomendada segun perfil                              |
| `POST`   | `/routines/templates/{id}/use`      | Clona una plantilla a la cuenta del usuario                     |
| `POST`   | `/routines/recommendation/use`      | Clona la plantilla recomendada                                  |
| `GET`    | `/exercises`                        | Catalogo liviano: global + propios del usuario (`custom`), ETag |
| `GET`    | `/exercises/{id}`                   | Detalle de un ejercicio visible, con descripcion y tecnica      |
| `GET`    | `/exercises/{id}/guide`             | "Detalles con video": consejos de tecnica con IA y video corto  |
| `POST`   | `/exercises/{id}/guide/video-unavailable` | El video no cargo: verifica y pasa a otro                 |
| `POST`   | `/exercises`                        | Crea un ejercicio propio (solo lo ve quien lo crea)             |
| `POST`   | `/exercises/{id}/logs`              | Registro suelto de un ejercicio (fuera de rutina)               |
| `GET`    | `/exercises/last-performance?ids=`  | La vez pasada y la mejor serie de cada ejercicio                |
| `POST`   | `/workout-sessions`                 | Guarda una sesion completa, una fila por serie (idempotente)    |
| `POST`   | `/workout-log`                      | Legado: un registro por ejercicio. La app ya no lo usa          |
| `GET`    | `/progress/summary`                 | Resumen de progreso                                             |
| `GET`    | `/progress/activity`                | Legado: ultimas 10 sesiones. La app usa `/workout-sessions`     |
| `GET`    | `/workout-sessions?cursor=&limit=`  | Historial paginado (sin registros sueltos)                      |
| `GET`    | `/workout-sessions/{id}`            | Detalle por serie, plan del momento y records                   |
| `PUT`    | `/workout-sessions/{id}`            | Corrige series, esfuerzo y notas                                |
| `DELETE` | `/workout-sessions/{id}`            | Borra la sesion con sus series                                  |
| `GET`    | `/progress/exercises`               | Ejercicios entrenados con su mejor fuerza estimada              |
| `GET`    | `/exercises/{id}/history?limit=`    | Progreso de un ejercicio: puntos por sesion y records           |
| `GET`    | `/progress/weekly?tz=&weeks=`       | Totales por semana (sesiones, dias, series, volumen)            |
| `GET`    | `/metrics`                          | Ultimas 100 metricas corporales                                 |
| `POST`   | `/metrics`                          | Agrega metrica corporal                                         |
| `GET`    | `/nutrition?date=`                  | Entradas de nutricion del dia                                   |
| `POST`   | `/nutrition`                        | Agrega entrada                                                  |
| `GET`    | `/nutrition/summary?date=`          | Macros del dia                                                  |
| `POST`   | `/ai/chat`                          | Mensaje al agente IA (Qwen en Groq, con herramientas)           |
| `GET`    | `/ai/models`                        | Modelo de IA en uso, si se puede elegir y uso del dia           |
| `PUT`    | `/ai/preferences`                   | Elige el modelo del coach y el de rutinas                       |
| `POST`   | `/routines/generate`                | Borrador de rutina con IA, JSON estricto (no se guarda)         |
| `GET`    | `/ai/conversations`                 | Conversaciones (las 50 mas recientes)                           |
| `DELETE` | `/ai/conversations/{id}`            | Borra una conversacion propia con sus mensajes (204/404)        |
| `GET`    | `/ai/conversations/{id}/messages`   | Mensajes de una conversacion                                    |
| `GET`    | `/ai/recommendations`               | Ultimas 50 recomendaciones                                      |
| `POST`   | `/activity/ping?tz=`                | La app esta abierta: suma tiempo de uso (204)                   |
| `GET`    | `/admin/stats?tz=`                  | Admin: cifras de usuarios, uso y entrenamientos                 |
| `GET`    | `/admin/users?q=&status=&page=&tz=` | Admin: lista de usuarios con su actividad                       |
| `GET`    | `/admin/users/{id}?tz=`             | Admin: detalle de actividad de un usuario                       |
| `PATCH`  | `/admin/users/{id}/status`          | Admin: habilita o inhabilita una cuenta                         |

## Autenticacion

### `POST /auth/register`

```json
{ "name": "Diego", "email": "diego@example.com", "password": "password123" }
```

### `POST /login`

```json
{ "email": "diego@example.com", "password": "password123" }
```

### `POST /auth/google`

```json
{ "idToken": "<ID token que la app obtiene de Google Sign-In>" }
```

- Se verifica la firma con las claves publicas de Google, el emisor (`accounts.google.com`), el vencimiento, la audiencia (uno de `LOCOMOTORA_GOOGLE_CLIENT_IDS`) y `email_verified = true`.
- Busca primero la cuenta vinculada (`users.google_subject`). Si no hay, busca por email y la vincula. Si tampoco hay, crea la cuenta sin contrasena, con el nombre y la foto de Google y el perfil vacio (pasa por el onboarding).
- Errores: `401` token invalido, email sin verificar o cuenta no activa; `409` el email ya esta vinculado a otra cuenta de Google; `503` sin `LOCOMOTORA_GOOGLE_CLIENT_IDS`.
- Una cuenta creada con Google que intenta `POST /login` recibe `401` "This account signs in with Google".
- Cuenta inhabilitada por un admin: `POST /login` (con la contrasena correcta) y `POST /auth/google` responden `403` "Account is disabled". Con una contrasena incorrecta sigue siendo `401`, asi no se revela el estado de la cuenta.

Respuesta de los tres:

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
  "source": "MANUAL",
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
- `objective` (Fase 4, H9): `BUILD_MUSCLE`, `IMPROVE_STRENGTH`, `LOSE_WEIGHT`, `IMPROVE_ENDURANCE` o `GENERAL_FITNESS`. Se aceptan los alias viejos (`HYPERTROPHY`, `HIPERTROFIA`, `GAIN_MUSCLE` → `BUILD_MUSCLE`). Cualquier otro valor: 400. Es el mismo vocabulario del onboarding y de `/routines/templates?goal=`.
- `source` (solo al crear): `MANUAL` (por defecto), `AI` o `TEMPLATE`. Al editar se ignora.
- Se requiere al menos un dia con al menos un ejercicio. Si no se envia `days`, se acepta `exercises` (formato legado) y se crea un unico "Dia 1".
- Cada ejercicio: `name` obligatorio, `sets >= 1`, `restSeconds >= 0`, al menos uno de `reps`/`repsMin`/`repsMax`, y `repsMin <= repsMax`.
- Si `exerciseId` es de un ejercicio que el usuario ve (global o propio) se usa; si no, se busca por nombre entre esos y, si no hay coincidencia, **se crea un ejercicio propio** del usuario (Fase 4, H10). Ya no se crean ejercicios globales desde una rutina.
- `PUT` sincroniza por `id`: un dia o ejercicio con `id` existente de esta rutina se actualiza (conserva la fila y su historial); sin `id`, con un `id` ajeno o repetido, se crea uno nuevo; lo que no viene en el payload se borra. Un ejercicio puede cambiar de dia conservando su `id`. `target_weight_kg` y `tempo` no viajan en el request y se conservan.
- Las plantillas globales no se pueden editar (400).

### Rutina actual y ciclo de dias (R2)

- `current`, `lastTrainedAt` y `nextDayId` (y `lastTrainedAt` por dia) vienen en `GET /routines`, `GET /routines/{id}`, las respuestas de crear, editar y duplicar, y en la rutina de `GET /today`. Las plantillas no los traen (`current = false`, nulls).
- **Rutina actual:** la elegida con `PUT /routines/{id}/current` (`user_profiles.current_routine_id`, V15). Si no hay una elegida, o se borro, se deduce como antes: la mas reciente entre la ultima entrenada y la ultima creada o editada. `GET /today`, la lista y el detalle usan la misma regla. `PUT` sobre una rutina ajena o borrada: 404 y no guarda nada.
- **Ciclo:** `nextDayId` es el dia que sigue (por `dayIndex`) al ultimo entrenado de esa rutina; si nunca se entreno, el primero; despues del ultimo, vuelve al primero.

### `POST /routines/{id}/duplicate?suffix=(copia)`

Copia una rutina propia (activa) con sus dias y ejercicios, sin compartir ids. El titulo lleva el sufijo (por defecto ` (copia)`), recortado para no pasar 180 caracteres. Conserva rangos de reps y el origen (`AI` o `TEMPLATE`; si no, `MANUAL`). 201 con la rutina nueva; 404 si no es del usuario.

### Ejercicios propios: `GET /exercises`, `POST /exercises`

- `GET /exercises` devuelve el catalogo global (~890 ejercicios) mas los ejercicios propios del usuario, sin `description` ni `instructions` (vienen en `null`). Cada item trae `custom`, `stressZones`, `muscleGroup` (grupo para navegar: Pecho, Espalda, Hombros, Bíceps, Tríceps, Antebrazos, Abdomen, Glúteos, Piernas, Estiramientos o Cardio y pliometría), `secondaryMuscles`, `nameEn`, `exerciseType` (STRENGTH, STRETCHING, CARDIO, PLYOMETRICS, POWERLIFTING, OLYMPIC, STRONGMAN), `mechanic` (COMPOUND, ISOLATION o null), `core` (ejercicio habitual; los propios cuentan como core) y `thumbUrl` (null hasta tener imagenes). `primaryMuscleGroup` es uno de los 16 musculos: Pecho, Espalda, Lumbares, Trapecio, Hombros, Cuello, Bíceps, Tríceps, Antebrazos, Abdomen, Glúteos, Cuádriceps, Isquiotibiales, Pantorrillas, Aductores, Abductores. La respuesta lleva un ETag debil y va comprimida con gzip si el cliente lo acepta; con `If-None-Match` igual responde 304.
- `GET /exercises/{id}` devuelve el mismo item con `description` e `instructions` (pasos numerados en los ejercicios nuevos). 404 si no existe o es un ejercicio propio de otro usuario.
- `POST /exercises` con `{ "name": "Remo con banda", "primaryMuscleGroup": "Espalda", "equipment": "BAND" }` crea un ejercicio que solo ve ese usuario. `equipment` opcional: `BARBELL`, `DUMBBELL`, `MACHINE`, `CABLE`, `BODYWEIGHT`, `FREE_WEIGHT`, `SMITH_MACHINE`, `EZ_BAR`, `BAND`, `KETTLEBELL` u `OTHER`. Si el usuario ya ve un ejercicio con ese nombre (sin distinguir mayusculas ni espacios repetidos), responde 200 con ese en vez de duplicarlo; si lo crea, 201.
- Un ejercicio propio de otro usuario no se puede usar en rutinas ni en sesiones: `POST /workout-sessions` responde 400.

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
          "notes": null,
          "equipment": "BARBELL",
          "instructions": "Tecnica del catalogo (R1: la app la muestra en \"Tecnica\" al entrenar, aun sin conexion)"
        }
      ],
      "lastTrainedAt": "2026-11-11T19:00:00Z"
    }
  ],
  "exercises": [],
  "current": true,
  "lastTrainedAt": "2026-11-11T19:00:00Z",
  "nextDayId": "id del dia que toca"
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

## Historial y progreso (Fase 3)

Todos filtran por el usuario autenticado: una sesion de otro usuario responde 404.

### `GET /workout-sessions?cursor=&limit=20`

```json
{
  "items": [
    {
      "id": "...", "kind": "ROUTINE", "routineId": "...", "routineTitle": "Upper/Lower", "dayTitle": "Lower A",
      "startedAt": "...", "completedAt": "...", "durationSeconds": 3240,
      "completedSets": 13, "exerciseCount": 4, "totalVolumeKg": 12797.50,
      "perceivedExertion": 8, "recordCount": 2
    }
  ],
  "nextCursor": "opaco o null"
}
```

- De la sesion mas nueva a la mas vieja. `limit` entre 1 y 50 (por defecto 20). Pasar `nextCursor` como `cursor` para la pagina siguiente; `null` indica que no hay mas. Cursor invalido: 400.
- No incluye registros sueltos (`QUICK_LOG`). Los titulos salen de la copia guardada en la sesion (V9) o, en sesiones viejas, de la rutina.
- `recordCount`: ejercicios cuya mejor serie supera la mejor fuerza estimada de las sesiones anteriores. La primera vez que se hace un ejercicio no es record.

### `GET /workout-sessions/{id}`

`{ session: <item de la lista>, notes, exercises: [{ position, exerciseId, name, primaryMuscleGroup, routineExerciseId, plan: { repsMin, repsMax, weightKg, restSeconds } | null, notes, sets: [{ id, setNumber, reps, weightKg, completed, record }] }] }`

- `plan` es la copia del momento (null en sesiones anteriores a V9). `record = true` marca la serie que supero la mejor marca previa.

### `PUT /workout-sessions/{id}`

```json
{
  "perceivedExertion": 9,
  "notes": "corregido",
  "sets": [ { "id": "...", "reps": 9, "weightKg": 65 } ],
  "deletedSetIds": [ "..." ]
}
```

- Corrige valores de series existentes y borra series. No agrega series nuevas.
- `perceivedExertion` y `notes` se reemplazan por lo enviado (null los borra).
- 400 si una serie no es de la sesion, si una serie se edita y se borra a la vez, o si la sesion quedaria sin series hechas (para eso esta `DELETE`). Todo es una transaccion: un error no deja cambios a medias.
- Devuelve el detalle actualizado.

### `DELETE /workout-sessions/{id}`

Borra la sesion y sus series. 204; 404 si no existe o es de otro usuario.

### `GET /progress/exercises`

`[{ exerciseId, name, primaryMuscleGroup, sessionCount, lastCompletedAt, bestEstimatedOneRepMaxKg }]`, del ultimo entrenado al mas viejo. Incluye registros sueltos.

### `GET /exercises/{id}/history?limit=30`

`{ exerciseId, name, primaryMuscleGroup, points: [...], records: { bestEstimatedOneRepMax, heaviest, mostVolume } }`

- `points`: las ultimas `limit` sesiones (1 a 100) con el ejercicio, de la mas vieja a la mas nueva: `{ sessionId, completedAt, bestWeightKg, bestReps, estimatedOneRepMaxKg, volumeKg, setCount }`. La mejor serie es la de mayor fuerza estimada.
- `records` se calculan sobre todo el historial: `{ value, weightKg, reps, completedAt, sessionId }`. `value` es la fuerza estimada, el peso o el volumen segun el record.
- 404 si el ejercicio no existe.

### `GET /progress/weekly?tz=America/Argentina/Buenos_Aires&weeks=8`

`[{ weekStart, sessions, trainedDays, completedSets, volumeKg }]`, de la semana mas vieja a la actual, con ceros en las semanas sin entrenar. `weeks` entre 1 y 52. Sin registros sueltos.

## Progreso

- `GET /progress/summary` → `{ completedWorkouts, activeRoutines, activeDaysLast30, latestMetric }`
- `GET /progress/activity` (legado) → ultimas 10 sesiones: `{ id, routineId, routineTitle, routineDayId, routineDayTitle, completedAt, exercises: [{ id, exerciseId, exerciseName, primaryMuscleGroup, reps, weightKg, completedAt }] }`
- `GET /metrics`, `POST /metrics` → `{ id, measuredAt, weightKg, bodyFatPercentage, waistCm, chestCm, hipCm, armCm, legCm, notes }`

## Nutricion

- `GET /nutrition?date=YYYY-MM-DD`
- `POST /nutrition` → `{ entryDate, mealType, description*, calories, proteinG, carbsG, fatG, fiberG, source, metadataJson }`
- `GET /nutrition/summary?date=YYYY-MM-DD`

## IA

Agente de fitness con **Qwen 3.8 27B** (`qwen/qwen3.8-27b`) servido por **GroqCloud**, a traves de su API compatible con OpenAI (`POST {base}/chat/completions`). El backend es el unico que habla con Groq: la clave nunca llega a la app. **Solo responde el modelo**: no hay respuestas predeterminadas. Sin clave, `/ai/chat` y `/routines/generate` devuelven 503; el resto de la app funciona igual.

### Configuracion

| Variable                                   | Por defecto                          | Uso                                                                  |
| ------------------------------------------ | ------------------------------------ | -------------------------------------------------------------------- |
| `GROQ_API_KEY`                             | vacia                                | Clave de GroqCloud (console.groq.com → API Keys). Secreta            |
| `LOCOMOTORA_AI_BASE_URL`                   | `https://api.groq.com/openai/v1`     | Solo para un proxy o pruebas con un servidor falso                   |
| `LOCOMOTORA_AI_ALLOWED_MODELS`             | `qwen/qwen3.8-27b`                   | Modelos ofrecidos. Con uno solo, Perfil lo muestra sin poder elegir  |
| `LOCOMOTORA_AI_COACH_MODEL`                | `qwen/qwen3.8-27b`                   | Modelo por defecto del chat                                          |
| `LOCOMOTORA_AI_ROUTINE_MODEL`              | `qwen/qwen3.8-27b`                   | Modelo por defecto para rutinas                                      |
| `LOCOMOTORA_AI_COACH_REASONING_EFFORT`     | `none`                               | `none`, `default`, `low`, `medium`, `high`. Razonar gasta mas tokens |
| `LOCOMOTORA_AI_ROUTINE_REASONING_EFFORT`   | `none`                               | Igual, para rutinas. Subirlo requiere un plan de Groq con mas TPM    |
| `LOCOMOTORA_AI_COACH_MAX_TOKENS`           | `700`                                | Tokens maximos de cada respuesta del chat (usa 100–200)              |
| `LOCOMOTORA_AI_ROUTINE_MAX_TOKENS`         | `1000`                               | Tokens maximos de la rutina (usa 440–860). No pasar el OTPM          |
| `LOCOMOTORA_AI_MAX_TOOL_ROUNDS`            | `2`                                  | Rondas de herramientas por mensaje antes de la respuesta final       |
| `LOCOMOTORA_AI_CHAT_TOKEN_BUDGET`          | `7000`                               | Tokens de un mensaje sumando sus rondas (Groq gratis: 8K por minuto) |
| `LOCOMOTORA_AI_DAILY_COACH_MESSAGES`       | `30`                                 | Mensajes por usuario cada 24 h (0 = sin limite)                      |
| `LOCOMOTORA_AI_DAILY_ROUTINE_GENERATIONS`  | `5`                                  | Rutinas generadas por usuario cada 24 h                              |
| `LOCOMOTORA_AI_DAILY_TOKEN_BUDGET`         | `150000`                             | Tokens de toda la app cada 24 h (Groq gratis: 200K por dia). 0 = sin tope |
| `LOCOMOTORA_AI_TIMEOUT_SECONDS`            | `60`                                 | Espera maxima por llamada a Groq                                     |

Con Docker, la clave se pone en `locomotora-backend/.env` (copiar `.env.example`; git y la imagen lo ignoran). Sin Docker, se exporta en la terminal antes de `sh ./mvnw spring-boot:run`.

Limites de Groq para `qwen/qwen3.8-27b` (pagina de rate limits, nov 2026): 30 peticiones/min, 1.000/dia, 8.000 tokens/min y 200.000 tokens/dia. **Medido con la clave real (15 nov):** la cuenta gratis tambien tiene **1.000 tokens de salida por minuto (OTPM)**, que la pagina no muestra. Groq rechaza de entrada (429 "Request too large") una peticion cuyo `max_completion_tokens` pase ese limite; por eso los maximos son 700 y 1000, y la rutina usa un formato compacto. Con un plan pago se pueden subir. Son de la cuenta, no del usuario: por eso el contexto es corto, las herramientas devuelven poco y hay un tope diario global. El modelo esta en "Preview" en Groq; si cambia, basta con `LOCOMOTORA_AI_*_MODEL` y `LOCOMOTORA_AI_ALLOWED_MODELS`.

### Como funciona

- **Chat (agente):** prompt de sistema con reglas de seguridad y un contexto corto del usuario (edad, sexo, altura, peso, nivel, dias, minutos, estilo, objetivos, lesiones, equipo, preferencias de alimentacion, rutinas activas y constancia). Los ultimos 8 mensajes como historial. El modelo puede pedir herramientas; el backend las ejecuta con el usuario autenticado y le devuelve el resultado.
- **Alcance:** el coach responde sobre entrenamiento, técnica, rutinas, nutrición deportiva, recuperación, hábitos y motivación, la app y los datos del usuario, incluidos cálculos al servicio de eso (kg/lb, macros, volumen, 1RM). Rechaza en una o dos frases, sin responder ni en parte, lo demás (cálculos sueltos, cultura general, programación, tareas, política, finanzas, medicina no relacionada con el ejercicio) y propone algo de fitness. Ante la duda interpreta la pregunta en clave de fitness; en preguntas mixtas responde solo la parte de fitness. El rechazo lo escribe el modelo (no hay textos fijos). Las reglas están en `CoachPrompts.coachSystem` y un recordatorio corto (`CoachPrompts.SCOPE_NOTE`) viaja al final del último mensaje del usuario, solo hacia el modelo: sin él, Qwen seguía respondiendo fuera de tema en conversaciones donde ya lo había hecho antes.
- **Formato de las respuestas:** texto con listas de guiones y negritas `**texto**` para lo importante (pesos, series y repeticiones, nombres de rutinas, resultados de calculos, la frase clave; maximo 5). El backend quita titulos y asteriscos sin cerrar; la app muestra las negritas y ademas resalta las cantidades con unidad (kg, g, kcal, %, 4x8).
- **Herramientas** (solo lectura, `AiToolRegistry`): `search_exercises(muscle?, equipment?, query?, only_my_equipment?)`, `list_my_routines()`, `get_routine(routine_id)`, `get_recent_training(days)`, `get_exercise_progress(exercise_name)`. Argumentos validados, resultados recortados (maximo 15 ejercicios, 10 sesiones, 2.500 caracteres). Una rutina de otro usuario responde `not_found`. Cada llamada se guarda en `ai_messages` con rol `TOOL`.
- **Rutinas:** el backend filtra el catalogo por el equipo del usuario y hace una sola llamada con `response_format: json_schema` y `strict: true` (Groq no permite herramientas y JSON estructurado juntos). El esquema fija la cantidad de dias (`minItems`/`maxItems`) y de ejercicios por dia (6 hasta 3 dias, 5 con 4–5, 4 con 6–7), y es compacto para entrar en 1.000 tokens de salida: cada ejercicio es `{ref, sets, reps: "6-8", rest}`, una indicacion de tecnica por dia (hasta 5 dias) y el nivel, la duracion y el foco los pone el backend. Despues valida todo en codigo.
- **Datos personales:** nunca se envian nombre, email, fecha de nacimiento, foto ni notas medicas. Emails, telefonos y numeros largos se reemplazan en los mensajes antes de enviarlos. Los logs guardan modelo, estado, latencia y tokens, no el contenido. Recomendado: activar *Zero Data Retention* en console.groq.com → Settings → Data Controls.
- **Seguridad:** antes, se detectan senales de riesgo (sintomas cardiacos, trastornos alimentarios, embarazo, lesiones, sustancias, menores) y se agregan instrucciones; despues, si la respuesta del modelo no deriva a un profesional se agrega un aviso fijo, y se revisa la respuesta (menos de 1.200 kcal, ayunos de 24 h o mas).
- **Errores:** un 429/498 de Groq se reintenta hasta dos veces si `retry-after` es de 5 s o menos; 500/502/503 se reintentan una vez; un 429 "Request too large" no se reintenta. Si el modelo arma mal una llamada a herramienta (400 `tool_use_failed`), el agente pide la respuesta sin herramientas. Los parametros de las herramientas no usan booleanos: Qwen los escribe como `True` y Groq rechaza la llamada. Si sigue fallando: el chat responde con las reglas locales (`source = LOCAL`) y la generacion responde 429 o 503.

### `GET /ai/models`

```json
{
  "configured": true,
  "selectable": false,
  "coach":   { "selected": "qwen/qwen3.8-27b", "defaultModel": "qwen/qwen3.8-27b", "custom": false, "dailyLimit": 30, "usedToday": 3 },
  "routine": { "selected": "qwen/qwen3.8-27b", "defaultModel": "qwen/qwen3.8-27b", "custom": false, "dailyLimit": 5,  "usedToday": 0 },
  "models": [ { "id": "qwen/qwen3.8-27b", "label": "Qwen 3.8 27B", "description": "...", "speed": "FAST", "cost": "LOW" } ]
}
```

### `PUT /ai/preferences`

`{ "coachModel": "qwen/qwen3.8-27b", "routineModel": null }` → misma respuesta que `GET /ai/models`. `null` o vacio = modelo por defecto. Un modelo no permitido → 400. Con un solo modelo la app no lo usa, pero queda para cuando se permitan varios.

### `POST /ai/chat`

`{ conversationId?, message (max 4000) }` → `{ conversationId, answer, model, source, toolsUsed }`.

- `toolsUsed`: herramientas que consulto el agente para esta respuesta, sin repetir (por ejemplo `["get_recent_training", "search_exercises"]`). La app las muestra como chips bajo la respuesta.

- `source`: siempre `AI`. Si el modelo no puede responder, el endpoint devuelve un error y **no guarda nada** (ni la pregunta ni la conversacion), asi la app puede reintentar sin duplicar.
- Errores: `conversationId` de otro usuario → 404; mal formado → 400; IA no activa → 503 `AI is not configured`; limite diario del usuario → 429 `Daily AI limit reached`; presupuesto diario de la app → 429 `AI daily budget reached`; Groq saturado → 429 `AI provider is busy, try again in a minute`; Groq caido o sin respuesta → 503 `AI service unavailable`; respuesta vacia → 502.
- Si el modelo se niega, su propio texto es la respuesta.
- Cada respuesta guarda en `metadata`: origen, modelo pedido, `stopReason`, tokens, llamadas al modelo, herramientas usadas, riesgos detectados y avisos. Un mensaje = una fila en `ai_usage` con los tokens de todas sus rondas.

`GET /ai/conversations/{id}/messages` devuelve tambien las filas `TOOL` (en `content`, el nombre de la herramienta): la app las agrupa en la respuesta que las sigue. `DELETE /ai/conversations/{id}` responde 204, o 404 si no existe o es de otro usuario. `GET /ai/recommendations` sigue igual.

### `POST /routines/generate`

```json
{ "prompt": "Quiero priorizar pecho", "daysPerWeek": 4, "sessionMinutes": 60,
  "objective": "BUILD_MUSCLE", "level": "INTERMEDIATE", "focusMuscles": ["Pecho"], "model": null }
```

Todo es opcional: lo que falta sale del perfil. Respuesta (no se guarda; la app la abre en el wizard y la guarda con `POST /routines`, `source = AI`, que aplica sus validaciones de siempre):

```json
{
  "title": "Torso / Pierna", "description": "...", "objective": "BUILD_MUSCLE", "difficulty": "INTERMEDIATE",
  "daysPerWeek": 4, "estimatedDurationMinutes": 60, "model": "qwen/qwen3.8-27b", "warnings": [],
  "days": [
    { "dayIndex": 1, "title": "Torso A", "focus": "Pecho, Espalda",
      "exercises": [ { "exerciseId": "uuid", "name": "Press banca", "primaryMuscleGroup": "Pecho", "equipment": "BARBELL",
                       "sets": 4, "repsMin": 6, "repsMax": 10, "restSeconds": 120, "notes": "..." } ] }
  ]
}
```

- El modelo elige ejercicios por numero de una lista que arma el backend; el backend los traduce a ids y descarta los que no existen.
- Reglas: series 1–10 (principiante: hasta 4), reps 1–50 con `repsMin <= repsMax`, descanso 0–600 s, hasta 10 ejercicios por dia y como mucho los dias pedidos.
- `warnings`: `UNKNOWN_EXERCISES_DROPPED`, `VALUES_ADJUSTED`, `FEWER_DAYS`, `EXTRA_DAYS_DROPPED`, `DAY_TOO_LONG` (no entra en los minutos), `HIGH_VOLUME` (mas de 25 series semanales de un grupo), `SEE_PROFESSIONAL` (se menciono una lesion o condicion de salud).
- Errores: 503 sin clave o Groq caido, 429 limite del usuario, presupuesto diario o Groq saturado, 422 el modelo se nego, 502 respuesta cortada, invalida o sin dias validos, 400 modelo no permitido o parametros fuera de rango.

### Prueba manual

Ver `docs/PRUEBA_IA.md`.

### "Detalles con video": `GET /exercises/{id}/guide`

Consejos de técnica generados con IA y un video corto de YouTube que muestra exactamente el ejercicio, el mismo para todos los usuarios. Se guarda por ejercicio en `exercise_guides` (V18, V19). Los videos del catálogo los busca por adelantado un proceso diario del backend (`CatalogVideoJob`); al pedido solo se buscan para ejercicios propios del usuario. Si faltan los consejos, el pedido los genera y cuenta como un mensaje del coach.

```json
{
  "exerciseId": "…", "exerciseName": "Press de banca plano",
  "tips": { "summary": "…", "cues": ["…"], "mistakes": ["…"], "breathing": "…" },
  "video": { "youtubeId": "…", "title": "…", "channel": "…", "durationSeconds": 84,
             "watchUrl": "https://www.youtube.com/watch?v=…", "thumbnailUrl": "https://i.ytimg.com/vi/…/hqdefault.jpg" },
  "videoStatus": "FOUND",
  "searchUrl": "https://www.youtube.com/results?search_query=…",
  "aiAvailable": true
}
```

- `videoStatus`: `FOUND`; `NOT_FOUND` (ningún candidato era exactamente este ejercicio; se vuelve a buscar a los 30 días); `PENDING` (el proceso diario todavía no llegó a este ejercicio); `UNAVAILABLE` (YouTube no está configurado o falló).
- `video.title` y `video.channel` pueden venir `null` unos días en videos cargados por migración: los trae el refresco.
- Búsqueda: YouTube Data API v3, videos públicos, embebibles y de 15 s a 5 min; primero en español (`<nombre> técnica correcta`), si no en inglés (`<name_en> proper form`). Antes de la IA, reglas fijas descartan títulos de recopilaciones, con otro equipo, otra inclinación o una sola mano cuando el ejercicio no lo es (`VideoRules`). La IA ordena hasta 3 videos que muestran exactamente el ejercicio, solo con confianza alta: el primero queda como video y los otros como alternativas.
- Proceso diario (`CatalogVideoJob`, cada 30 min): llena por prioridad (habituales, los que están en rutinas, fuerza antes que estiramientos) con hasta `locomotora.youtube.daily-search-budget` búsquedas por día (95; el día de YouTube termina a medianoche del Pacífico), un ejercicio cada 20 s por el límite de tokens por minuto de Groq y un tope de tokens propio (`fill.daily-token-cap`). El uso de IA se registra sin usuario (`CATALOG`). Cada 6 h refresca los videos con más de 7 días: confirma que siguen disponibles, actualiza título, canal y duración, pasa a una alternativa si el principal se cayó y vuelve a la cola si no queda ninguna.
- Configuración: `YOUTUBE_API_KEY`. Sin clave, solo consejos y `searchUrl`.

### Video que no carga: `POST /exercises/{id}/guide/video-unavailable`

Cuerpo `{ "videoId": "…", "errorCode": 150 }` (el código del reproductor de YouTube; la app solo lo manda con 2, 5, 100, 101 o 150; el 153 es configuración propia y no reemplaza el video). Devuelve la guía con `notice`:

- `STILL_AVAILABLE`: YouTube confirma que el video está bien (1 unidad de cuota); no se cambia para nadie.
- `REPLACED`: pasó a la siguiente alternativa para todos (sin costo) o, si no quedaban, encontró otro video con una búsqueda nueva que excluye los que fallaron.
- `NO_OTHER`: no hay otro video que muestre exactamente el ejercicio.
- `LIMIT`: el usuario ya hizo `locomotora.youtube.user-daily-replacements` búsquedas hoy (5).

Si el video reportado ya no es el actual (otro usuario lo reemplazó), devuelve el actual con `REPLACED`.

### Exportar los videos a una migración

`tools/exercise-videos/export_videos.py` (ver su README) genera una migración con los ids encontrados para que otra base arranque con los mismos videos sin gastar su cuota.
- Errores: 404 si el ejercicio no existe o es propio de otro usuario; 429 si el usuario pasó su límite diario del coach o la IA está saturada; 503 si la IA no responde y todavía no hay consejos guardados.

## Actividad de usuarios (admin)

Panel de Perfil → "Actividad de usuarios". Solo lo ve una cuenta con `users.role = 'ADMIN'`; `GET /me` devuelve `isAdmin`. El rol **se asigna a mano en la base** (ninguna migracion ni variable lo asigna):

```sql
UPDATE users SET role = 'ADMIN', updated_at = now() WHERE email = '<email>';
```

### Cuentas inhabilitadas

- El filtro JWT revisa en cada peticion el `status` y el `role` actuales de la cuenta (cache en memoria de 60 s). Un token valido de una cuenta que ya no esta `ACTIVE` recibe `401` "Account is disabled", y la app cierra la sesion.
- Al cambiar una cuenta desde el panel, el cache se borra en el momento. Un cambio hecho a mano en la base tarda hasta 60 s.
- `/admin/**` exige el rol `ADMIN` (403 si no) y los servicios lo vuelven a revisar.

### `POST /activity/ping?tz=America/Lima`

La app lo manda al abrirse, cada 60 s mientras esta en pantalla y al pasar a segundo plano. El servidor guarda `users.last_seen_at` y suma a `user_daily_activity` (dia local segun `tz`) el tiempo desde el ping anterior si fue hace 2 minutos o menos; si no, cuenta una sesion nueva sin sumar el hueco. La app no informa duraciones. Tope: 24 h por dia. Responde `204`.

### `GET /admin/stats?tz=`

```json
{
  "users": { "total": 12, "active": 11, "disabled": 1, "admins": 1, "new7d": 2, "new30d": 5, "google": 4, "password": 9 },
  "usage": { "active24h": 3, "active7d": 6, "active30d": 9, "seconds7d": 14400, "seconds30d": 52000, "averageSecondsPerUser30d": 5777 },
  "training": { "workouts30d": 40, "seconds30d": 150000 },
  "daily": [{ "date": "2026-11-24", "activeUsers": 3, "seconds": 2400 }]
}
```

- `users`: cuentas no borradas. `google`/`password`: forma de entrar (una cuenta puede tener las dos).
- `usage`: desde que existe el ping (V22). `active*` = usuarios con un ping en las ultimas 24 h, 7 y 30 dias.
- `training`: entrenamientos terminados (sin registros sueltos) de los ultimos 30 dias; una sesion de mas de 6 h se corta en 6 h.
- `daily`: los ultimos 14 dias, con los dias sin uso en 0.

### `GET /admin/users?q=&status=ACTIVE|DISABLED&page=0&tz=`

Paginas de 30, primero los que usaron la app mas recientemente. `q` busca en nombre y email.

```json
{
  "items": [{
    "id": "...", "name": "Ana", "email": "ana@example.com", "status": "ACTIVE", "role": "USER",
    "provider": "GOOGLE", "createdAt": "...", "lastActivityAt": "...", "usageSeconds30d": 3600, "workouts30d": 6
  }],
  "page": 0, "pageSize": 30, "total": 12
}
```

`provider`: `GOOGLE`, `PASSWORD` o `BOTH`. `lastActivityAt`: el ultimo ping o el ultimo entrenamiento, lo mas reciente.

### `GET /admin/users/{id}?tz=`

La fila de la lista mas `onboardingCompleted`, `routines` (activas), `workoutsTotal`, `trainingSecondsTotal`, `trainingSeconds30d`, `lastWorkoutAt`, `sessions30d`, `aiRequests30d`, `daily` (30 dias: `date`, `seconds`, `sessions`), `history` (ultimos 10 cambios: `action`, `reason`, `adminName`, `createdAt`) y `canChangeStatus`. Solo datos de cuenta y actividad: nada de salud, fotos ni contenido de los entrenamientos.

### `PATCH /admin/users/{id}/status`

```json
{ "status": "DISABLED", "reason": "Spam" }
```

- `status`: `ACTIVE` o `DISABLED`; `reason` opcional (hasta 300). Devuelve el detalle actualizado.
- Queda registrado en `admin_audit_log`. Si el estado ya era ese, no cambia nada.
- Errores: `400` sobre la propia cuenta; `403` sobre otra cuenta admin; `404` si no existe o esta borrada.

