package com.cappleapple.racksnstands.gametest;
import com.cappleapple.racksnstands.RacksNStands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in development capture utility; excluded from distributed JARs. */
@EventBusSubscriber(modid=RacksNStands.MOD_ID,value=Dist.CLIENT)
public final class VisualQa {
    private static int ticks;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("racksnstands.visualQa")) return;
        var client=Minecraft.getInstance();if(client.player==null||client.level==null) return;
        MaterialVisualQa.tick();
        if(ticks==0) { client.options.hideGui=false;client.options.pauseOnLostFocus=false; }
        if(++ticks%200==0) {
            if(ticks==200) for(int x:new int[]{74,76,78}) {
                if(client.level.getBlockEntity(new net.minecraft.core.BlockPos(x,65,54)) instanceof com.cappleapple.racksnstands.blockentity.FixtureBlockEntity f)
                    RacksNStands.LOGGER.info("QA transform {}: {} {}",f.profileId(),f.displayedStack(0),f.resolvedTransform(0).rotation());
            }
            Screenshot.grab(client.gameDirectory,"racksnstands-qa-"+(ticks/200)+".png",client.getMainRenderTarget(),message -> RacksNStands.LOGGER.info("QA capture: {}",message.getString()));
        }
        // Leave the opt-in development client open for interactive review.
    }
}
