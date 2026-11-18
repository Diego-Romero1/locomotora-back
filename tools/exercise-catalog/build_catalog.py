"""Genera la migracion V16 del catalogo de ejercicios a partir de free-exercise-db.

Entradas (en esta carpeta):
  source/free-exercise-db.json  fuente vendorizada (ver SOURCE.md)
  existing_map.json             ejercicios actuales -> id de la fuente (+ nombre nuevo opcional)
  translations/*.json           id de la fuente -> [nombre en espanol, paso 1, paso 2, ...]

Uso:  python3 -I build_catalog.py [--check-names archivo.txt]
  --check-names: lista de nombres globales de una base real (uno por linea) para validar choques.

Salida: src/main/resources/db/migration/V16__exercise_catalog.sql

Las validaciones cortan con error: traducciones completas, nombres unicos (contra el catalogo
actual y entre si), musculos y equipos mapeados, ids de is_core existentes.
"""
import glob
import json
import os
import re
import sys
import uuid

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "src", "main", "resources", "db", "migration", "V16__exercise_catalog.sql")
NAMESPACE = uuid.UUID("6f1c2a52-3c1e-4c55-9a51-6b1f0e3c7d10")

MUSCLES = {
    "chest": "Pecho", "lats": "Espalda", "middle back": "Espalda", "lower back": "Lumbares",
    "traps": "Trapecio", "shoulders": "Hombros", "neck": "Cuello", "biceps": "Bíceps",
    "triceps": "Tríceps", "forearms": "Antebrazos", "quadriceps": "Cuádriceps",
    "hamstrings": "Isquiotibiales", "glutes": "Glúteos", "calves": "Pantorrillas",
    "adductors": "Aductores", "abductors": "Abductores", "abdominals": "Abdomen",
}
EQUIPMENT = {
    "barbell": "BARBELL", "dumbbell": "DUMBBELL", "cable": "CABLE", "machine": "MACHINE",
    "body only": "BODYWEIGHT", None: "BODYWEIGHT", "kettlebells": "KETTLEBELL", "bands": "BAND",
    "e-z curl bar": "EZ_BAR", "medicine ball": "OTHER", "exercise ball": "OTHER",
    "foam roll": "OTHER", "other": "OTHER",
}
LEVELS = {"beginner": "BEGINNER", "intermediate": "INTERMEDIATE", "expert": "ADVANCED"}
TYPES = {
    "strength": "STRENGTH", "stretching": "STRETCHING", "cardio": "CARDIO", "plyometrics": "PLYOMETRICS",
    "powerlifting": "POWERLIFTING", "olympic weightlifting": "OLYMPIC", "strongman": "STRONGMAN",
}
# Etiqueta visible (columna category, la que ya muestra la app).
CATEGORY_LABEL = {
    "STRENGTH": "Hipertrofia", "POWERLIFTING": "Fuerza", "OLYMPIC": "Halterofilia", "STRONGMAN": "Strongman",
    "STRETCHING": "Movilidad", "PLYOMETRICS": "Pliometría", "CARDIO": "Cardio",
}

# Ejercicios nuevos que entran al catalogo "core" (el que ve la IA y prioriza la sugerencia),
# ademas de todos los ejercicios actuales. Cubren los grupos nuevos y el equipo nuevo.
CORE_NEW = """
Palms-Up_Barbell_Wrist_Curl_Over_A_Bench Palms-Down_Wrist_Curl_Over_A_Bench Farmers_Walk
Seated_Dumbbell_Palms-Up_Wrist_Curl Wrist_Roller Reverse_Barbell_Curl Zottman_Curl
Hyperextensions_Back_Extensions Superman Rack_Pulls Reverse_Hyperextension
Barbell_Shrug Smith_Machine_Upright_Row Upright_Barbell_Row
Thigh_Adductor Monster_Walk
Isometric_Neck_Exercise_-_Front_And_Back Lying_Face_Down_Plate_Neck_Resistance
One-Arm_Kettlebell_Swings Two-Arm_Kettlebell_Row Kettlebell_One-Legged_Deadlift Two-Arm_Kettlebell_Military_Press
Kettlebell_Thruster Front_Squats_With_Two_Kettlebells
Band_Pull_Apart Lateral_Raise_-_With_Bands Shoulder_Press_-_With_Bands Squats_-_With_Bands
Upright_Row_-_With_Bands Hip_Extension_with_Bands
Mountain_Climbers Russian_Twist Inverted_Row Glute_Kickback Side_Bridge Bodyweight_Squat
Step-up_with_Knee_Raise Natural_Glute_Ham_Raise Rope_Crunch Pallof_Press
Preacher_Curl Lying_Triceps_Press Trap_Bar_Deadlift Smith_Machine_Calf_Raise
""".split()

