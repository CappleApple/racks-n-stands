package com.cappleapple.racksnstands.gametest;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.event.FixtureItemExtractEvent;
import com.cappleapple.racksnstands.interaction.ArmorSwap;
import com.cappleapple.racksnstands.interaction.FixtureInteraction;
import com.cappleapple.racksnstands.registry.FixtureCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.*;
import java.util.function.Consumer;

@GameTestHolder(RacksNStands.MOD_ID) @PrefixGameTestTemplate(false)
public final class ExchangeGameTests {
    private static FixtureBlockEntity fixture(GameTestHelper h,String id) {
        var p=new BlockPos(1,2,1);h.setBlock(p,RacksNStands.FIXTURES.get(id).get());return (FixtureBlockEntity)h.getBlockEntity(p);
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void exchangeKeepsEnchantmentsOnTheirOwnStacks(GameTestHelper h) {
        var f=fixture(h,"polearm_rack");var player=h.makeMockPlayer(GameType.SURVIVAL);
        var registry=h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var bow=new ItemStack(Items.BOW);bow.enchant(registry.getHolderOrThrow(Enchantments.POWER),4);
        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(registry.getHolderOrThrow(Enchantments.SHARPNESS),2);
        f.insert(0,bow,false);f.insert(1,new ItemStack(Items.IRON_SWORD),false);player.setItemInHand(InteractionHand.MAIN_HAND,sword.copy());
        h.assertTrue(FixtureInteraction.swapHeld(f,0,player,InteractionHand.MAIN_HAND),"Occupied unrestricted slot exchanges held item");
        h.assertTrue(ItemStack.matches(f.displayedStack(0),sword)&&ItemStack.matches(player.getMainHandItem(),bow),"Enchantment components follow their original item");
        h.assertTrue(!f.displayedStack(1).isEnchanted(),"Neighbor does not inherit enchantments");
        sword.enchant(registry.getHolderOrThrow(Enchantments.UNBREAKING),3);
        h.assertTrue(f.displayedStack(0).get(DataComponents.ENCHANTMENTS).getLevel(registry.getHolderOrThrow(Enchantments.UNBREAKING))==0,"Inserted stack has independent component ownership");
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void stackedHeldItemAndFullInventoryConserveCounts(GameTestHelper h) {
        var f=fixture(h,"sword_floor_stand");var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(f.getBlockPos().getX()+.5,f.getBlockPos().getY(),f.getBlockPos().getZ()+.5);
        f.insert(0,new ItemStack(Items.DIAMOND_SWORD),false);
        for(int n=0;n<p.getInventory().items.size();n++) p.getInventory().items.set(n,new ItemStack(Items.COBBLESTONE,64));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE,64));
        h.assertTrue(FixtureInteraction.swapHeld(f,0,p,InteractionHand.MAIN_HAND),"Full inventory still permits an exchange");
        h.assertTrue(p.getMainHandItem().is(Items.DIAMOND_SWORD)&&f.displayedStack(0).is(Items.APPLE)&&f.displayedStack(0).getCount()==1,"Old display moves to hand and one held item enters stand");
        int total=f.displayedStack(0).getCount();
        for(var stack:p.getInventory().items) if(stack.is(Items.APPLE)) total+=stack.getCount();
        for(var e:h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(f.getBlockPos()).inflate(4))) if(e.getItem().is(Items.APPLE)) total+=e.getItem().getCount();
        h.assertTrue(total==64,"Stack remainder is retained or dropped exactly once");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void canceledOccupiedExchangeIsAtomic(GameTestHelper h) {
        var f=fixture(h,"generic_wall_display");var p=h.makeMockPlayer(GameType.SURVIVAL);f.insert(0,new ItemStack(Items.DIAMOND),false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE,12));
        Consumer<FixtureItemExtractEvent> veto=e -> { if(e.fixture==f) { h.assertTrue(f.extract(0,1,false).isEmpty(),"Exchange rejects reentrant mutation");e.setCanceled(true); } };
        NeoForge.EVENT_BUS.addListener(veto);
        try { h.assertTrue(!FixtureInteraction.swapHeld(f,0,p,InteractionHand.MAIN_HAND),"Extraction veto cancels the entire exchange"); }
        finally { NeoForge.EVENT_BUS.unregister(veto); }
        h.assertTrue(f.displayedStack(0).is(Items.DIAMOND)&&p.getMainHandItem().is(Items.APPLE)&&p.getMainHandItem().getCount()==12,"Both original stacks survive cancellation");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void individualArmorExchangeUsesEquippedSlot(GameTestHelper h) {
        var f=fixture(h,"helmet_stand");var p=h.makeMockPlayer(GameType.SURVIVAL);
        f.insert(0,new ItemStack(Items.DIAMOND_HELMET),false);p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE,23));
        h.assertTrue(ArmorSwap.swap(f,p),"Single armor stand exchanges equipped armor");
        h.assertTrue(p.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET)&&f.displayedStack(0).is(Items.IRON_HELMET)&&p.getMainHandItem().getCount()==23,"Equipped exchange leaves held stack untouched");
        p.getItemBySlot(EquipmentSlot.HEAD).enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.BINDING_CURSE),1);
        h.assertTrue(!ArmorSwap.swap(f,p),"Binding still prevents removing equipped armor");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_BOOTS));
        h.assertTrue(!FixtureInteraction.swapHeld(f,0,p,InteractionHand.MAIN_HAND)&&f.displayedStack(0).is(Items.IRON_HELMET),"Armor fixtures retain their corresponding slot restriction");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void generalFurnitureHasNoItemTypeRestrictions(GameTestHelper h) {
        for(var kind:FixtureCatalog.ALL) if(!kind.armor()) {
            var f=fixture(h,kind.id());
            for(var item:new Item[]{Items.APPLE,Items.BOW,Items.TRIDENT,Items.LEATHER_HELMET,Items.COMMAND_BLOCK,Items.FEATHER})
                for(int n=0;n<f.slotCount();n++) h.assertTrue(f.accepts(n,new ItemStack(item)),"General/legacy furniture accepts arbitrary items: "+kind.id());
        }
        var body=fixture(h,"generic_pedestal");var player=h.makeMockPlayer(GameType.SURVIVAL);body.insert(0,new ItemStack(Items.WOLF_ARMOR),false);
        h.assertTrue(FixtureInteraction.swapEquipped(body,0,player)&&player.getMainHandItem().is(Items.WOLF_ARMOR),"Animal armor falls back to the hand instead of a nonexistent player body slot");
        h.assertTrue(!FixtureCatalog.get("necklace_bust").available()&&!FixtureCatalog.get("staff_mount").available()&&FixtureCatalog.get("curio_cabinet").available(),"Redundant specialty forms are absent from the craftable catalog");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void heldArmorChoosesItsSlotAnywhereOnMannequin(GameTestHelper h) {
        var f=fixture(h,"armor_mannequin");var player=h.makeMockPlayer(GameType.SURVIVAL);
        Item[] oldArmor={Items.IRON_HELMET,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS};
        Item[] newArmor={Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS};
        for(int clicked=0;clicked<4;clicked++) for(int part=0;part<4;part++) {
            for(int slot=0;slot<4;slot++) { f.extract(slot,1,false);f.insert(slot,new ItemStack(oldArmor[slot]),false); }
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(newArmor[part]));
            h.assertTrue(FixtureInteraction.swapHeld(f,clicked,player,InteractionHand.MAIN_HAND),"Any clicked mannequin region accepts matching held armor");
            h.assertTrue(player.getMainHandItem().is(oldArmor[part]),"Replaced armor returns to the same hand");
            for(int slot=0;slot<4;slot++) h.assertTrue(f.displayedStack(slot).is(slot==part?newArmor[part]:oldArmor[slot]),"Only the armor's matching equipment slot changes");
        }
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.NETHERITE_CHESTPLATE));
        h.assertTrue(FixtureInteraction.swapHeld(f,3,player,InteractionHand.OFF_HAND)&&f.displayedStack(1).is(Items.NETHERITE_CHESTPLATE),"Offhand armor also targets its matching slot");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        h.assertTrue(FixtureInteraction.swapHeld(f,0,player,InteractionHand.MAIN_HAND)&&f.displayedStack(0).isEmpty(),"Empty-hand removal still targets the clicked part");h.succeed();
    }

}
