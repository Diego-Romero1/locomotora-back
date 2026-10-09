# Plan de implementación — Rutinas: crear, entrenar y registrar

Actualizado: 2026-10-09

> Prerrequisito: leer [`SYSTEM_OVERVIEW.md`](SYSTEM_OVERVIEW.md). Los códigos H1, H2… refieren a los hallazgos de ese documento.
> Tamaños: **S** = menos de 1 día, **M** = 1 a 3 días, **L** = más de 3 días (una persona).

## Objetivo

Que un usuario pueda:

1. **Armar una rutina multi-día en pocos minutos** desde el celular, sin conocimientos previos.
2. **Ejecutarla en el gimnasio** serie por serie, con una mano y aunque no haya conexión.
3. **Tener un registro fiel** de lo que hizo y verlo como historial y progreso.

## Principios de producto

- **Valores por defecto inteligentes.** Cada campo arranca con un valor sensato que sale del onboarding y del objetivo. El usuario corrige; no completa desde cero.
- **Partir de algo.** Plantilla, rutina propia duplicada o borrador IA: todos los caminos terminan en el mismo editor.
- **Un solo editor para crear y editar.** Editar nunca destruye días ni desvincula historial.
- **Gimnasio primero.** Botones grandes, pocos toques, funciona offline, nada se pierde si se cierra la app.
- **El registro es inmutable respecto de la rutina.** Cambiar la rutina no reescribe lo ya entrenado.

## Criterios de "listo" del ciclo completo

| Criterio                                                         | Meta                         |
| ---------------------------------------------------------------- | ---------------------------- |
| Crear rutina de 3 días × 5 ejercicios desde cero                 | ≤ 2 minutos                  |
| Registrar una serie igual a la prescrita o a la vez anterior     | 1 toque                      |
| Entrenamientos perdidos por cerrar la app o perder conexión      | 0                            |
| Historial muestra el día correcto y cada serie con peso y reps   | 100 % de las sesiones nuevas |

---

## Fase 0 — Estabilizar (antes de construir encima)

Objetivo: dejar de perder o falsear datos con lo que ya existe.

| #   | Tarea                                                                                                                                                                                                   | Hallazgo | Tamaño |
| --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ------ |
| 0.1 | Commitear el wizard y los cambios pendientes del frontend. Decidir si se unifica en un monorepo en la raíz o se mantienen dos repos (recomendado: monorepo, para versionar `docs/` junto al código).     | H5       | S      |
| 0.2 | Registrar el día entrenado: agregar `routineDayId` opcional a `WorkoutLogRequest`, validar que pertenezca a la rutina (si no viene, usar el primero). En el frontend, enviar `dayId` desde `WorkoutScreen`. | H2       | S      |
| 0.3 | Mitigar el aplanado multi-día hasta la Fase 1: si `routine.days.length > 1`, el botón Editar muestra un aviso en lugar de abrir el wizard.                                                                | H1       | S      |
| 0.4 | En el wizard, usar una clave local única por entrada (no `exerciseId`), para que un ejercicio pueda repetirse.                                                                                          | H1       | S      |
| 0.5 | Migración `V7__normalize_exercise_catalog.sql`: unificar `Cuadriceps`→`Cuádriceps` y `Triceps`→`Tríceps`. Agregar etiqueta `SMITH_MACHINE` en el frontend.  | H8       | S      |
| 0.6 | Pedir confirmación al cerrar el entrenamiento con ✕ si hay series marcadas.                                                                                                                             | H6       | S      |

**Hecho cuando:** un entrenamiento del Día 3 aparece como Día 3 en `/progress/activity`; editar una rutina de 4 días no la reduce a 1; el filtro de músculos no tiene duplicados.

**Avance (2026-10-09)**, en la rama `fase-0-estabilizacion` de backend y frontend:

| #   | Estado | Nota                                                                                                                                  |
| --- | ------ | ------------------------------------------------------------------------------------------------------------------------------------- |
| 0.1 | ✅      | El wizard ya estaba commiteado en el frontend (`acfeabe`). Decisión: se mantienen dos repos y `docs/` vive en el repo del backend.      |
| 0.2 | ✅      | `routineDayId` en `/workout-log`, con 400 si el día no es de la rutina. 4 tests nuevos (`WorkoutLogServiceTest`) y prueba contra la API. |
| 0.3 | ✅      | Aviso en `RoutineDetailScreen` y protección repetida en el wizard.                                                                     |
| 0.4 | ✅      | Claves `entry-N` por ejercicio elegido; el `exerciseId` va en la metadata de cada entrada.                                             |
| 0.5 | ✅      | V7 aplicada en la base local: quedan 10 grupos musculares. Etiqueta "Máquina Smith" en el wizard.                                      |
| 0.6 | ✅      | `usePreventRemove`: cubre ✕, gesto y botón atrás de Android mientras haya series marcadas y no se haya guardado.                        |

