# Plan de implementación — Rutinas: crear, entrenar y registrar

Actualizado: 2026-11-18

> Prerrequisito: leer [`SYSTEM_OVERVIEW.md`](SYSTEM_OVERVIEW.md). Los códigos H1, H2… refieren a los hallazgos de ese documento.
> Tamaños: **S** = menos de 1 día, **M** = 1 a 3 días, **L** = más de 3 días (una persona).

## Plan vigente (aprobado el 2026-11-11)

El 2026-11-11 se aprobo reordenar las fases para registrar bien antes de crear mas facil: cada sesion guardada con `/workout-log` perdia las series para siempre, y las plantillas ya cubrian las rutinas de varios dias. Las tareas de abajo conservan sus codigos originales; esta tabla dice en que fase nueva cae cada una.

| Fase nueva                    | Contenido                                                                                       | Tareas originales                    | Estado        |
| ----------------------------- | ----------------------------------------------------------------------------------------------- | ------------------------------------ | ------------- |
| 1. Registro fiel              | V9, `POST /workout-sessions` idempotente, estado por serie guardado en el telefono, pausar/retomar/descartar, cierre con RPE y notas, cola de envio, tests | 2.B1, 2.B2, 2.B5, 2.F1, 2.F6, 2.F7, 4.2 (reducer) | ✅ 2026-11-11 |
| 2. Entrenar sin friccion      | "La vez pasada" y prellenado, botones + y −, timer de descanso, lista del dia, saltar o reemplazar ejercicio, pantalla Hoy con rotacion de dias (`GET /today`) y barra de 4 pestañas | 2.B3, 2.B4, 2.F2, 2.F3, 2.F4, 2.F5, 2.F8 | ✅ 2026-11-12 |
| 3. Medir el rendimiento       | Historial paginado, detalle, editar y borrar sesiones, progreso por ejercicio, records, racha semanal | 3.1–3.5                              | ✅ 2026-11-12 |
| 4. Crear facil                | Wizard v2 multi-dia, punto de partida, duplicar, ejercicios propios, i18n del wizard            | Fase 1 original (1.F1–1.F8, 1.B2–1.B5) | ✅ 2026-11-12 |
| 5. Base tecnica e IA real     | Testcontainers, refactor de plantillas, N+1, JWT/refresh, OpenAPI, restos del starter, IA real  | Fase 4 y Fase 5 originales           | 🟡 IA real ✅ 2026-11-13; base tecnica pendiente |

Decisiones aprobadas junto con el plan:

- **Sin señal primero:** la sesion vive en el telefono (AsyncStorage) y se envia una sola vez al final, con un `clientSessionId` para reintentar sin duplicar.
- **Plan del momento en cada serie:** el log copia reps objetivo, peso objetivo y descanso; la sesion copia los titulos de rutina y dia.
- **Cuatro pestañas** (Hoy, Rutinas, Progreso, Perfil); el coach IA pasa a "Crear con IA" y a una tarjeta en Hoy. Hecho en la Fase 2.
- **Racha semanal** (semanas seguidas cumpliendo los dias objetivo). Se adelanto a la Fase 2 porque la pantalla Hoy la muestra.
- **Series de calentamiento** (`set_type`): opcional, despues de la Fase 3.

## Rediseño de Rutinas R1–R4 (aprobado el 2026-11-16)

Propuesta de experiencia de uso para crear, editar y entrenar, publicada como artefacto ("Rediseño de Rutinas"). Se aprobó completa, con este orden: R1 Entrenar, R2 Rutina actual, R3 Editar en sitio, R4 Crear.

| Fase | Contenido | Estado |
| ---- | --------- | ------ |
| R1 · Entrenar | Una sola lista de series, peso sugerido, avance automático, saltar / hacer después, técnica, terminar con pendientes, resumen con cambios a la rutina, pantalla encendida | ✅ 2026-11-17 |
| R2 · Rutina actual | `current_routine_id`, ciclo de días en Hoy, Rutinas y detalle | ✅ 2026-11-17 |
| R3 · Editar en sitio | Modo edición en el detalle, hoja de ejercicio, arrastrar | ✅ 2026-11-17 |
| R4 · Crear | Una sola puerta con vista previa, asistente de 3 pasos, sugerencias por día | ✅ 2026-11-17 |

### Avance de R1 (2026-11-17, rama `main`)

Backend:

- `instructions` del catálogo en cada ejercicio de la rutina (`ExerciseResponse`, rutinas y plantillas). Sin cambios en el envío de la sesión.

Frontend:

- Estado de la sesión v3 (`workoutSession.ts`), con migración desde v1 y v2: día en el ciclo y el que sigue, nota y técnica por ejercicio, series del plan, saltado, reemplazo recordado, descanso con `advanceTo`.
- Acciones nuevas: saltar (o terminar antes) y deshacer, hacer después (reubica el descanso), aplicar el peso sugerido. Al completar un ejercicio el descanso apunta al siguiente pendiente y, al terminar o saltarlo, la pantalla pasa sola; sin descanso configurado pasa enseguida. Elegir otro ejercicio a mano cancela el salto. Corregir una serie hecha no arrastra el peso a las pendientes.
- `progressionSuggestion()`: doble progresión con las series de mayor peso de la vez pasada (sube 2,5 kg, o 1 kg con mancuernas; repite; o busca más reps). Se aplica con un toque.
- `routineChanges.ts`: reemplazos y series distintas al plan; el resumen ofrece "Guardar en la rutina" y lo aplica con `PUT /routines/{id}` conservando ids.
- Pantalla dividida en piezas: `WorkoutHeader` (día, tiempo, barra por series), `SetList`, `SuggestionCard`, `RestTimerSheet` (ejercicio terminado o próxima serie con su peso), `WorkoutSheets` (menú, técnica, terminar), `WorkoutSummary` (comparación con la vez pasada con la misma cantidad de series, esfuerzo en 5 opciones guardado como RPE 3/5/7/9/10, próximo día). `ExerciseListSheet` muestra estados, deshacer salto y terminar. Botón principal abajo; vibración corta al registrar.
- `expo-keep-awake` (dependencia nueva): la pantalla no se apaga durante el entrenamiento.
- Corregido en `Screen`: en web, pasar de "cargando" a contenido dejaba el detalle de rutina en blanco (se mezclaban `flex` y `flexGrow`).
- 27 tests nuevos (161 en total). Backend: 154 tests.
- Probado en Expo web (390×844) contra el backend en Docker: sugerencia y aplicación, descanso con ajuste de peso, avance automático, saltar, técnica, corregir, lista, terminar con pendientes, resumen y guardado (la sesión no incluye el ejercicio saltado y la rutina quedó con la serie agregada).

