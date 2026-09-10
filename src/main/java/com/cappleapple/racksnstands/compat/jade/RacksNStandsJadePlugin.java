package com.cappleapple.racksnstands.compat.jade;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.block.FixtureBlock;
import com.cappleapple.racksnstands.config.FixtureConfig;
import com.cappleapple.racksnstands.display.RepairTarget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.*;
import java.util.List;

/** Jade is compile-only. Its plugin loader discovers this class only when Jade is installed. */
@WailaPlugin
public final class RacksNStandsJadePlugin implements IWailaPlugin {
    public static final ResourceLocation UID=RacksNStands.id("repair_progress");
    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new Provider(),FixtureBlock.class);
        // Progress already arrives in this mod's authoritative block-entity snapshots.
        registration.markAsClientFeature(UID);
    }
    /** Use one unshadowed text pass over a translucent bar instead of Jade's opaque default. */
    private static final class RepairBarStyle extends ProgressStyle {
        private int fill=0x806fc5ff,text=0xffeff8ff;
        @Override public ProgressStyle color(int first,int second) { fill=first;return this; }
        @Override public ProgressStyle textColor(int color) { text=color;return this; }
        @Override public void render(net.minecraft.client.gui.GuiGraphics graphics,float x,float y,float width,float height,float progress,Component label) {
            com.cappleapple.racksnstands.display.render.RepairBar.draw(graphics,x,y,width,height,progress,label,IDisplayHelper.get().opacity(),fill,text);
        }
    }
    public static final class Provider implements IBlockComponentProvider {
        @Override public ResourceLocation getUid() { return UID; }
        @Override public int getDefaultPriority() { return 11000; }
        @Override public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config) {
            if(!FixtureConfig.REPAIR_TOOLTIP.get()) return;
            var target=RepairTarget.find(accessor.getLevel(),accessor.getHitResult());
            if(target==null) return;
            // Keep the inspection focused on the selected slot instead of listing the whole rack.
            tooltip.remove(JadeIds.UNIVERSAL_ITEM_STORAGE);
            if(!target.hasDurability()) return;
            var helper=IElementHelper.get();var progress=target.progress();
            tooltip.add(List.of(helper.smallItem(target.stack()),helper.text(target.stack().getHoverName())));
            tooltip.add(helper.progress((float)progress.completion(),target.durability(),new RepairBarStyle(),BoxStyle.getNestedBox(),true));
            tooltip.add(RepairTarget.remaining(progress));
        }
    }
}
