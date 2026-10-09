# Locomotora — Contexto técnico para agentes

Actualizado: 2026-10-09

Este es el punto de entrada para cualquier persona o agente que trabaje en el repositorio.

| Documento                                        | Para qué sirve                                                    |
| ------------------------------------------------ | ----------------------------------------------------------------- |
| [`SYSTEM_OVERVIEW.md`](SYSTEM_OVERVIEW.md)       | Etapa actual del sistema y hallazgos (H1, H2…)                    |
| [`ROADMAP_RUTINAS.md`](ROADMAP_RUTINAS.md)       | Plan de implementación: crear, entrenar y registrar rutinas       |
| [`API.md`](API.md)                               | Endpoints, contratos y ejecución local                            |
| [`DATABASE_SCHEMA.md`](DATABASE_SCHEMA.md)       | Tablas, migraciones y comportamiento de FKs                       |
| [`ROUTINE_TEMPLATES.md`](ROUTINE_TEMPLATES.md)   | Plantillas globales y recomendación                               |
| [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md)           | Reglas de UI                                                      |
| `design_handoff_locomotora_redesign/`            | Prototipo HTML del rediseño completo (referencia visual)          |
| `design_routine_wizard_redesign/`                | Prototipo HTML del wizard de creación de rutinas                  |

---

## 1. Qué es el sistema

App móvil de entrenamiento. El foco actual es el ciclo **crear rutina → entrenar → guardar registro**. Además tiene onboarding, perfil, métricas corporales, nutrición y un coach IA (simulado).

Arquitectura cliente-servidor: app Expo/React Native ↔ API REST Spring Boot ↔ PostgreSQL.

## 2. Repositorio

```
locomotora-app/                      ← la raíz NO es un repo git (tiene un .git vacío)
├── locomotora-backend/              ← repo principal (locomotora-back). Spring Boot 3.5 / Java 21
│   └── docs/                        ← esta documentación (de todo el sistema)
├── locomotora-frontend/             ← repo propio (locomotora-app). Expo 54 / RN 0.81 / TS 5.9
└── rediseno-creacion-rutinas.html   ← canvas de diseño exportado (2.5 MB), sin versionar
```

La documentación de todo el sistema, incluido el frontend, se commitea en el repo del backend.

El código vive en WSL (`/home/romero/my-stuff/locomotora-app`). Ejecutar las herramientas de Node desde WSL: desde Windows, `npx` falla con rutas UNC.

## 3. Backend

### 3.1 Stack

| Tecnología     | Detalle                                                                |
| -------------- | ---------------------------------------------------------------------- |
| Java           | 21                                                                     |
| Spring Boot    | 3.5.0 (web, security, validation, jdbc, data-jpa, actuator)            |
| Acceso a datos | `JdbcTemplate` con SQL explícito. Sin entidades JPA.                   |
| Migraciones    | Flyway, V1–V7. `ddl-auto=validate`.                                    |
| Base de datos  | PostgreSQL 17 (Docker, puerto local `5433`)                            |
| Seguridad      | Spring Security + JWT HS256 propio (`JwtService`), BCrypt              |
| Tests          | JUnit 5 + Mockito. 10 tests (contexto, `RoutineService`, `WorkoutLogService`). |

Variables relevantes (`application.properties`): `SPRING_DATASOURCE_URL`, `LOCOMOTORA_JWT_SECRET` (por defecto inseguro), `LOCOMOTORA_JWT_EXPIRATION_MINUTES` (1440), `LOCOMOTORA_CORS_ALLOWED_ORIGIN_PATTERNS`.

### 3.2 Paquetes

Arquitectura por módulo: `controller → service → repository (JdbcTemplate)`, DTOs como `record` en `dto/`.

```
com.locomotora.demo
├── config/SecurityConfig            filtros, CORS, BCrypt
├── security/                        JwtService, JwtAuthenticationFilter
├── common/                          ApiException, CurrentUser.id(), GlobalExceptionHandler
├── auth/                            /auth/register, /login
├── user/                            /me, /profile, /profile/photo, /profile/onboarding, /goals
├── routine/
│   ├── RoutineController            /routine, /routines, /routine-categories, /exercises, /workout-log
│   ├── RoutineTemplateController    /routines/templates, /routines/recommendation  ← SQL en el controller (H12)
│   ├── service/                     RoutineService (escritura), RoutineReadService (lectura),
│   │                                WorkoutLogService, ExerciseProgressService
│   ├── repository/                  RoutineRepository, RoutineWriteRepository, WorkoutSessionRepository
│   ├── model/                       RoutineHeader, RoutineDayHeader
│   └── dto/
├── progress/                        /progress/summary, /progress/activity, /metrics
├── nutrition/                       /nutrition, /nutrition/summary
└── ai/                              /ai/chat, /ai/conversations, /ai/recommendations
```

