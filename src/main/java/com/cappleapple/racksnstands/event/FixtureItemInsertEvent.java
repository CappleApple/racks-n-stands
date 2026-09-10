package com.cappleapple.racksnstands.event;
import com.cappleapple.racksnstands.api.DisplayFixture;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
public final class FixtureItemInsertEvent extends Event implements ICancellableEvent {
    public final DisplayFixture fixture;public final int slot;private final ItemStack stack;
    public FixtureItemInsertEvent(DisplayFixture fixture,int slot,ItemStack stack) { this.fixture=fixture;this.slot=slot;this.stack=stack.copy(); }
    public ItemStack stack() { return stack.copy(); }
}
