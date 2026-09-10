package com.cappleapple.racksnstands.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Immutable, registry-backed texture samples. Keys match the original model materials. */
public record MaterialPalette(Map<String,BlockState> materials) {
    public static final List<String> GROUPS=List.of("wood","dark","metal","cloth","stone","trim","base");
    public static final MaterialPalette EMPTY=new MaterialPalette(Map.of());
    private static final Codec<String> GROUP=Codec.STRING.validate(key -> GROUPS.contains(key)?DataResult.success(key):DataResult.error(() -> "Unknown fixture material: "+key));
    public static final Codec<MaterialPalette> CODEC=Codec.unboundedMap(GROUP,BlockState.CODEC).xmap(MaterialPalette::new,MaterialPalette::materials);
    public MaterialPalette { materials=Map.copyOf(materials); }
    public MaterialPalette with(String group,BlockState sample) {
        if(!GROUPS.contains(group)) throw new IllegalArgumentException("Unknown fixture material: "+group);
        var copy=new HashMap<>(materials);copy.put(group,sample);return new MaterialPalette(copy);
    }
}
