package com.cappleapple.racksnstands.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.AABB;

public record DisplaySlotDefinition(int index, DisplayFilter filter, Optional<EquipmentSlot> equipment,
        DisplayTransform transform, AABB bounds, boolean wornArmor) {
    private static final Codec<AABB> BOX = Codec.doubleRange(-2, 3).listOf().comapFlatMap(v -> {
        if (v.size() != 6 || v.get(0) >= v.get(3) || v.get(1) >= v.get(4) || v.get(2) >= v.get(5))
            return com.mojang.serialization.DataResult.error(() -> "Bounds require [minX,minY,minZ,maxX,maxY,maxZ] with positive extents");
        return com.mojang.serialization.DataResult.success(new AABB(v.get(0),v.get(1),v.get(2),v.get(3),v.get(4),v.get(5)));
    }, b -> List.of(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ));
    public static final Codec<DisplaySlotDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.intRange(0, 15).fieldOf("index").forGetter(DisplaySlotDefinition::index),
        DisplayFilter.CODEC.fieldOf("filter").forGetter(DisplaySlotDefinition::filter),
        EquipmentSlot.CODEC.optionalFieldOf("equipment_slot").forGetter(DisplaySlotDefinition::equipment),
        DisplayTransform.CODEC.fieldOf("transform").forGetter(DisplaySlotDefinition::transform),
        BOX.fieldOf("interaction_bounds").forGetter(DisplaySlotDefinition::bounds),
        Codec.BOOL.optionalFieldOf("worn_armor", false).forGetter(DisplaySlotDefinition::wornArmor)
    ).apply(i, DisplaySlotDefinition::new));
}