Pendiente de R1: probar en un teléfono (vibración, pantalla encendida y hojas en iOS).

### Avance de R4 (2026-11-17, rama `main`)

Sin cambios de backend.

Frontend:

- "Nueva rutina" es la única puerta: recomendada para ti ("Ver rutina"), plantillas con filtro por cantidad de días, con IA y desde cero. Duplicar una propia queda en el menú "⋯" de esa rutina.
- Vista previa (detalle en modo plantilla): avisos de si encaja con el perfil (equipo que falta, duración contra tu tiempo, ejercicios que cargan una zona con molestia, otro objetivo; `profileFit.ts`, reusa las reglas del plan del onboarding). "Usar esta rutina" la copia y la deja como actual; "Personalizar" abre el editor en modo crear.
- El editor de R3 ahora también crea (`RoutineEditor` con `routineId = null`, ruta `RoutineEdit`): plantilla o resultado de la IA, con "Usar como rutina actual" (activado por defecto) y confirmación al salir sin guardar.
- IA: "Usar esta rutina" (la guarda como AI y queda actual) y "Personalizar" (editor en modo crear). Antes abría el paso 4 del asistente.
- Desde cero en 3 pasos (`RoutineBuilderScreen` reescrito): estructura (días y minutos del perfil, split recomendado, nombre y músculos de cada día; objetivo y nivel plegados en "Ajustes"), ejercicios (el mismo `DraftDaysEditor` del editor) y revisar (nombre en un solo lugar, series por semana, avisos, "Usar como rutina actual", "Guardar y empezar"). Retomar un borrador es una tarjeta (antes un Alert, que no funcionaba en web). Confirmaciones con `ConfirmSheet`.
- "Completar con sugerencias" (`suggestExercisesForDay`, reglas locales): un ejercicio por músculo del día (grandes primero) y un segundo hasta llenar los minutos de la sesión (3 a 8), con el equipo del perfil, sin ejercicios avanzados para principiantes (primero máquinas y mancuernas), sin los que cargan una zona con molestia y prefiriendo no repetir los de otros días. Si el día no tiene músculos marcados (o las plantillas traen una descripción), usa los músculos de sus ejercicios.
- `DraftDaysEditor` (días y ejercicios) lo comparten el editor y el asistente. `useExerciseCatalog` comparte el catálogo; `useFitProfile` lee el perfil.
- Al crear una rutina por cualquier camino, "Volver" lleva a la lista de rutinas (`openCreatedRoutine`).
- Se borraron `DraftExerciseCard` y los textos del asistente viejo.
- 12 tests nuevos (190 en total). Lint: 6 errores, todos anteriores (uno menos: el del asistente viejo).
- Probado en Expo web contra el backend en Docker, con un perfil de principiante con mancuernas y máquinas, 45 min y molestia de rodilla: avisos de la vista previa, personalizar y descartar, filtro de plantillas, usar plantilla (queda actual y "Volver" va a Rutinas), desde cero con sugerencias en los 3 días (máquinas, sin barra ni ejercicios de rodilla) y guardar.

Pendiente de R4: probar en un teléfono y probar "Usar esta rutina" / "Personalizar" con una generación real de la IA (no se generó para no gastar cuota).

## Catálogo de ejercicios C1–C3 (propuesta 5, 2026-11-17)

Propuesta "Catálogo" en el artefacto de rutinas: catálogo amplio por grupo muscular, lista con texto grande y vista previa. Fases: C1 datos, C2 dónde vive el catálogo en la app y su lista, C3 imágenes. Decisiones tomadas por defecto (sin respuesta del usuario): datos de free-exercise-db traducidos por Claude, importar los 876, 16 músculos. Las imágenes de free-exercise-db no se usan (origen sin licencia clara).

| Fase | Contenido | Estado |
| ---- | --------- | ------ |
| C1 · Datos | V16 con 891 ejercicios globales, traducción, `is_core`, zonas de carga, lista liviana + detalle, tope del catálogo para la IA, 16 músculos | ✅ 2026-11-17 |
| C2 · Catálogo en la app | Rutinas › Ejercicios con grupos (mapa muscular y contador), lista con texto grande y filtros, detalle con "Agregar a una rutina", la misma fila en los selectores, catálogo guardado en el teléfono | ✅ 2026-11-18 |
| C3 · Imágenes | Fuente con licencia clara, miniaturas (`thumb_url`) | Pendiente |

### Avance de C1 (2026-11-17, rama `main`)

