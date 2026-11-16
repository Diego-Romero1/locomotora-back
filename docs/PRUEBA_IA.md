# Prueba manual del agente IA (Qwen 3.8 27B en GroqCloud)

Guia para poner la clave de Groq y comprobar a mano que el coach y "Crear con IA" funcionan. Las variables y el contrato estan en `API.md` → IA.

## 1. Crear la clave

1. Entra a https://console.groq.com y crea una cuenta.
2. Ve a **API Keys** → **Create API Key**. Copia la clave (empieza con `gsk_`); Groq no la vuelve a mostrar.
3. Recomendado: en **Settings → Data Controls** activa **Zero Data Retention**, para que Groq no guarde los mensajes ni siquiera para diagnostico.
4. En **Settings → Limits** mira los limites de `qwen/qwen3.8-27b` de tu plan. Si son distintos de 8.000 tokens/min y 200.000 tokens/dia, ajusta `LOCOMOTORA_AI_DAILY_TOKEN_BUDGET` (por ejemplo, el 75 % del limite diario).

Nunca pongas la clave en el frontend, en `docker-compose.yml` ni en un commit.

## 2. Donde poner la clave

### Con Docker (lo normal)

```bash
cd locomotora-backend
cp .env.example .env
```

Edita `locomotora-backend/.env` y completa:

```
GROQ_API_KEY=gsk_...
```

`.env` esta en `.gitignore` y en `.dockerignore`. Despues reconstruye y levanta el backend:

```bash
cd locomotora-backend && docker compose up --build -d
```

Comprueba que el contenedor ve la clave, sin imprimirla:

```bash
docker exec locomotora-backend sh -c 'test -n "$GROQ_API_KEY" && echo "clave cargada" || echo "falta GROQ_API_KEY"'
```

### Sin Docker (backend en la terminal)

```bash
cd locomotora-backend && GROQ_API_KEY=gsk_... sh ./mvnw spring-boot:run
```

## 3. Prueba por la API

En una terminal, con el backend en `http://127.0.0.1:8080`. Usa un usuario de prueba (o tu email y contraseña en `/login`):

```bash
B=http://127.0.0.1:8080
T=$(curl -s -X POST $B/auth/register -H 'content-type: application/json' \
  -d '{"name":"Prueba IA","email":"prueba-ia@test.local","password":"Password123!"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')
```

Si el usuario ya existe, usa `/login` con `{"email":"prueba-ia@test.local","password":"Password123!"}`.

**3.1 La IA esta activa.** Debe decir `"configured":true` y `"selectable":false`:

```bash
curl -s $B/ai/models -H "Authorization: Bearer $T"
```

**3.2 Pregunta simple.** `source` debe ser `"AI"` y `model` `"qwen/qwen3.8-27b"`:

```bash
curl -s -X POST $B/ai/chat -H "Authorization: Bearer $T" -H 'content-type: application/json' \
  -d '{"message":"¿Cuanta proteina necesito para ganar musculo?"}'
```

**3.3 Pregunta que usa herramientas.** El agente deberia consultar el catalogo y nombrar ejercicios reales:

```bash
curl -s -X POST $B/ai/chat -H "Authorization: Bearer $T" -H 'content-type: application/json' \
  -d '{"message":"Dame 3 ejercicios de espalda que pueda hacer con mi equipo"}'
```

**3.4 Seguridad.** La respuesta debe terminar con un aviso ⚠️ que recomienda consultar a un medico:

```bash
curl -s -X POST $B/ai/chat -H "Authorization: Bearer $T" -H 'content-type: application/json' \
  -d '{"message":"Me dio dolor en el pecho haciendo sentadillas, ¿sigo?"}'
```

**3.5 Generar una rutina.** Debe devolver 3 dias con ejercicios del catalogo; `warnings` puede venir vacio:

```bash
curl -s -X POST $B/routines/generate -H "Authorization: Bearer $T" -H 'content-type: application/json' \
  -d '{"daysPerWeek":3,"sessionMinutes":60,"objective":"BUILD_MUSCLE","level":"INTERMEDIATE","prompt":"Quiero priorizar pecho"}'
```

**3.6 Que quedo guardado.** Herramientas usadas por mensaje y tokens del dia:

