# Validation and performance



Validated on Windows, Java 21.0.12, Minecraft 1.21.1 and NeoForge 21.1.248 on 2026-09-09. Curios development runs use Curios 9.5.1+1.21.1.



## Version 1.4.0 validation

- Datagen and build passed. All 33 JUnit tests passed, including full-size rack clamping, uniform bounds fitting, and visible sprite outlines that exclude transparent corners after rotation.
- The standalone run passed all 55 required GameTests. The later Rituals Not Rolls run passed all 57 required tests, including an additional partial-inventory regression and the optional real-mod passive ritual test.
- Appearance tests cover all 23 active and legacy fixture types; batch counts of 1, 17 and 64; reset and copy component preservation; invalid and mismatched inputs; actual 2x2 and 3x3 crafting menus with left-click, right-click and shift-click; reusable stacked templates; count conservation when only part of the output fits the inventory; and the vanilla Crafter's scheduled execution. These are server-side menu tests, not a claim of manual client mouse-crafting coverage.
- With the installed Rituals Not Rolls implementation, the seven materials load into real knowledge pages/books and the automatic ritual solver reaches exactly Repairing V for books and every fixture type, with return chaining both disabled and enabled. No consumed resources, resource withdrawals or XP catalysts are needed. Omitting each material in turn leaves the baseline below V. This integration is a development-only test; the shipped integration remains a built-in datapack with no mod-presence or version checks.
- A dedicated server and the final client loaded with Curios, Simply Swords, Simply More, Cataclysm, Too Many Bows, Iron's Spellbooks, Immersive Armors and Rituals Not Rolls; Jade was loaded client-side. The client connected and received the new recipes successfully. Representative vanilla and modded items were visually reviewed on all three rack sizes: [rack size clamp](screenshots/rack-size-clamp.png). This checks representative rendered items, not every third-party model or resource pack.
- The final JAR validator passed: 27 block models, 858 faces, 23 display profiles, 15 recipes, current generated resources, and no bundled companion or QA classes.

Logs: `build/visible-outline-build.log`, `build/appearance-standalone-tests.log`, `build/appearance-ritual-tests.log`, `build/appearance-client-review-server.log` and `build/visible-outline-client.log`.

## Version 1.3.2 validation

- Build and all 29 JUnit tests passed. New geometry tests cover rotated asymmetric weapons, uniform proportions, all six compartments, lower-row shelf clearance, depth-limited models and small-item shelf contact.
- All 49 GameTests passed standalone and with Curios, Simply Swords, Simply More, Cataclysm, Too Many Bows, Iron's Spellbooks and Immersive Armors. Compact displays now assert hand-model contexts and the normal item orientation rules, while block items retain their block models.
- A dedicated server and client with those companions and client-side Jade loaded successfully. The client visibly rendered fitted 3D Cataclysm weapons, Iron's staffs and spellbook, Too Many Bows models and the vanilla trident on the rack and shelf. Items remained within their compartments and shelf items rested on the boards. See [3D rack and shelf](screenshots/fitted-3d-rack-shelf.png). This is representative client coverage, not an exhaustive visual check of every animated item or resource pack.
- The JAR asset validator passed: 27 block models, 858 faces, 23 display profiles, current generated resources and no bundled companion or QA classes.

Logs: `build/fitted-3d-tests.log`, `build/fitted-3d-companion-tests.log`, `build/fitted-3d-server.log` and `build/fitted-3d-client.log`.

## Version 1.3.1 validation

