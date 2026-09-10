# Racks N' Stands

Minecraft **1.21.1**, NeoForge **21.1.248+**, Java **21**. Mod ID: `racksnstands`. Java namespace: `com.cappleapple.racksnstands`.

Equipment furniture with one shared storage, filtering, interaction, and rendering framework. Furniture chooses the presentation. The real **Repairing I–X** treasure enchantment supplies maintenance power. Crafting materials never change repair strength.

## Install

Download the latest JAR from [GitHub Releases](https://github.com/CappleApple/racks-n-stands/releases). Report bugs on the [issue tracker](https://github.com/CappleApple/racks-n-stands/issues).

Put `racksnstands-1.3.2.jar` in the client and server `mods` directories. Curios compatibility targets the 1.21.1 **9.5.1** API and is optional. A Repairing I–X datapack definition for Rituals Not Rolls is included in the JAR and is available to its normal data loader.

The first release uses oak furniture, dark oak accents, metal supports, and fabric display forms. These are original baked cuboid models referring to Minecraft textures. Resource packs can replace the models and texture references. No assets or implementation were taken from another equipment-display mod.

## Use

Furniture items stack to 64. Only matching enchantments and sampled materials combine in a stack.

- Click anywhere on the Full Armor Stand with armor to insert or replace that armor's matching slot. Empty-hand removal targets the clicked part.
- Right-click a slot to insert, take, or exchange the held item. Occupied slots swap silently; invalid armor is ignored.
- Sneak + right-click a furniture part with any block item to copy its texture to all matching material parts on that fixture. The sample is not consumed. The pedestal base, body, and cap are separate groups.
- Sneak + right-click without a block item to exchange equipped items. Armor stands swap their bound equipment slots; the Full Armor Stand swaps all four atomically. General displays use the displayed item's equipment slot, falling back to the main hand.
- Empty-hand sneak + right-click a displayed Curio to equip it into a compatible functional slot, preferring empty slots. Occupied slots swap their previous Curio onto the display.
- General furniture accepts any item. Only armor stands enforce an equipment slot; datapacks can add explicit filters.
- Displays hold one item by default. Set `interaction.allow_full_stacks = true` to allow each general slot to hold a full stack. Natural item limits still apply; armor slots remain single-piece. Turning the option off preserves existing stored stacks.
- Click the actual slot position on racks and shelves.
- Apply a Repairing book to a fixture item in an anvil, or use Rituals Not Rolls when installed. Place the enchanted fixture to activate maintenance.
- Hoppers and other standard item handlers obey the same filters. Comparators report occupancy from 0 to 15.

Fixture items and displayed gear are separate drops. Enchantments and customized materials survive survival fixture drops and re-placement. Customized materials also appear on inventory, held, and dropped fixture models. Creative insertion transfers the held item; creative breaking drops stored gear once and does not duplicate the fixture.

## Fixtures

| Fixture ID | Slots | Purpose |
|---|---:|---|
| `armor_mannequin` | 4 | Full Armor Stand; helmet, chest, legs, boots and atomic loadout exchange |
| `helmet_stand` | 1 | Worn helmet |
| `chestplate_stand` | 1 | Torso / bust |
| `leggings_stand` | 1 | Worn leggings |
| `boots_stand` | 1 | Worn boots |
| `tool_rack` | 6 | Large Item Rack; any items |
| `weapon_rack` | 4 | Item Rack; any items |
| `polearm_rack` | 3 | Small Item Rack; any items |
| `sword_floor_stand` | 1 | Display Stand; horizontal weapon/tool presentation across centered supports |
| `generic_pedestal` | 1 | Full-height Recessed Pedestal; embedded blade with its hilt above the block |
| `generic_wall_display` | 1 | Wall presentation; any item |
| `generic_tabletop_display` | 1 | Display; mounts on floors, walls or ceilings; any item |
| `curio_cabinet` | 6 | Wall-mounted Display Shelf; any items |

All IDs use `racksnstands:`. These 13 designs are craftable and available in the creative tab. Ten retired specialty IDs remain registered to preserve placed blocks and their stored gear, but have no recipes or creative entries. There is no separate staff stand or necklace bust in the active catalog.

Weapons and tools select their in-hand models on stands, pedestals and wall displays. The Large Item Rack and Display Shelf also use their 3D models, uniformly scaled to fit the width, height and depth of each compartment; shelf items rest on the shelf surfaces. Baked geometry retains the hand model's scale and is centered for furniture placement; native custom renderers retain their hand rendering path. Swords and other weapons point down; axes, hammers and tridents point up. The freestanding Display Stand holds weapons/tools sideways across its centered supports. Bow orientation is preserved. Bundled optional rules cover Simply Swords, Simply More, Cataclysm, Too Many Bows and Iron’s Spellbooks; packs can override item/tag rules. The pedestal extends a displayed sword into the air above its top. Fixture inventory, held and dropped models use normal Minecraft block transforms. Furniture hitboxes leave gaps between supports and keep shallow shelves against their wall. The Full Armor Stand has a wider torso hitbox and chest target. All armor stands face the placing player. Native armor-layer hooks support custom armor such as Immersive Armors and Iron’s Spellbooks; elytras use resting wings.

## Repairing

Repairs accumulate continuously and apply every **20 game ticks** by default. The rate is **20% of maximum durability per enchantment level over 3,600 seconds** of world game time. These are separate settings: `interval_ticks` controls update frequency, `calculation_period_seconds` controls the rate's time basis, and `percent_per_level` defaults to `0.2`.

Each update earns `maxDurability * percent_per_level * level * elapsedTicks / (calculation_period_seconds * 20)` durability. Fractional points carry into later updates, so changing update frequency does not change repair speed. Repairing I restores 20% per hour; Repairing V restores 100% per hour. Missing durability determines time to completion, and completed items never bank surplus repair.

Elapsed game time and fractional progress persist across chunk unloads and world saves. On chunk reload, maintenance applies the accumulated repair once. No chunks are force-loaded, and time while the server is stopped does not count. If unloaded catch-up is disabled, earned loaded time before the save still survives. Slot exchanges settle earned repairs before moving gear, then reset the incoming item's fractional credit.

Look at a stored armor piece or durable item to see its current/max durability and time until fully repaired. A translucent light-blue bar follows the selected item, including the upper half of the Full Armor Stand. It shows no slot number or armor-slot name, and its durability label has no shadow. With Jade installed, that information appears in Jade; otherwise a compact native HUD appears at the top of the screen. The client option `rendering.show_repair_tooltip` controls it. Progress uses server-synchronized rates and timestamps, including after live config changes.

Repair particles and sounds have independent enable switches. Sounds play after each stored item restores `repair_sound_interval_percent` percent of its maximum durability (default `1.0`, meaning 1%). Sound progress survives saves and chunk unloads, while large catch-up batches produce at most one sound per fixture update. `repair_sound_volume` defaults to `0.15` and `repair_sound_pitch` to `1.7`; all these settings update live.

Repair affects Minecraft durability only. Idle fixtures have no block-entity ticker; eligible fixtures use configurable scheduled block ticks. Fraction-only bookkeeping does not send an inventory packet every update. Actual durability changes synchronize the item and progress snapshot.

Configuration is in `config/racksnstands-common.toml` and `config/racksnstands-client.toml`. Saved TOML edits apply automatically while the game is running; no `/reload` or restart is needed. Common settings apply on the server, and rendering settings apply on each client. The client render-distance default is 64 blocks. See [packmaker documentation](docs/PACKMAKERS.md) for all settings and acquisition overrides.

## Build and test

```powershell
.\gradlew.bat runData
.\gradlew.bat test build
.\gradlew.bat runGameTestServer
.\gradlew.bat runGameTestServer -PwithJade
python tools/validate_assets.py
python tools/prepare_curios_tests.py
.\gradlew.bat runGameTestServer -PwithCurios
.\gradlew.bat runClient
```

Generated resources are checked in, so normal builds do not require datagen first. GameTest and visual-QA classes are excluded from the distributable JAR. Do not put the test datapack in a production world; it gives vanilla items test Curios metadata.

The test server uses vanilla empty templates, so `minecraft` and `racksnstands` must both be enabled in its GameTest namespace setting. Check for **All required tests passed** in the log: a launcher exit code alone does not prove tests ran.

## Documentation

- [Block texture customization](docs/MATERIALS.md)
- [Per-item and item-tag orientation, scale and offset](docs/ITEM_TRANSFORMS.md)
- [Packmaker profiles, tags, predicates, configuration, and recipes](docs/PACKMAKERS.md)
- [Curios and Rituals Not Rolls integration](docs/COMPATIBILITY.md)
- [Public API and lifecycle](docs/API.md)
- [Performance scenes and validation evidence](docs/VALIDATION.md)

Development commands require permission level 2:

```text
/racksnstands debug config
/racksnstands debug fixture
/racksnstands debug repairing
/racksnstands debug classification
/racksnstands reload_displays
/racksnstands debug scene 100
/racksnstands debug scene 500
/racksnstands debug scene 1000
```

The first three inspect the targeted slot and held item. `reload_displays` performs a normal server `/reload`, then synchronizes the authoritative profiles. Scene commands create furniture in an empty grid offset from the command origin and refuse to replace existing blocks.