```bash
docker exec locomotora-postgres psql -U postgres -d locomotora -c \
  "SELECT m.role, m.model, m.metadata->>'source' AS source, m.metadata->>'toolCalls' AS tools, left(m.content, 60) AS texto
   FROM ai_messages m JOIN ai_conversations c ON c.id = m.conversation_id JOIN users u ON u.id = c.user_id
   WHERE u.email = 'prueba-ia@test.local' ORDER BY m.created_at"
docker exec locomotora-postgres psql -U postgres -d locomotora -c \
  "SELECT feature, model, input_tokens, output_tokens, created_at FROM ai_usage ORDER BY created_at DESC LIMIT 10"
```

Para borrar el usuario de prueba al terminar:

```bash
docker exec locomotora-postgres psql -U postgres -d locomotora -c "DELETE FROM users WHERE email = 'prueba-ia@test.local'"
```

## 4. Prueba en el telefono

1. Abre la app con el backend actualizado (`npm run start` en `locomotora-frontend`).
2. **Perfil → Coach IA:** debe mostrar "Qwen 3.8 27B" como modelo en uso, sin opciones para elegir, y el uso del dia.
3. **Hoy → tarjeta del coach:** pregunta "¿Cómo voy con mis entrenamientos?". Con sesiones guardadas, debe citar fechas o volumen reales (usa `get_recent_training`).
4. Pregunta "¿Cómo mejoro mi press de banca?" y despues "Dame una rutina de 4 días": debe aparecer la tarjeta "Crear rutina con IA".
5. **Rutinas → Nueva rutina → Con IA:** genera una rutina y toca "Usar esta rutina". Debe abrir el paso Revisar del asistente; al guardar queda con origen IA.

## 5. Si algo no anda

| Sintoma | Causa probable | Que hacer |
| ------- | -------------- | --------- |
| `"configured":false` | El backend no ve la clave | Revisa `.env` y reconstruye con `docker compose up --build -d` |
| `source: "LOCAL"` en todas las respuestas | Clave invalida (401), presupuesto diario agotado o Groq caido | `docker logs locomotora-backend 2>&1 \| grep Groq` muestra el estado HTTP de cada llamada |
| `Groq error ... status=401` | Clave mal copiada o revocada | Crea otra clave en la consola |
| `Groq error ... status=429 ... limit=OTPM` | Limite de 1.000 tokens de salida por minuto (plan gratis) | Espera un minuto. Una rutina usa casi todo ese minuto: el chat justo despues puede responder con reglas locales |
| `Groq error ... status=429 ... limit=TPM` | Limite de 8.000 tokens por minuto | Espera un minuto; si pasa seguido, baja `LOCOMOTORA_AI_MAX_TOOL_ROUNDS` a 1 |
| `Groq: la peticion supera el limite por minuto de la cuenta` | `LOCOMOTORA_AI_*_MAX_TOKENS` mayor que el OTPM de tu plan | Vuelve a 700 (chat) y 1000 (rutinas), o mejora el plan en Groq |
| `/routines/generate` responde 502 | La respuesta no cumplio el esquema o se corto (`json_validate_failed`) | Reintenta. Solo con un plan con mas OTPM conviene subir `LOCOMOTORA_AI_ROUTINE_MAX_TOKENS` |
| Rutinas pobres o repetitivas | El modelo responde sin razonar (`none`) | Prueba `LOCOMOTORA_AI_ROUTINE_REASONING_EFFORT=low` en `.env` (gasta mas tokens) |

Los logs del backend nunca muestran la clave ni el texto de los mensajes: solo modelo, estado, latencia y tokens.

## 6. Resultado de la prueba con la clave real (15 nov 2026)

Chat (9 preguntas) y rutinas (2 a 6 dias) respondidos por Qwen 3.8 27B, de 0,6 a 8 segundos. Se corrigio durante la prueba: booleanos en herramientas (`tool_use_failed`), el limite OTPM de 1.000, rutinas de 4 o mas dias cortadas por tokens, negritas markdown en las respuestas y ejercicios por encima de la cabeza con molestias de hombro. Ver el detalle en `ROADMAP_RUTINAS.md`.

## 7. Una verificacion pendiente

La documentacion de Groq se contradice sobre `reasoning_effort` para Qwen 3.8 27B: una pagina dice que `default` es el modo con razonamiento y otra que no devuelve razonamiento, y no queda claro si los tokens de razonamiento cuentan para el limite por minuto. Por eso la app usa `none` (sin razonar). Con la clave real conviene comparar una rutina con `none` y otra con `low` (paso 3.5) y mirar los tokens en `ai_usage`.
