"""Genera las ilustraciones animadas del catalogo (C3) a partir de wger (set Everkinetic, CC BY-SA 3.0).

Entradas (en esta carpeta):
  source/wger-images.json  copia de la API de imagenes de wger (ver SOURCE.md)
  wger_map.json            ejercicio de wger -> ejercicios de nuestro catalogo

Uso:  python3 build_media.py [--check-db filas.txt]
  Necesita Pillow con WebP. Descarga los originales a .cache/ (no se versiona).
  --check-db: lineas "external_id|nombre" de los ejercicios globales de una base real, para validar
              que todos los destinos existen.

Salidas:
  src/main/resources/static/exercise-media/wger-<id>-<n>.webp  cuadros (linea negra con transparencia)
  src/main/resources/db/migration/V17__exercise_media.sql      columnas y URLs en exercises
  ATTRIBUTION.md                                                autor, licencia, fuente y cambios

Cambios sobre los originales (la licencia pide indicarlos): fondo blanco quitado (la linea queda con
transparencia para teñirla segun el tema), recorte comun a todos los cuadros, lienzo cuadrado y
reduccion a 512 px. Las versiones derivadas se publican con la misma licencia (CC BY-SA 3.0).
"""
import json
import os
import re
import sys
import urllib.request

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "..")
OUT_DIR = os.path.join(ROOT, "src", "main", "resources", "static", "exercise-media")
SQL_OUT = os.path.join(ROOT, "src", "main", "resources", "db", "migration", "V17__exercise_media.sql")
CACHE = os.path.join(HERE, ".cache")
SIZE = 512
URL_PREFIX = "/exercise-media/"


def fail(message):
    sys.exit("ERROR: " + message)


def sql(value):
    return "NULL" if value is None else "'" + str(value).replace("'", "''") + "'"


def frame_order(image):
    """Bench-press-1.png -> 1; los nombres sin numero (uuid) van por id de imagen."""
    match = re.search(r"-(\d)(?:-\d+x\d+)?\.(?:png|jpe?g)$", image["image"])
    return (int(match.group(1)) if match else 9, image["id"])


def download(image):
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, f"{image['id']}_{os.path.basename(image['image'])}")
    if not os.path.exists(path):
        request = urllib.request.Request(image["image"], headers={"User-Agent": "exercise-media-builder"})
        with urllib.request.urlopen(request, timeout=60) as response, open(path, "wb") as handle:
            handle.write(response.read())
    return path


def to_line_alpha(path):
    """Dibujo de linea sobre blanco -> linea negra con transparencia (alpha = oscuridad del trazo)."""
    source = Image.open(path).convert("RGBA")
    white = Image.new("RGBA", source.size, (255, 255, 255, 255))
    gray = Image.alpha_composite(white, source).convert("L")
    # Trazo mas firme y sin el gris claro de la compresion.
    alpha = gray.point(lambda value: 0 if value > 238 else min(255, int((255 - value) * 1.25)))
    line = Image.new("RGBA", source.size, (0, 0, 0, 0))
    line.putalpha(alpha)
    return line


def square_box(box, width, height):
    left, top, right, bottom = box
    pad = int(max(right - left, bottom - top) * 0.05)
    left, top, right, bottom = left - pad, top - pad, right + pad, bottom + pad
    side = max(right - left, bottom - top)
    cx, cy = (left + right) / 2, (top + bottom) / 2
    return (int(cx - side / 2), int(cy - side / 2), int(cx - side / 2) + side, int(cy - side / 2) + side)


