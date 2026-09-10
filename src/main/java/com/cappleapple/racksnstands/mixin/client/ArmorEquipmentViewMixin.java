package com.cappleapple.racksnstands.mixin.client;

import com.cappleapple.racksnstands.display.render.ArmorRenderContext;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class ArmorEquipmentViewMixin {
    @Inject(method="getItemBySlot",at=@At("HEAD"),cancellable=true)
    private void racksnstands$displayEquipment(EquipmentSlot slot,CallbackInfoReturnable<ItemStack> callback) {
        var stack=ArmorRenderContext.equipment((Player)(Object)this,slot);
        if(stack!=null) callback.setReturnValue(stack);
    }
    @Inject(method="getArmorSlots",at=@At("HEAD"),cancellable=true)
    private void racksnstands$displayArmor(CallbackInfoReturnable<Iterable<ItemStack>> callback) {
        var stacks=ArmorRenderContext.armor((Player)(Object)this);
        if(stacks!=null) callback.setReturnValue(stacks);
    }
}