# Zonas de carga para los ejercicios nuevos (los actuales ya las tienen desde V13). Se evaluan
# sobre el nombre en ingles en minusculas; estiramientos y liberacion con rodillo no cargan zonas.
ZONE_RULES = {
    "KNEE": [r"squat", r"lunge", r"leg press", r"leg extension", r"step.?up", r"split", r"pistol", r"hack",
             r"box jump", r"bound", r"hop", r"jump", r"sled push", r"thruster", r"wall sit"],
    "LOWER_BACK": [r"deadlift", r"good morning", r"bent.?over.*row", r"barbell row", r"t-bar", r"pendlay", r"clean",
                   r"snatch", r"rack pull", r"stiff.?leg", r"romanian", r"hyperextension", r"keg", r"atlas",
                   r"log lift", r"tire flip", r"yoke", r"back squat", r"barbell squat", r"front squat", r"zercher",
                   r"sandbag", r"swing"],
    "SHOULDER": [r"overhead", r"military", r"shoulder press", r"push press", r"jerk", r"snatch", r"handstand",
                 r"\bdips?\b", r"bench press", r"pullover", r"upright", r"behind the neck", r"front raise",
                 r"arnold", r"muscle.?up", r"decline push", r"bradford"],
    "ELBOW": [r"skull", r"lying triceps", r"french", r"triceps extension", r"tricep extension", r"\bdips?\b",
              r"close.?grip bench", r"barbell curl", r"jm press", r"tate press"],
    "WRIST": [r"push.?up", r"front squat", r"clean", r"close.?grip bench", r"barbell curl", r"wrist curl",
              r"handstand", r"wrist roller"],
    "NECK": [r"shrug", r"neck"],
    "HIP": [r"sumo", r"adduct", r"abduct", r"split squat", r"lunge", r"leg raise", r"hip circle"],
    "ANKLE": [r"lunge", r"split squat", r"calf raise", r"jump", r"hop", r"bound", r"sprint", r"rope jumping",
              r"skipping"],
}
NO_ZONES_TYPES = {"STRETCHING"}


def fail(message):
    sys.exit("ERROR: " + message)


def sql(value):
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "true" if value else "false"
    return "'" + str(value).replace("'", "''") + "'"


def jsonb(values):
    return sql(json.dumps(values, ensure_ascii=False)) + "::jsonb"


def muscles(fedb_list, primary=None):
    out = []
    for raw in fedb_list:
        if raw not in MUSCLES:
            fail(f"musculo sin mapear: {raw}")
        name = MUSCLES[raw]
        if name != primary and name not in out:
            out.append(name)
    return out


def zones(item, kind):
    if kind in NO_ZONES_TYPES or "smr" in item["id"].lower():
        return []
    name = item["name"].lower()
    return [zone for zone, patterns in ZONE_RULES.items() if any(re.search(p, name) for p in patterns)]


def equipment(item):
    if item["equipment"] not in EQUIPMENT:
        fail(f"equipo sin mapear: {item['equipment']} ({item['id']})")
    code = EQUIPMENT[item["equipment"]]
    if code == "MACHINE" and "smith" in item["name"].lower():
        return "SMITH_MACHINE"
    return code


def upper(value):
    return value.upper() if value else None


