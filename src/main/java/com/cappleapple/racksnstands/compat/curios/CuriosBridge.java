package com.cappleapple.racksnstands.compat.curios;
import com.cappleapple.racksnstands.config.FixtureConfig;
import net.neoforged.fml.ModList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.Set;
public final class CuriosBridge {
    private CuriosBridge() {}
    private static final java.util.Set<net.minecraft.resources.ResourceLocation> WARNED=java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static boolean available() { return ModList.get().isLoaded("curios") && FixtureConfig.CURIOS.get(); }
    public static boolean swap(com.cappleapple.racksnstands.blockentity.FixtureBlockEntity fixture,int slot,net.minecraft.world.entity.player.Player player) {
        return available()&&InstalledCurios.swap(fixture,slot,player);
    }
    public static Set<String> slots(ItemStack stack, Level level) {
        if(!available()) return Set.of();
        try { return InstalledCurios.slots(stack,level); }
        catch(RuntimeException e) {
            var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if(WARNED.add(id)) com.cappleapple.racksnstands.RacksNStands.LOGGER.warn("Curios classification failed for {}: {}",id,e.toString());
            return Set.of();
        }
    }
}