- `tools/exercise-catalog/`: fuente vendorizada (`SOURCE.md` con commit y licencia), `existing_map.json` (los 109 ejercicios de las migraciones → id de la fuente, con nombres en español para los que estaban en inglés), `translations/*.json` (780 ejercicios: nombre y 2 a 4 pasos con "tú") y `build_catalog.py`, que valida (traducciones completas, nombres únicos contra el catálogo actual y entre sí, músculos y equipos mapeados) y genera `V16__exercise_catalog.sql`. Para cambiar el catálogo se edita la fuente o las traducciones y se genera una migración nueva; V16 no se toca.
- V16: columnas `source`, `external_id`, `name_en`, `exercise_type` (STRENGTH, STRETCHING, CARDIO, PLYOMETRICS, POWERLIFTING, OLYMPIC, STRONGMAN), `mechanic`, `force_type`, `is_core` y las de media (`image_url`, `thumb_url`, `media_license`, `media_author`, `media_source_url`, vacías hasta C3). Los ejercicios actuales conservan su id (rutinas e historial) y se enriquecen; los nuevos usan un uuid determinista y `ON CONFLICT DO NOTHING`. `category` pasa a ser la etiqueta visible por tipo (Hipertrofia, Fuerza, Halterofilia, Strongman, Movilidad, Pliometría, Cardio).
- 16 músculos (`ExerciseGroups.MUSCLES`): se suman Lumbares, Trapecio, Cuello, Antebrazos, Aductores y Abductores. Grupo de navegación (`muscleGroup`): Lumbares y Trapecio en Espalda, Cuello en Hombros, piernas juntas en Piernas, y aparte Estiramientos y Cardio y pliometría.
- `is_core`: los 109 actuales más 44 nuevos que cubren los músculos y el equipo nuevos (153). Zonas de carga de los nuevos por reglas sobre el nombre en inglés, coherentes con V13; los estiramientos no cargan zonas.
- API: `GET /exercises` liviano (sin `description` ni `instructions`, con ETag débil y gzip: ~47 KB comprimido, 304 si no cambió) y `GET /exercises/{id}` con el detalle. Equipo nuevo `EZ_BAR`.
- IA: `ExerciseCatalogFilter.forPrompt` deja fuera estiramientos y cardio y manda como mucho 120 ejercicios (primero los core; si el usuario pide grupos, el resto de esos grupos, compuestos antes). `search_exercises` devuelve primero los core. El equipo del perfil cubre KETTLEBELL, BAND y EZ_BAR.
- Frontend: tipos nuevos, `useExerciseDetail` trae la técnica al abrir el detalle o la hoja de técnica (reemplazos en el entrenamiento), Inicio agrupa por `muscleGroup` en orden fijo con los habituales primero, el selector ordena propios → habituales → resto y busca también por nombre en inglés, la sugerencia por día prefiere habituales y compuestos y no usa estiramientos ni cardio.
- Tests: `ExerciseCatalogMigrationTest` (Testcontainers: migra a V15, guarda ids y aplica V16), `ExerciseCatalogFilterTest`, y en el frontend sugerencias y grupos de Inicio. Backend 167 tests, frontend 192.

Pendiente de C1: generar una rutina real con la IA para medir los tokens del prompt (no se generó para no gastar cuota).

### Avance de C2 (2026-11-18, rama `main`)

Decisiones de la propuesta aplicadas con la recomendación: el catálogo vive en Rutinas con el selector "Mis rutinas | Ejercicios" (sale de Hoy) y la vista previa es un mapa muscular propio (`react-native-body-highlighter` 3.2.0, MIT).

- Catálogo único en el teléfono (`useExerciseCatalog`): una sola copia en memoria para toda la app, guardada en AsyncStorage por usuario con su ETag y revalidada una vez por apertura (`If-None-Match` → 304). Sin red se usa la copia guardada (selector, reemplazo al entrenar, catálogo). `TabNavigator` ya no lo descarga por su cuenta. Backend: CORS expone `ETag`.
- Mapa muscular (`MuscleMap`): principal en el color de la app y secundarios más suaves; en miniaturas, recorte con zoom a torso, cintura o piernas según el músculo. Los 16 músculos se traducen a zonas del dibujo (`muscleMap.ts`; Abductores usa glúteo porque el dibujo no los tiene).
- Pantallas: Rutinas › Ejercicios (buscador sin acentos y en inglés + una tarjeta por grupo con su zona y cantidad, y "Todos"), `ExerciseList` (filtro por músculo en los grupos con varios, por equipo y "Míos"), `ExerciseDetail` (mapa de frente y espalda, equipo, nivel y tipo, pasos numerados, "Ver mi progreso" y "Agregar a una rutina"). Las rutas pasan el grupo o el id, ya no la lista entera.
- "Agregar a una rutina": rutina propia → día → se suma al final con las series y reps del objetivo (`addExerciseToRoutine`, mismo reducer del editor) y `PUT /routines/{id}`. No repite un ejercicio en el mismo día.
- La misma fila (`ExerciseRow`: mapa, nombre a 17 px y equipo · nivel · tipo) en el catálogo, el selector del editor y el reemplazo al entrenar; la hoja de técnica muestra el mapa.
- Se quitó la sección de ejercicios de Hoy y el registro suelto de una serie desde el detalle (el endpoint `POST /exercises/{id}/logs` sigue).
- Tests: agregar a una rutina, mapa muscular, búsqueda y orden, grupos y pasos (frontend 201). Jest usa el mock oficial de AsyncStorage.

Pendiente de C2: probar en un teléfono (rendimiento del mapa en listas largas y toques sobre las miniaturas).

### Avance de R3 (2026-11-17, rama `main`)

Sin cambios de backend: `PUT /routines/{id}` ya sincroniza por id.

Frontend:

