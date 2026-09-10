package com.cappleapple.racksnstands.config;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import java.util.*;

/** FML watches TOML files and invalidates ConfigValue caches. Restart dormant maintenance work too. */
public final class LiveConfig {
    private static final Set<FixtureBlockEntity> LOADED=Collections.newSetFromMap(new WeakHashMap<>());
    public static synchronized void track(FixtureBlockEntity fixture) { LOADED.add(fixture); }
    public static synchronized void untrack(FixtureBlockEntity fixture) { LOADED.remove(fixture); }
    private static synchronized List<FixtureBlockEntity> loaded() { return List.copyOf(LOADED); }
    public static void reloaded(ModConfigEvent.Reloading event) {
        if(!event.getConfig().getModId().equals(RacksNStands.MOD_ID)) return;
        RacksNStands.LOGGER.info("Applied saved config {}",event.getConfig().getFileName());
        if(event.getConfig().getSpec()!=FixtureConfig.COMMON) return;
        var server=ServerLifecycleHooks.getCurrentServer();
        refresh(server);
    }
    public static void refresh(net.minecraft.server.MinecraftServer server) {
        if(server!=null) server.execute(() -> {
            for(var fixture:loaded()) if(!fixture.isRemoved()&&fixture.getLevel()!=null&&!fixture.getLevel().isClientSide) fixture.configChanged();
        });
    }
    private LiveConfig() {}
}
