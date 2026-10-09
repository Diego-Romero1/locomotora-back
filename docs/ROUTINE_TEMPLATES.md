# Plantillas de rutinas de hipertrofia

Actualizado: 2026-10-09

## Objetivo

Sistema de plantillas globales de hipertrofia para gimnasio que permite:

- recomendar una rutina compatible con el perfil del usuario,
- mostrar el detalle multi-dia,
- clonar la plantilla a la cuenta del usuario para usarla como rutina propia,
- sumar otros objetivos y categorias en el futuro sin cambiar el modelo.

## Decision de modelo

No hay tabla separada de templates. Se reutiliza `routines`:

- plantilla global: `user_id = NULL`
- origen: `source = 'TEMPLATE'`
- identificador funcional estable: `template_key`
- tipo de split: `routine_split`
- categoria: Hipertrofia (`category_id`)

Asi se reutiliza toda la logica de dias y ejercicios.

## Plantillas disponibles (V6)

| `template_key`                         | Nivel        | Dias | Min | Split             |
| -------------------------------------- | ------------ | ---- | --- | ----------------- |
| `FULL_BODY_BEGINNER_3_DAYS`            | Principiante | 3    | 55  | `FULL_BODY`       |
| `UPPER_LOWER_BEGINNER_4_DAYS`          | Principiante | 4    | 60  | `UPPER_LOWER`     |
| `UPPER_LOWER_INTERMEDIATE_4_DAYS`      | Intermedio   | 4    | 70  | `UPPER_LOWER`     |
| `PUSH_PULL_LEGS_INTERMEDIATE_3_DAYS`   | Intermedio   | 3    | 65  | `PUSH_PULL_LEGS`  |
| `PUSH_PULL_LEGS_INTERMEDIATE_6_DAYS`   | Intermedio   | 6    | 75  | `PUSH_PULL_LEGS`  |
| `PPL_UPPER_LOWER_INTERMEDIATE_5_DAYS`  | Intermedio   | 5    | 70  | `PPL_UPPER_LOWER` |
| `BRO_SPLIT_ADVANCED_5_DAYS`            | Avanzado     | 5    | 80  | `BRO_SPLIT`       |

Todas tienen `objective = 'HYPERTROPHY'`.

## Respuesta API

Se devuelve un `RoutineResponse` con `routineSplit`, `source`, `isTemplate`, `templateKey`, `days` y `exercises` (lista plana del primer dia, por compatibilidad). Ver `API.md`.

## Clonacion

`POST /routines/templates/{id}/use` copia rutina, dias y ejercicios a la cuenta del usuario:

- la copia queda con `source = 'TEMPLATE'` pero sin `template_key`, por lo que `isTemplate = false` y es editable;
- para entrenar una plantilla hay que clonarla primero (`/workout-log` rechaza rutinas que no son del usuario).

## Recomendacion

Heuristica simple y explicable (`RoutineTemplateController`):

- objetivo principal del usuario (`BUILD_MUSCLE` se normaliza a `HYPERTROPHY`),
- nivel de experiencia,
- dias disponibles por semana,
- duracion por sesion,
- `user_preferences.preferred_training_style` como desempate.

## Limitaciones actuales

- **Editar una plantilla clonada la aplana a un solo dia** (H1 en `SYSTEM_OVERVIEW.md`). Corregido por la Fase 1 del roadmap; hasta entonces, la tarea 0.3 bloquea la edicion de rutinas multi-dia.
- La recomendacion no usa `user_equipment` ni `user_limitations`, aunque el onboarding ya los guarda.
- Solo hay plantillas de hipertrofia.
- La logica y el SQL viven en el controller (H12).
- No hay tests de recomendacion ni de clonacion.

## Siguientes pasos

Ver `ROADMAP_RUTINAS.md`:

1. Wizard v2 multi-dia que edite plantillas clonadas sin perder dias (Fase 1).
2. CTA "Usar y empezar" desde el detalle de la plantilla (tarea 2.F8).
3. Mover la logica a `RoutineTemplateService` con tests (tareas 4.1 y 4.3).
4. Filtrar por equipamiento y limitaciones del usuario.
5. Plantillas para otros objetivos (fuerza, perdida de grasa, resistencia).
