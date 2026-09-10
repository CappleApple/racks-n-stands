package com.cappleapple.racksnstands.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.critereon.ItemPredicate;

/** Denials win, equipment binding is mandatory, positive selectors are alternatives. */
public record DisplayFilter(List<ResourceLocation> categories, List<ResourceLocation> acceptedTags,
        List<ResourceLocation> deniedTags, List<ResourceLocation> allowedItems, List<ResourceLocation> deniedItems,
        List<String> curioSlots, boolean anyCurio, Optional<ItemPredicate> predicate) {
    public static final Codec<DisplayFilter> CODEC = RecordCodecBuilder.create(i -> i.group(
        ResourceLocation.CODEC.listOf().optionalFieldOf("accepted_categories", List.of()).forGetter(DisplayFilter::categories),
        ResourceLocation.CODEC.listOf().optionalFieldOf("accepted_item_tags", List.of()).forGetter(DisplayFilter::acceptedTags),
        ResourceLocation.CODEC.listOf().optionalFieldOf("denied_item_tags", List.of()).forGetter(DisplayFilter::deniedTags),
        ResourceLocation.CODEC.listOf().optionalFieldOf("allowed_items", List.of()).forGetter(DisplayFilter::allowedItems),
        ResourceLocation.CODEC.listOf().optionalFieldOf("denied_items", List.of()).forGetter(DisplayFilter::deniedItems),
        Codec.STRING.listOf().optionalFieldOf("accepted_curio_slots", List.of()).forGetter(DisplayFilter::curioSlots),
        Codec.BOOL.optionalFieldOf("any_curio", false).forGetter(DisplayFilter::anyCurio),
        ItemPredicate.CODEC.optionalFieldOf("predicate").forGetter(DisplayFilter::predicate)
    ).apply(i, DisplayFilter::new));
    public DisplayFilter {
        categories = List.copyOf(categories); acceptedTags = List.copyOf(acceptedTags); deniedTags = List.copyOf(deniedTags);
        allowedItems = List.copyOf(allowedItems); deniedItems = List.copyOf(deniedItems); curioSlots = List.copyOf(curioSlots);
        if (curioSlots.stream().anyMatch(s -> s.isBlank() || s.length() > 128)) throw new IllegalArgumentException("Invalid Curios slot identifier");
    }
}
