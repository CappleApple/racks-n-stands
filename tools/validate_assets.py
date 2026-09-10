"""Validate generated furniture faces and distributable mod resources. No dependencies."""
import json
from pathlib import Path
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
models = root / "src/generated/resources/assets/racksnstands/models/block"
axes = {"west": (0, False), "east": (0, True), "down": (1, False),
        "up": (1, True), "north": (2, False), "south": (2, True)}
face_count = 0
for path in sorted(models.glob("*.json")):
    model = json.loads(path.read_text())
    faces = []
    for element in model["elements"]:
        low, high = element["from"], element["to"]
        assert all(a <= b for a, b in zip(low, high)), path
        assert all(-16 <= value <= 32 for value in low+high), (path, "Minecraft block model coordinate bounds")
        for name, definition in element["faces"].items():
            axis, positive = axes[name]
            plane = high[axis] if positive else low[axis]
            uv = definition["uv"]
            group = ["wood", "dark", "metal", "cloth", "stone", "trim", "base"].index(definition["texture"][1:])
            assert definition["tintindex"] == group, (path, "missing material metadata")
            assert all(0 <= coordinate <= 16 for coordinate in uv), (path, uv)
            projected = [i for i in range(3) if i != axis]
            for old_axis, old_plane, old_low, old_high in faces:
                if axis == old_axis and abs(plane - old_plane) < 1e-8:
                    overlap = [min(high[i], old_high[i]) - max(low[i], old_low[i]) for i in projected]
                    assert not all(size > 1e-8 for size in overlap), (path, name, "coplanar overlap")
            faces.append((axis, plane, low, high))
    face_count += len(faces)

# Item forms inherit Minecraft's normal block transforms and fit inside one cube.
for path in (models.parent / "item").glob("*.json"):
    model = json.loads(path.read_text())
    assert model["parent"] == "minecraft:block/block" and "display" not in model, path
    for element in model["elements"]:
        assert all(-1e-8 <= coordinate <= 16+1e-8 for key in ["from", "to"] for coordinate in element[key]), path
pedestal = json.loads((models / "generic_pedestal.json").read_text())
top_area = sum((e["to"][0]-e["from"][0])*(e["to"][2]-e["from"][2]) for e in pedestal["elements"] if "up" in e["faces"] and e["to"][1] == 16)
assert abs(top_area - 244) < 1e-8, "Full-block pedestal must retain its open recessed slot"

print(f"Validated {len(list(models.glob('*.json')))} block models, {face_count} faces; no overlapping coplanar faces or out-of-bounds UVs.")

version = next(line.split("=", 1)[1].strip() for line in (root / "gradle.properties").read_text().splitlines() if line.startswith("mod_version="))
jar = root / f"build/libs/racksnstands-{version}.jar"
if jar.exists():
    with ZipFile(jar) as archive:
        names = archive.namelist()
        assert not any("/gametest/" in n or n.startswith(".cache/") for n in names)
        assert all(n.startswith("com/cappleapple/racksnstands/") for n in names if n.endswith(".class")), "Third-party classes must not be bundled"
        assert not any(n.startswith(prefix) for n in names for prefix in ["top/theillusivec4/", "net/sweenus/", "net/rosemarythyme/", "snownee/"])
        assert len([n for n in names if n.startswith("data/racksnstands/recipe/") and n.endswith(".json")]) == 15
        assert len([n for n in names if n.startswith("data/racksnstands/racksnstands/item_transforms/") and n.endswith(".json")]) == len(list((root / "src/generated/resources/data/racksnstands/racksnstands/item_transforms").glob("*.json")))
        for suffix in ["json", "vsh", "fsh"]:
            assert f"assets/racksnstands/shaders/core/masked_glint.{suffix}" in names
        for path in (root / "src/generated/resources").rglob("*.json"):
            name = path.relative_to(root / "src/generated/resources").as_posix()
            assert json.loads(archive.read(name)) == json.loads(path.read_text()), (name, "stale JAR resource")
        enchantment = json.loads(archive.read("data/racksnstands/enchantment/repairing.json"))
        assert enchantment["max_level"] == 10
        assert len([n for n in names if n.startswith("data/racksnstands/racksnstands/displays/") and n.endswith(".json")]) == 23
        for path in (root / "src/generated/resources/data/racksnstands/loot_table/blocks").glob("*.json"):
            loot = json.loads(path.read_text())
            assert "racksnstands:materials" in loot["pools"][0]["entries"][0]["functions"][0]["include"], path
        assert json.loads(archive.read("assets/racksnstands/lang/en_us.json"))["block.racksnstands.armor_mannequin"] == "Full Armor Stand"
        print(f"Validated {jar.name}: current generated resources, 23 profiles, Repairing I-X, no bundled companion or QA classes.")