def main():
    args = sys.argv[1:]
    extra_names = set()
    if args[:1] == ["--check-names"]:
        extra_names = {line.strip().lower() for line in open(args[1], encoding="utf-8") if line.strip()}

    source = {item["id"]: item for item in json.load(open(os.path.join(HERE, "source", "free-exercise-db.json")))}
    existing = json.load(open(os.path.join(HERE, "existing_map.json"), encoding="utf-8"))["map"]
    translations = {}
    for path in sorted(glob.glob(os.path.join(HERE, "translations", "*.json"))):
        for key, value in json.load(open(path, encoding="utf-8")).items():
            if key in translations:
                fail(f"traduccion duplicada: {key}")
            if not isinstance(value, list) or len(value) < 2 or not all(isinstance(v, str) and v.strip() for v in value):
                fail(f"traduccion invalida: {key}")
            translations[key] = value

    mapped = {}
    for name, entry in existing.items():
        if entry["id"]:
            if entry["id"] not in source:
                fail(f"id mapeado inexistente: {entry['id']}")
            if entry["id"] in mapped:
                fail(f"id mapeado dos veces: {entry['id']}")
            mapped[entry["id"]] = name

    new_ids = [key for key in source if key not in mapped]
    missing = [key for key in new_ids if key not in translations]
    extra = [key for key in translations if key not in source or key in mapped]
    if missing or extra:
        fail(f"traducciones faltantes {missing[:5]} / sobrantes {extra[:5]}")

    taken = {}
    for name, entry in existing.items():
        final = entry.get("rename") or name
        if final.lower() in taken:
            fail(f"nombre actual repetido: {final}")
        taken[final.lower()] = name
    for name in extra_names:
        taken.setdefault(name, "(base)")
    # Los nombres viejos de los renombrados quedan libres al aplicarse el UPDATE antes del INSERT.
    for key in new_ids:
        name = translations[key][0].strip()
        if len(name) > 180:
            fail(f"nombre largo: {name}")
        if name.lower() in taken:
            fail(f"nombre repetido: {name} ({key} vs {taken[name.lower()]})")
        taken[name.lower()] = key

    unknown_core = [key for key in CORE_NEW if key not in source]
    if unknown_core:
        fail(f"is_core con ids inexistentes: {unknown_core}")

    lines = [
        "-- Catalogo ampliado de ejercicios (C1). GENERADO por tools/exercise-catalog/build_catalog.py:",
        "-- no editar a mano. Fuente: free-exercise-db (Unlicense), nombres e instrucciones traducidos.",
        "-- Los ejercicios actuales conservan su id (rutinas e historial) y se enriquecen; los nuevos",
        "-- usan un uuid determinista por id de la fuente, asi la migracion es idempotente.",
        "",
        "ALTER TABLE exercises",
        "    ADD COLUMN source varchar(40) NOT NULL DEFAULT 'LOCOMOTORA',",
        "    ADD COLUMN external_id varchar(120),",
        "    ADD COLUMN name_en varchar(180),",
        "    ADD COLUMN exercise_type varchar(30) NOT NULL DEFAULT 'STRENGTH',",
        "    ADD COLUMN mechanic varchar(20),",
        "    ADD COLUMN force_type varchar(20),",
        "    ADD COLUMN is_core boolean NOT NULL DEFAULT false,",
        "    ADD COLUMN image_url text,",
        "    ADD COLUMN thumb_url text,",
        "    ADD COLUMN media_license varchar(80),",
        "    ADD COLUMN media_author varchar(180),",
        "    ADD COLUMN media_source_url text,",
        "    ADD CONSTRAINT exercises_exercise_type_check CHECK (exercise_type IN",
        "        ('STRENGTH', 'STRETCHING', 'CARDIO', 'PLYOMETRICS', 'POWERLIFTING', 'OLYMPIC', 'STRONGMAN')),",
        "    ADD CONSTRAINT exercises_mechanic_check CHECK (mechanic IS NULL OR mechanic IN ('COMPOUND', 'ISOLATION')),",
        "    ADD CONSTRAINT exercises_force_type_check CHECK (force_type IS NULL OR force_type IN ('PUSH', 'PULL', 'STATIC'));",
        "",
        "CREATE UNIQUE INDEX idx_exercises_unique_external_id ON exercises (external_id)",
        "    WHERE external_id IS NOT NULL AND created_by_user_id IS NULL;",
        "CREATE INDEX idx_exercises_core ON exercises (is_core) WHERE is_core;",
        "CREATE INDEX idx_exercises_type ON exercises (exercise_type);",
        "",
        "-- 1) Ejercicios actuales: referencia a la fuente, datos que faltaban y nombres en espanol.",
        "CREATE TEMP TABLE catalog_existing (",
        "    name text PRIMARY KEY, new_name text, external_id text, name_en text, exercise_type text,",
        "    mechanic text, force_type text, difficulty text, secondary jsonb",
        ") ON COMMIT DROP;",
        "INSERT INTO catalog_existing VALUES",
    ]
    rows = []
    for name, entry in sorted(existing.items()):
        item = source.get(entry["id"]) if entry["id"] else None
        if item:
            primary = MUSCLES[item["primaryMuscles"][0]]
            rows.append("    (" + ", ".join([
                sql(name), sql(entry.get("rename")), sql(item["id"]), sql(item["name"]),
                sql(TYPES[item["category"]]), sql(upper(item.get("mechanic"))), sql(upper(item.get("force"))),
                sql(LEVELS.get(item.get("level"))), jsonb(muscles(item["primaryMuscles"][1:] + item["secondaryMuscles"], primary)),
            ]) + ")")
        else:
            rows.append("    (" + ", ".join([sql(name), sql(entry.get("rename"))] + ["NULL"] * 6 + ["'[]'::jsonb"]) + ")")
    lines.append(",\n".join(rows) + ";")
    lines += [
        "",
        "UPDATE exercises e",
        "SET name = COALESCE(c.new_name, e.name),",
        "    external_id = c.external_id,",
        "    name_en = c.name_en,",
        "    exercise_type = COALESCE(c.exercise_type, e.exercise_type),",
        "    mechanic = c.mechanic,",
        "    force_type = c.force_type,",
        "    difficulty = COALESCE(e.difficulty, c.difficulty),",
        "    secondary_muscle_groups = CASE WHEN e.secondary_muscle_groups = '[]'::jsonb",
        "        THEN c.secondary - COALESCE(e.primary_muscle_group, '') ELSE e.secondary_muscle_groups END,",
        "    is_core = true,",
        "    updated_at = now()",
        "FROM catalog_existing c",
        "WHERE e.created_by_user_id IS NULL AND e.name = c.name;",
        "",
        "-- 2) Ejercicios nuevos.",
        "INSERT INTO exercises (id, name, name_en, source, external_id, primary_muscle_group, secondary_muscle_groups,",
        "    equipment, difficulty, exercise_type, category, mechanic, force_type, instructions, stress_zones, is_core)",
        "VALUES",
    ]
    rows = []
    core = set(CORE_NEW)
    for key in sorted(new_ids):
        item = source[key]
        values = translations[key]
        kind = TYPES[item["category"]]
        primary = MUSCLES[item["primaryMuscles"][0]]
        steps = "\n".join(f"{index}. {step.strip()}" for index, step in enumerate(values[1:], start=1))
        rows.append("    (" + ", ".join([
            sql(str(uuid.uuid5(NAMESPACE, "free-exercise-db:" + key))), sql(values[0].strip()), sql(item["name"]),
            "'FREE_EXERCISE_DB'", sql(key), sql(primary),
            jsonb(muscles(item["primaryMuscles"][1:] + item["secondaryMuscles"], primary)),
            sql(equipment(item)), sql(LEVELS.get(item.get("level"))), sql(kind), sql(CATEGORY_LABEL[kind]),
            sql(upper(item.get("mechanic"))), sql(upper(item.get("force"))), sql(steps),
            jsonb(zones(item, kind)), sql(key in core),
        ]) + ")")
    lines.append(",\n".join(rows))
    lines.append("ON CONFLICT DO NOTHING;")
    lines.append("")

    with open(OUT, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines))
    print(f"{OUT}: {len(existing)} actuales, {len(new_ids)} nuevos, {len(core)} core nuevos")


if __name__ == "__main__":
    main()