---

## Fase 1 — Crear rutinas: Wizard v2 multi-día

### Flujo propuesto

```
"Nueva rutina"
   │
   ├─ Elegir punto de partida:  [Plantilla]  [Desde cero]  [Con IA]  [Duplicar una mía]
   │                                 │             │            │              │
   │                                 └─────────────┴─────┬──────┴──────────────┘
   ▼                                                     ▼
Paso 1  Base            nombre (opcional, se autogenera), objetivo, nivel, días/semana, duración
Paso 2  Estructura      N días propuestos según el split; renombrar, reordenar, agregar o quitar
Paso 3  Ejercicios      pestañas por día; catálogo prefiltrado por el foco del día; series/reps/descanso
Paso 4  Revisar         resumen por día, series semanales por grupo muscular, avisos → Guardar
   │
   ▼
Detalle de la rutina con CTA "Empezar Día 1"
```

### Detalle de los pasos

**Paso 1 — Base.** Ya existe y se mantiene. Cambios:
- El nombre pasa a ser opcional: si queda vacío se propone uno (por ejemplo "Hipertrofia 4 días").
- Los valores siguen prellenándose desde `GET /profile/onboarding`.

**Paso 2 — Estructura semanal (nuevo).** Al elegir N días se proponen N días con nombre y foco según el split sugerido. Se reutiliza la lógica del backend (`deriveRecommendedSplit`):

| Días/semana | Split sugerido     | Días propuestos                                  |
| ----------- | ------------------ | ------------------------------------------------ |
| 1–3         | Full Body          | Full Body A, B, C                                |
| 4           | Upper / Lower      | Upper A, Lower A, Upper B, Lower B               |
| 5           | PPL + Upper/Lower  | Push, Pull, Legs, Upper, Lower                   |
| 6           | PPL × 2            | Push A, Pull A, Legs A, Push B, Pull B, Legs B   |

El usuario puede cambiar de split con chips, renombrar días, reordenarlos y agregar o quitar. Cada día lleva chips de grupos musculares (foco), que prefiltran el catálogo en el paso 3.

**Paso 3 — Ejercicios por día.** Pestañas por día. Para cada día:
- Selector de catálogo (pantalla modal) con búsqueda y filtro de músculo, prefiltrado por el foco del día.
- Configuración compacta por ejercicio con `ExerciseConfigCard` (ya existe): series, reps o rango, descanso.
- "Copiar ejercicios de otro día", reordenar, eliminar con deshacer.
- Valores por defecto según objetivo:

| Objetivo                         | Series | Reps   | Descanso |
| -------------------------------- | ------ | ------ | -------- |
| Ganar músculo (`BUILD_MUSCLE`)   | 3      | 8–12   | 90 s     |
| Fuerza (categoría Fuerza)        | 5      | 3–5    | 180 s    |
| Perder grasa (`LOSE_WEIGHT`)     | 3      | 12–15  | 45 s     |
| Resistencia (`IMPROVE_ENDURANCE`)| 3      | 15–20  | 30 s     |
| General (`GENERAL_FITNESS`)      | 3      | 10–12  | 60 s     |

**Paso 4 — Revisar y guardar.** Resumen por día, series semanales por grupo muscular y avisos que **no bloquean**: día sin ejercicios, duración estimada mayor a la elegida, grupo muscular sin series. Al guardar se va al detalle con "Empezar Día 1".

### Frontend

