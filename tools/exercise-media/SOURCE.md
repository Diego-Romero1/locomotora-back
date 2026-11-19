# Fuente de las ilustraciones (C3)

- **De dónde:** API de imágenes de [wger](https://wger.de) (`/api/v2/exerciseimage/`), consultada el 2026-11-18.
  `source/wger-images.json` guarda la copia usada (solo los ejercicios mapeados, con sus campos de licencia).
- **Qué se usa:** solo ilustraciones de línea (estilo 1) del set **Everkinetic** (y el press de hombros en máquina
  de wger.de, del mismo estilo), con al menos dos cuadros por ejercicio (inicio y final). Otros estilos de wger
  (fotos de usuarios, 3D) no se mezclan.
- **Licencia:** CC BY-SA 3.0. Hay que mostrar autor y licencia junto a la imagen (la app lo hace en el detalle y en
  la hoja de técnica) e indicar los cambios. Las versiones derivadas que genera `build_media.py` se distribuyen con
  la misma licencia. Detalle por ejercicio en `ATTRIBUTION.md`.
- **Excluidos a propósito:** ver `_comment` en `wger_map.json`.

## Actualizar

1. Ajustar `wger_map.json` (y, si hace falta, refrescar `source/wger-images.json` desde la API).
2. `python3 build_media.py --check-db filas.txt` (Pillow con WebP). Regenera los cuadros en
   `src/main/resources/static/exercise-media/`, `ATTRIBUTION.md` y la migración.
3. Si V17 ya se aplicó en alguna base, no se edita: renombrar la salida a una migración nueva.
