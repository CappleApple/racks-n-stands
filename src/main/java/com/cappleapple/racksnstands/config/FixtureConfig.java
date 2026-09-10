package com.cappleapple.racksnstands.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FixtureConfig {
    public static final ModConfigSpec COMMON;
    public static final ModConfigSpec CLIENT;
    public static final ModConfigSpec.IntValue MAX_LEVEL, INTERVAL_TICKS;
    public static final ModConfigSpec.DoubleValue PERCENT, CALCULATION_PERIOD_SECONDS, SOUND_INTERVAL_PERCENT, SOUND_VOLUME, SOUND_PITCH;
    public static final ModConfigSpec.BooleanValue UNLOADED, DAMAGED_ONLY, PARTICLES, SOUND, AUTOMATION, SWAP, CURIOS, REPAIR_TOOLTIP, FULL_STACKS;
    public static final ModConfigSpec.IntValue RENDER_DISTANCE;
    static {
        var b = new ModConfigSpec.Builder();
        b.push("repairing");
        MAX_LEVEL = b.comment("Effective maintenance level cap. Enchantment acquisition max_level is independently datapack-defined.").defineInRange("max_level", 10, 1, 255);
        PERCENT = b.defineInRange("percent_per_level", 0.20, 0.000001, 100.0);
        INTERVAL_TICKS = b.comment("How often earned repair progress is applied, in game ticks.").defineInRange("interval_ticks",20,1,72000);
        CALCULATION_PERIOD_SECONDS = b.comment("Rate calculation period in seconds: each level repairs percent_per_level of maximum durability over this period. Fractional durability accumulates between updates.").defineInRange("calculation_period_seconds",3600.0,.05,31536000.0);
        UNLOADED = b.comment("Count world game time while the chunk is unloaded; never real time while the server is stopped.").define("repair_while_chunk_unloaded", true);
        DAMAGED_ONLY = b.comment("When false, full durable stacks are also checked. Completed items never bank surplus repairs.").define("repair_damaged_items_only", true);
        PARTICLES = b.define("repair_particles", true);
        SOUND = b.define("repair_sound", true);
        SOUND_INTERVAL_PERCENT = b.comment("Play a repair sound after each item restores this percentage of its maximum durability. 1.0 means 1%. Multiple thresholds or items in one update produce one sound.").defineInRange("repair_sound_interval_percent",1.0,.01,100.0);
        SOUND_VOLUME = b.comment("Repair sound volume; zero is silent.").defineInRange("repair_sound_volume",.15,0.0,16.0);
        SOUND_PITCH = b.comment("Repair sound pitch, within Minecraft's audible pitch range.").defineInRange("repair_sound_pitch",1.7,.5,2.0);
        b.pop().push("interaction");
        SWAP = b.define("armor_quick_swap", true);
        FULL_STACKS = b.comment("Allow general display slots to hold full item stacks, up to the item stack limit. Armor slots remain single-item. Disabling this does not delete existing stored stacks.").define("allow_full_stacks",false);
        b.pop().push("automation");
        AUTOMATION = b.define("allow_automation", true);
        b.pop().push("curios");
        CURIOS = b.define("enable_curios_integration", true);
        b.pop();
        COMMON = b.build();
        var c = new ModConfigSpec.Builder();
        c.push("rendering");
        REPAIR_TOOLTIP = c.comment("Show repair progress for the looked-at slot. Uses Jade when installed.").define("show_repair_tooltip",true);
        RENDER_DISTANCE = c.defineInRange("display_item_render_distance", 64, 8, 256);
        c.pop();
        CLIENT = c.build();
    }
    public static long intervalTicks() { return INTERVAL_TICKS.get(); }
    public static long calculationTicks() { return Math.max(1,Math.round(CALCULATION_PERIOD_SECONDS.get()*20)); }
    private FixtureConfig() {}
}