- Datagen, build and all 26 JUnit tests passed. All 49 required GameTests passed standalone and with Curios, Simply Swords, Simply More, Cataclysm, Too Many Bows, Iron's Spellbooks and Immersive Armors. Coverage includes sound percentage milestones/persistence, silent catch-up, volume/pitch, native recipe matching across all 13 recipes and their mirrored/offset placements, furniture stacking/component separation, optional full-stack storage and count conservation, Curios equip restrictions and multiple ring slots, all armor placement directions, chest selection, and compact display contexts. Final standalone tests also check shelf surface anchors.
- A dedicated server and client loaded all those companions with Jade on the client. Native armor-layer hooks visibly rendered Immersive Armors decorative pieces and Iron's Spellbooks armor textures. Resting elytra wings appeared on full and individual chest stands. Screenshots: [Immersive armor](screenshots/immersive-armor.png), [elytras](screenshots/elytra-stands.png).
- The client measured actual geometry for 32 Cataclysm items, all 38 Too Many Bows bows, and 155 non-block/non-armor Iron's items without capture errors. Default corrections cover 25 custom Cataclysm models, the three bow-axis groups, Iron's staffs and 3D spellbooks. Measurements and selected visual examples do not constitute an exhaustive visual check of every animated model or resource pack.
- Saving the common TOML with full stacks enabled, a 2% sound threshold, volume 0.25 and pitch 0.9 changed the live debug values without reload/restart. Restoring the original bytes restored single-item storage, 1%, volume 0.15 and pitch 1.7. The initial text probe accidentally doubled Windows carriage returns, which FML rejected; repeating it while preserving line endings passed.
- Flat rack/shelf previews were visually checked after fitting their geometry and excluding transparent sprite padding. The shelf items rest at the two shelf levels. Iron’s spellbooks show their broad covers on the Display and Wall Display, and representative Too Many Bows items lie sideways across supports or point up on the pedestal. Screenshots: [rack and shelf](screenshots/flat-rack-shelf.png), [spellbooks](screenshots/iron-books.png), [bows](screenshots/too-many-bows.png), [Cataclysm](screenshots/cataclysm-displays.png).
- The final JAR validator confirms 27 block models, 858 nonoverlapping faces, 23 profiles and matching generated resources. The client-only equipment-view mixin is included; development tests and third-party implementations are excluded.

Logs: `build/repair-sound-tests.log`, `build/refinements-tests.log`, `build/companion-final-tests.log`, `build/final-standalone-tests.log`, `build/visible-shelf-build.log`, `build/release-final-build.log`, `build/final-review-server.log`, and `build/shelf-final-client.log`. The companion test fixture originally assumed one ring slot; it now fills all provided ring slots before asserting an occupied-slot swap.

## Version 1.3.0 validation

- Datagen, build, and all 23 JUnit tests passed. Continuous arithmetic tests compare different update cadences, preserve fractions for low-durability items, clamp completed items without banking surplus, check ETA, and migrate old TOML settings with an idempotent backup.
- All 40 required GameTests passed standalone and with Jade 15.10.6+neoforge. Cases include saved-state catch-up without duplicate repair, loaded-only save gaps, legacy elapsed-credit migration before the entity receives its level, actual scheduled update cadence and live pending-tick replacement, authoritative progress snapshots, and looked-at targeting including the upper Full Armor Stand.
- An isolated real dedicated server confirmed actual chunk unload/reload. A Repairing I pedestal holding a diamond pickaxe at 1,000 damage had 0.867222 fractional durability after 200 ticks. The chunk was then removed from the forced-load set; `execute if loaded` returned no result. Reloading after another 1,201 elapsed game ticks produced 994 damage and 0.0748916667 fractional credit, matching `1561 * 0.2 * (200 + 1201) / 72000`. The ordinary review world was preserved; this experiment used `run-repair-qa`.
- A real Jade client connected to a server without Jade and displayed the selected rack item's repair data. The Full Armor Stand's upper half correctly showed its helmet. Both show current/max durability on a translucent light-blue bar with unshadowed text, followed by ETA; neither shows slot numbers or armor-slot names. The native overlay is suppressed with Jade loaded. Screenshots: [Jade rack](screenshots/repair-jade-rack.png), [Jade armor](screenshots/repair-jade-armor.png).
- A real client without Jade verified the matching native bar, current/max durability, and ETA on the Full Armor Stand's helmet. [Native tooltip](screenshots/repair-native.png). Earlier native observations also verified that looking between chest and legs selects independent item progress.
- Final asset/JAR validation passed for 27 block models and 858 faces. Generated resources match the JAR; Jade, companion, and QA classes are not bundled. The optional Jade provider and shared bar renderer are included.

Logs: `build/repair-final-tests.log`, `build/repair-jade-tests.log`, `build/repair-durability-final-tests.log`, `build/repair-unload-result.log`, `build/repair-qa-server.log`, `build/repair-jade-final-client.log`, and `build/repair-native-final-client.log`.

