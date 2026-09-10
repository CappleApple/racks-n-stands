package com.cappleapple.racksnstands.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** A pack-defined orientation/size adjustment, resolved once per visible stack update. */
public record ItemDisplayRule(int priority,List<ResourceLocation> items,List<ResourceLocation> tags,
        List<ResourceLocation> fixtures,Optional<List<Double>> rotation,double scale,List<Double> offset,boolean relativeRotation,Optional<net.minecraft.world.item.ItemDisplayContext> context,Optional<List<Double>> modelTranslation) {
    private static final Codec<List<Double>> ANGLES=Codec.doubleRange(-360,360).listOf().validate(v -> v.size()==3
        ? com.mojang.serialization.DataResult.success(v):com.mojang.serialization.DataResult.error(() -> "Rotation needs three Euler angles"));
    private static final Codec<List<Double>> OFFSET=Codec.doubleRange(-2,2).listOf().validate(v -> v.size()==3
        ? com.mojang.serialization.DataResult.success(v):com.mojang.serialization.DataResult.error(() -> "Offset needs three coordinates"));
    public static final Codec<ItemDisplayRule> CODEC=RecordCodecBuilder.create(i -> i.group(
        Codec.intRange(-10000,10000).optionalFieldOf("priority",0).forGetter(ItemDisplayRule::priority),
        ResourceLocation.CODEC.listOf().optionalFieldOf("items",List.of()).forGetter(ItemDisplayRule::items),
        ResourceLocation.CODEC.listOf().optionalFieldOf("item_tags",List.of()).forGetter(ItemDisplayRule::tags),
        ResourceLocation.CODEC.listOf().optionalFieldOf("fixtures",List.of()).forGetter(ItemDisplayRule::fixtures),
        ANGLES.optionalFieldOf("rotation").forGetter(ItemDisplayRule::rotation),
        Codec.doubleRange(.01,4).optionalFieldOf("scale",1.0).forGetter(ItemDisplayRule::scale),
        OFFSET.optionalFieldOf("offset",List.of(0.0,0.0,0.0)).forGetter(ItemDisplayRule::offset),
        Codec.BOOL.optionalFieldOf("relative_rotation",false).forGetter(ItemDisplayRule::relativeRotation),
        net.minecraft.world.item.ItemDisplayContext.CODEC.optionalFieldOf("display_context").forGetter(ItemDisplayRule::context),
        Codec.doubleRange(-16,16).listOf().validate(v -> v.size()==3?com.mojang.serialization.DataResult.success(v):com.mojang.serialization.DataResult.error(() -> "Model translation needs three coordinates")).optionalFieldOf("model_translation").forGetter(ItemDisplayRule::modelTranslation)
    ).apply(i,ItemDisplayRule::new));
    public ItemDisplayRule {
        items=List.copyOf(items);tags=List.copyOf(tags);fixtures=List.copyOf(fixtures);rotation=rotation.map(List::copyOf);offset=List.copyOf(offset);modelTranslation=modelTranslation.map(List::copyOf);
        if(items.isEmpty()&&tags.isEmpty()) throw new IllegalArgumentException("An item display rule needs items or item_tags");
    }
    public boolean matches(ItemStack stack,ResourceLocation fixture) {
        if(stack.isEmpty()||!fixtures.isEmpty()&&!fixtures.contains(fixture)) return false;
        if(items.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return true;
        for(var tag:tags) if(stack.is(TagKey.create(Registries.ITEM,tag))) return true;
        return false;
    }
    public DisplayTransform apply(DisplayTransform base) {
        var angles=rotation.orElse(base.rotation());
        if(relativeRotation&&rotation.isPresent()) angles=List.of(base.rotation().get(0)+angles.get(0),base.rotation().get(1)+angles.get(1),base.rotation().get(2)+angles.get(2));
        return new DisplayTransform(List.of(base.x()+offset.get(0),base.y()+offset.get(1),base.z()+offset.get(2)),angles,
            List.of(Math.min(4,base.sx()*scale),Math.min(4,base.sy()*scale),Math.min(4,base.sz()*scale)),context.orElse(base.context),modelTranslation.orElse(base.modelTranslation()));
    }
}
