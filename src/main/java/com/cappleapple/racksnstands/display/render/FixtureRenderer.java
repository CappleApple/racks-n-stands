package com.cappleapple.racksnstands.display.render;
import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.block.FixtureBlock;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.config.FixtureConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** One renderer for all fixtures. Only immutable transform metadata is cached. */
public final class FixtureRenderer implements BlockEntityRenderer<FixtureBlockEntity> {
    private final net.minecraft.client.renderer.entity.ItemRenderer items;
    private final WornArmorRenderer armor;
    private final PreviewLayout previews=new PreviewLayout();
    private MultiBufferSource previousBuffers,maskedBuffers;
    private static final org.joml.Quaternionf[] FACING={Axis.YP.rotationDegrees(180),Axis.YP.rotationDegrees(90),Axis.YP.rotationDegrees(0),Axis.YP.rotationDegrees(-90)};
    public FixtureRenderer(BlockEntityRendererProvider.Context context) { items=context.getItemRenderer();armor=new WornArmorRenderer(context); }
    @Override public void render(FixtureBlockEntity fixture,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(FACING[fixture.getBlockState().getValue(FixtureBlock.FACING).get2DDataValue()]);pose.translate(-.5,0,-.5);
        if(fixture.getBlockState().hasProperty(com.cappleapple.racksnstands.block.SurfaceDisplayBlock.FACE)) {
            switch(fixture.getBlockState().getValue(com.cappleapple.racksnstands.block.SurfaceDisplayBlock.FACE)) {
                case WALL -> { pose.translate(0,0,1);pose.mulPose(Axis.XP.rotationDegrees(-90)); }
                case CEILING -> { pose.translate(0,1,1);pose.mulPose(Axis.XP.rotationDegrees(180)); }
                default -> { }
            }
        }
        if(previousBuffers!=buffers) { previousBuffers=buffers;maskedBuffers=MaskedGlint.wrap(buffers); }
        var spriteBuffers=maskedBuffers;
        var slots=fixture.profile().slots();
        for(int n=0;n<slots.size();n++) {
            var slot=slots.get(n);
            var stack=fixture.displayedStack(slot.index());if(stack.isEmpty()) continue;
            var t=fixture.resolvedTransform(slot.index());pose.pushPose();pose.translate(t.x(),t.y(),t.z());
            boolean armorRendered=false;
            if(slot.wornArmor()&&slot.equipment().isPresent()) {
                pose.pushPose();pose.mulPose(t.quaternion());pose.scale(t.sx(),t.sy(),t.sz());
                armorRendered=armor.render(stack,slot.equipment().get(),pose,buffers,light,partial,((FixtureBlock)fixture.getBlockState().getBlock()).kind().tall());
                pose.popPose();
            }
            if(!armorRendered) {
                int seed=fixture.getBlockPos().hashCode()+slot.index();
                var model=items.getModel(stack,fixture.getLevel(),null,seed);
                if(t.context==net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) model=HandDisplayModel.select(model,stack,t.context);
                boolean atlasModel=!model.isCustomRenderer()||stack.is(net.minecraft.world.item.Items.TRIDENT)&&t.context==net.minecraft.world.item.ItemDisplayContext.FIXED;
                boolean projectedGlint=stack.is(net.minecraft.tags.ItemTags.COMPASSES)||stack.is(net.minecraft.world.item.Items.CLOCK);
                // Measure and fit after the item's full orientation, but apply the fit in fixture
                // coordinates so rotated weapons keep their depth and shelf contact point.
                if(fixture.fitsCompartment()) previews.apply(fixture,slot.index(),stack,model,items,pose,fixture.profileId().equals(RacksNStands.id("curio_cabinet")));
                PreviewLayout.orient(pose,t);
                items.render(stack,t.context,false,pose,atlasModel&&!projectedGlint?spriteBuffers:buffers,light,overlay,model);
            }
            pose.popPose();
        }
        pose.popPose();
    }
    @Override public int getViewDistance() { return FixtureConfig.RENDER_DISTANCE.get(); }
    @Override public AABB getRenderBoundingBox(FixtureBlockEntity fixture) { return fixture.renderBounds(); }
    @EventBusSubscriber(modid=RacksNStands.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e) { e.registerBlockEntityRenderer(RacksNStands.FIXTURE_ENTITY.get(),FixtureRenderer::new); }
    }
}