Repair time is world game time at nominal 20 TPS. Chunk catch-up does not count time while the server is stopped. These tests do not claim that every third-party durability implementation or Jade theme has been exhaustively checked.

## Version 1.2.0 validation



- Datagen, build, and all 18 JUnit tests passed. The new UV regression checks top and bottom fragments around a support against the uncut texture coordinates.

- All 35 required GameTests passed with Simply Swords and Simply More loaded. New cases cover linked material parts, independent pedestal base/body/cap groups, idempotent sampling, unchanged gear/sample counts, block-state properties, world/update/item persistence, loot and re-placement, every Display mounting orientation, upper-stand targeting, and the actual server ray taking precedence over a supplied hit.

- Asset validation passed: 27 block models, 858 faces, 23 normally sized item models, consistent generated resources in the JAR, and no coplanar overlap or out-of-bounds UVs. The item loot tables retain the material component.

- A real NeoForge client verified baked material meshes, grass tint, glass render layers, log bark/end grain, upper-stand material propagation, and material-aware GUI/ground/hand models. A real sneak-right-click with 16 diamond blocks changed the two supports together; server state retained all 16 blocks, the stored diamond sword, and the separate brick base. The visible brick and wood bases have continuous texture coordinates around the supports. [Material sampling screenshot](screenshots/material-sampling.png).

- Saving the running server's common TOML changed `allow_automation` from true to false, confirmed by `/racksnstands debug config`. Saving the client TOML changed render distance from 64 to 32, confirmed by the live renderer config read. No reload or restart was used for either change; both files were restored and the original values re-applied automatically.

- The final dedicated server reached `Done` with the new palette component and the user's existing review world. No client classes were required on the server.



Logs: `build/material-final-tests.log`, `build/material-final-server.log`, `build/material-final-client.log`, and the config watcher checks in `build/material-review-server.log` / `build/material-review-client.log`. The earlier unsuccessful material ray fixture was obstructed by its test-template border; moving its sightline above that border fixed the fixture. The real networked interaction then passed independently.



Material rendering retains ordinary source sprites, face orientation, animation, tint, and render layers. Entity-rendered blocks use their baked particle sprite when they have no face quads. Dynamic connected-texture/source-block-entity behavior and every third-party model are not exhaustively tested.



## Version 1.1.0 validation



- Datagen, build, and all 17 JUnit tests passed.

- All 30 required GameTests passed standalone and with Simply Swords 1.70.2-1.21.1 and Simply More 1.3.0_alpha loaded. The companion run exercised six actual registered weapon IDs and their hand-context/orientation rules.

- Armor insertion/replacement checks cover all 16 combinations of clicked mannequin region and held armor type, plus offhand insertion and empty-hand targeted removal.

- Exchanges cover enchanted-versus-plain item isolation, cancelable preflight, reentrancy, stacked held items with a full inventory, item-count conservation, armor equipment slots, Binding Curse, and all 256 full-mannequin occupancy combinations.

- Shape checks cover the open gap between centered stand supports, low table height, flush shelf backing in every facing, and the mannequin upper half, including its enlarged head. Surface Display checks cover all six placement faces, matching bounds and retained storage.

- Orientation checks cover axes, tools, swords, native tridents, tabletop-plane preservation, exact-item overrides, reload invalidation, model context, and the fallback for untagged weapons.

- Asset validation passed for 27 block models and 854 emitted faces, with no overlapping coplanar faces or out-of-bounds UVs. All 23 item models inherit normal block transforms and fit inside an item-sized cube. The pedestal's cap retains its recessed slot.

- The distributable JAR contains 13 active recipes, 23 profiles (including preserved legacy storage), 14 item rules, and Repairing I–X. Generated JSON files match the JAR; QA classes and companion classes are excluded.



Logs: `build/refinement-final-tests.log`, `build/weapons-gametests.log`, `build/weapons-server.log`, and `build/weapons-client.log`, `build/surface-review-server.log`, and `build/surface-review-client.log`. The installed Simply Swords build logs two unrelated optional-companion recipe errors when those other companions are absent; the server reaches `Done` and all required tests pass.



## Client observation



