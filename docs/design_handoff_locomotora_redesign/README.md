# Handoff: Rediseño de app de rutinas (Locomotora)

## Overview
Rediseño completo del frontend de la app de entrenamiento personalizado: onboarding, home, rutinas (manual/IA/plantillas), coach IA conversacional, sesión de entrenamiento activa, progreso y perfil (light/dark mode). Estilo visual: colores sólidos (sin gradientes), iconografía emoji, tipografía Manrope + Space Grotesk.

## About the Design Files
The file `Locomotora.dc.html` in this bundle is a **design reference built in HTML** (a Design Component prototype) — it shows intended look, layout, states, and interactions. It is **not production code to copy directly**. The task is to **recreate this design inside the existing `locomotora-frontend` codebase** (React Native + Expo Router, TypeScript, React Navigation), reusing its existing screens under `src/screens/*`, its navigation structure (`TabNavigator`, `RoutinesNavigator`, `AppNavigator`), its theme system (`src/constants/theme.ts`, `hooks/use-theme-color.ts`), its i18n setup (`src/i18n`), and its services/hooks (`src/services/*`, `src/hooks/*`) for data instead of the mocked in-memory state used in the prototype.

## Fidelity
**High-fidelity (hifi).** Colors, typography, spacing, copy (in Spanish), and component layout in the HTML file are final — recreate them pixel-close using React Native primitives (`View`, `Text`, `Pressable`/`TouchableOpacity`, `TextInput`, `ScrollView`/`FlatList`) and the existing theming approach, not with raw HTML/CSS.

## Screens / Views

### 1. Onboarding (5 steps)
- Step 1 — Objetivo: 4 selectable cards (icon 42×42 rounded-13px chip, title 16/700, desc 12.5px secondary), radio-style selection (border + soft bg highlight + check dot when selected).
- Step 2 — Nivel y frecuencia: 3 pill chips (Principiante/Intermedio/Avanzado) in a row, then 5 day-count pills (2–6 días) wrapping.
- Step 3 — Equipo: 2-column grid of 6 toggle cards (icon + label), multi-select.
- Step 4 — Datos físicos: height/weight number inputs side by side (48px height, 14px radius), optional injury/limitation text input.
- Step 5 — Generando: spinning ring (84px, 3px border, accent top-color, 1.1s linear spin) with sparkle icon center, 3 pulsing checklist rows animating in sequence, auto-advances to Home after ~2.2s.
- Persistent top bar: back button (36px circle) + 5-dot step indicator (active dot widens to 20px) + footer CTA button (52px, full width, pill).

### 2. Home
- Header: greeting "Hola, {name} 👋" + subtitle, circular avatar button (42px) top-right → profile.
- Streak banner: soft-primary background card, flame emoji (38px) right, streak count + label left.
- 3-up stat cards (icon, big value, label) in a grid.
- "Tu rutina de hoy" card: title, exercise-count/duration meta, split badge, up to 4 exercise preview rows (dot + name + sets×reps), full-width primary CTA "Empezar entrenamiento".
- AI tip card: sparkle icon chip, tip copy, chevron to open Coach.
- Horizontal-scroll muscle-group chips ("Explorar ejercicios").

