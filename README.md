# Racks N' Stands

Racks N' Stands adds equipment furniture for displaying, swapping, and slowly repairing gear.

The mod includes armor stands/mannequins, weapon and tool racks, pedestals, wall displays, shelves, and smaller equipment stands. Furniture can inherit block textures for pack-friendly visual customization, while the **Repairing I–X** enchantment controls passive repair speed.

Requires Minecraft 1.21.1, NeoForge 21.1.248 or newer, and Java 21. Install the mod on both the client and server.

## Using the furniture

Most displays work directly in-world rather than through a container screen.

- Right-click a display slot to insert, remove, or swap an item.
- Armor furniture only accepts equipment for the appropriate body slot.
- The Full Armor Stand can swap an entire armor set with the player at once.
- Empty-hand sneak + right-click performs the fixture's equipment-swap action.
- Curios displays can equip a stored Curio into a compatible slot when Curios is installed.
- Hoppers and other NeoForge item handlers can interact with fixtures while respecting their filters.
- Comparators output an occupancy signal.

General-purpose displays hold one item per slot by default. Packs that want storage-style racks can enable full stacks in the common config; armor slots remain single-item.

Recipes accept tagged materials, including mixed wood types and matching modded ingredients. The recipe book unlocks wooden furniture with any tagged planks and the Recessed Pedestal with any tagged stone bricks. Ingredient choices do not change the furniture appearance; use material customization for that. See [recipe ingredients](docs/PACKMAKERS.md#recipe-ingredients) for the accepted tags.

## Furniture

The active set includes:

| Fixture | Slots | Use |
| --- | ---: | --- |
| Full Armor Stand (`armor_mannequin`) | 4 | Full worn armor + loadout swapping |
| Helmet Stand | 1 | Helmet display |
| Chestplate Stand | 1 | Chest / elytra display |
| Leggings Stand | 1 | Leg armor display |
| Boots Stand | 1 | Boots display |
| Large Item Rack | 6 | General weapons/tools |
| Item Rack | 4 | General weapons/tools |
| Small Item Rack | 3 | Narrow/polearm display |
| Display Stand | 1 | Horizontal weapon/tool display |
| Recessed Pedestal | 1 | Full-height item display |
| Wall Display | 1 | Wall-mounted item |
| Display | 1 | Floor/wall/ceiling display |
| Display Shelf | 6 | Curios/general items |

Older registered fixture IDs remain available for world compatibility even when they are no longer craftable.

## Item rendering

Racks N' Stands tries to display equipment using the model players recognize from their hands rather than flattening everything into inventory icons.

Items are fitted to the available space while keeping their proportions. Default orientation rules distinguish things such as swords, axes/hammers, bows, tridents, and pedestal weapons, and packs can replace or extend those rules with item/tag profiles.

There are built-in optional rules for several weapon/content mods, including Simply Swords, Simply More, Cataclysm, Too Many Bows, and Iron's Spellbooks.

Detailed transform/profile documentation:

- [Item transforms](docs/ITEM_TRANSFORMS.md)
- [Packmaker profiles and filters](docs/PACKMAKERS.md)

## Material customization

Sneak + right-click a fixture with a block item to sample that block's texture onto the selected furniture material group. The block is not consumed.

Some furniture has multiple independently sampled groups—for example, a pedestal can have a different base, body, and cap.

The customization survives breaking/placing the fixture and is also visible on its inventory/held model.

A customized fixture can be crafted by itself to reset its material appearance, or used as a template to copy its appearance onto an uncustomized stack of the same fixture.

See [docs/MATERIALS.md](docs/MATERIALS.md) for pack/resource details.

## Repairing

The **Repairing** enchantment belongs to the furniture, not the stored item. Any normal durability item placed on an enchanted fixture slowly repairs while it remains there.

The default rate is:

```text
20% of the item's maximum durability
per Repairing level
per 3600 seconds of world game time
```

So with defaults:

- Repairing I restores 20% of max durability per hour.
- Repairing V restores 100% per hour.

Repair progress is calculated continuously and applied on a configurable interval. Fractional progress is carried forward, so changing the update interval does not change the effective repair rate.

Unloaded fixtures can catch up from elapsed **world game time** when their chunk loads again. The mod does not force-load chunks, and server-offline time is not counted.

Repairing only affects normal Minecraft durability.

## Repair HUD

Looking at a repairable stored item shows its current/max durability and estimated time until full repair.

If Jade is installed, that information is added to Jade. Otherwise the mod uses its own compact HUD.

This can be disabled in the client config.

## Rituals Not Rolls

Racks N' Stands includes optional Rituals Not Rolls data for acquiring Repairing through its enchanting system.

The default material set provides enough passive power for Repairing V under the shipped Rituals Not Rolls rules. Rituals Not Rolls is not required for the furniture or repair mechanic itself.

See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) for the material values and optional-mod behavior.

## Configuration

```text
config/racksnstands-common.toml
config/racksnstands-client.toml
```

Common settings cover storage/automation, Repairing rate/timing, unloaded catch-up, sounds, and related gameplay behavior. Client settings cover rendering distance and the repair overlay.

Config files are watched while the game is running, so supported changes can take effect without a restart.

Full packmaker/config reference: [docs/PACKMAKERS.md](docs/PACKMAKERS.md).

## Documentation

- [Material customization](docs/MATERIALS.md)
- [Item orientation/scale/offset rules](docs/ITEM_TRANSFORMS.md)
- [Packmaker configuration, profiles, predicates, and recipes](docs/PACKMAKERS.md)
- [Curios and Rituals Not Rolls compatibility](docs/COMPATIBILITY.md)
- [Public API](docs/API.md)
- [Testing and performance notes](docs/VALIDATION.md)

## Development commands

Commands below require permission level 2:

```text
/racksnstands debug config
/racksnstands debug fixture
/racksnstands debug repairing
/racksnstands debug classification
/racksnstands reload_displays
```

There are also debug-scene commands for building larger furniture layouts during rendering/performance testing.

## Building

Requires Java 21.

```powershell
.\gradlew.bat runData
.\gradlew.bat test build
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
```

Generated resources are committed, so ordinary builds do not require datagen first.

Development/GameTest fixtures are excluded from the release jar.