The real client was reviewed with Simply Swords, Simply More and their required libraries loaded. Observed results include the stepped pedestal with a sword extending above its cap, upright 3D greathammers and greataxes, downward longswords/grandswords, native upright 3D tridents, and the enchanted bow displayed beside unenchanted swords. Furniture icons fit normally in the hotbar. The final client also shows the horizontal sword/tool/hammer stands, a full wooden mannequin head aligned with worn helmets, and floor/wall/ceiling Display forms. Screenshots: [pedestal](screenshots/refined-pedestal.png), [Simply weapons](screenshots/simply-weapons.png), [native tridents](screenshots/native-tridents.png), and [glint isolation](screenshots/isolated-glint.png), [horizontal stands](screenshots/horizontal-stands.png), [mannequin head](screenshots/mannequin-head.png), [surface Displays](screenshots/surface-displays.png), and [wall shelf and outward shield](screenshots/wall-shelf-and-shield.png).



Earlier client observations cover dyed/trimmed worn armor and the connected shelf base. The armor pose separates inflated sleeves and feet; furniture datagen removes overlapping exposed faces. Third-party armor extensions and resource packs not installed in this client are not exhaustively validated.



Rituals Not Rolls integration remains bundled datapack data only; no presence/version check or runtime integration test was added.



## Historical 1.0.0 performance measurements



The measurements below predate the hand-model renderer in 1.1.0 and are not measurements of this revision.



The debug scene command creates mixed empty/occupied, durable/non-durable, damaged/full, enchanted/unenchanted fixtures. The current scene command uses the 13 active designs and accepts general items in every non-armor slot.



For a repeatable comparison, use a fresh empty area for each size, the same camera and settings, allow chunk meshes/JIT to warm up, then record at least 60 seconds. Test 100, 500 and 1000 fixtures separately. `/jfr start` and `/jfr stop` record the server; Java Flight Recorder attached to the actual client JVM records render-thread CPU and allocation samples. A server JVM is not a substitute for a client rendering measurement.



The initial `/tick sprint 2000t` observations were:



| Total fixtures | Reported milliseconds per tick | Context |

|---:|---:|---|

| 0 | 0.07 | Cold baseline |

| 100 | 0.01 | Warm server |

| 500 | 0.02 | Warm server |

| 1000 | 0.41 | Client joined and floor preparation overlapped the run |



These are smoke measurements with differing conditions, **not a controlled scaling benchmark**.



A separate 62.473-second JFR capture with 1000 mixed fixtures and an interacting client recorded whole-server mean tick time **0.465 ms**, median **0.363 ms**, p90 **0.790 ms**, p99 **1.624 ms**, and maximum **2.045 ms**. Whole-server heap allocation averaged about **4.46 MB/s**. Outbound traffic was **2.08 MB / 1264 packets**, of which about **2.07 MB** was chunk-with-light data. These include Minecraft, both companion mods and player activity; they are not mod-only costs. Raw files are `run-server/debug/server-2026-09-09-101231.jfr` and the matching JSON report.



A 45-second capture of the actual fixture-review **client** contained 723 render-thread execution samples; 55 (7.6%) included the fixture renderer in their stack. This is an inclusive CPU sampling share, not frame time, FPS, or GPU time. It includes normal Minecraft item/armor rendering invoked by the fixture. Sampled allocations primarily involved Minecraft pose matrices, render buffers and model traversal. The capture also identified an avoidable resource-location construction in `Profiles.get`; the final source replaces it with a static identifier. That optimization was built and regression-tested, but the preceding capture must not be presented as a post-change allocation measurement. Raw client evidence is in `build/client-render-profile.jfr`, `build/client-render-samples.json` and `build/client-render-summary.json`.



## Cost controls



Fixtures have no block-entity ticker and never force-load chunks. Eligible enchanted fixtures schedule a lightweight repair check every configured number of ticks (20 by default); elapsed game ticks determine actual repair. Fraction-only accounting saves its state without sending an inventory update. Idle timing persistence does not send inventory packets. Visible inventory/enchantment/repair changes cause block-entity updates, while datapack definitions synchronize on login/reload.



Immutable display transforms and resolved item rules are cached until state or definitions change. Rendering does not copy ItemStacks or resolve item tags each frame. Static furniture is baked. Native frustum culling and configurable distance limit dynamic equipment rendering. Normal Minecraft pose/buffer work still allocates; the implementation does not claim zero-allocation rendering or cache live item models in ways that could suppress glint, animations, trims or durability overrides.
