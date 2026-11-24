# Despliegue gratis: Render + Neon

Para uso personal. La app móvil se distribuye como APK, así que solo se despliega el backend.

| Pieza | Servicio | Plan gratis |
|---|---|---|
| Backend (este `Dockerfile`) | [Render](https://render.com) – Web Service | 512 MB, 0,1 CPU, se apaga tras 15 min sin tráfico y tarda en despertar |
| PostgreSQL | [Neon](https://neon.com) | 1 GB por proyecto, 100 CU-horas al mes, se suspende tras 5 min y despierta sola |

La base de datos gratuita de Render no sirve porque se borra a los 30 días.

## 1. Neon (base de datos)
1. Crea un proyecto con **Postgres 17** en la región **AWS US East 2 (Ohio)** (`aws-us-east-2`), que es la que está junto al backend.
2. En *Connect*, desactiva *Connection pooling*. Flyway y Hibernate usan *prepared statements*, que no funcionan bien con PgBouncer en modo transacción.
3. De la cadena `postgresql://USUARIO:CLAVE@HOST/neondb?sslmode=require`, separa:
   - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://HOST/neondb?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME` = `USUARIO`
   - `SPRING_DATASOURCE_PASSWORD` = `CLAVE`

Flyway crea las tablas en el primer arranque.

## 2. Render (backend)
*New → Web Service → repositorio `locomotora-back`*, con estos ajustes:
- **Language:** Docker
- **Branch:** `main`
- **Region:** Ohio (US East), la misma zona que Neon
- **Instance type:** Free
- **Health check path:** `/actuator/health`

Variables de entorno:

| Variable | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | las de Neon |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | `5` |
| `SERVER_TOMCAT_THREADS_MAX` | `20` |
| `LOCOMOTORA_JWT_SECRET` | aleatorio y largo (`openssl rand -base64 48`) |
| `GROQ_API_KEY` | tu clave de GroqCloud |
| `YOUTUBE_API_KEY` | tu clave de YouTube Data API |
| `LOCOMOTORA_GOOGLE_CLIENT_IDS` | el Web client ID de Google Cloud (login con Google; sin el, `POST /auth/google` responde 503) |

No definas `SERVER_PORT`. Render pasa el puerto en `PORT`, y `application.properties` lo usa.

El `Dockerfile` limita la JVM con `JAVA_TOOL_OPTIONS`: heap al 60 %, GC serial y solo C1. Se puede sobrescribir con una variable del mismo nombre.

## 3. APK
Compila la app con `EXPO_PUBLIC_API_URL=https://<servicio>.onrender.com`. Mientras el servidor despierta, `src/api/client.ts` (`wakeServer`) espera a `/actuator/health` hasta 5 minutos antes de mandar la primera petición.

## Limitaciones
- **Primera petición lenta:** tras 15 min sin uso, la primera petición espera a que Spring Boot arranque. Con 0,1 CPU y 512 MB (prueba local con `docker run --cpus=0.1 --memory=512m`) tardó unos 4 minutos y usó ~240 MB.
- **Tareas programadas:** el job que llena los videos (`CatalogVideoJob`) solo corre mientras el servicio está despierto.
- **Disco efímero:** Render borra los archivos locales en cada reinicio. Todo el estado vive en Postgres.

## Login con Google
1. En Google Cloud Console, en el mismo proyecto:
   - **Pantalla de consentimiento OAuth** (Google Auth Platform → Branding/Audience): tipo *External*, nombre de la app, email de soporte, permisos `openid`, `email` y `profile`. Pásala a **In production**; en *Testing* solo entran los usuarios de prueba (máximo 100). Con solo esos permisos no hace falta la verificación de Google; si subes un logo, Google pide verificar la marca (página de inicio y política de privacidad).
   - **Cliente OAuth tipo "Web application"**: su client ID es el *Web client ID*.
   - **Cliente OAuth tipo "Android"**: package `com.locomotora.entrenamiento` y el SHA-1 del keystore con el que EAS firma el APK (`npx eas-cli credentials` → Android → preview). Si después se publica en Play Store, otro cliente Android con el SHA-1 de *App signing* de Play Console. Si el SHA-1 no coincide, Google responde `DEVELOPER_ERROR`.
2. Backend (Render): `LOCOMOTORA_GOOGLE_CLIENT_IDS` = el Web client ID.
3. App: el Web client ID en `app.json` → `expo.extra.googleWebClientId` (no es secreto). Vacío = el botón no aparece.
4. Hay que **compilar un APK nuevo** (`npm run build:apk`): el login con Google usa un módulo nativo. La versión de la app pasó a 1.1.0, así que las actualizaciones OTA de ahora en adelante solo llegan a ese APK nuevo; el APK 1.0.0 deja de recibirlas.

El botón no aparece en web ni en Expo Go (no tienen el módulo nativo). Para probarlo en desarrollo hace falta un development build (`expo-dev-client`) o el APK.