### 3.3 Seguridad y errores

- Stateless, CSRF deshabilitado. Públicos: `POST /login`, `POST /auth/register`, `GET /actuator/health`.
- JWT con `sub` (UUID), `email`, `iat`, `exp`. Firma comparada en tiempo constante.
- `CurrentUser.id()` devuelve el UUID del usuario autenticado.
- Tabla `refresh_tokens` creada pero sin flujo de refresh.
- Errores: `{ timestamp, status, message }`. `ApiException` define el status; validación → 400.

### 3.4 Flujo de rutinas (lo más relevante hoy)

- **Crear/editar** (`RoutineService`): normaliza y valida días y ejercicios, inserta la rutina con `source = 'MANUAL'`, y luego `replaceDays` **borra todos los días y los recrea**. Cada ejercicio se resuelve con `findOrCreateExercise` (por `exerciseId`, después por nombre, y si no existe crea uno global).
- **Leer** (`RoutineReadService`): arma `RoutineResponse` con `days[]` y `exercises` (primer día, legado). Consultas N+1.
- **Plantillas**: rutinas con `user_id IS NULL` y `source = 'TEMPLATE'`. "Usar" clona rutina, días y ejercicios al usuario.
- **Registrar** (`WorkoutLogService`): crea una sesión `COMPLETED` asociada al primer día y una fila de log por ejercicio.

Ver H1–H4 en `SYSTEM_OVERVIEW.md` para los problemas de este flujo.

## 4. Base de datos (resumen)

Detalle en `DATABASE_SCHEMA.md`.

```
users ─┬─ user_profiles (1:1)   user_goals   user_preferences   user_equipment   user_limitations
       ├─ routines ── routine_days ── routine_exercises ──► exercises (catálogo)
       │     └─► routine_categories
       ├─ workout_sessions ── workout_exercise_logs ──► exercises
       ├─ body_metrics   nutrition_entries
       └─ ai_conversations ── ai_messages     ai_recommendations
```

- Catálogo: 109 ejercicios globales (V5 + V6). 7 plantillas de hipertrofia multi-día (V6).
- Borrar `routine_days` borra en cascada `routine_exercises`, y deja en `NULL` `workout_sessions.routine_day_id` y `workout_exercise_logs.routine_exercise_id`.
- Rutinas: soft delete con `is_active = false`.

## 5. Frontend

### 5.1 Stack

Expo 54, React 19.1, React Native 0.81, TypeScript 5.9, React Navigation 7 (stack + bottom tabs), Axios, i18next (es/en), `expo-secure-store` (sesión e idioma), `expo-haptics`, `expo-image-picker`, fuentes Manrope y Space Grotesk.

Expo Router está instalado y quedan archivos del starter en `app/` y `components/`, pero **la app usa `src/navigation`** (entrada: `index.tsx`).

### 5.2 Estructura

```
src/
├── api/client.ts              Axios: baseURL por EXPO_PUBLIC_API_URL o IP LAN autodetectada, timeout 10 s
├── store/authContext.tsx      sesión persistida en SecureStore (isSessionHydrated, setSession, clearSession)
├── theme/                     colors.ts (dark/light), ThemeContext
├── constants/theme.ts         Spacing, FontFamily
├── i18n/                      index.ts, storage.ts, locales/es.json, locales/en.json
├── hooks/                     useAuth, useRoutine (rutina de hoy), useRoutines (CRUD)
├── services/                  authService, profileService, routineService, routineCategoryService,
│                              progressService, nutritionService, aiService
├── navigation/                AppNavigator, TabNavigator, RoutinesNavigator
├── screens/                   Auth, Profile (Onboarding, Profile), Home (Home, ExerciseGroup, ExerciseDetail),
│                              Routines (Routines, RoutineDetail, RoutineForm, RoutineWizard),
│                              Workout, Progress, AI
├── components/
│   ├── ui/                    Screen, Typography, Input, Pill, Badge, SelectCard, MultiSelectCard,
│   │                          ProgressStepper, StepContainer
│   ├── common/                Button, Card
│   ├── routines/wizard/       Stepper, FieldDropdown, ExerciseConfigCard   ← sin commitear (H5)
│   └── workout/               ExerciseCatalogGroupCard
└── utils/                     exerciseCatalog, aiRoutineGenerator (IA simulada), progressInsights
```

### 5.3 Navegación

