package com.cappleapple.racksnstands.api;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
public interface DisplayFixture {
    ResourceLocation profileId();
    DisplayProfile profile();
    /** Borrowed read-only stack. Mutate through the item handler, never through this reference. */
    ItemStack displayedStack(int slot);
    int slotCount();
}