def main():
    args = sys.argv[1:]
    known = None
    if args[:1] == ["--check-db"]:
        known = set()
        for line in open(args[1], encoding="utf-8"):
            if "|" in line:
                external_id, name = line.rstrip("\n").split("|", 1)
                if external_id:
                    known.add("ext:" + external_id)
                known.add("name:" + name)

    snapshot = json.load(open(os.path.join(HERE, "source", "wger-images.json"), encoding="utf-8"))
    mapping = json.load(open(os.path.join(HERE, "wger_map.json"), encoding="utf-8"))["map"]
    licenses = snapshot["licenses"]
    by_exercise = {}
    for image in snapshot["images"]:
        by_exercise.setdefault(str(image["exercise"]), []).append(image)

    os.makedirs(OUT_DIR, exist_ok=True)
    seen_targets = {}
    rows = []
    credits = []
    for wger_id, targets in sorted(mapping.items(), key=lambda item: int(item[0])):
        images = sorted(by_exercise.get(wger_id, []), key=frame_order)
        if len(images) < 2:
            fail(f"wger {wger_id}: necesita al menos 2 ilustraciones de linea")
        license_ids = {str(image["license"]) for image in images}
        if len(license_ids) != 1 or next(iter(license_ids)) not in licenses:
            fail(f"wger {wger_id}: licencia desconocida o mezclada {license_ids}")
        license_info = licenses[next(iter(license_ids))]
        authors = sorted({image["license_author"].strip() for image in images if image["license_author"].strip()})
        author = ", ".join(authors) if authors else "colaboradores de wger.de"

        lines = [to_line_alpha(download(image)) for image in images]
        width, height = lines[0].size
        if any(line.size != (width, height) for line in lines):
            # Algun par viene con tamaños distintos: se lleva todo al del primero.
            lines = [line if line.size == (width, height) else line.resize((width, height), Image.LANCZOS) for line in lines]
        boxes = [line.getchannel("A").point(lambda value: 255 if value > 40 else 0).getbbox() for line in lines]
        if any(box is None for box in boxes):
            fail(f"wger {wger_id}: imagen vacia")
        union = (min(b[0] for b in boxes), min(b[1] for b in boxes), max(b[2] for b in boxes), max(b[3] for b in boxes))
        crop = square_box(union, width, height)

        frames = []
        for index, line in enumerate(lines, start=1):
            canvas = Image.new("RGBA", (crop[2] - crop[0], crop[3] - crop[1]), (0, 0, 0, 0))
            canvas.paste(line, (-crop[0], -crop[1]))
            canvas = canvas.resize((SIZE, SIZE), Image.LANCZOS)
            filename = f"wger-{wger_id}-{index}.webp"
            canvas.save(os.path.join(OUT_DIR, filename), "WEBP", quality=88, method=6)
            frames.append(URL_PREFIX + filename)

        source_url = f"https://wger.de/en/exercise/{wger_id}/view-base"
        for target in targets:
            if target in seen_targets:
                fail(f"{target} aparece en wger {seen_targets[target]} y {wger_id}")
            if known is not None and target not in known:
                fail(f"{target} no existe en la base")
            seen_targets[target] = wger_id
            kind, value = target.split(":", 1)
            condition = (f"external_id = {sql(value)}" if kind == "ext"
                         else f"external_id IS NULL AND name = {sql(value)}")
            rows.append((condition, frames, author, license_info["name"], source_url))
        credits.append((wger_id, snapshot["exercise_names_en"].get(wger_id, ""), frames, author, license_info, source_url, targets))

    lines = [
        "-- Ilustraciones animadas del catalogo (C3). GENERADO por tools/exercise-media/build_media.py: no editar.",
        "-- Fuente: wger.de, set Everkinetic, CC BY-SA 3.0 (ver tools/exercise-media/ATTRIBUTION.md).",
        "-- media_frames: cuadros en orden (inicio, final...) que la app alterna; las rutas son relativas",
        "-- al servidor de media (hoy el backend, /exercise-media/**).",
        "",
        "ALTER TABLE exercises",
        "    ADD COLUMN media_frames jsonb NOT NULL DEFAULT '[]'::jsonb,",
        "    ADD CONSTRAINT exercises_media_frames_check CHECK (jsonb_typeof(media_frames) = 'array');",
        "",
    ]
    for condition, frames, author, license_name, source_url in rows:
        lines.append(
            "UPDATE exercises SET media_frames = " + sql(json.dumps(frames)) + "::jsonb, "
            f"image_url = {sql(frames[0])}, thumb_url = {sql(frames[0])}, media_author = {sql(author)}, "
            f"media_license = {sql(license_name)}, media_source_url = {sql(source_url)}, updated_at = now()\n"
            f"WHERE created_by_user_id IS NULL AND {condition};"
        )
    lines.append("")
    with open(SQL_OUT, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines))

    md = [
        "# Atribución de las ilustraciones de ejercicios",
        "",
        "Las ilustraciones animadas del catálogo vienen de [wger](https://wger.de) (set Everkinetic) y se",
        "publican bajo [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/). Generado por `build_media.py`.",
        "",
        "**Cambios hechos sobre los originales:** se quitó el fondo blanco (la línea queda con transparencia para",
        "teñirla según el tema de la app), se recortaron todos los cuadros de un ejercicio con el mismo encuadre",
        "cuadrado y se redujeron a 512 px en WebP. Estas versiones derivadas se distribuyen con la misma licencia",
        "CC BY-SA 3.0.",
        "",
        "| wger | Ejercicio (wger) | Cuadros | Autor | Licencia | Usado en |",
        "| --- | --- | --- | --- | --- | --- |",
    ]
    for wger_id, name, frames, author, license_info, source_url, targets in credits:
        used = ", ".join(target.split(":", 1)[1] for target in targets)
        md.append(f"| [{wger_id}]({source_url}) | {name} | {len(frames)} | {author} | [{license_info['name']}]({license_info['url']}) | {used} |")
    md.append("")
    with open(os.path.join(HERE, "ATTRIBUTION.md"), "w", encoding="utf-8") as handle:
        handle.write("\n".join(md))

    print(f"{len(credits)} ejercicios de wger, {sum(len(c[2]) for c in credits)} cuadros, {len(rows)} ejercicios del catalogo")


if __name__ == "__main__":
    main()
