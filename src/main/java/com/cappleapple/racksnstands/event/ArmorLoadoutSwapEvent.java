package com.cappleapple.racksnstands.event;
import com.cappleapple.racksnstands.api.DisplayFixture;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
/** Fired during locked preflight, before any equipment is changed. */
public final class ArmorLoadoutSwapEvent extends Event implements ICancellableEvent {
    public final DisplayFixture fixture;public final Player player;
    public ArmorLoadoutSwapEvent(DisplayFixture fixture,Player player) { this.fixture=fixture;this.player=player; }
}