| #    | Tarea                                                                                                                                                                                                                         | Tamaño |
| ---- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------ |
| 1.F1 | Extraer el estado del wizard a un reducer puro `routineDraftReducer` (ej. `src/features/routineBuilder/`). Acciones: `setBase`, `setDays`, `addExercise`, `updateExercise`, `moveExercise`, `removeExercise`, `copyDay`.        | M      |
| 1.F2 | Modelo de borrador con clave local y `id` de servidor por día y por ejercicio (ver abajo), más los mapeos `routineToDraft` y `draftToPayload`. Así editar preserva identidad (H1, H4).                                          | S      |
| 1.F3 | Paso 2 (estructura) y paso 3 con pestañas por día. Nuevos componentes: `DayTabs`, `ExercisePickerSheet`.                                                                                                                     | L      |
| 1.F4 | Paso 4 (revisión) con resumen por grupo muscular y avisos.                                                                                                                                                                    | M      |
| 1.F5 | Autoguardado del borrador en almacenamiento local (recomendado `@react-native-async-storage/async-storage`; `expo-secure-store` no es para datos de este tamaño). Al volver: "Tenés un borrador sin guardar".               | S      |
| 1.F6 | Pantalla "Nueva rutina" con los cuatro puntos de partida. "Plantilla" y "Duplicar" abren el wizard en el paso 4 con el borrador cargado.                                                                                     | M      |
| 1.F7 | Mover a i18n los textos hardcodeados del wizard y de la pantalla IA (H11).                                                                                                                                                   | S      |
| 1.F8 | Quitar la mitigación 0.3: el wizard ya edita rutinas multi-día.                                                                                                                                                              | S      |

Modelo de borrador sugerido:

```ts
interface RoutineDraft {
  base: { title: string; categoryId?: string; objective: Goal; difficulty: RoutineDifficulty;
          daysPerWeek: number; estimatedDurationMinutes: number };
  days: DraftDay[];
}
interface DraftDay {
  key: string;          // uuid local, estable mientras se edita
  id?: string;          // id de routine_days en el servidor (si existe)
  title: string;
  focus: string[];      // grupos musculares
  exercises: DraftExercise[];
}
interface DraftExercise {
  key: string;          // uuid local: permite repetir un ejercicio
  id?: string;          // id de routine_exercises en el servidor (si existe)
  exerciseId: string;   // id del catálogo
  name: string; primaryMuscleGroup?: string; equipment?: string;
  sets: number; repsMin?: number; repsMax?: number; reps?: number;
  restSeconds: number; notes?: string;
}
```

### Backend

| #    | Tarea                                                                                                                                                                                                                                                                                                         | Hallazgo | Tamaño |
| ---- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ------ |
| 1.B1 | **Update que preserva identidad.** `RoutineService.updateRoutine` actualiza por `id`: días y ejercicios con `id` existente se actualizan, los nuevos se insertan y los ausentes se borran. Para no chocar con los `UNIQUE (routine_id, day_index)` y `(routine_day_id, position)`, hacerlos `DEFERRABLE INITIALLY DEFERRED` en una migración. | H4       | M      |
| 1.B2 | **Ejercicios propios explícitos.** Agregar `exercises.created_by_user_id` (NULL = global). `GET /exercises` devuelve globales + propios; nuevo `POST /exercises` para crear uno propio. `findOrCreateExercise` deja de crear ejercicios globales.                                                            | H10      | M      |
| 1.B3 | `POST /routines/{id}/duplicate` para "Duplicar una mía".                                                                                                                                                                                                                                                     | —        | S      |
| 1.B4 | `source` opcional en `RoutineRequest` (`MANUAL` o `AI`), para que lo generado por IA quede marcado.                                                                                                                                                                                                          | —        | S      |
| 1.B5 | Unificar el vocabulario de objetivo (`BUILD_MUSCLE`, `LOSE_WEIGHT`, `GENERAL_FITNESS`, `IMPROVE_ENDURANCE`) y mapear `HYPERTROPHY` en un solo lugar, o migrar las plantillas.                                                                                                                                 | H9       | S      |
| 1.B6 | Tests de integración de `RoutineService`: crear multi-día, editar preservando ids, repetir ejercicio en dos días.                                                                                                                                                                                            | H15      | M      |

**Hecho cuando:** se crea una rutina de 4 días desde cero; se edita una plantilla clonada de 6 días sin perder días; después de editar, el historial sigue mostrando el día entrenado; el borrador sobrevive a cerrar la app.

---

## Fase 2 — Entrenar: reproductor de entrenamiento v2

### Decisión: offline-first con un único envío idempotente

