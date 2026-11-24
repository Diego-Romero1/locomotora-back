"""Exporta los videos del catalogo ya elegidos a una migracion, para que otra base (produccion) arranque con
los mismos videos sin gastar su cuota de YouTube ni esperar el llenado diario.

Uso (lee el CSV de la consulta de README.md por la entrada estandar):
  docker exec locomotora-postgres psql -U postgres -d locomotora -c "\\copy (...) TO STDOUT WITH CSV" \\
    | python3 export_videos.py --version 21 --label exercise_videos_core [--core-only]

Que lleva la migracion (politica de YouTube: sus datos se refrescan cada 30 dias):
  - solo los ids de video (principal y alternativas); titulo, canal y duracion los trae el refresco del backend;
  - los ejercicios sin video (NOT_FOUND), para que la otra base no los busque de nuevo hasta dentro de 30 dias.
Los ejercicios se buscan por external_id o, si no tienen, por nombre (como V16 y V17). Nunca pisa un video
que la otra base ya tenga.
"""
import argparse
import csv
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
MIGRATIONS = os.path.join(HERE, "..", "..", "src", "main", "resources", "db", "migration")


def sql(value):
    return "NULL" if value is None else "'" + str(value).replace("'", "''") + "'"


def target(external_id, name):
    if external_id:
        return f"e.external_id = {sql(external_id)}"
    return f"e.external_id IS NULL AND e.name = {sql(name)}"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", type=int, required=True)
    parser.add_argument("--label", required=True)
    parser.add_argument("--core-only", action="store_true")
    args = parser.parse_args()

    found, not_found = [], []
    for row in csv.reader(sys.stdin):
        if not row:
            continue
        external_id, name, is_core, status, video_id, alternatives = row[:6]
        if args.core_only and is_core not in ("t", "true"):
            continue
        if status == "FOUND" and video_id:
            alts = [item["id"] for item in json.loads(alternatives or "[]") if item.get("id")]
            found.append((external_id, name, video_id, alts))
        elif status == "NOT_FOUND":
            not_found.append((external_id, name))
    if not found and not not_found:
        sys.exit("ERROR: no hay filas para exportar (revisa la consulta)")

    lines = [
        f"-- Videos del catalogo exportados por tools/exercise-videos/export_videos.py: no editar a mano.",
        f"-- {len(found)} ejercicios con video y {len(not_found)} sin video. Solo ids: el refresco del backend trae",
        "-- titulo, canal y duracion. No pisa videos que esta base ya tenga.",
        "",
    ]
    for external_id, name, video_id, alts in found:
        alternatives = json.dumps([{"id": alt} for alt in alts])
        lines.append(
            "INSERT INTO exercise_guides (exercise_id, video_status, video_youtube_id, video_alternatives, video_source, video_checked_at)\n"
            f"SELECT e.id, 'FOUND', {sql(video_id)}, {sql(alternatives)}::jsonb, 'MIGRATION', now() FROM exercises e\n"
            f"WHERE e.created_by_user_id IS NULL AND {target(external_id or None, name)}\n"
            "ON CONFLICT (exercise_id) DO UPDATE SET video_status = 'FOUND', video_youtube_id = EXCLUDED.video_youtube_id,\n"
            "    video_title = NULL, video_channel = NULL, video_duration_seconds = NULL,\n"
            "    video_alternatives = EXCLUDED.video_alternatives, video_source = 'MIGRATION', video_checked_at = now(),\n"
            "    video_refreshed_at = NULL, updated_at = now()\n"
            "WHERE exercise_guides.video_status <> 'FOUND';"
        )
    for external_id, name in not_found:
        lines.append(
            "INSERT INTO exercise_guides (exercise_id, video_status, video_source, video_checked_at)\n"
            f"SELECT e.id, 'NOT_FOUND', 'MIGRATION', now() FROM exercises e\n"
            f"WHERE e.created_by_user_id IS NULL AND {target(external_id or None, name)}\n"
            "ON CONFLICT (exercise_id) DO NOTHING;"
        )
    lines.append("")
    path = os.path.join(MIGRATIONS, f"V{args.version}__{args.label}.sql")
    if os.path.exists(path):
        sys.exit(f"ERROR: {path} ya existe")
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines))
    print(f"{path}: {len(found)} con video, {len(not_found)} sin video")


if __name__ == "__main__":
    main()
