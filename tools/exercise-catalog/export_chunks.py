"""Exporta los ejercicios a traducir en lotes de texto compacto (para la traduccion manual asistida)."""
import json, os, sys
out = sys.argv[1]
size = int(sys.argv[2]) if len(sys.argv) > 2 else 80
data = json.load(open("source/free-exercise-db.json"))
mapped = {v["id"] for v in json.load(open("existing_map.json"))["map"].values() if v["id"]}
todo = [e for e in data if e["id"] not in mapped]
os.makedirs(out, exist_ok=True)
for n in range(0, len(todo), size):
    lines = []
    for e in todo[n:n + size]:
        steps = " ".join(e["instructions"])
        if len(steps) > 520:
            steps = steps[:520].rsplit(" ", 1)[0] + "…"
        lines.append(f'{e["id"]} | {e["name"]} | {e["category"]} | {e["equipment"]} | {",".join(e["primaryMuscles"])} | {steps}')
    open(f"{out}/chunk-{n // size:02d}.txt", "w").write("\n".join(lines) + "\n")
print(len(todo), "a traducir en", (len(todo) + size - 1) // size, "lotes;", len(mapped), "ya mapeados")
