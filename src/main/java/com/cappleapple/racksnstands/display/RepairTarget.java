package com.cappleapple.racksnstands.display;

import com.cappleapple.racksnstands.block.*;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.interaction.FixtureInteraction;
import com.cappleapple.racksnstands.repairing.RepairProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Shared targeting and text for the native HUD and optional Jade provider. */
public record RepairTarget(FixtureBlockEntity fixture,int slot) {
    public static RepairTarget find(Level level,BlockHitResult hit) {
        if(level==null||hit.getType()!=HitResult.Type.BLOCK) return null;
        var state=level.getBlockState(hit.getBlockPos());
        if(!(state.getBlock() instanceof FixtureBlock)) return null;
        if(!(level.getBlockEntity(FixtureBlock.base(hit.getBlockPos(),state)) instanceof FixtureBlockEntity fixture)) return null;
        var point=FixtureInteraction.local(hit.getLocation().subtract(Vec3.atLowerCornerOf(fixture.getBlockPos())),state.getValue(FixtureBlock.FACING));
        if(state.hasProperty(SurfaceDisplayBlock.FACE)) point=SurfaceDisplayBlock.unmount(state.getValue(SurfaceDisplayBlock.FACE),point);
        return new RepairTarget(fixture,FixtureInteraction.slotAt(fixture.profile(),point));
    }
    public ItemStack stack() { return fixture.displayedStack(slot); }
    public boolean hasDurability() { return !stack().isEmpty()&&stack().isDamageableItem(); }
    public RepairProgress progress() { return fixture.repairProgress(slot,fixture.getLevel().getGameTime()); }
    public Component durability() {
        int maximum=stack().getMaxDamage();
        return Component.translatable("tooltip.racksnstands.repair.detail",Math.clamp(maximum-stack().getDamageValue(),0,maximum),maximum);
    }
    public static Component remaining(RepairProgress progress) {
        if(progress.full()) return Component.translatable("tooltip.racksnstands.repair.full");
        if(!progress.active()) return Component.translatable("tooltip.racksnstands.repair.inactive");
        long seconds=progress.remainingTicks()/20+(progress.remainingTicks()%20==0?0:1);
        long days=seconds/86400,hours=seconds/3600%24,minutes=seconds/60%60,s=seconds%60;
        String duration=days>0?days+"d "+hours+"h "+minutes+"m":hours>0?hours+"h "+minutes+"m "+s+"s":minutes>0?minutes+"m "+s+"s":s+"s";
        return Component.translatable("tooltip.racksnstands.repair.remaining",duration);
    }
}
