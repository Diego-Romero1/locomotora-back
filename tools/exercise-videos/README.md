# Videos del catálogo

El backend busca por adelantado un video corto de YouTube por ejercicio (proceso diario `CatalogVideoJob`):
unas 100 búsquedas por día (~75 ejercicios), primero los habituales, después los que están en rutinas y
después el resto. Cada video queda guardado al momento para todos los usuarios en `exercise_guides`.

Necesita `YOUTUBE_API_KEY` y `GROQ_API_KEY` en `locomotora-backend/.env`. Ajustes en `application.properties`
(`locomotora.youtube.*`).

## Ver el avance

```bash
docker exec locomotora-postgres psql -U postgres -d locomotora -c "SELECT e.is_core, g.video_status, count(*) FROM exercises e LEFT JOIN exercise_guides g ON g.exercise_id = e.id WHERE e.created_by_user_id IS NULL GROUP BY 1, 2 ORDER BY 1 DESC, 2"
```

## Exportar a una migración

Al terminar una tanda (habituales, después el resto), exportar lo encontrado para que otra base arranque con
los mismos videos:

```bash
docker exec locomotora-postgres psql -U postgres -d locomotora -c "\copy (SELECT e.external_id, e.name, e.is_core, g.video_status, g.video_youtube_id, g.video_alternatives::text FROM exercise_guides g JOIN exercises e ON e.id = g.exercise_id WHERE e.created_by_user_id IS NULL AND g.video_status IN ('FOUND', 'NOT_FOUND') ORDER BY e.name) TO STDOUT WITH CSV" | python3 tools/exercise-videos/export_videos.py --version 20 --label exercise_videos_core --core-only
```

La migración guarda solo los ids (las políticas de YouTube piden refrescar sus datos cada 30 días): el
refresco del backend trae título, canal y duración en la otra base.
