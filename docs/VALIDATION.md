# Testing and performance notes

Racks N' Stands touches block models, item rendering, equipment exchange, Curios, passive repair, crafting, automation, and optional compatibility mods. The test setup is split between unit/GameTests and a smaller set of client/server checks where rendering or another mod is involved.

Raw screenshots and retained logs are useful for regressions, but they are not intended as proof that every third-party model/resource pack has been exhaustively tested.

## Automated tests

For normal changes, run:

```powershell
.\gradlew.bat test build
.\gradlew.bat runGameTestServer
```

Current automated coverage includes:

- fixture storage/filter behavior;
- armor insertion/replacement and whole-set swapping;
- item-count conservation during exchanges/crafting;
- material sampling and copied/reset appearance components;
- tagged recipe ingredients, recipe-book unlocks, and recipe overlap;
- block/item model geometry and fitting bounds;
- item orientation/profile rules;
- Repairing arithmetic, fractional carry, catch-up, ETA, and save/load behavior;
- scheduled repair ticks and config changes;
- Curios slot/equipment restrictions when the optional runtime is present; and
- resource/datagen validation.

The release jar should not contain GameTest/visual-QA fixtures or bundled third-party implementations.

## Optional-mod test runs

Use the matching runtime when changing one of these integrations rather than relying only on the standalone tests:

- Curios
- Jade
- Rituals Not Rolls
- Simply Swords / Simply More
- Cataclysm
- Too Many Bows
- Iron's Spellbooks
- Immersive Armors

The Gradle project includes focused runs for Curios/Jade and can be pointed at a local Rituals Not Rolls mods directory.

## Sable / Create Aeronautics

The standalone suite checks inventory clearing, saved item components, full-stack racks, both removal orders of the Full Armor Stand, and normal item drops after a transfer. The optional suite uses real Sable sub-level assembly and its reverse block-transfer path.

Set `sableModsDir` to a local mods directory containing exactly one `sable-neoforge-*.jar`:

```powershell
.\gradlew.bat runGameTestServer "-PsableModsDir=<mods-directory>"
.\gradlew.bat runGameTestServer "-PsableModsDir=<mods-directory>" -PwithAeronautics
```

Replace `<mods-directory>` with the installed mods directory. The second command also loads `create-1.21.1-*.jar` and `create-aeronautics-bundled-*.jar` from it. Their bundled dependencies are loaded normally. These runs use `run-sable-gametest/`, separate from the standalone test world. Sable test sources and extracted compile libraries are enabled only for this test setup; they are excluded from the release JAR.

The 1.4.2 fix passed 33 unit tests, 67 standalone server GameTests, and 72 server GameTests with Sable 2.0.5. The 72-test suite also passed with Create 6.0.10 and Create Aeronautics 1.3.2 loaded on NeoForge 21.1.248. The five Sable cases cover rack and mannequin transfers in both directions, both mannequin removal orders, item components, appearance, repair progress, and ordinary breaking aboard ships. Ship creation, transfer, and cleanup are separated by server ticks so Sable can finish its plot updates. These checks exercise server behavior; client ship controls and visual rendering were not manually tested for this fix.

## Client checks

Rendering is intentionally checked in a real development client because baked item/armor models can behave differently from pure geometry tests.

Before a release that touches rendering, inspect a representative set of:

- vanilla swords, axes/hammers, bows, trident, shield, armor, and elytra;
- one small and one oversized modded weapon;
- a custom 3D weapon/model;
- a spellbook/staff or similarly unusual model;
- rack, shelf, pedestal, wall, and surface displays; and
- an armor mannequin with dyed/trimmed or modded armor.

Useful retained screenshots live under `docs/screenshots/`.

The goal is to catch clipping, wrong hand-model context, bad axis/orientation, transparent-padding problems, shelf contact, and custom armor-layer issues. A few representative screenshots are more useful than a large numeric “models checked” count with no indication of what was visually inspected.

## Repairing checks

When changing Repairing behavior, test both loaded and unloaded fixtures.

Important cases:

- low-durability items where fractional progress matters;
- different Repairing levels;
- full repair without banking excess progress;
- swapping/removing an item after partial progress;
- saving/reloading the world;
- unloading/reloading the chunk;
- server shutdown (offline time should not count for game-time catch-up); and
- Jade versus the built-in repair HUD.

The default formula should remain independent of the scheduled update cadence.

## Material customization

Material sampling should be checked with more than opaque cubes. Include at least:

- logs or another directional texture;
- tinted blocks such as grass;
- translucent blocks such as glass; and
- furniture with more than one material group.

After sampling, break/re-place the fixture and check its item/held rendering as well as the world model.

Copy/reset crafting should preserve unrelated components and item counts.

## Crafting

Recipe changes should be checked against the loaded item tags, including optional mods that add ingredients.

- Substitute every tag member at each ingredient position.
- Mix valid materials within a recipe, such as different wood types.
- Check normal, mirrored, and offset layouts.
- Compare ingredient intersections between recipes to catch overlapping layouts that only appear with particular tag combinations.
- Trigger recipe-book unlocks with alternative materials, including non-oak planks and stone-brick variants.

The accepted tags and remaining exact ingredients are listed in [recipe ingredients](PACKMAKERS.md#recipe-ingredients).

## Rituals Not Rolls

The built-in Repairing definition should be checked against the installed Rituals Not Rolls implementation when either side's data format or solver behavior changes.

With the shipped defaults, the full material set should reach Repairing V without requiring Consumption or Experience Catalysts. Removing one required contribution should leave the baseline below that level.

This is integration coverage, not a runtime dependency; Racks N' Stands still works without Rituals Not Rolls installed.

## Dedicated server

A normal server smoke run should reach `Done`, accept a client, save, and shut down without requiring client-only render classes.

This is also a good place to verify common-config live reload and server-to-client repair settings.

## Performance scenes

The debug scene commands can generate repeatable layouts of 100, 500, or 1000 fixtures for profiling.

For useful comparisons:

1. Use a fresh empty area for each size.
2. Keep camera position, graphics settings, and fixture mix consistent.
3. Allow chunk meshes and the JVM to warm up first.
4. Profile the actual client if the question is rendering cost; server tick timing is not a substitute for client frame/render cost.
5. Use `/jfr start` / `/jfr stop` for server work and a client JVM capture for render-thread CPU/allocation sampling.

Historical measurements in earlier revisions were smoke/profile snapshots under different conditions and should not be treated as a stable benchmark. Re-measure after meaningful renderer or ticker changes instead of comparing against those numbers blindly.

## Manual compatibility limits

The generic renderer/profile system cannot guarantee perfect presentation for every third-party animated model, custom item renderer, armor replacement layer, connected-texture block, shader, or resource pack.

When a specific mod needs a correction, add a focused profile/test case and document that combination in [COMPATIBILITY.md](COMPATIBILITY.md) rather than presenting representative coverage as universal compatibility.
