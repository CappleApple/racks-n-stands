package com.cappleapple.racksnstands.mixin;

import com.cappleapple.racksnstands.crafting.AppearanceRecipe;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Vanilla consumes one ingredient per slot; appearance recipes transform the whole target stack. */
@Mixin(ResultSlot.class)
public abstract class AppearanceResultSlotMixin {
    @Shadow @Final private CraftingContainer craftSlots;
    @Unique private int racksnstands$target=-1;
    @Unique private int racksnstands$count;
    @Inject(method="onTake",at=@At("HEAD"))
    private void racksnstands$captureBatch(Player player,ItemStack result,CallbackInfo ci) {
        racksnstands$target=-1;
        var positioned=craftSlots.asPositionedCraftInput();var input=positioned.input();
        var plan=AppearanceRecipe.selectedPlan(input,player.level());
        if(plan!=null) {
            racksnstands$target=plan.target()%input.width()+positioned.left()
                +(plan.target()/input.width()+positioned.top())*craftSlots.getWidth();
            racksnstands$count=input.getItem(plan.target()).getCount();
        }
    }
    @ModifyArgs(method="onTake",at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/CraftingContainer;removeItem(II)Lnet/minecraft/world/item/ItemStack;"))
    private void racksnstands$consumeBatch(Args args) {
        if((int)args.get(0)==racksnstands$target) args.set(1,racksnstands$count);
    }
    @Inject(method="onTake",at=@At("RETURN"))
    private void racksnstands$clearBatch(Player player,ItemStack result,CallbackInfo ci) { racksnstands$target=-1; }
}
