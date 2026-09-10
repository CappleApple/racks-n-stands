package com.cappleapple.racksnstands.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public record DisplayProfile(List<DisplaySlotDefinition> slots, ResourceLocation behavior) {
    public static final Codec<DisplayProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
        DisplaySlotDefinition.CODEC.listOf().fieldOf("slots").forGetter(DisplayProfile::slots),
        ResourceLocation.CODEC.optionalFieldOf("behavior", ResourceLocation.parse("racksnstands:normal")).forGetter(DisplayProfile::behavior)
    ).apply(i, DisplayProfile::new));
    public DisplayProfile {
        slots = slots.stream().sorted(Comparator.comparingInt(DisplaySlotDefinition::index)).toList();
        if (slots.isEmpty() || slots.size() > 16) throw new IllegalArgumentException("Profile needs 1..16 slots");
        for (int n = 0; n < slots.size(); n++) if (slots.get(n).index() != n) throw new IllegalArgumentException("Slot indices must be unique and contiguous from zero");
        if (behavior.equals(ResourceLocation.parse("racksnstands:armor_loadout"))) {
            var seen = new HashSet<net.minecraft.world.entity.EquipmentSlot>();
            for (var slot : slots) slot.equipment().ifPresent(seen::add);
            if (slots.size() != 4 || !seen.containsAll(List.of(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET)))
                throw new IllegalArgumentException("Armor loadout requires four distinct vanilla armor bindings");
        }
    }
}
