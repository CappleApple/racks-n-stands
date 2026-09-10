package com.cappleapple.racksnstands.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;
import java.util.List;

/** Immutable, validated transform. Rotation is precomputed once per profile load. */
public final class DisplayTransform {
    private static final Codec<List<Double>> VECTOR = Codec.doubleRange(-16, 16).listOf().validate(v -> v.size() == 3
        ? com.mojang.serialization.DataResult.success(v) : com.mojang.serialization.DataResult.error(() -> "Expected exactly three finite coordinates"));
    private static final Codec<List<Double>> ROTATION = Codec.doubleRange(-360, 360).listOf().validate(v -> v.size() == 3
        ? com.mojang.serialization.DataResult.success(v) : com.mojang.serialization.DataResult.error(() -> "Expected three Euler angles"));
    public static final Codec<DisplayTransform> CODEC = RecordCodecBuilder.create(i -> i.group(
        VECTOR.optionalFieldOf("translation", List.of(0.5, 0.5, 0.5)).forGetter(t -> t.translation),
        ROTATION.optionalFieldOf("rotation", List.of(0.0, 0.0, 0.0)).forGetter(t -> t.rotation),
        VECTOR.optionalFieldOf("scale", List.of(0.5, 0.5, 0.5)).forGetter(t -> t.scale),
        ItemDisplayContext.CODEC.optionalFieldOf("display_context", ItemDisplayContext.FIXED).forGetter(t -> t.context),
        VECTOR.optionalFieldOf("model_translation",List.of(0.0,0.0,0.0)).forGetter(t -> t.modelTranslation)
    ).apply(i, DisplayTransform::new));
    private final List<Double> translation, rotation, scale,modelTranslation;
    private final Quaternionf quaternion;
    public final ItemDisplayContext context;
    public DisplayTransform(List<Double> translation, List<Double> rotation, List<Double> scale, ItemDisplayContext context) {
        this(translation,rotation,scale,context,List.of(0.0,0.0,0.0));
    }
    public DisplayTransform(List<Double> translation,List<Double> rotation,List<Double> scale,ItemDisplayContext context,List<Double> modelTranslation) {
        this.modelTranslation=List.copyOf(modelTranslation);
        if (scale.stream().anyMatch(v -> !Double.isFinite(v) || v <= 0 || v > 4)) throw new IllegalArgumentException("Scale must be finite and in (0,4]");
        this.translation = List.copyOf(translation); this.rotation = List.copyOf(rotation); this.scale = List.copyOf(scale); this.context = context;
        quaternion = new Quaternionf().rotationXYZ((float)Math.toRadians(rotation.get(0)), (float)Math.toRadians(rotation.get(1)), (float)Math.toRadians(rotation.get(2)));
    }
    public List<Double> modelTranslation() { return modelTranslation; }
    public List<Double> rotation() { return rotation; }
    public double x() { return translation.get(0); } public double y() { return translation.get(1); } public double z() { return translation.get(2); }
    public float sx() { return scale.get(0).floatValue(); } public float sy() { return scale.get(1).floatValue(); } public float sz() { return scale.get(2).floatValue(); }
    public org.joml.Quaternionf quaternion() { return quaternion; }
}
