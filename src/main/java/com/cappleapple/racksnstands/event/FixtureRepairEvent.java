package com.cappleapple.racksnstands.event;
import com.cappleapple.racksnstands.api.DisplayFixture;
import java.util.List;
import net.neoforged.bus.api.Event;
/** One post-commit event for an entire fixture repair batch. */
public final class FixtureRepairEvent extends Event {
    public final DisplayFixture fixture;public final List<Integer> repairedSlots;
    public FixtureRepairEvent(DisplayFixture fixture,List<Integer> slots) { this.fixture=fixture;this.repairedSlots=List.copyOf(slots); }
}
