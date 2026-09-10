"""Install an isolated Curios integration-test datapack into a development world."""
import argparse
import json
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("world", nargs="?", default="run-gametest/world")
args = parser.parse_args()
pack = Path(args.world) / "datapacks/racksnstands_curios_tests"

def write(path, value):
    target = pack / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")

write("pack.mcmeta", {"pack": {"pack_format": 48, "description": "Racks N' Stands isolated Curios test fixtures"}})
for slot, items in {"ring": ["minecraft:feather", "minecraft:iron_sword"], "necklace": ["minecraft:string"], "sigil": ["minecraft:paper"]}.items():
    write(f"data/racksnstands_test/curios/slots/{slot}.json", {"size": 1, "validators": ["curios:tag"]})
    write(f"data/curios/tags/item/{slot}.json", {"replace": False, "values": items})
write("data/racksnstands_test/curios/entities/player.json", {"entities": ["minecraft:player"], "slots": ["ring", "necklace", "sigil"]})
print(f"Prepared {pack.resolve()}")