- "Editar" en el detalle ya no abre el asistente de 4 pasos: la misma pantalla pasa a modo edición (`RoutineEditor`). Nombre, días con su cantidad de ejercicios y "+ Día", ejercicios como filas compactas, "+ Agregar ejercicio", ajustes plegados (objetivo, nivel, minutos) y "Guardar cambios" fijo abajo (activo solo si hay cambios).
- Hoja del ejercicio (`ExerciseEditSheet`): series, atajos de reps (4–6 … 15–20) y de descanso (0:45 … 3:00) con "Otro" para el valor exacto, nota para cuando entrenes, reemplazar (el selector de ejercicios tiene modo de selección única), subir/bajar y quitar.
- Ordenar arrastrando el asa (`EditableExerciseList`, con `PanResponder`: funciona en teléfono y en web); las demás filas se corren para mostrar dónde cae y la fila no sale de la lista. Subir/bajar queda como alternativa.
- Opciones del día (`DayOptionsSheet`): nombre, duplicar, copiar ejercicios de otro día (antes era un Alert), mover antes/después y quitar.
- Quitar un ejercicio o un día ofrece "Deshacer". Descartar con cambios pide confirmación, también al salir con el gesto o el botón atrás (`beforeRemove`). Aviso si hay un entrenamiento de esa rutina en curso. Aviso de días sin ejercicios (se quitan al guardar).
- Borrador: acciones nuevas `moveExerciseTo`, `replaceExercise` (conserva la fila), `duplicateDay`, `restoreDay`, nota en `updateExercise` (hasta 300 caracteres) y `draftSignature` para saber si hay cambios.
- `BottomSheet` sube con el teclado en iOS.
- 10 tests nuevos (178 en total). Lint sin errores nuevos (7, todos anteriores).
- Probado en Expo web contra el backend en Docker: atajos, nota, arrastrar, duplicar día, quitar y deshacer, agregar, descartar y guardar. En la base, los días y ejercicios existentes conservaron su id; el día duplicado y el ejercicio agregado son filas nuevas.

Pendiente de R3: probar en un teléfono el arrastre y el gesto atrás con cambios sin guardar (en la web el historial del navegador no pasa por la navegación de la app).

### Avance de R2 (2026-11-17, rama `main`)

Backend:

- `V15__current_routine.sql`: `user_profiles.current_routine_id` (FK con `ON DELETE SET NULL`; borrar una rutina, que es un soft delete, también limpia la elección).
- `CurrentRoutineService`: resuelve la rutina actual (la elegida, o la regla anterior como respaldo) y agrega a cada rutina propia `current`, `lastTrainedAt`, `nextDayId` y `lastTrainedAt` por día. Lo usan `/today`, `GET /routines`, `GET /routines/{id}` y las respuestas de crear, editar y duplicar. `PUT /routines/{id}/current` elige la actual.
- `TrainingService` ya no calcula la rutina ni el día por su cuenta (se quitó `findLastTrainedDayId`).
- 8 tests nuevos (162 en total).

Frontend:

- `features/routines/routineCycle.ts`: estados del ciclo (hecho en esta vuelta, toca, pendiente), fechas relativas y minutos estimados por día. `DayCycleStrip` (tira compartida) y `DayPickerSheet` (elegir otro día).
- Hoy: tira del ciclo, "Día 2 de 4 · 6 ejercicios · ~55 min", "Hacer otro día" en una hoja (antes llevaba al detalle) y "Ver rutina".
- Rutinas: sin pestañas ni filtro de categorías. Arriba la rutina actual con su ciclo y "Empezar <día>"; debajo las demás con cuándo se usaron. Plantillas, recomendada e IA siguen en "Nueva rutina".
- Detalle: barra propia (volver, Editar, "⋯" con usar como actual, duplicar y eliminar con confirmación), insignia "Rutina actual" o "Usar como rutina actual", tira del ciclo para elegir el día, última marca por ejercicio, técnica y "Ver progreso" al tocar un ejercicio, botón fijo "Empezar <día> · ~N min". Se refresca al volver.
- `BottomSheet` pasa a `components/ui`. Se borraron los textos que solo usaba la lista vieja.
- 7 tests nuevos (168 en total). Lint: 7 errores (eran 9), todos anteriores.
- Probado en Expo web contra el backend en Docker: con dos rutinas, la recién creada era la de Hoy (regla vieja); al elegir la otra como actual, Hoy, Rutinas y el detalle pasaron a mostrarla, con su ciclo y la última marca de cada ejercicio.

Pendiente de R2: probar en un teléfono.

### Avance de la Fase 1 (2026-11-11, rama `main`)

Backend:

- `V9__workout_sessions_v2.sql`: `client_session_id` + `UNIQUE (user_id, client_session_id)`, `kind` (`ROUTINE`/`FREE`/`QUICK_LOG`), `routine_title`, `day_title`; en logs `exercise_position`, `target_reps_min`, `target_reps_max`, `target_weight_kg`, `rest_seconds`; indice `(exercise_id, created_at desc)`. Marca como `QUICK_LOG` los registros sueltos ya existentes.
- `WorkoutSessionController` + `WorkoutSessionService` + `WorkoutSessionWriteRepository`: `POST /workout-sessions` (contrato en `API.md`).
- `POST /exercises/{id}/logs` crea sesiones `QUICK_LOG`, que ya no cuentan en `/progress/summary` ni en el contexto del coach (H18).
- JSON mal formado responde 400 en lugar de 500.
- 11 tests nuevos en `WorkoutSessionServiceTest` (25 en total). Probado contra Postgres: reintento devuelve `200` y la misma sesion, una fila por serie, duracion real.

Frontend:

- `src/features/workout/`: `workoutSession.ts` (reducer puro por serie, duracion sin pausas, armado del envio), `syncQueue.ts` (cola con reintento y clasificacion de errores), `workoutStorage.ts` (AsyncStorage por usuario), `WorkoutSessionContext.tsx` (estado global, guardado en cada cambio, reenvio al volver a la app) y `useStartWorkout.ts` (no pisa una sesion en curso).
- `WorkoutScreen` reescrita: peso y reps por serie, agregar o quitar series, salir pausa, cierre con duracion, volumen, RPE y notas, descartar con confirmacion.
- Inicio muestra "Entrenamiento en curso · Continuar" y las sesiones sin enviar o rechazadas.
- La pantalla de entrenamiento ya no recibe la rutina por parametro (`Workout: undefined`). Se quito `postWorkoutLog` del frontend.
- `jest-expo` configurado: 33 tests de reducer y cola (`npm test`). Se agrego `@react-native-async-storage/async-storage`.
- Probado: el envio que arma la app contra el backend real coincide en series, volumen y duracion.

