package com.cappleapple.racksnstands.display.render;

import com.cappleapple.racksnstands.RacksNStands;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import java.util.IdentityHashMap;

/** Select hand-specific geometry without importing the player's grip position into furniture. */
@EventBusSubscriber(modid=RacksNStands.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class HandDisplayModel extends BakedModelWrapper<BakedModel> {
    private static final IdentityHashMap<BakedModel,HandDisplayModel> MODELS=new IdentityHashMap<>();
    private static final PoseStack SELECTION_POSE=new PoseStack();
    private final double cx,cy,cz;
    private final boolean trident;
    private HandDisplayModel(BakedModel model,ItemStack stack) {
        super(model);trident=stack.is(Items.TRIDENT);
        double[] low={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
        double[] high={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        if(!model.isCustomRenderer()) {
            var random=RandomSource.create(42);
            for(var pass:model.getRenderPasses(stack,true)) for(int side=0;side<7;side++) {
                random.setSeed(42);
                for(var quad:pass.getQuads(null,side==6?null:Direction.values()[side],random)) {
                    int[] vertices=quad.getVertices();int stride=vertices.length/4;
                    for(int v=0;v<4;v++) for(int axis=0;axis<3;axis++) {
                        float value=Float.intBitsToFloat(vertices[v*stride+axis]);
                        low[axis]=Math.min(low[axis],value);high[axis]=Math.max(high[axis],value);
                    }
                }
            }
        }
        cx=Double.isFinite(low[0])?(low[0]+high[0])*.5:.5;
        cy=Double.isFinite(low[1])?(low[1]+high[1])*.5:.5;
        cz=Double.isFinite(low[2])?(low[2]+high[2])*.5:.5;
    }
    public static BakedModel select(BakedModel model,ItemStack stack,ItemDisplayContext context) {
        SELECTION_POSE.pushPose();
        try { model=model.applyTransform(context,SELECTION_POSE,false); }
        finally { SELECTION_POSE.popPose(); }
        var cached=MODELS.get(model);
        if(cached==null) { cached=new HandDisplayModel(model,stack);MODELS.put(model,cached); }
        return cached;
    }
    @Override public BakedModel applyTransform(ItemDisplayContext context,PoseStack pose,boolean left) {
        if(trident) {
            // Native BEWLR geometry is centered on x/z=0, y=-11.5/16 after its Y flip.
            pose.translate(.5,1.21875,.5);
        } else if(originalModel.isCustomRenderer()) {
            return originalModel.applyTransform(context,pose,left);
        } else {
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
            var scale=originalModel.getTransforms().getTransform(context).scale;
            pose.scale(scale.x(),scale.y(),scale.z());
            pose.translate(.5-cx,.5-cy,.5-cz);
        }
        return this;
    }
    @SubscribeEvent public static void modelsReloaded(ModelEvent.BakingCompleted event) { MODELS.clear(); }
}
