package com.cappleapple.racksnstands.mixin;

import com.cappleapple.racksnstands.block.FixtureItem;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(AbstractContainerMenu.class)
public abstract class AppearanceQuickCraftMixin {
    /** Stop repeated shift crafting when copying leaves a different appearance-reset result. */
    @Redirect(method="doClick",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;isSameItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean racksnstands$sameCraftedComponents(ItemStack next,ItemStack previous) {
        return previous.getItem() instanceof FixtureItem?ItemStack.isSameItemSameComponents(next,previous):ItemStack.isSameItem(next,previous);
    }
}