Pendiente de la Fase 1: probarla en un telefono real (modo avion, matar la app a mitad), porque hasta ahora se verifico con tests y contra la API.

### Avance de la Fase 2 (2026-11-12, rama `main`)

Backend:

- `GET /today?tz=` (`TrainingController`, `TrainingService`, `TrainingStatsRepository`): rutina actual (la mas reciente entre la ultima entrenada y la ultima editada), dia que toca con rotacion por `day_index`, semana en la zona horaria del telefono, racha semanal y ultimo entrenamiento. Reglas puras en `TrainingCalendar`.
- `GET /exercises/last-performance?ids=`: series de la ultima vez y mejor serie historica (Epley) por ejercicio.
- `equipment` en cada ejercicio de la respuesta de rutina, para el salto de los botones + y −.
- Parametros mal formados (`ids`, falta de parametros) responden 400 en lugar de 500.
- 13 tests nuevos (38 en total). Probado contra Postgres: rotacion A → B → C, semana en hora de Buenos Aires, ultima vez y mejor serie correctas, 400 con ids o zona invalidos.

Frontend:

- Pantalla de entrenamiento: la serie activa se muestra grande con botones + y − (2,5 kg; 1 kg con mancuernas; el peso nuevo se arrastra a las pendientes) y un boton "Registrar serie". Linea "La vez pasada" y aviso cuando los valores serian record. Al abrir la sesion se prellenan peso y reps con la vez pasada.
- Timer de descanso: arranca al registrar una serie con el descanso del ejercicio, +15 s y saltar. Se guarda como hora de fin, asi que sigue bien con la pantalla bloqueada. Al terminar vibra en pantalla y, fuera de la app, avisa con una notificacion local (`expo-notifications`).
- Lista de los ejercicios del dia para saltar a cualquiera y reemplazar uno sin series hechas, con buscador filtrado por el mismo grupo muscular.
- Cierre con los records de la sesion.
- Pantalla Hoy (`TodayCard`, `useToday`): semana con los dias entrenados, racha semanal, dia que toca con sus ejercicios y "Empezar", "Elegir otro dia", ultimo entrenamiento. Se quito `MOCK_STREAK` y el hook `useRoutine`.
- Barra de 4 pestañas: Hoy, Rutinas, Progreso, Perfil. El coach sigue en la tarjeta de Hoy.
- El estado guardado paso a la version 2; una sesion de la Fase 1 se migra al abrirla.
- 16 tests nuevos de reducer (49 en total).

Pendiente de la Fase 2: probar en un telefono el timer con la pantalla bloqueada y la notificacion. En Expo Go las notificaciones locales funcionan; las remotas no hacen falta.

### Revision antes de la Fase 3 (2026-11-12)

Se reviso lo commiteado de las Fases 1 y 2: tests, tipos y lint en verde; el backend de Docker ya corre la Fase 2. En la base no hay todavia sesiones guardadas desde la app con el formato nuevo, asi que falta la prueba en el telefono. Se corrigieron dos problemas:

- Guardar mientras habia un envio en curso dejaba la sesion en la cola hasta el siguiente disparador. Ahora el envio se repite al terminar si se pidio otro mientras tanto.
- `expo-notifications` se cargaba al abrir la app, y en Expo Go (Android) eso muestra un error por las notificaciones remotas. Ahora se carga al programar el primer aviso de descanso.

### Avance de la Fase 3 (2026-11-12, rama `main`)

Backend (`WorkoutHistoryController`, `WorkoutHistoryService`, `WorkoutHistoryRepository`, contratos en `API.md`):

- `GET /workout-sessions` paginado por cursor, sin registros sueltos, con cantidad de records por sesion.
- `GET /workout-sessions/{id}` con cada serie, el plan del momento y la serie record marcada.
- `PUT /workout-sessions/{id}` corrige valores de series, borra series, esfuerzo y notas, en una transaccion. `DELETE` borra la sesion.
- `GET /progress/exercises`, `GET /exercises/{id}/history` (puntos por sesion y records de todo el historial) y `GET /progress/weekly` (semanas en la zona horaria del telefono, con ceros).
- Record = superar la mejor fuerza estimada (Epley) de las sesiones anteriores; la primera vez no cuenta.
- 14 tests nuevos (52 en total). Probado contra Postgres: paginacion, records, correccion con recalculo del volumen, rollback al intentar dejar la sesion sin series, 404 tras borrar, semanas con ceros.

Frontend:

- Progreso con cuatro secciones: Resumen (racha semanal, dias de la semana, total, volumen de 8 semanas, mejores marcas), Historial (por mes, paginado, con las sesiones sin enviar arriba), Ejercicios (con buscador) y Cuerpo (medidas y nutricion, lo que ya existia).
- Detalle de sesion: series contra el plan, records, notas; modo edicion para corregir valores, quitar series, esfuerzo y notas, y borrar la sesion.
- Progreso de un ejercicio: grafico (fuerza estimada, mas peso o volumen) con `react-native-svg`, records que abren su sesion y sesiones recientes.
- "Ultimo entrenamiento" en Hoy abre su detalle.
- Se borraron `utils/progressInsights.ts` (racha diaria, volumen y records calculados con las ultimas 10 sesiones) y el uso de `/progress/activity`.
- 7 tests nuevos de agrupado y escalas de graficos (56 en total). Contrato verificado: los tipos de la app coinciden campo por campo con las respuestas del backend.

Pendiente de la Fase 3: probar las pantallas en un telefono.

### Avance de la Fase 4 (2026-11-12, rama `main`)

Antes de empezar se reviso el estado: las Fases 1 a 3 estan commiteadas y los tests en verde. Sigue sin haber sesiones guardadas desde la app con el formato nuevo (falta la prueba en el telefono).

