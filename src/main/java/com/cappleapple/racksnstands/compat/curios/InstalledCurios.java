package com.cappleapple.racksnstands.compat.curios;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;
/** Only reached after ModList confirms Curios is installed and enabled. */
final class InstalledCurios {
    static boolean swap(com.cappleapple.racksnstands.blockentity.FixtureBlockEntity fixture,int slot,net.minecraft.world.entity.player.Player player) {
        if(player.isSpectator()||!player.mayBuild()||!player.getMainHandItem().isEmpty()||!fixture.beginTransaction()) return false;
        try {
            fixture.settleForInteraction();
            var stored=fixture.displayedStack(slot).copy();if(stored.isEmpty()) return false;
            var inventory=CuriosApi.getCuriosInventory(player).orElse(null);if(inventory==null) return false;
            var incoming=stored.copyWithCount(1);
            // Empty compatible functional slots win; then try occupied slots in stable identifier/index order.
            var handlers=new java.util.TreeMap<>(inventory.getCurios());
            for(int pass=0;pass<2;pass++) for(var entry:handlers.entrySet()) {
                var owner=entry.getValue();var handler=owner.getStacks();
                for(int index=0;index<handler.getSlots();index++) {
                    if(index<owner.getActiveStates().size()&&!owner.getActiveStates().get(index)) continue;
                    var previous=handler.getStackInSlot(index).copy();
                    if(previous.isEmpty()!=(pass==0)||!handler.isItemValid(index,incoming)) continue;
                    // The Curios handler runs its native equip/unequip hooks, binding curse and permission events.
                    if(!previous.isEmpty()&&!ItemStack.matches(handler.extractItem(index,previous.getCount(),true),previous)) continue;
                    if(!fixture.accepts(slot,previous)||!previous.isEmpty()&&previous.getCount()>fixture.itemLimit(slot,previous)) continue;
                    if(!previous.isEmpty()&&net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.cappleapple.racksnstands.event.FixtureItemInsertEvent(fixture,slot,previous.copy())).isCanceled()) return false;
                    var removed=previous.isEmpty()?incoming:stored;
                    if(net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.cappleapple.racksnstands.event.FixtureItemExtractEvent(fixture,slot,removed.copy())).isCanceled()) return false;
                    if(fixture.isRemoved()||!player.getMainHandItem().isEmpty()||!ItemStack.matches(fixture.displayedStack(slot),stored)||!ItemStack.matches(handler.getStackInSlot(index),previous)) return false;
                    var remainder=stored.copyWithCount(stored.getCount()-1);
                    fixture.commitSlot(slot,previous.isEmpty()?remainder:previous);
                    handler.setStackInSlot(index,incoming);
                    // Curios' living tick applies equipment lifecycle, attributes and network synchronization once.
                    if(!previous.isEmpty()&&!remainder.isEmpty()&&!player.getInventory().add(remainder)) player.drop(remainder,false);
                    player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();player.containerMenu.broadcastChanges();
                    return true;
                }
            }
            return false;
        } finally { fixture.endTransaction(); }
    }
    static Set<String> slots(ItemStack stack, Level level) { return CuriosApi.getItemStackSlots(stack, level).keySet(); }
}
