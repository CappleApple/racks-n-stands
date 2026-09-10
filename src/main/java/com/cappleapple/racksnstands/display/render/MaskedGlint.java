package com.cappleapple.racksnstands.display.render;

import com.cappleapple.racksnstands.RacksNStands;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import java.io.IOException;

/** Keeps a sprite's glint out of its transparent pixels, even over coplanar items. */
@EventBusSubscriber(modid=RacksNStands.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class MaskedGlint extends RenderStateShard {
    private static ShaderInstance shader;
    private static final RenderType SOLID=create(false), TRANSLUCENT=create(true);
    private MaskedGlint() { super("racksnstands_masked_glint",() -> {},() -> {}); }

    private static RenderType create(boolean translucent) {
        var state=RenderType.CompositeState.builder()
            .setShaderState(new ShaderStateShard(() -> shader))
            .setTextureState(MultiTextureStateShard.builder()
                .add(ItemRenderer.ENCHANTED_GLINT_ITEM,true,false)
                .add(TextureAtlas.LOCATION_BLOCKS,false,false).build())
            .setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL)
            .setDepthTestState(EQUAL_DEPTH_TEST).setTransparencyState(GLINT_TRANSPARENCY)
            .setTexturingState(GLINT_TEXTURING);
        if(translucent) state.setOutputState(ITEM_ENTITY_TARGET);
        return RenderType.create("racksnstands_masked_glint"+(translucent?"_translucent":""),
            DefaultVertexFormat.POSITION_TEX,VertexFormat.Mode.QUADS,1536,state.createCompositeState(false));
    }
    @SubscribeEvent public static void shaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),RacksNStands.id("masked_glint"),DefaultVertexFormat.POSITION_TEX),loaded -> shader=loaded);
    }
    @SubscribeEvent public static void buffers(RegisterRenderBuffersEvent event) {
        // Fixed buffers keep glint alive while ItemRenderer fills its base buffer,
        // and draw it after the base surfaces using Minecraft's normal batching.
        event.registerRenderBuffer(SOLID);event.registerRenderBuffer(TRANSLUCENT);
    }
    static MultiBufferSource wrap(MultiBufferSource source) {
        return type -> source.getBuffer(type==RenderType.glint()?SOLID:type==RenderType.glintTranslucent()?TRANSLUCENT:type);
    }
}