Backend:

- `V10__own_exercises_and_objectives.sql`: `exercises.created_by_user_id` (ejercicios propios); el nombre es unico entre los globales y, por separado, dentro de los propios de cada usuario. Objetivo `HYPERTROPHY` → `BUILD_MUSCLE` en las rutinas (H9).
- `V11__deferrable_exercise_references.sql`: las FK hacia `exercises` se verifican al final de la transaccion. Sin esto, borrar un usuario con ejercicios propios en uso fallaba por el orden de la cascada (se encontro en la prueba contra Postgres).
- `GET /exercises` devuelve globales + propios (`custom`); `POST /exercises` crea uno propio (o devuelve el existente con ese nombre). `findOrCreateExercise` ya no crea ejercicios globales (H10) y solo acepta ejercicios que el usuario ve; `POST /workout-sessions` tambien.
- `POST /routines/{id}/duplicate`. `source` (MANUAL, AI, TEMPLATE) al crear (1.B4). `Objectives` unifica el vocabulario de objetivo y valida (1.B5).
- 7 tests nuevos (59 en total).

Frontend:

- `src/features/routineBuilder/routineDraft.ts`: borrador puro con claves locales e ids de servidor, splits y dias propuestos, valores por objetivo, conversion `routineToDraft` / `draftToPayload` que conserva rangos, avisos de revision (1.F1, 1.F2). `draftStorage.ts` guarda el borrador de una rutina nueva en el telefono (1.F5).
- `RoutineBuilderScreen` (Wizard v2, 4 pasos): base; estructura (dias por semana, split recomendado, renombrar, ordenar, musculos de cada dia, agregar o quitar dias); ejercicios por dia (pestañas, catalogo filtrado por los musculos del dia, varios a la vez, ejercicio propio, rango de reps, copiar de otro dia, quitar con deshacer); revisar (dias con minutos estimados, series por semana por musculo, avisos que no bloquean, "Guardar y empezar el dia 1") (1.F3, 1.F4).
- `NewRoutineScreen`: recomendada (usar y empezar, o personalizar), plantillas, desde cero, duplicar una propia, con IA. Plantilla, copia e IA abren el wizard en revision (1.F6). El resultado de IA se guarda como `AI`.
- El detalle de rutina ya edita rutinas de varios dias (se quito la proteccion de la Fase 0, 1.F8) y suma "Duplicar".
- Textos del wizard y de la pantalla de IA en i18n (1.F7, H11). Se borraron el wizard viejo y `ExerciseConfigCard`.
- 17 tests nuevos del borrador (73 en total).
- Probado contra Postgres con el codigo real del borrador: una plantilla de 6 dias se clona y se edita (renombrar, cambiar un rango, repetir un ejercicio, quitar un dia) sin perder dias ni rangos (36 de 36), ningun dia cambia de id y el historial sigue mostrando el dia entrenado.

Pendiente de la Fase 4: probar el wizard en un telefono y medir el criterio de "rutina de 4 dias desde cero en menos de 2 minutos".

### Cambio de proveedor: Qwen 3.8 27B en GroqCloud (2026-11-15, rama `main`)

Se saco la integracion con Claude y el agente pasa a usar `qwen/qwen3.8-27b` en GroqCloud. Decisiones del usuario: mantener el mecanismo de modelos con solo Qwen permitido (Perfil lo muestra sin poder elegir), que el backend filtre el catalogo y la rutina salga de una sola llamada con JSON estricto, y pruebas de integracion con Testcontainers. Contrato de la API sin cambios incompatibles, salvo `source = AI` en lugar de `CLAUDE` y el campo nuevo `selectable` en `/ai/models`.

Backend:

- `GroqModelClient` (`RestClient`, sin SDK) reemplaza a `AnthropicModelClient`; se quito la dependencia `anthropic-java`. Reintenta una vez ante 429/498 con `retry-after` corto y ante 500/502/503. Los logs no guardan contenido.
- `AiModelClient` con herramientas (`ToolSpec`, `ToolCall`, turnos `TOOL`) y salida JSON estricta. Groq no permite usar las dos en la misma llamada: el chat usa herramientas y las rutinas JSON.
- Agente del coach (`AiService.runAgent`): hasta 2 rondas de herramientas y un presupuesto de 7.000 tokens por mensaje (el plan gratis de Groq da 8.000 por minuto); despues pide la respuesta final sin herramientas. Un registro en `ai_usage` por mensaje, con todos sus tokens.
- `AiToolRegistry`: `search_exercises`, `list_my_routines`, `get_routine`, `get_recent_training`, `get_exercise_progress`. Solo lectura, con el usuario autenticado, argumentos validados y resultados recortados. Se guardan en `ai_messages` con rol `TOOL` (ya permitido desde V1).
- `ExerciseCatalogFilter`: el equipo del onboarding (DUMBBELLS, MACHINES...) se traduce al del catalogo (DUMBBELL, MACHINE, CABLE...). Lo usan la herramienta y la generacion de rutinas.
- `PersonalDataFilter` (emails, telefonos, numeros largos) y `SafetyGuard` (riesgos antes, avisos fijos y revision de la respuesta despues).
- Rutinas: reglas nuevas (principiante hasta 4 series; avisos `DAY_TOO_LONG`, `HIGH_VOLUME`, `SEE_PROFESSIONAL`). Guardar sigue pasando por `POST /routines` y sus validaciones.
- Tope diario global de tokens (`LOCOMOTORA_AI_DAILY_TOKEN_BUDGET`) calculado con `ai_usage`. Sin tablas nuevas.
- `.env.example`; `.env` ignorado por git y por la imagen de Docker. Testcontainers 1.21.4 (compatible con Docker 29).
- 120 tests (36 nuevos): cliente Groq contra un servidor HTTP simulado, herramientas, agente, seguridad y datos personales, y una prueba de integracion con Postgres real y un Groq falso (herramientas con alcance por usuario, JSON estricto, guardado, 429 y presupuesto).