En el gimnasio la conexión suele ser mala y una sesión dura entre 45 y 90 minutos. El celular guarda el estado de la sesión localmente en cada cambio y, al terminar, hace **un solo POST** con todas las series. Un `clientSessionId` generado en el celular permite reintentar sin duplicar. Es más simple y más robusto que sincronizar serie por serie.

Nuevo endpoint `POST /workout-sessions`:

```json
{
  "clientSessionId": "6f1c…  (uuid generado en el celular)",
  "routineId": "…",
  "routineDayId": "…",
  "startedAt": "2026-10-09T18:02:00Z",
  "completedAt": "2026-10-09T19:10:00Z",
  "perceivedExertion": 8,
  "notes": "Buena sesión",
  "exercises": [
    {
      "routineExerciseId": "…",
      "exerciseId": "…",
      "position": 1,
      "notes": null,
      "sets": [
        { "setNumber": 1, "reps": 10, "weightKg": 60, "completed": true },
        { "setNumber": 2, "reps": 9,  "weightKg": 60, "completed": true }
      ]
    }
  ]
}
```

- Una fila por serie en `workout_exercise_logs` (el esquema ya tiene `set_number`).
- `routineExerciseId` puede ser `null` para ejercicios agregados o reemplazados en la sesión; `exerciseId` es obligatorio.
- Si el `clientSessionId` ya existe para ese usuario, se devuelve la sesión existente (idempotencia).
- `/workout-log` se mantiene como adaptador hasta que el frontend migre y luego se elimina.

### Backend

| #    | Tarea                                                                                                                                       | Hallazgo | Tamaño |
| ---- | ------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ------ |
| 2.B1 | Migración: `workout_sessions.client_session_id uuid` con índice único `(user_id, client_session_id)`.                                       | —        | S      |
| 2.B2 | `WorkoutSessionService` + `POST /workout-sessions` (contrato de arriba), con validación de día y ejercicios.                               | H2, H3   | M      |
| 2.B3 | `GET /workout-sessions/last-performance?exerciseIds=a,b,c`: últimas series registradas por ejercicio, para prellenar y mostrar "Última vez". | —        | S      |
| 2.B4 | Rotación de días: `GET /routine` devuelve también `nextDayId` (el día siguiente al último entrenado de esa rutina, por `day_index`).          | H7       | S      |
| 2.B5 | Tests: idempotencia, una fila por serie, día correcto, ejercicio fuera de la rutina.                                                       | H15      | M      |

### Frontend

| #    | Tarea                                                                                                                                                                         | Hallazgo | Tamaño |
| ---- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ------ |
| 2.F1 | Reducer puro `workoutSessionReducer` con estado **por serie** (peso, reps, completada) y persistencia local en cada cambio (clave `workout:active`).                          | H3, H6   | M      |
| 2.F2 | Prellenado: peso/reps de la última vez para ese ejercicio; si no hay, las reps prescritas. Mostrar "Última vez: 60 kg × 10, 10, 9" debajo del nombre.                        | —        | S      |
| 2.F3 | Timer de descanso: al marcar una serie arranca la cuenta regresiva con el `restSeconds` del ejercicio, con +15 s y "Saltar". Vibración al terminar (`expo-haptics` ya está). | —        | M      |
| 2.F4 | Agregar o quitar series, saltar ejercicio, reemplazar ejercicio (selector filtrado por el mismo grupo muscular), notas por ejercicio.                                          | —        | M      |
| 2.F5 | Vista de lista de ejercicios del día para saltar a cualquiera, además de anterior/siguiente.                                                                                  | —        | S      |
| 2.F6 | Salida con dos opciones: "Pausar" (queda guardado) y "Descartar". En Home, banner "Entrenamiento en curso — Continuar".                                                      | H6       | S      |
| 2.F7 | Pantalla de cierre: duración, volumen total (Σ kg × reps), series hechas, récords logrados, selector de esfuerzo (RPE 1–10), notas. Si no hay conexión, queda en cola y reintenta. | H6       | M      |
| 2.F8 | Home muestra "Hoy: Lower A" usando `nextDayId` e inicia ese día. Desde el detalle de una plantilla, CTA "Usar y empezar" (clona y abre el entrenamiento).                     | H7       | S      |

**Hecho cuando:** se registra un entrenamiento completo en modo avión y se sincroniza después sin duplicados; se mata la app a mitad y se retoma; cada serie queda en la base con su `set_number`; el timer de descanso funciona.

---

## Fase 3 — Consultar el registro: historial y progreso

