# Locomotora — Estado del sistema

Actualizado: 2026-11-11

> Este documento describe **en qué etapa está el sistema** y qué problemas tiene hoy.
> El plan para seguir está en [`ROADMAP_RUTINAS.md`](ROADMAP_RUTINAS.md).
> El detalle técnico (estructura, endpoints, convenciones) está en [`AGENT_CONTEXT.md`](AGENT_CONTEXT.md).

## Objetivo del producto

Locomotora es una app móvil de entrenamiento. El foco actual del producto es el ciclo central:

1. **Crear** una rutina de forma fácil e intuitiva desde el celular.
2. **Realizar** esa rutina en el gimnasio, ejercicio por ejercicio y serie por serie.
3. **Guardar el registro** de lo entrenado y poder consultarlo como historial y progreso.

Perfil, nutrición, métricas corporales y coach IA existen, pero son secundarios a este ciclo.

## Etapa actual: MVP funcional en estabilización

El sistema tiene **mucha superficie y poca profundidad**: casi todos los módulos tienen backend y pantalla, pero el ciclo central (crear → entrenar → registrar) funciona solo en el camino feliz y tiene errores que hacen perder o falsear datos.

| Fase del roadmap original     | Estado         | Comentario                                                                                         |
| ----------------------------- | -------------- | -------------------------------------------------------------------------------------------------- |
| 1. Base backend               | ✅ Hecho        | Auth JWT, Flyway, capa de servicios, Docker. Faltan refresh tokens.                                 |
| 2. Rutinas y entrenamientos   | ⚠️ ~60 %        | CRUD, catálogo (109 ejercicios) y 7 plantillas listos. Wizard de un solo día. Registro superficial. |
| 3. Progreso y perfil          | ⚠️ Parcial      | Onboarding, perfil, métricas, actividad reciente. Sin PRs, volumen ni racha real.                   |
| 4. IA y nutrición             | 🟡 Simulado     | Coach y generador de rutinas son reglas por palabras clave, sin modelo real.                        |
| 5. Escalabilidad y calidad    | ⚠️ Bajo         | 38 tests backend, 49 tests frontend (reducer y cola), sin CI ni OpenAPI.                             |

Además hay un **rediseño visual en curso** (handoff en `locomotora-backend/docs/design_handoff_locomotora_redesign/` y wizard en `locomotora-backend/docs/design_routine_wizard_redesign/`). El wizard nuevo está implementado (un solo día por rutina).

## Estado del ciclo central

| Paso                 | Qué funciona hoy                                                                                       | Qué falta o falla                                                                                     |
| -------------------- | ------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------- |
| Crear (manual)       | Wizard de 3 pasos: datos → elegir ejercicios → series/reps/descanso. Prellenado desde el onboarding.     | Un solo día por rutina aunque pide "días por semana". Al editar, aplana rutinas multi-día (H1).        |
| Crear (plantilla)    | 7 plantillas de hipertrofia multi-día, recomendación automática y clonación a "Mis rutinas".            | Para entrenar una plantilla hay que clonarla primero; luego no se puede editar sin romperla (H1).     |
| Crear (IA)           | Pantalla con prompt y resultado.                                                                       | Generador local por palabras clave con espera simulada. Un solo día. Se guarda como `MANUAL`.          |
| Entrenar             | Serie activa prellenada con la vez pasada, botones + y −, timer de descanso con vibración y aviso, lista del día, reemplazar ejercicio, aviso de récord. Guardado en el teléfono. | Sin calentamiento marcado (opcional, después de la Fase 3).                                          |
| Registrar            | `POST /workout-sessions`: una fila por serie, día correcto, duración real, RPE y notas, idempotente. Cola de reenvío sin señal. | —                                                                                                     |
| Consultar historial  | `GET /progress/activity` (últimas 10 sesiones) y resumen (`/progress/summary`).                        | Sin historial paginado, sin detalle por serie, sin progreso por ejercicio. Racha hardcodeada en 7.     |

## Hallazgos principales

Los identificadores (H1, H2…) se usan en el roadmap.

> **Avance Fase 2 · Entrenar sin fricción (2026-11-12, `main`):** H7 resuelto. Pantalla Hoy con el día que toca, semana y racha semanal; "la vez pasada", botones + y −, timer de descanso con aviso, lista del día y reemplazo de ejercicio. Barra de 4 pestañas.
>
> **Avance Fase 1 · Registro fiel (2026-11-11, `main`):** H3 resuelto: una fila por serie con `POST /workout-sessions`. H6 resuelto: la sesion se guarda en el telefono en cada cambio, se retoma desde Inicio, tiene duracion real y se reenvia sin duplicar. H18 resuelto. El plan cambio de orden: ver "Plan vigente" en `ROADMAP_RUTINAS.md`.
>
> **Avance Fase 0 (2026-10-09, rama `fase-0-estabilizacion`):** H2 resuelto (desde el detalle de rutina; Home sigue entrenando el primer día, ver H7). H8 resuelto. H1 mitigado: la edición de rutinas de varios días queda bloqueada hasta el Wizard v2. H6 mitigado: se pide confirmación antes de salir con series marcadas. H5 resuelto: el frontend ya estaba commiteado (`acfeabe`) y `docs/` pasó al repo del backend. El texto de abajo describe el estado previo a la Fase 0.

### Críticos: pérdida o corrupción de datos

- **H1 — Editar una rutina multi-día la convierte en una rutina de un día.**
  `RoutineWizardScreen` aplana `routine.days` en una sola lista y guarda un único día. Si el usuario edita una plantilla clonada (por ejemplo PPL de 6 días), pierde la estructura sin aviso. Además, si un mismo ejercicio aparece en dos días, se usan claves repetidas (`exerciseId`) y la configuración de uno pisa la del otro.
  Archivo: `locomotora-frontend/src/screens/Routines/RoutineWizardScreen.tsx` (carga en edición, ~línea 157).