Frontend: textos sin "Claude", `source = AI`, Perfil › Coach IA muestra el modelo en uso sin elegir, avisos nuevos de rutina.

Prueba con la clave real (2026-11-15): chat de 9 preguntas (proteina, ejercicios con su equipo, su rutina, sus sesiones, dolor en el pecho, dieta de 800 kcal, intento de inyeccion, datos personales, seguimiento con historial) y rutinas de 2 a 6 dias, todo respondido por Qwen. Hallazgos corregidos:

- Qwen escribe los booleanos como `True` y Groq rechazaba la llamada a la herramienta (400 `tool_use_failed`): el parametro pasa a texto (`equipment_scope`) y, si vuelve a pasar, el agente responde sin herramientas.
- La cuenta gratis tiene un limite no documentado de 1.000 tokens de salida por minuto (OTPM): `max_completion_tokens` del chat baja a 700 y el de rutinas a 1.000; hasta 2 reintentos por 429 corto; "Request too large" no se reintenta y se registra.
- Las rutinas de 4 o mas dias se cortaban por tokens (`json_validate_failed`): esquema compacto (`reps` como texto, una indicacion por dia, nivel/duracion/foco calculados por el backend) y cantidad de dias fija con `minItems`/`maxItems`. Medido: 2 dias 438 tokens, 3 dias 512, 4 dias 715, 5 dias 819, 6 dias 855-859.
- Respuestas con negritas markdown: se piden en texto plano y el backend las quita.
- Con molestia de hombro, la rutina de 6 dias incluia presses por encima de la cabeza: reglas por lesion en el prompt.
- El agente no mencionaba los datos ocultados: el prompt explica `[email]`/`[telefono]`.

Pendiente: probar en el telefono y decidir el `reasoning_effort` de las rutinas (hoy `none`; razonar gasta tokens de salida, que son el limite mas chico).

### Coach: pestaña y UX (2026-11-16)

- La barra vuelve a 5 pestañas (Hoy, Rutinas, Coach, Progreso, Perfil), como en el handoff de diseño. La barra se oculta con el teclado en el Coach.
- Pantalla del coach: retoma la ultima conversacion; historial en hoja inferior (abrir, nueva, borrar); bienvenida con saludo, 4 preguntas sugeridas (tecnica, nutricion, progreso, mi rutina), acceso a "Crear rutina con IA" y aviso de que no reemplaza a un profesional; respuestas con listas y el aviso de seguridad destacado; chips con las herramientas que uso el agente ("Reviso tus sesiones", "Busco ejercicios"); indicador de escritura que pasa a "Consultando tus datos…"; error con "Toca para reintentar"; modelo y uso del dia en el encabezado, aviso cuando quedan 5 mensajes y campo bloqueado al llegar al limite.
- Backend: `toolsUsed` en `POST /ai/chat`, `DELETE /ai/conversations/{id}`, lista de conversaciones limitada a 50, nombres de equipo legibles en `search_exercises`, prompts con tildes (el modelo imitaba la falta de tildes) y "tu" en lugar de "vos", objetivo en palabras en el coach local.
- Verificado en el navegador (Expo web, 390×844) contra Qwen real: bienvenida, respuesta, herramientas, tarjeta de rutina, historial y retomar conversacion. 127 tests backend y 86 frontend.

### Coach: solo responde la IA y teclado (2026-11-16)

- Auditoria: en la base habia 14 respuestas predeterminadas (13 del motor de palabras clave de junio y 1 del coach local del 14 nov, sin clave) y 2 de Qwen. Ninguna migracion inserta mensajes: las generaba `LocalCoach`.
- Se quito `LocalCoach` y la respuesta fija de rechazo. Si Qwen no responde, `/ai/chat` devuelve 429/503/502 con el motivo y no guarda nada (ni la pregunta ni la conversacion); la pregunta, las llamadas a herramientas y la respuesta se guardan juntas solo cuando hay respuesta. El aviso de seguridad fijo solo se agrega si el modelo no derivo a un profesional.
- Se borraron las 14 respuestas predeterminadas, sus preguntas y las conversaciones que quedaron vacias (respaldo en `respaldos/2026-11-16-respuestas-automaticas-borradas.csv`, fuera de los repos).
- Teclado: el coach usaba `KeyboardAvoidingView` solo en iOS; en Android (edge-to-edge desde Expo SDK 58) el teclado tapaba el campo. Ahora usa `padding` en las dos plataformas y lleva el ultimo mensaje a la vista al abrir el teclado. Falta probarlo en el telefono.
- App: errores distinguidos (IA no activa, limite propio, presupuesto de la app, Groq saturado, sin respuesta) con "Toca para reintentar"; sin IA activa el campo se bloquea con la explicacion. 129 tests backend y 86 frontend.

### Coach: alcance solo fitness (2026-11-16)

- Problema: el agente respondia cualquier cosa ("¿cuanto es 2 + 2?" → "4.", quien fue Cristobal Colon).
- Reglas de alcance en el prompt (que responde, que no, como decidir y como rechazar) sin sobre-restringir: ante la duda se interpreta en clave de fitness, los calculos de entrenamiento y nutricion siguen permitidos, las preguntas mixtas se responden en su parte de fitness y el rechazo lo escribe el modelo con una propuesta concreta.
- Recordatorio de alcance al final del ultimo mensaje del usuario (solo hacia el modelo): con historial que ya tenia respuestas fuera de tema, Qwen seguia el patron en 2 de 4 intentos; con el recordatorio, 0 de 4, y las preguntas de fitness se siguen respondiendo.
- El contexto del usuario pasa los codigos del perfil en palabras ("barra", "ganar músculo", "intermedio"): con codigos en ingles el modelo decia "barbell" y una vez respondio en ingles.
- El aviso de menos de 1.200 kcal ya no salta con calorias quemadas ("correr 30 minutos quema 300 kcal").
- Evaluado contra Qwen real: 9/9 preguntas fuera de tema rechazadas (incluido "ignora tus reglas"), la mixta respondida solo en su parte de fitness, 10/10 de fitness respondidas (incluidas conversiones, volumen, sueño, creatina, frase motivacional y uso de la app) y la conversacion "contaminada" rechazada. 131 tests backend.