### 3. Rutinas — list
- Header "Rutinas" + inline "+ Nueva" pill button.
- Segmented control: Mis rutinas / Sugeridas IA / Plantillas.
- Horizontal category filter chips (Todas/Fuerza/Hipertrofia/Resistencia).
- Routine cards: title/description, "Mía"/"IA" badge, meta chip row (días/sem, duración, # ejercicios).

### 4. Rutinas — detail
- Back button, title, description, meta chip row, day tabs (Día 1/2/3), exercise list rows (thumb icon, name, sets×reps, rest time), full-width "Iniciar entrenamiento" CTA.

### 5. Rutinas — create
- Mode segmented toggle: "✨ Generar con IA" / "Armar manual".
- **AI mode**: prompt suggestion chips, freeform textarea, "Generar rutina" CTA → loading state (spinner + copy) → result card (title/desc/meta/exercise preview) with Regenerar (secondary) / Usar esta rutina (primary) actions.
- **Manual mode**: routine name input, scrollable exercise catalog list with toggle-select rows (rotating "+" becomes "×" check when selected), selected-count label, "Guardar rutina" CTA.

### 6. Coach IA (chat)
- Header: sparkle avatar chip, "Coach IA" title + subtitle.
- Scrollable message list: coach bubbles left-aligned (rounded 16/16/16/4, soft-alt bg), user bubbles right-aligned (primary bg, dark/contrast text, rounded 16/16/4/16), typing indicator bubble, inline routine-suggestion card (title/desc/meta + "Usar esta rutina" button) when the coach proposes a plan.
- Quick-reply chips row above the input.
- Input row: pill text input + circular send button.

### 7. Progreso
- Title, 3-up stat cards (racha, entrenos, peso delta).
- "Esta semana": 7-day row, dot indicator per day (filled = trained).
- "Volumen levantado": simple bar chart (6 weekly bars, fixed heights, primary color).
- "Récords personales": list rows (trophy icon, exercise name + date, PR value in primary color).

### 8. Perfil
- Avatar (58px) + name + "Miembro desde…" caption.
- Goal/days/experience meta chips row (with emoji).
- "Apariencia" section: dark-mode row with pill switch (custom-built, not native Switch — track 48×28, thumb 22px sliding).
- "Cuenta" section: menu rows (icon chip, label, chevron) — Editar datos de onboarding, Notificaciones, Privacidad, Ayuda y soporte.

### Bottom nav bar (main app only)
5 tabs: Inicio, Rutinas, Coach IA, Progreso, Perfil. Icon + 10px label, active = primary color, inactive = secondary text color. Blurred translucent background, top border.

### Active workout session (full-screen overlay, replaces main app+nav)
- Top bar: close (X) button, progress track bar (fills by exercise index/total), counter label "n / total".
- Exercise name + muscle/equipment subtitle, large icon hero block (150px, soft-primary bg).
- Sets table: header row (SET/KG/REPS/blank), one row per set with editable-looking weight/reps values and a circular check toggle (filled when completed).
- Rest-suggestion card (clock icon + suggested rest text).
- Footer: square secondary "back" button + full-width primary "next exercise / finalizar entrenamiento" button (label changes on last exercise).

## Interactions & Behavior
- **Onboarding**: linear stepper; Next disabled until required fields for that step are filled (goal on step 1; experience+days on step 2; height+weight on step 4). Step 4→5 triggers a simulated ~2.2s "generating" delay, then routes to Home.
- **Theme toggle**: single switch in Profile flips entire app between light/dark palettes (see Design Tokens) — no per-screen overrides.
- **Routine creation (AI)**: textarea + prompt chips → "Generar rutina" shows a ~1.6s loading state → renders a result card → user can Regenerate (new fake result) or Accept (adds to "Mis rutinas" and returns to list).
- **Routine creation (manual)**: multi-select exercise catalog rows toggle a filled circular check (rotated + to make an × affordance); Save creates a routine with default 3×10 sets per selected exercise and returns to list.
- **Coach chat**: user message appends immediately; ~1.3s simulated "typing" delay before the coach reply appears; replies matching fitness/routine keywords include an inline routine-suggestion card with an "Usar esta rutina" action that adds the routine and jumps to Rutinas.
- **Workout session**: tapping a set's check circle toggles its completed state (visual only, doesn't gate progression); Next Exercise advances index (last exercise button becomes "Finalizar entrenamiento" and ends the session); Back steps back; X ends the session at any point.
- **Animations**: step content fades/slides in (`floatUp`, 0.4s ease) on step change and on AI result reveal; onboarding spinner and generating-checklist items use continuous `spin`/`pulse` keyframe loops while active — implement as reanimated/Animated equivalents, not literal CSS keyframes.
- No hover states (mobile-only prototype); all interactive elements are tap targets ≥ 36px, primary CTAs are 52px tall.

