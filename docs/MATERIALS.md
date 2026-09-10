# Custom furniture materials

Hold any block item and sneak + right-click the furniture surface you want to change. All pieces on that fixture sharing the clicked material change together: either metal support recolors both, for example. The pedestal has separate base, body, and cap materials. Sampling copies appearance without consuming the held block or exchanging any stored/equipped item. Repeating the same sample leaves the appearance unchanged.

The same interaction works on armor stands, racks, shelves, legacy fixtures, and Displays mounted on floors, walls, or ceilings. The Full Armor Stand shares its wooden material across the head, torso, arms, and legs, including the upper block. Normal held-item exchanges and sneak equipment swaps still work when the held item is not a block.

Materials persist through chunk unloads, world saves, survival breaking, and re-placement. Inventory, hand, and dropped models display the saved appearance. Only the texture changes; collision, storage, durability maintenance, block behavior, and light emission remain those of the fixture. No block-entity inventory or other private data is copied from a sample.

Textures use a consistent pixel scale with continuous coordinates across exposed fragments and joints. Long parts tile at block boundaries. Source face textures, their rotation/mirroring, biome colors, animation, and translucent/cutout render layers are retained. Logs therefore show bark and end grain on their corresponding faces. Blocks with no usable baked face, such as entity-rendered blocks, fall back to their baked particle texture. Connected textures and dynamic appearance dependent on a source block entity are not copied.

## Saved data

The `racksnstands:materials` item component contains a map from original material group to a registered block state. The block entity synchronizes the same map as `Materials`. Ordinary commands can supply block-state properties, which are also respected on held block items carrying `minecraft:block_state`.

| Group | Used for |
|---|---|
| `wood` | Wood framework, backing, armor forms |
| `dark` | Dark wood bases, rims and shelf edges |
| `metal` | Metal pegs and supports |
| `cloth` | Cushions and covered armor forms |
| `stone` | Pedestal body and middle tiers |
| `trim` | Pedestal top cap |
| `base` | Pedestal bottom plinth |

Only groups present in the clicked fixture are editable. Group names remain stable across block facing and mounting changes. Default models use face `tintindex` values in the group order above as material metadata; the custom model clears these indices before rendering. Resource packs replacing fixture geometry should preserve those indices for editable parts.

Example item:

```mcfunction
/give @s racksnstands:sword_floor_stand[racksnstands:materials={dark:{Name:"minecraft:bricks"},metal:{Name:"minecraft:gold_block"}}]
```

Operators can restore an original material with `/data remove block <x> <y> <z> Materials.<group>`, or remove `Materials` to restore all defaults. Use the lower block position for the Full Armor Stand.
