"""Ayuda para el mapeo manual: busca ejercicios de la fuente por palabras (todas deben aparecer)."""
import json, sys
data = json.load(open("source/free-exercise-db.json"))
for query in sys.argv[1:]:
    words = query.lower().split("+")
    hits = [e for e in data if all(w in (e["name"] + " " + e["id"]).lower() for w in words)]
    print(f"## {query}: " + " | ".join(f'{e["id"]} [{e["equipment"]},{e["primaryMuscles"][0]}]' for e in hits[:8]))
