# 1.4.2 - 2026-09-13

## Fixed

- Stored equipment duplicating onto the ground when fixtures move into or out of Sable / Create Aeronautics ships.
- Full Armor Stands destroying their other half during ship transitions.

# 1.4.1 - 2026-09-12

## Changed

- Furniture recipes accept tagged planks, wooden rods, iron ingots, wooden slabs, stone bricks, wooden chests, and carved pumpkins.
- Individual chestplate, leggings, and boots stands accept any armor piece in the corresponding vanilla armor tag.
- Wooden furniture recipes unlock with any tagged planks; the Recessed Pedestal recipe unlocks with any tagged stone bricks.

# 1.4.0 - 2026-09-09

## Added

- Craft customized stands and displays alone to reset their appearance while retaining enchantments and other item data.
- Copy appearance to an entire uncustomized stack of the same fixture type using a reusable customized template, preserving the target stack's enchantments.
- Added golden apples, diamonds and hearts of the sea to the built-in Repairing ritual, for seven materials total.

## Changed

- Rebalanced Repairing ritual power so the seven distinct known materials reach level V passively, with higher levels requiring boosted power.
- Set furniture enchantability to the neutral ritual baseline so the same offerings work on both furniture and books.
- Item racks display models at full size when they fit and only shrink oversized models to their slot bounds.
- Exclude transparent texture corners when fitting rotated sprites, allowing narrow swords and tools to use their available space.

# 1.3.2 - 2026-09-09

## Changed

- Restored 3D in-hand item models and datapack orientations on the Large Item Rack and Display Shelf.
- Uniformly fit displayed models to compartment width, height and depth while keeping shelf items on their shelf boards.

# 1.3.1 - 2026-09-09

## Added

- Configurable repair sound percentage threshold, defaulting to 1% of maximum durability restored.
- Live repair sound volume and pitch settings.
- Optional full-stack display storage, disabled by default.
- Empty-hand sneak interaction equips or swaps displayed Curios into matching functional slots.
- Default orientation rules for Cataclysm, Too Many Bows, and Iron's Spellbooks weapons, staffs, and spellbooks.
- Repairing enchantment description translation.

## Changed

- Furniture items stack to 64 when their enchantments and materials match.
- Renamed Generic Wall Display to Wall Display.
- Gave Large Item Rack, Item Rack, Small Item Rack, and Display Stand distinct crafting layouts.
- Removed instructional furniture item tooltips.
- Large Item Rack and Display Shelf use flat inventory previews, sized to fit their compartments.
- Shelf items rest on the shelf surfaces using their visible model bounds.
- Expanded the Full Armor Stand chest hitbox and chest selection region.

## Fixed

- Repair sounds playing for every whole durability point instead of a percentage milestone.
- Duplicate crafting inputs making four furniture recipes collide.
- Full and individual armor stands displaying armor away from the placing player.
- Elytras rendering as item icons instead of resting wings.
- Immersive Armors pieces missing and Iron's Spellbooks armor using missing textures.

# 1.3.0 — 2026-09-09

## Added

- Continuous fractional durability repair with saved progress and chunk-load catch-up.
- Separate repair update ticks and rate-calculation seconds settings.
- Current/max durability, a translucent light-blue repair bar and time remaining for the looked-at item, with optional Jade integration.
- Migration for previous timing settings and saved elapsed repair credits.

## Changed

- Default repair updates run every 20 ticks.
- Default repair rate is 20% per level over 3,600 seconds.
- Live cadence changes replace pending repair updates immediately.

## Fixed

- Repairs waiting for the entire rate-calculation period before awarding any durability.
- Exchanges copying gear before settling its earned repair.
- Loaded-only saves losing progress earned since the last repair update.

# 1.2.0 ï¿½ 2026-09-09

## Added

- Sneak-right-click block texture sampling for matching material parts on all fixtures.
- Saved materials on placed furniture and its inventory, held, and dropped forms.
- Independent pedestal base, body, and cap materials.
- A live config diagnostic command.

## Changed

- Renamed Full Armor Mannequin to Full Armor Stand.
- Pedestal bottoms use polished andesite by default.
- Saved config changes refresh loaded-fixture maintenance scheduling.

## Fixed

- Discontinuous top and bottom texture coordinates around furniture supports.
- Stretched texture scale on tall furniture parts.
- Interaction targeting now uses the current server-side player ray.

# 1.1.0 ï¿½ 2026-09-09

## Added

- Full wooden mannequin head and automatic matching-slot insertion/replacement when clicking anywhere with armor.
- Display mounts for floors, walls and ceilings.
- Full-height stone pedestal with a recessed blade slot and sword placement extending above its cap.
- In-hand weapon/tool models and bundled Simply Swords / Simply More orientations.
- Per-item display-context overrides and relative rotations that preserve tabletop placement.

## Changed

- Consolidated furniture into 13 active designs; general displays accept any item, with armor-slot restrictions confined to armor stands.
- Occupied slots silently exchange held items; sneak interactions exchange equipped items, including individual armor pieces.
- Axes, greataxes, hammers and tridents face upright; other non-bow weapons face down.
- Removed retired specialty furniture from recipes and creative entries.
- Display Stands hold weapons and tools sideways across two centered metal supports.
- Display Shelves place against walls with flush backing.
- Renamed Generic Tabletop Display to Display.
- Selection and collision shapes follow actual furniture solids, gaps, facing and mannequin halves.

## Fixed

- Enchantment glint bleeding through transparent item pixels onto neighboring displays.
- Oversized furniture icons, held items and dropped items.
- Lower display rows sitting below the rack base.
- Shields inheriting a sideways hand-grip rotation.
- Stack conservation and component isolation during occupied-slot exchanges.

# 1.0.0 — 2026-09-09

## Added

- Twenty-three equipment display fixtures with shared storage and rendering.
- Repairing I–X treasure enchantment and configurable passive durability repair.
- Atomic vanilla armor loadout swapping and targeted slot interaction.
- Datapack display profiles, filters, item categories and native item predicates.
- Larger upright tools and tridents, downward swords, and per-item or item-tag transform rules.
- Optional Curios displays and native Rituals Not Rolls acquisition data.
- Automation, comparator output, debug commands, datagen and test scenes.

## Fixed

- Standalone armor placement, sleeve/boot overlaps, and furniture faces that caused z-fighting.
- Item rotation signs for Minecraft fixed display transforms and spacing between repeated weapons.
- Repeated resource identifier allocation during profile lookup.
- Connected the Curio Cabinet bottom shelf across its full width to both side panels.
- Removed the Rituals Not Rolls dependency/version gate and reflective test; integration is bundled Repairing datapack data only.
