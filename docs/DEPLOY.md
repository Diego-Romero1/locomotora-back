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

No definas `SERVER_PORT`. Render pasa el puerto en `PORT`, y `application.properties` lo usa.

El `Dockerfile` limita la JVM con `JAVA_TOOL_OPTIONS`: heap al 60 %, GC serial y solo C1. Se puede sobrescribir con una variable del mismo nombre.

## 3. APK
Compila la app con `EXPO_PUBLIC_API_URL=https://<servicio>.onrender.com`. Mientras el servidor despierta, `src/api/client.ts` (`wakeServer`) espera a `/actuator/health` hasta 5 minutos antes de mandar la primera petición.

## Limitaciones
- **Primera petición lenta:** tras 15 min sin uso, la primera petición espera a que Spring Boot arranque. Con 0,1 CPU y 512 MB (prueba local con `docker run --cpus=0.1 --memory=512m`) tardó unos 4 minutos y usó ~240 MB.
- **Tareas programadas:** el job que llena los videos (`CatalogVideoJob`) solo corre mientras el servicio está despierto.
- **Disco efímero:** Render borra los archivos locales en cada reinicio. Todo el estado vive en Postgres.