| #   | Tarea                                                                                                                                                                                                               | Tamaño |
| --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------ |
| 3.1 | `GET /workout-sessions` paginado por cursor y `GET /workout-sessions/{id}` con detalle por ejercicio y serie.                                                                                                       | M      |
| 3.2 | `PUT` y `DELETE /workout-sessions/{id}` para corregir errores de carga.                                                                                                                                            | S      |
| 3.3 | `GET /exercises/{id}/history`: mejor serie por sesión, 1RM estimado (Epley: `peso × (1 + reps / 30)`) y récords personales.                                                                                        | M      |
| 3.4 | Ampliar `/progress/summary`: racha real, volumen semanal, sesiones de la semana contra `daysPerWeek`. Recomendado: racha **semanal** (semanas seguidas cumpliendo los días objetivo), más justa para quien entrena 3 veces por semana. Eliminar `MOCK_STREAK`. | M      |
| 3.5 | Pantallas: Historial (lista por fecha), Detalle de sesión, Progreso de un ejercicio (gráfico simple).                                                                                                               | L      |

---

## Fase 4 — Calidad y deuda técnica (en paralelo)

Hacer un mínimo en cada fase, no al final.

| #   | Tarea                                                                                                                                       | Hallazgo | Tamaño |
| --- | ------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ------ |
| 4.1 | Tests backend con Testcontainers (Postgres real) para rutinas, plantillas y sesiones.                                                      | H15      | M      |
| 4.2 | `jest-expo` en el frontend y tests de los reducers puros (`routineDraftReducer`, `workoutSessionReducer`), donde vive la lógica importante. | H15      | M      |
| 4.3 | Pasar `RoutineTemplateController` a `RoutineTemplateService` + repositorio, reutilizando los mapeos de `RoutineRepository`.                 | H12      | M      |
| 4.4 | Eliminar `routineCategoriesAvailable()` y las ramas SQL alternativas.                                                                       | H14      | S      |
| 4.5 | Resolver N+1: cargar días y ejercicios de varias rutinas en dos consultas.                                                                 | H13      | S      |
| 4.6 | `springdoc-openapi` para tener el contrato generado.                                                                                       | —        | S      |
| 4.7 | Borrar restos del starter de Expo Router (`app/`, `components/` del starter).                                                              | H17      | S      |
| 4.8 | Secreto JWT obligatorio fuera de desarrollo y flujo de refresh token.                                                                      | H16      | M      |

---

## Fase 5 — IA real para generar rutinas (después de la Fase 1)

- `POST /routines/generate` en el backend: un modelo de lenguaje con salida estructurada (JSON schema) limitada a `exerciseId` del catálogo, usando perfil, equipamiento, limitaciones, días y duración.
- El resultado se valida con las mismas reglas de `RoutineService`.
- La respuesta es un **borrador** que se abre en el paso 4 del Wizard v2 para revisar y editar; se guarda con `source = AI`.
- Reemplaza `src/utils/aiRoutineGenerator.ts` y la espera simulada de `RoutineFormScreen`.

---

## Orden y dependencias

```
Fase 0 ──► 1.B1 (update que preserva ids) ──┬──► Fase 1 frontend (Wizard v2) ──► Fase 5 (IA)
                                            │
                                            └──► Fase 2 (entrenamiento) ──► Fase 3 (historial)

Fase 4: en paralelo, un poco en cada fase.
```

- 1.B1 va primero porque las fases 1 y 2 dependen de que los ids de días y ejercicios sean estables.
- Con dos personas, Wizard v2 (frontend) y Fase 2 (backend + frontend) pueden avanzar en paralelo después de 1.B1.

## Primer sprint sugerido

1. 0.1 — Commitear y decidir estructura de repositorio.
2. 0.2 — Registrar el día entrenado.
3. 0.3 + 0.4 — Proteger rutinas multi-día en el wizard y usar claves locales.
4. 0.5 — Normalizar catálogo (V7).
5. 0.6 — Confirmar salida del entrenamiento.
6. 1.B1 — Update que preserva identidad, con sus tests (1.B6).
7. Si sobra tiempo: 2.B1 + 2.B2 + 2.F1 juntos (endpoint nuevo y estado por serie). Hacer solo 2.F1 no alcanza, porque `/workout-log` guarda una fila por ejercicio y las series se seguirían perdiendo.
