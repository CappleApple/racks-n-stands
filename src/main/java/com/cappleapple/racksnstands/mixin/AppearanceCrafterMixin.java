package com.cappleapple.racksnstands.mixin;

import com.cappleapple.racksnstands.crafting.AppearanceRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.function.Consumer;

/** Match manual crafting's batch consumption in the vanilla automated crafter as well. */
@Mixin(CrafterBlock.class)
public abstract class AppearanceCrafterMixin {
    @Redirect(method="dispenseFrom",at=@At(value="INVOKE",target="Lnet/minecraft/core/NonNullList;forEach(Ljava/util/function/Consumer;)V"))
    private void racksnstands$consumeBatch(NonNullList<ItemStack> stacks,Consumer<ItemStack> vanilla,BlockState state,ServerLevel level,BlockPos pos) {
        var input=CraftingInput.of(3,3,stacks);var plan=AppearanceRecipe.selectedPlan(input,level);
        if(plan==null) { stacks.forEach(vanilla);return; }
        var target=input.getItem(plan.target());
        for(var stack:stacks) if(!stack.isEmpty()) stack.shrink(stack==target?stack.getCount():1);
    }
}