- **H2 — El entrenamiento siempre se registra como Día 1.**
  El frontend navega a `Workout` con `dayId`, pero `postWorkoutLog` no lo envía, y `WorkoutLogService` usa `findFirstRoutineDayId`. Además, Home inicia el entrenamiento sin `dayId`, así que siempre se entrena el primer día.
  Archivos: `WorkoutScreen.tsx`, `routineService.ts`, `WorkoutLogService.java`.

- **H3 — No se registran las series.**
  En `WorkoutScreen` el estado de peso/reps es por ejercicio, no por serie: escribir en la fila de la serie 2 cambia todas las filas. El backend guarda una sola fila con `set_number = 1`. No se puede calcular volumen ni récords.

- **H4 — Editar una rutina desvincula el historial.** ✅ Resuelto en 1.B1.
  `RoutineService.replaceDays` borra y recrea todos los días y ejercicios. Por las FK `ON DELETE SET NULL`, los registros previos pierden `routine_day_id` y `routine_exercise_id` (se conserva `exercise_id`). Los DTOs ya traen `id` de día y de ejercicio, pero se ignoran.

- **H5 — Trabajo sin versionar.**
  `RoutineWizardScreen.tsx` y `src/components/routines/wizard/*` están sin trackear en git, y hay 9 archivos modificados sin commitear. La raíz del workspace no es un repositorio (tiene un `.git` vacío); backend y frontend tienen repos separados.

### Altos: experiencia del ciclo central

- **H6 — El entrenamiento en curso no se guarda.** Cerrar con ✕ o matar la app pierde todo, sin confirmación. `started_at` se guarda igual a `completed_at`, así que no hay duración.
- **H7 — "Rutina de hoy" es la última rutina modificada y siempre su primer día.** ✅ Resuelto en la Fase 2: `GET /today` elige la rutina actual y rota los días. La racha semanal reemplaza a `MOCK_STREAK`.
- **H8 — Catálogo con grupos musculares duplicados.** V5 usa `Cuádriceps`/`Tríceps` y V6 inserta ejercicios con `Cuadriceps`/`Triceps`: aparecen como filtros distintos en el wizard. `SMITH_MACHINE` no tiene etiqueta en el frontend.
- **H9 — Vocabulario de objetivo inconsistente.** Onboarding y wizard usan `BUILD_MUSCLE`; las plantillas usan `HYPERTROPHY`. Solo la recomendación normaliza.
- **H10 — Ejercicios "personalizados" contaminan el catálogo global.** `findOrCreateExercise` crea un ejercicio global si el nombre no existe. Un nombre mal escrito queda visible para todos los usuarios.
- **H18 — Los registros sueltos contaban como entrenamientos.** ✅ Resuelto en la Fase 1 (V9, `kind = 'QUICK_LOG'`).
  `POST /exercises/{id}/logs` creaba una sesion `COMPLETED` por cada registro desde el detalle de un ejercicio, y sumaba en "entrenamientos completados" y "dias activos".

- **H11 — Textos hardcodeados en español** en el wizard y la pantalla de IA (`GOALS`, `LEVEL_LABELS`, `EQUIPMENT_LABELS`, chips de prompt), aunque la app tiene i18n `es`/`en`.

### Medios: deuda técnica

- **H12** — `RoutineTemplateController` (626 líneas) tiene SQL y lógica de negocio en el controller, y duplica mapeos de `RoutineRepository`.
- **H13** — Consultas N+1: listar rutinas hace una consulta por día y otra por ejercicio de cada día. Lo mismo en `/progress/activity`.
- **H14** — `routineCategoriesAvailable()` consulta `information_schema` en cada request. V2 ya es obligatoria, así que el chequeo sobra.
- **H15** — Cobertura de tests mínima: 1 test de contexto y 5 de `RoutineService`. Nada para registro de entrenamientos, plantillas ni frontend.
- **H16** — Secreto JWT por defecto en `application.properties` y `docker-compose.yml`. Refresh tokens sin implementar.
- **H17** — Restos del starter de Expo Router (`app/`, `components/hello-wave.tsx`, etc.) conviven con React Navigation.

## Lo que ya está bien resuelto (no rehacer)

- Esquema de datos: `routines → routine_days → routine_exercises` y `workout_sessions → workout_exercise_logs` ya soportan multi-día, rangos de reps (`reps_min/reps_max`), peso objetivo, tempo, set por set, RPE y notas. **No hace falta rediseñar el modelo**, solo usarlo bien.
- Backend de creación: `POST/PUT /routines` ya acepta `days[]` multi-día con validación.
- Plantillas globales en la misma tabla `routines` (`user_id NULL`, `template_key`), con recomendación por objetivo, nivel, días y duración.
- Sesión persistida con `expo-secure-store`, URL de API por variable de entorno con autodetección de IP LAN para Expo Go.
- Sistema de diseño (tokens dark/light, Manrope + Space Grotesk) y componentes base (`Screen`, `Typography`, `StepContainer`, `Stepper`, `ExerciseConfigCard`).
- i18n con `i18next` (es/en).

## Verificación realizada (2026-10-09)

- `tsc --noEmit` del frontend: sin errores.
- Último reporte Surefire del backend: 6 tests, 0 fallos.
- Revisión manual del código de backend (módulos `routine`, `progress`, migraciones V1–V6) y frontend (navegación, servicios, pantallas de rutinas y entrenamiento).
- No se levantó la app ni la base de datos; los hallazgos salen de leer el código.
