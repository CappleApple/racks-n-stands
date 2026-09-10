package com.cappleapple.racksnstands.display.render;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;

/** Native worn armor hooks rendered without constructing display entities. */
final class WornArmorRenderer {
    private final HumanoidModel<LivingEntity> inner,outer,body;
    private final net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<LivingEntity,HumanoidModel<LivingEntity>,HumanoidModel<LivingEntity>> layer;
    private final java.util.List<HumanoidModel<LivingEntity>> models;
    private final ElytraModel<LivingEntity> elytra;
    private final net.minecraft.client.model.geom.ModelPart wings;
    private static final net.minecraft.resources.ResourceLocation ELYTRA_TEXTURE=net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/entity/elytra.png");
    WornArmorRenderer(BlockEntityRendererProvider.Context context) {
        wings=context.bakeLayer(ModelLayers.ELYTRA);elytra=new ElytraModel<>(wings);
        inner=new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        outer=new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        body=new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER));
        models=java.util.List.of(body,inner,outer);
        layer=new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(new net.minecraft.client.renderer.entity.RenderLayerParent<LivingEntity,HumanoidModel<LivingEntity>>() {
            public HumanoidModel<LivingEntity> getModel() { return body; }
            public net.minecraft.resources.ResourceLocation getTextureLocation(LivingEntity entity) { return ELYTRA_TEXTURE; }
        },inner,outer,Minecraft.getInstance().getModelManager());
    }
    boolean render(ItemStack stack,EquipmentSlot slot,PoseStack pose,MultiBufferSource buffers,int light,float partial,boolean fullStand) {
        if(stack.is(Items.ELYTRA)&&slot==EquipmentSlot.CHEST) {
            // Resting wing pose belongs to the stand, independent of the viewing player's animation/skin.
            wings.getAllParts().forEach(net.minecraft.client.model.geom.ModelPart::resetPose);
            elytra.young=false;elytra.riding=false;elytra.attackTime=0;
            pose.pushPose();pose.scale(1,-1,-1);
            if(!fullStand) pose.scale(.68F,.68F,.68F);
            pose.translate(0,0,.125);
            elytra.renderToBuffer(pose,ItemRenderer.getArmorFoilBuffer(buffers,RenderType.armorCutoutNoCull(ELYTRA_TEXTURE),stack.hasFoil()),light,OverlayTexture.NO_OVERLAY);
            pose.popPose();return true;
        }
        if(!(stack.getItem() instanceof ArmorItem)||Minecraft.getInstance().player==null) return false;
        var viewer=Minecraft.getInstance().player;
        // Use the native armor layer so mixins and GeoRenderProvider can own their models and textures.
        // Its equipment reads are projected for this call; the viewing player's inventory remains untouched.
        for(var base:models) {
            base.head.resetPose();base.hat.resetPose();base.body.resetPose();base.leftArm.resetPose();base.rightArm.resetPose();base.leftLeg.resetPose();base.rightLeg.resetPose();
            base.young=false;base.crouching=false;base.riding=false;base.attackTime=0;base.setAllVisible(true);
            base.rightArm.x=-7.1F;base.leftArm.x=7.1F;base.rightLeg.x=-3.1F;base.leftLeg.x=3.1F;
            base.rightLeg.xRot=.025F;base.leftLeg.xRot=.025F;
        }
        pose.pushPose();pose.scale(1,-1,-1);
        try(var equipment=new ArmorRenderContext(viewer,slot,stack)) {
            layer.render(pose,buffers,light,viewer,0,0,partial,viewer.tickCount+partial,0,0);
        } finally { pose.popPose(); }
        return true;
    }
}
