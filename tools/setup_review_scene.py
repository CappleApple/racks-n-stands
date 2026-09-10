"""Create RCON commands for a compact, repeatable visual QA lineup in the disposable test world."""
import json
from pathlib import Path

commands = ["fill 64 64 46 98 64 58 minecraft:smooth_stone"]

def stack(item, **components):
    result = {"id": "minecraft:" + item, "count": 1}
    if components:
        result["components"] = components
    return result

glint = {"minecraft:enchantments": {"levels": {"minecraft:unbreaking": 3}}}
trim = {"minecraft:trim": {"material": "minecraft:gold", "pattern": "minecraft:sentry"}}
dyed = {"minecraft:dyed_color": {"rgb": 10040012, "show_in_tooltip": True}}
fixtures = [
    ("helmet_stand", [stack("leather_helmet", **dyed, **glint)]),
    ("chestplate_stand", [stack("leather_chestplate", **dyed, **trim)]),
    ("leggings_stand", [stack("diamond_leggings", **trim)]),
    ("boots_stand", [stack("netherite_boots", **trim, **glint)]),
    ("sword_floor_stand", [stack("netherite_sword", **glint)]),
    ("tool_rack", [stack(x) for x in ["diamond_pickaxe", "iron_axe", "netherite_hoe", "diamond_shovel", "golden_pickaxe", "stone_axe"]]),
    ("polearm_rack", [stack("diamond_sword"), stack("bow", **glint), stack("netherite_sword")]),
    ("armor_mannequin", [stack(x, **trim, **glint) for x in ["diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots"]]),
    ("generic_pedestal", [stack("diamond_sword", **glint)]),
    ("generic_wall_display", [stack("shield")]),
    ("generic_tabletop_display", [stack("apple")]),
    ("curio_cabinet", [stack("feather"), stack("string"), stack("diamond_helmet"), stack("apple"), stack("diamond"), stack("paper")]),
    ("polearm_rack", [stack("trident", **glint), stack("trident"), stack("trident")]),
    ("sword_floor_stand", [stack("blaze_rod")]),
]
for index, (fixture, items) in enumerate(fixtures):
    x = 66 + index * 2
    nbt = {"Items": [{"Slot": n, "Stack": item} for n, item in enumerate(items) if item]}
    commands.append(f"setblock {x} 65 54 racksnstands:{fixture}[facing=north,half=lower]")
    commands.append(f"data merge block {x} 65 54 " + json.dumps(nbt, separators=(",", ":")))
    if fixture == "armor_mannequin":
        commands.append(f"setblock {x} 66 54 racksnstands:{fixture}[facing=north,half=upper]")
commands += ["kill @e[type=minecraft:item]", "save-all flush"]
out = Path("build/review-scene-commands.json")
out.parent.mkdir(exist_ok=True)
out.write_text(json.dumps(commands, indent=2))
print(out)