## State Management
Prototype state (to be replaced by real store/services, keeping the shape as a guide):
- `theme`: 'dark' | 'light'
- `ob`: `{ step, goal, experience, days, equipment[], height, weight, limitation }` — onboarding answers, should persist to the user's profile via `authService`/`profileService`.
- `activeTab`: which bottom-nav tab is active.
- `routinesView`: 'list' | 'detail' | 'create'; `routinesTab`: 'mine' | 'ai' | 'templates'; `selectedCategory`; `selectedRoutineId`; `selectedDayIdx`.
- `createMode`: 'ai' | 'manual'; `aiPrompt`; `aiGenerating`; `aiResult`; `manualTitle`; `manualSelected[]` — replace the simulated `setTimeout` AI generation with a real call via `src/services/aiService.ts`.
- `coachMessages[]`, `coachInput`, `coachTyping` — replace simulated coach reply logic with a real conversational endpoint.
- `workout`: `{ active, routine, exIdx, checkedSets }` — set-completion should persist per session (see `src/hooks/useRoutine.ts` / `useRoutines.ts` and `progressService.ts` for where completed sets/volume likely need to be logged for the Progress screen).
- `userRoutines[]` — replace with data from `routineService.ts` / `routineCategoryService.ts`.

## Design Tokens

### Colors — Dark (default)
- bg: `#03060B` · phoneBg (screen bg): `#070B14` · surface: `#111A2B` · card: `#131C30` · cardAlt: `#182036`
- border: `rgba(255,255,255,0.07)`
- text: `#F8FAFC` · textSecondary: `#8B98AC`
- primary: `#22C55E` · primarySoft: `rgba(34,197,94,0.14)`
- accent: `#F97316` · accentSoft: `rgba(249,115,22,0.14)`
- danger: `#EF4444` · lime: `#A3E635`
- primary-contrast text (on primary buttons): `#03150A`

### Colors — Light
- bg: `#EEF2F0` · phoneBg: `#F8FAFC` · surface/card: `#FFFFFF` · cardAlt: `#F1F5F4`
- border: `#E2E8E4`
- text: `#0F172A` · textSecondary: `#5B6B66`
- primary: `#16A34A` · primarySoft: `rgba(22,163,74,0.10)`
- accent: `#EA580C` · accentSoft: `rgba(234,88,12,0.10)`
- danger: `#DC2626` · lime: `#65A30D`
- primary-contrast text: `#052e13`

### Typography
- Headings ("Space Grotesk", 500/600/700): H1 26px/700/line-height 32px (18–19px for greeting/routine title); page titles 24px/700.
- Body ("Manrope", 400/500/600/700/800): body 14–16px; secondary/meta 12–13.5px; tiny meta/labels 10.5–11.5px, often 700–800 weight with letter-spacing .5–1.2px for eyebrows/uppercase labels.

### Spacing / Radius
- Screen horizontal padding: 20–24px. Card/section gap: 8–18px depending on density.
- Radius scale: pills/buttons 999px; large cards 18–22px; medium cards/rows 14–16px; small chips/icon-wraps 12–13px.
- Icon-chip sizes: 38–42px square, 12–13px radius. Avatar: 42px (small) / 58px (profile) circular.

### Shadows
- Only the outer phone-frame shadow in the prototype (`0 30px 80px rgba(0,0,0,0.5)`) — not applicable to a native app frame; the actual mobile UI itself is flat/borderless (no drop shadows on cards, just 1px borders).

## Assets
- Icons: emoji glyphs only (💪 🔥 🎯 📊 ✨ 🏆 📅 ⏱️ 🧍 🏋️ 🥋 🔴 🔔 ⚙️ etc.) — no icon files; map to `@expo/vector-icons` only if the team prefers vector icons for accessibility/scaling, otherwise render emoji as `Text`.
- Fonts: Manrope (400/500/600/700/800) and Space Grotesk (500/600/700) via Google Fonts — bundle as local fonts with `expo-font` rather than a web `<link>`.
- No raster images/photos used anywhere in this design.

## Files
- `Locomotora.dc.html` — the full interactive HTML prototype (all screens, states, styles, and mock logic in one file).
