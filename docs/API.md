# Developer API

Public types are under `com.cappleapple.racksnstands.api`. Register integrations during mod setup, before datapack reloads. Server storage changes must run on the server thread.

## Display and classification

- `DisplayFixture` exposes profile ID, immutable profile metadata, capacity and a borrowed read-only stack view.
- `DisplayProfile`, `DisplaySlotDefinition`, `DisplayFilter`, and `DisplayTransform`, and `ItemDisplayRule` have public codecs. Collections are immutable; treat the cached transform quaternion as read-only.
- `Classification.register(DisplayCategoryProvider)` adds an independent classifier. Add resource-location categories to the supplied set. Avoid world scans; classification runs only when needed for interactions, automation, visible stack/profile changes or diagnostics.
- `Profiles.registerDefault(id, profile)` establishes a fallback and registered capacity for an extension fixture. JSON may override its slot properties. Capacity changes are rejected.
- `FixtureInteraction.registerBehavior(id, behavior)` adds a server interaction handler. Return true when consumed; false falls through to standard targeted insertion/removal/exchange. The incoming hit is recomputed from the server player's view.

No compatibility code is selected by another item's Java package or a mod-name substring. Equipment uses native item metadata; custom classification can use tags, components and addon APIs.

## Sharing the backend

`FixtureBlock` is extensible and accepts a `FixtureCatalog.Kind` descriptor. Register your block and block item normally, and give the block a `newBlockEntity` override returning:

```java
new FixtureBlockEntity(MY_BLOCK_ENTITY_TYPE.get(), pos, state)
```

Register that BlockEntityType with your block as a valid block. The shared entity resolves its profile by the actual registered block ID, so your namespace is preserved. Register a matching default profile with the same slot count as the descriptor. Register the standard item handler capability using `fixture.automation()` and the shared client `FixtureRenderer` for your type. Use the supplied fixture loot-table pattern to copy enchantments while the block lifecycle drops contents separately. Do not also put contents into the fixture drop.

Shared `FixtureGeometry` solids determine default shapes and datagen models, while subclass overrides may provide other block shapes or placement rules. `SurfaceDisplayBlock` adds `AttachFace` placement and matching geometry/item transforms without changing the storage entity type. `Kind.tall()` currently identifies the built-in mannequin; custom multi-block furniture should implement its own placement/removal rules around the shared storage entity.

`displayedStack` and `IItemHandler.getStackInSlot` return borrowed stacks, as standard item-handler contracts expect. Do not mutate them. `insert`, `extract` and the automation wrapper enforce filters, unit capacity, simulation, events and reentrancy locks. Automation disabling is checked on every call, including through already-cached capability objects.

## Repair

`RepairingHost.repairingLevel()` exposes effective maintenance power. `RepairMath.rate`, `RepairMath.advance`, and `RepairProgress.of` implement the continuous rate, fractional accumulation, and remaining-time calculation. The old discrete `RepairMath.repair` helper remains available for external callers. Register fixture item IDs in `#racksnstands:repairing_hosts` for real enchantment compatibility. A tag alone does not add scheduling behavior to another mod's unrelated block entity.

The shared entity stores real ItemEnchantments and per-slot fractional durability credits. Changes settle previous occupants' elapsed time before new items arrive. A successful armor transaction exchanges all bound slots (one individual armor slot or four mannequin slots) and resets its credits. Scheduled block updates account world-time deltas. Serialization preserves the accounting timestamp, save timestamp, and fractional balances. Client snapshots include per-slot server rates, balances, cadence, and the accounting timestamp so progress never uses a mismatched client common config. `settleForInteraction` requires the transaction lock and must run before copying exchange snapshots.

## Events

Events are posted on `NeoForge.EVENT_BUS`:

| Event | Timing | Cancelable |
|---|---|---|
| `FixtureItemInsertEvent` | Before insertion, including armor preflight | Yes |
| `FixtureItemExtractEvent` | Before extraction, including armor preflight | Yes |
| `ArmorLoadoutSwapEvent` | After all armor validation, before individual slot events and commit | Yes |
| `FixtureRepairEvent` | Once after a batch actually repairs items | No |

Insert/extract event stacks are defensive copies. During preflight the fixture rejects reentrant transfers. Simulation does not post events or change state. Listeners should veto operations or observe them; they must not independently mutate the player inventory during a loadout event. The transaction verifies both snapshots again after listeners run and abandons its own commit if another handler changed them.

Armor swapping calls `ItemStack.canEquip`, rejects oversized armor stacks and the `PREVENT_ARMOR_CHANGE` effect for noncreative players, then uses ordinary player equipment setters. NeoForge's noncancelable `LivingEquipmentChangeEvent` is delivered by the normal entity lifecycle after equipment changes; this mod does not fabricate a pre-event or post duplicates.

Breaking the fixture bypasses extraction vetoes so an integration cannot silently destroy inaccessible gear. `dropContents` drains storage once before spawning drops and is idempotent across removal paths. Creative and double-block removal use the same storage owner. No inventories are serialized into dropped fixture items.

## Client boundaries

Client presentation code and the equipment-view mixin are loaded only on the client distribution. No stack copies are made per frame; transforms, humanoid models and render bounds are cached. Actual item and armor buffers remain live to preserve glint, trims, animation and item-renderer behavior. Profile synchronization uses one bounded, server-to-client definition payload per resource, with no custom client-to-server inventory action packets.

## Exchanges and hand models

`FixtureInteraction.swapHeld` preflights both insertion and extraction before a single-slot commit. `commitSlot` requires the fixture transaction lock and copies its replacement stack. Slot snapshots are checked again after listeners run. No enchantment or other item components are merged between the exchanged stacks. `ArmorSwap` supports one bound armor slot as well as the full four-slot mannequin.

The client selects hand-specific models through `BakedModel.applyTransform`, then centers standard baked geometry for display while retaining its hand scale. Center metadata and wrappers are discarded on model baking/reload. Item overrides and render passes remain live. A dedicated glint shader masks standard atlas-backed item glint with sprite alpha; fixed render buffers preserve correct base/glint ordering.

Repair sound milestones are persisted separately in `RepairSoundProgress`, as per-slot percentages restored since the last sound threshold. They are server-only bookkeeping, reset with incoming gear, and do not change repair events or durability accounting.


Native armor rendering uses `HumanoidArmorLayer`, allowing Immersive Armors and GeckoLib/GeoRenderProvider hooks to provide their own geometry and textures. A client-only, thread-scoped equipment view supplies the displayed armor to those hooks and restores ordinary getters in `finally`. It neither spawns entities nor changes player inventory contents. Elytras use the resting vanilla wing model. Armor model parts reset between items to prevent shared pose state leaking between fixtures.

`PreviewLayout` measures geometry after the resolved item rotation, scale and model translation, excluding transparent sprite padding across animation frames. It caches bounds by fixture revision, resolved transform and baked model identity. `CompartmentFit` uniformly reduces oversized models in all three dimensions. The fit is applied in fixture coordinates before item orientation. Large racks center their 3D models; shelves align their bottoms with the profile's Y anchor, including displayed block items. Lower shelf compartments use their shorter physical height. Actual item rendering remains live.
