package com.cappleapple.racksnstands.interaction;
import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.event.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import net.neoforged.neoforge.common.NeoForge;

public final class ArmorSwap {
    private ArmorSwap() {}
    public static boolean swap(FixtureBlockEntity fixture,Player player) {
        if(fixture.profile().slots().stream().anyMatch(s -> s.equipment().isEmpty())||player.isSpectator()||!fixture.beginTransaction()) return false;
        try {
            fixture.settleForInteraction();
            int count=fixture.slotCount();
            ItemStack[] fromPlayer=new ItemStack[count],fromFixture=new ItemStack[count];EquipmentSlot[] equipment=new EquipmentSlot[count];
            for(int n=0;n<count;n++) {
                equipment[n]=fixture.profile().slots().get(n).equipment().orElseThrow();
                var carried=player.getItemBySlot(equipment[n]);var stored=fixture.displayedStack(n);
                if(carried.getCount()>1||stored.getCount()>1||!fixture.accepts(n,carried)||!fixture.accepts(n,stored)) return false;
                if(!stored.isEmpty()&&!stored.canEquip(equipment[n],player)) return false;
                if(!carried.isEmpty()&&!carried.canEquip(equipment[n],player)) return false;
                if(!player.isCreative()&&EnchantmentHelper.has(carried,EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) return false;
                fromPlayer[n]=carried.copy();fromFixture[n]=stored.copy();
            }
            if(NeoForge.EVENT_BUS.post(new ArmorLoadoutSwapEvent(fixture,player)).isCanceled()) return false;
            for(int n=0;n<count;n++) {
                if(!fromPlayer[n].isEmpty()&&NeoForge.EVENT_BUS.post(new FixtureItemInsertEvent(fixture,n,fromPlayer[n])).isCanceled()) return false;
                if(!fromFixture[n].isEmpty()&&NeoForge.EVENT_BUS.post(new FixtureItemExtractEvent(fixture,n,fromFixture[n])).isCanceled()) return false;
            }
            // Reentrant handlers cannot mutate the fixture; detect external player mutations too.
            if(fixture.isRemoved()) return false;
            for(int n=0;n<count;n++) if(!ItemStack.matches(player.getItemBySlot(equipment[n]),fromPlayer[n])||!ItemStack.matches(fixture.displayedStack(n),fromFixture[n])) return false;
            for(int n=0;n<count;n++) player.setItemSlot(equipment[n],fromFixture[n]);
            fixture.commitLoadout(fromPlayer);
            player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();player.containerMenu.broadcastChanges();
            return true;
        } finally { fixture.endTransaction(); }
    }
}
