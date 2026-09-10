# Packmaker guide

## Display profiles

Override `data/racksnstands/racksnstands/displays/<fixture_id>.json`. A different mod's fixture uses its own namespace under `data/<namespace>/racksnstands/displays/`. Generated built-in files are complete examples.

Profiles define presentation and acceptance, not registered block capacity. Built-in slot counts are fixed; changing capacity is rejected so existing gear cannot become inaccessible. Create a new registered fixture through the API for a different capacity. Individual definitions fail independently with a resource-specific warning and built-in fallback. Items already stored are preserved even if a reload changes the filter; extraction remains possible.

Example replacing the generic wall display with a small diamond or emerald display:

```json
{
  "behavior": "racksnstands:normal",
  "slots": [{
    "index": 0,
    "filter": {
      "allowed_items": ["minecraft:diamond", "minecraft:emerald"],
      "denied_item_tags": ["racksnstands:display_denied"]
    },
    "transform": {
      "translation": [0.5, 0.52, 0.70],
      "rotation": [0, 0, 45],
      "scale": [0.65, 0.65, 0.65],
      "display_context": "fixed"
    },
    "interaction_bounds": [0, 0, 0, 1, 1, 1]
  }]
}
```

Slot indices are unique and contiguous from zero, with at most 16 slots. Coordinates are block-local, relative to the north-facing base block. +X is right, +Y is up, and the front faces -Z. A wall fixture's backing is near Z=1. Euler rotation is in degrees, applied X then Y then Z. Positive scale components must be at most 4. The model and logical hit regions rotate together with the block facing. Interaction regions use `[minX,minY,minZ,maxX,maxY,maxZ]`; keep regions non-overlapping for predictable targeting. If the hit misses every region, the nearest region in the display plane is chosen.

`display_context` accepts Minecraft's normal item display-context names, such as `fixed`, `ground`, `gui`, or `head`. `equipment_slot` may bind an armor slot (`head`, `chest`, `legs`, `feet`); `worn_armor: true` invokes worn-armor rendering where supported. For worn armor, the transform is the humanoid model's origin at the head/shoulders. Native armor-layer hooks handle modded armor models and textures. Elytras render resting wings. If no supported worn model exists, the normal item model renders instead.

Behavior defaults to `racksnstands:normal`. `racksnstands:armor_loadout` requires exactly four slots, each bound to one of the four distinct vanilla armor slots. Other behavior IDs can be registered through the public interaction API.

The item named Display retains registry ID `racksnstands:generic_tabletop_display` for existing worlds. Its `face` state is `floor`, `wall`, or `ceiling`. Clicking the top/bottom/side of a support chooses that attachment; stored items, targeting and geometry rotate together. Its profile coordinates describe the floor form, with attachment rotation applied around the whole display afterward.

See [item transform rules](ITEM_TRANSFORMS.md) for per-item and per-tag orientation, scale, and offsets.

## Filters and categories

Available filter fields:

| Field | Meaning |
|---|---|
| `accepted_categories` | Item category resource locations |
| `accepted_item_tags` | Item tag locations without `#` |
| `denied_item_tags` | Denied item tags without `#` |
| `allowed_items` | Explicit registry IDs |
| `denied_items` | Explicit registry IDs |
| `accepted_curio_slots` | Exact Curios API string identifiers |
| `any_curio` | Accept any item with at least one valid Curios slot |
| `predicate` | Native Minecraft item predicate |

Global `#racksnstands:display_denied` and explicit denials always win. Equipment binding is mandatory. A predicate is an additional mandatory gate. The remaining positive selectors are alternatives: an explicit item allow, an accepted tag, a matching category, or matching Curios metadata can admit an item. With no positive selector, the slot is generic. All stock non-armor slots are generic; stock armor slots only enforce their equipment binding.

Define categories at `data/<namespace>/racksnstands/categories/<name>.json`, using the same filter structure. Category definitions cannot refer to other categories, preventing cycles. Example `data/mypack/racksnstands/categories/gems.json`:

```json
{"accepted_item_tags": ["c:gems"]}
```

Use `"accepted_categories": ["mypack:gems"]` in a slot. A native predicate example is `"predicate": {"items": "minecraft:diamond"}`; predicates decode with registry-aware operations and can use Minecraft components/subpredicates.

Shipped tags under `data/racksnstands/tags/item/` are `tools`, `swords`, `weapons`, `bows`, `shields`, `staffs`, `polearms`, `helmets`, `chestplates`, `leggings`, `boots`, `display_denied`, and `repairing_hosts`. Tag files refer to other tag IDs using `#`, as usual. Add modded IDs to those files or use explicit profile rules. Tools additionally recognize the TOOL component and NeoForge item abilities; ranged items recognize bow/crossbow use animations; shields recognize the shield ability. No item names are guessed from words such as "staff".

Datapack profiles synchronize on player login and server reload. Clients do not need the datapack separately. Texture/model overrides remain ordinary client resource packs.

## Configuration

`config/racksnstands-common.toml`:

```toml
[repairing]
max_level = 10
percent_per_level = 0.20
interval_ticks = 20
calculation_period_seconds = 3600.0
repair_while_chunk_unloaded = true
repair_damaged_items_only = true
repair_particles = true
repair_sound = true
repair_sound_interval_percent = 1.0
repair_sound_volume = 0.15
repair_sound_pitch = 1.7

[interaction]
armor_quick_swap = true
allow_full_stacks = false

[automation]
allow_automation = true

[curios]
enable_curios_integration = true
```

