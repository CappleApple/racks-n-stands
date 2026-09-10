# Item and item-tag display transforms

Weapons and tools use their in-hand models. Mining tools, axes, hammers and tridents point up; other weapons point down, except bows. Simply Swords, Simply More, Cataclysm, Too Many Bows, and Iron’s Spellbooks defaults are bundled. Those orientations are bundled datapack rules, so packs can replace them or add rules for individual modded items without editing every slot.

Create JSON files at:

```text
data/<namespace>/racksnstands/item_transforms/<rule_name>.json
```

An exact-item example:

```json
{
  "priority": 100,
  "items": ["minecraft:iron_sword"],
  "rotation": [0, 0, 135],
  "scale": 1.25,
  "offset": [0, 0.1, 0]
}
```

A tag example limited to item racks:

```json
{
  "priority": 50,
  "item_tags": ["racksnstands:upright_tools"],
  "fixtures": ["racksnstands:weapon_rack", "racksnstands:polearm_rack"],
  "rotation": [0, 0, -45],
  "scale": 1.15
}
```

`items` and `item_tags` are alternative selectors; at least one must be present. Tag IDs do not include `#` in this format. An omitted or empty `fixtures` array allows the rule on every fixture. Fixture filtering does not grant item acceptance: the slot's ordinary item filter still applies.

The Large Item Rack (`tool_rack`), Item Rack (`weapon_rack`) and Small Item Rack (`polearm_rack`) use in-hand geometry and the normal item orientation rules. Their base profile scale is 1: geometry that fits stays at full size, while larger models are uniformly clamped to each slot's width, height and depth after orientation and item-rule transforms. The Display Shelf (`curio_cabinet`) also fits 3D models; its profile Y positions anchor the bottom on the shelf surface, with the lower row's shorter clearance respected. Blocks retain their block geometry and are fitted too.

The highest-priority matching rule wins. Equal priorities use the rule resource location in ascending alphabetical order. Rules do not accumulate. To replace a built-in rule completely, override its exact file path. To override only selected items, give your rule a higher priority.

- `rotation` is optional and replaces the slot's X/Y/Z Euler angles in degrees. It is applied around the item center in fixture-local space. Hand-model baked geometry is centered with a 180-degree Y facing correction and retains its native hand scale. Its grip rotation and translation are omitted. Custom-renderer items retain their own hand transforms, so packs may need model-specific angles.
- `relative_rotation: true` adds the rule angles to the fixture plane, preserving horizontal tabletop placement. Bundled defaults use this; the default for user rules remains absolute rotation.
- `display_context` optionally selects a Minecraft model context, for example `thirdperson_righthand` or `fixed`. Weapons/tools automatically use the right-hand model unless a matching rule overrides it.
- `scale` is an optional uniform multiplier on the fixture slot's scale. Its default is 1.0; valid multipliers are 0.01–4.0, with final scale capped at 4 per axis.
- `offset` adds a block-local X/Y/Z translation to the slot position. Its default is `[0,0,0]`; each value can be -2 through 2. For large placement changes, also adjust the profile's logical interaction region.
- `model_translation` shifts the model in its own coordinates before the fixture scale and rotation. It defaults to `[0,0,0]`, with each coordinate between -16 and 16. Use it to center custom renderers whose origins follow the player’s grip. It is available on both transforms and item rules.
- `priority` defaults to 0 and ranges from -10000 through 10000.

Bundled rules live under `data/racksnstands/racksnstands/item_transforms/`:

- `upright_tools`: mining tools, priority 15, Z=-45.
- `downward_swords` / `downward_weapons`: swords and other weapons, priority 10, Z=135.
- `upright_axes_hammers`: vanilla/common axes, hammers and mace, priority 25, Z=-45.
- `upright_tridents`: native 3D trident, priority 30, zero rotation.
- `bows`: priority 30, zero rotation.
- `sideways_display_stand`: weapons/tools on the Display Stand, priority 60, Z=45. Native tridents use Z=90 and native Simply Swords 3D hammers use Z=135 at priority 65. These fixture-scoped rules leave other displays upright/downward as appropriate.
- `outward_shield`: native Minecraft shield, priority 50, fixed context with zero rotation. Its fixed model is already 3D and faces the viewer instead of importing the side-on hand grip.
- `simplyswords_axes`: optional exact greataxe IDs, priority 35, Z=-45.
- `simplyswords_3d_hammers`: optional exact greathammer IDs, priority 40, Z=45 for their authored 3D geometry.
- `simplymore_weapons`: all published weapon-family tags, priority 12, Z=135.
- `simplymore_axes_hammers`: greataxe and pernach tags, priority 35, Z=-45.

Cataclysm rules correct 25 custom-rendered weapons, with separate horizontal Display Stand variants and model-origin offsets. Too Many Bows rules cover all 38 registered bows in the tested version, grouped by their authored axes. Iron’s rules correct its five baked staffs, custom Pyrium staff, 14 three-dimensional spellbooks, and dynamic affinity ring. Staffs point down on vertical displays and lie across the Display Stand; books present their broad cover face. Additional Display-specific rules lift the books and staffs above the display surface. Bow models are scaled to fit the furniture. Ordinary rings and amulets retain their normal item appearance.

The highest-priority match still wins, so an exact user rule at priority 100 overrides any bundled default. Optional external IDs/tags are inert when absent and introduce no runtime dependency. Resource packs that replace weapon geometry may need their own orientation rules.

When no rule matches, NeoForge weapon/tool abilities and registered categories supply a default orientation. An explicit rule can override this fallback.

Run `/reload` or `/racksnstands reload_displays` after changing rules. The server synchronizes them to connected clients. Resolved transforms are cached until the visible item or profile/rule snapshot changes; no item/tag classification is repeated each rendered frame.
