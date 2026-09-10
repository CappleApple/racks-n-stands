package com.cappleapple.racksnstands.display.render;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.config.FixtureConfig;
import com.cappleapple.racksnstands.display.RepairTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid=RacksNStands.MOD_ID,value=Dist.CLIENT)
public final class RepairOverlay {
    private static final boolean JADE=ModList.get().isLoaded("jade");
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        var client=Minecraft.getInstance();
        if(JADE||!FixtureConfig.REPAIR_TOOLTIP.get()||client.screen!=null||client.options.hideGui||client.player==null||!(client.hitResult instanceof BlockHitResult hit)) return;
        var target=RepairTarget.find(client.level,hit);if(target==null||!target.hasDurability()) return;
        var progress=target.progress();var graphics=event.getGuiGraphics();var font=client.font;
        var title=target.stack().getHoverName();var detail=target.durability();
        var time=RepairTarget.remaining(progress);
        int width=Math.min(graphics.guiWidth()-16,Math.max(190,Math.max(font.width(title)+36,Math.max(font.width(detail),font.width(time))+16)));
        int x=(graphics.guiWidth()-width)/2,y=8;
        graphics.fill(x-1,y-1,x+width+1,y+55,0xff596676);graphics.fill(x,y,x+width,y+54,0xeb121822);
        graphics.renderItem(target.stack(),x+7,y+5);
        graphics.drawString(font,net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(title,width-36)),x+29,y+8,0xffffffff,false);
        RepairBar.draw(graphics,x+8,y+24,width-16,13,(float)progress.completion(),detail,1,0x806fc5ff,0xffeff8ff);
        graphics.drawString(font,time,x+8,y+43,0xffd7dfe7,false);
    }
}