### Avance de la Fase 5 · IA real con Claude (2026-11-13, rama `main`) — reemplazada el 2026-11-15

Decision: Claude (API de Anthropic, SDK `anthropic-java` 3.3.0) con **modelo intercambiable**. Cada usuario elige en Perfil que modelo usa para el chat y para crear rutinas; el servidor define que modelos se ofrecen y cual es el de por defecto, sin recompilar.

| Tarea   | Modelo por defecto | Por que                                                       |
| ------- | ------------------ | ------------------------------------------------------------- |
| Coach   | `claude-haiku-5-5` | Rapido y barato para preguntas frecuentes                     |
| Rutinas | `claude-sonnet-5-5`| Mas razonamiento para un plan semanal coherente               |
| Opcion  | `claude-opus-5-5`  | El mas capaz; se ofrece pero no es el de por defecto          |

Backend:

- `V12__ai_settings_and_usage.sql`: `user_ai_settings` (modelo elegido por tarea; NULL = por defecto) y `ai_usage` (una fila por llamada, con tokens).
- Paquete `ai`: `AiProperties` (`locomotora.ai.*`), `AiModelCatalog` (modelos permitidos y si piden respaldo en el servidor), `AiModelClient` + `AnthropicModelClient` (endpoint beta de mensajes; Sonnet y Opus con `fallbacks: default`, Haiku sin ellos; esfuerzo medio en el chat y alto en rutinas; sin `temperature` ni `thinking`), `AiSettingsService`, `AiContextRepository` + `UserContext` (perfil, objetivos, limitaciones, equipo, rutinas activas, ultimas 6 sesiones, mejores series de 90 dias).
- `POST /ai/chat` con Claude: historial de 12 mensajes, contexto del usuario en el system prompt, reglas de seguridad (no diagnostica, no dietas extremas). Sin clave o si Claude falla responde el coach local (`source = LOCAL`). La llamada al modelo no ocurre dentro de una transaccion.
- Corregido: el chat aceptaba un `conversationId` de otro usuario; ahora responde 404 antes de guardar nada.
- `POST /routines/generate`: salida estructurada (JSON schema) con ejercicios referidos por numero del catalogo visible del usuario; se valida en codigo (solo ejercicios del catalogo, sin repetidos por dia, rangos de series, reps y descanso, como mucho los dias pedidos) y devuelve un borrador con avisos. 503 sin clave, 429 al pasar el limite diario, 422 si el modelo se niega, 502 si la respuesta no sirve.
- `GET /ai/models` y `PUT /ai/preferences`. Limites diarios configurables (40 mensajes, 6 rutinas).
- Rutas inexistentes responden 404 (antes 500) y un metodo no soportado 405.
- 25 tests nuevos con el cliente simulado (84 en total). Probado de punta a punta con el backend real apuntando a un servidor falso de Claude (`LOCOMOTORA_AI_BASE_URL`): cabecera beta, `fallbacks`, `output_config` con esfuerzo y `json_schema`, historial alternado, uso y tokens guardados.

Frontend:

- `RoutineAIScreen` llama a `POST /routines/generate` con peticion libre, dias, minutos y modelo; muestra todos los dias con series, rango, descanso y nota, los avisos y el modelo usado. "Usar esta rutina" abre el wizard en revision (`generatedToRoutine` → `routineToDraft`, origen AI). Sin clave ofrece crear la rutina con el asistente.
- `AIScreen`: muestra el modelo que respondio, marca las respuestas basicas, mensajes claros para limite, IA no activa o sin conexion. Si la pregunta pide una rutina ofrece "Crear rutina con IA" con el texto ya cargado (antes armaba una rutina local de un dia).
- Perfil: seccion "Coach IA" (`AiModelSettings`) para elegir el modelo del chat y el de rutinas, con velocidad, costo, el de por defecto y el uso del dia. Se guarda al tocar.
- Se borro `src/utils/aiRoutineGenerator.ts`. Textos de IA en "tu" (quedaban en voseo).
- 7 tests nuevos (80 en total).

Para activarla (historico): `ANTHROPIC_API_KEY`. Desde el 2026-11-15 es `GROQ_API_KEY` (ver `PRUEBA_IA.md`).

Pendiente de la Fase 5: probar con una clave real (calidad de las respuestas y rutinas, tiempos), los 12 errores de lint de React Compiler, `set_type` (calentamiento) y la base tecnica (4.1, 4.3–4.8).

---

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

**Avance (2026-10-09)**, rama `fase-1-update-preserva-ids`:

- **1.B1 ✅** `RoutineService.syncDays` reemplaza a `replaceDays`; migración V8 con restricciones diferidas. 4 tests unitarios nuevos y prueba contra Postgres: intercambiar días, reordenar, mover un ejercicio de un día borrado a otro y quitar un día. El historial conserva día y ejercicio.
- **Adelanto de 1.F2:** el wizard actual envía los `id` de día y ejercicio al editar una rutina de un día, y conserva el título y el foco del día.
- **1.B6 parcial:** cubierto con tests unitarios (Mockito); los de integración con Testcontainers quedan para 4.1.
- Pendiente conocido: el wizard guarda reps como valor único, así que al editar se pierden los rangos (`repsMin`/`repsMax`). Se resuelve con el modelo de borrador del Wizard v2 (1.F2).

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

✅ Hecho el 2026-11-13 con Claude y pasado a Qwen 3.8 27B en GroqCloud el 2026-11-15 (ver arriba).

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