```
AppNavigator (stack)
├── Login                         sin sesión
├── Onboarding                    con sesión y onboarding incompleto
├── Main → TabNavigator
│   ├── Home
│   ├── Routines → RoutinesNavigator (stack)
│   │   ├── RoutinesList          pestañas: Mis rutinas / Sugeridas IA / Plantillas
│   │   ├── RoutineDetail         { id, mode?: "user" | "template" }
│   │   ├── RoutineForm           entrada: "Generar con IA" o "Armar manual"
│   │   └── RoutineWizard         { id? } crear/editar manual, 3 pasos
│   ├── Progress
│   ├── AI
│   └── Profile
├── Workout                       { routine, dayId? }  pantalla completa
├── ExerciseGroup                 { groupName, exercises }
└── ExerciseDetail                { exercise }
```

El catálogo de ejercicios se carga una vez en `TabNavigator` y se pasa como props a `RoutinesNavigator`.

### 5.4 Estado de pantallas

| Pantalla          | Estado        | Notas                                                                                         |
| ----------------- | ------------- | --------------------------------------------------------------------------------------------- |
| Login             | ✅             | Login y registro                                                                              |
| Onboarding        | ✅             | Objetivo, nivel, días, equipo, datos físicos, limitaciones → `PUT /profile/onboarding`        |
| Home              | ⚠️             | Rutina de hoy real (siempre primer día), racha hardcodeada (`MOCK_STREAK = 7`)                |
| Routines (lista)  | ✅             | Mis rutinas, recomendación, plantillas, filtro por categoría                                  |
| RoutineDetail     | ✅             | Pestañas por día, editar/eliminar, usar plantilla, empezar entrenamiento del día elegido      |
| RoutineForm (IA)  | 🟡 Simulado    | Generador local por palabras clave + `setTimeout` de 1,6 s, un solo día                       |
| RoutineWizard     | ⚠️             | 3 pasos, un solo día; aplana rutinas multi-día al editar (H1)                                 |
| Workout           | ⚠️             | Peso/reps compartidos entre series (H3), sin persistencia ni timer (H6)                       |
| Progress          | ✅             | Resumen, actividad reciente, métricas, nutrición del día                                      |
| AI                | 🟡 Simulado    | Chat persistido; respuestas por palabras clave en backend                                     |
| Profile           | ✅             | Datos, objetivo, foto, tema claro/oscuro, idioma, logout                                      |

## 6. Decisiones de diseño

| Decisión                              | Detalle                                                                                                   |
| ------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| SQL explícito con `JdbcTemplate`      | Flyway es la fuente de verdad del esquema; no hay entidades JPA. Mantenerlo salvo decisión explícita.     |
| Plantillas en la tabla `routines`     | `user_id NULL` + `template_key`. Evita duplicar la lógica de días y ejercicios.                           |
| Soft delete de rutinas                | Mantiene la referencia desde `workout_sessions`.                                                          |
| `exercises` de la respuesta (legado)  | Lista plana del primer día; el código nuevo debe usar `days`.                                             |
| `PUT /goals` como reemplazo total     | El cliente siempre manda la lista completa.                                                               |
| JWT propio HS256                      | Sin librería externa; comparación de firma en tiempo constante.                                           |
| Catálogo compartido entre usuarios    | Hoy todos los ejercicios son globales. La Fase 1 del roadmap agrega ejercicios propios por usuario.       |

## 7. Convenciones

### Backend

- Módulo por dominio: `controller` → `service` → `repository`. Nada de SQL en controllers nuevos.
- Errores con `new ApiException(HttpStatus.X, "mensaje")`.
- Usuario actual con `CurrentUser.id()`.
- Todo cambio de esquema va en una migración Flyway nueva (`V7__…`). Nunca editar migraciones aplicadas.
- Secretos por variable de entorno.

### Frontend

- Toda llamada HTTP pasa por `src/api/client.ts` y un servicio en `src/services/`, que normaliza la respuesta.
- Colores desde `useTheme()`, espaciado y fuentes desde `constants/theme.ts`. No hardcodear colores.
- Textos visibles con `t("…")` en `es.json` y `en.json`.
- Para lógica con muchos estados (wizard, entrenamiento), preferir reducers puros testeables (ver roadmap).

## 8. Comandos

```bash
# Backend + base de datos (Docker)
cd locomotora-backend && docker compose up --build -d

# Backend local contra la base de Docker
cd locomotora-backend && ./mvnw spring-boot:run

# Tests backend
cd locomotora-backend && ./mvnw test

# Frontend (desde WSL)
cd locomotora-frontend && npm run start

# Chequeo de tipos del frontend
cd locomotora-frontend && npx tsc --noEmit
```