`config/racksnstands-client.toml`:

```toml
[rendering]
display_item_render_distance = 64
show_repair_tooltip = true
```

`max_level` clamps effective repair strength. Change `data/racksnstands/enchantment/repairing.json` separately to change the enchantment's acquisition maximum. Higher levels can therefore be retained on items without exceeding the server's configured maintenance cap. Disabling `repair_damaged_items_only` also checks full durable stacks; it never modifies undamaged or non-durable stacks or banks surplus repairs. Full items use scheduled updates in that mode, so the default is more efficient.

Intervals are world ticks at nominal 20 TPS. Catch-up counts unloaded time while the world runs, without force-loading chunks. Loaded-only progress is captured during regular accounting and saves. No wall-clock timestamp is used. Pickup/reinsertion and loadout swaps start new per-slot intervals. A fixture cannot store repair credit to spend on future inserted items.

`allow_full_stacks` applies live to general display slots and automation. It respects each item's maximum stack size, allows automation to merge identical components, and keeps armor slots at one piece. Manual interaction swaps stacks. Disabling the option preserves larger stored stacks and allows their extraction; new insertions obey the single-item limit.

Empty-hand sneak interaction equips Curios into compatible active functional slots, preferring empty slots before replacing occupied ones. Native Curios validation, equip/unequip events, and binding restrictions apply. Cosmetic slots are not used. A stacked Curio equips one item: an empty equipment slot leaves the remainder on display; a swap returns the previous Curio to the display and any remainder to inventory or a drop.

## Acquisition, construction and visuals

Repairing is in vanilla treasure, random-loot and tradeable enchantment tags, and is absent from `in_enchanting_table`. Datapacks can replace those acquisition tags. Standard anvil books and real enchantment components work normally. Recipes live at `data/racksnstands/recipe/`; the initial designs use oak planks, sticks and centerpiece ingredients with distinct layouts. Recipe costs are independent of repair power. The 13 active recipes are unconditional; retired specialty fixtures have no recipe.

Models are at `assets/racksnstands/models/block/` and independent item geometry at `assets/racksnstands/models/item/`. The initial models reference vanilla textures; a resource pack can point their texture variables to its own `assets/racksnstands/textures/block/` images. Item geometry inherits `minecraft:block/block` display transforms. Models taller than one block are normalized into an item-sized cube, while placed models retain their full height. Built-in collision and selection shapes come from the same furniture solids as datagen, including rotated walls and split mannequin halves. A client resource pack alone does not change server collision geometry. Changing a baked model does not automatically move the displayed gear; pair geometry changes with a server display-profile override when needed.

Use `/racksnstands debug classification` while holding an item and looking at a fixture. It reports the slot, resolved categories, accepted and denied tags/items, equipment binding, Curios metadata, acceptance result, repair level, clock, credit, and next interval.

## Live configuration

Saving `config/racksnstands-common.toml` or `config/racksnstands-client.toml` updates the running config through NeoForge's file watcher. None of this mod's TOML settings requires a reload or restart. Common options are read on the logical server; client render distance is read by the equipment renderer. Saved common changes also refresh maintenance scheduling on loaded fixtures. `/racksnstands debug config` reports the currently active common values. NeoForge's global config watcher must be enabled (the default).

Datapack JSON remains part of Minecraft's normal data reload lifecycle. The TOML watcher does not reload unrelated datapacks or resource packs. See [material customization](MATERIALS.md) for saved appearance data and per-material block state examples.

## Continuous repair timing

`interval_ticks` is the scheduling cadence, independent of `calculation_period_seconds`, which is the rate denominator. Sub-point repair is persisted; a small item does not receive a free whole durability point every update. The default rate is `maximum durability * 0.2 * enchantment level / 72000` points per game tick. Every slot gets its own full rate, fractional balance, durability bar and ETA.

Old `interval_minutes` settings migrate to `calculation_period_seconds` by multiplying by 60; the new cadence starts at 20 ticks. The old default percentage of 0.1 becomes 0.2; other customized percentages are retained. Migration makes a `.pre-continuous-repair.bak` copy. Existing elapsed-tick repair credits migrate when their block entities load. Saving live rate changes settles loaded fixtures at their previous rates before applying the new rate; changing cadence replaces pending scheduled ticks immediately.

The progress HUD can be disabled with `rendering.show_repair_tooltip` in the client config. Jade supplies its own provider toggle as well. With Jade installed, the native overlay stays hidden.

## Repair effects

`repair_particles` and `repair_sound` independently enable particles and sound. Particles accompany updates that restore whole durability. `repair_sound_interval_percent` counts actual durability restored per item as a percentage of that item's maximum: `1.0` means 1%, `5.0` means 5%. The range is 0.01 to 100. Sound milestones retain their remainder across updates and saves, reset on replacement or full repair, and never transfer between items. Several items or catch-up milestones in one update produce one fixture sound. Disabled or zero-volume sounds do not accumulate a playback backlog.

`repair_sound_volume` accepts 0 to 16 (default 0.15); zero suppresses sound dispatch. Values above 1 extend audibility according to Minecraft's sound-distance behavior. `repair_sound_pitch` accepts 0.5 to 2 (default 1.7). These values are read live and do not affect repair speed.
