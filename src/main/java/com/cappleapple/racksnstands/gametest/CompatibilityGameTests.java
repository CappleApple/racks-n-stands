package com.cappleapple.racksnstands.gametest;
import com.cappleapple.racksnstands.*;
import com.cappleapple.racksnstands.api.*;
import com.cappleapple.racksnstands.block.*;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.datagen.FixtureData;
import com.cappleapple.racksnstands.display.*;
import com.cappleapple.racksnstands.event.*;
import com.cappleapple.racksnstands.interaction.ArmorSwap;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder(RacksNStands.MOD_ID) @PrefixGameTestTemplate(false)
public final class CompatibilityGameTests {
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void realEnchantment(GameTestHelper h) {
        var enchantment=h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(RacksNStands.id("repairing")).orElseThrow();
        h.assertTrue(enchantment.value().getMaxLevel()==10,"Repairing defines all ten real levels");
        h.assertTrue(!enchantment.is(net.minecraft.tags.EnchantmentTags.IN_ENCHANTING_TABLE),"Repairing is treasure-only");
        for(var block:RacksNStands.FIXTURES.values()) h.assertTrue(enchantment.value().canEnchant(new ItemStack(block.get())),"Every fixture is an enchantment host");
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void registeredItemPredicateUsesRegistryOps(GameTestHelper h) {
        var json=FixtureData.object("slots",FixtureData.array(FixtureData.object("index",0,"filter",FixtureData.object("predicate",FixtureData.object("items","minecraft:diamond")),"transform",FixtureData.object(),"interaction_bounds",FixtureData.array(0,0,0,1,1,1))));
        var parsed=Profiles.decode(Map.of(RacksNStands.id("displays/predicate_test"),json),h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE));
        var profile=parsed.displays().get(RacksNStands.id("predicate_test"));h.assertTrue(profile!=null,"Registry-aware predicate loads");
        h.assertTrue(Classification.accepts(profile.slots().getFirst(),new ItemStack(Items.DIAMOND),h.getLevel()),"Predicate accepts declared item");
        h.assertTrue(!Classification.accepts(profile.slots().getFirst(),new ItemStack(Items.EMERALD),h.getLevel()),"Predicate rejects other items");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void perSlotVetoAndReentrancy(GameTestHelper h) {
        var pos=new BlockPos(1,2,1);h.setBlock(pos,RacksNStands.FIXTURES.get("armor_mannequin").get());var fixture=(FixtureBlockEntity)h.getBlockEntity(pos);var player=h.makeMockPlayer(GameType.SURVIVAL);
        fixture.insert(0,new ItemStack(Items.IRON_HELMET),false);player.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.IRON_BOOTS));
        Consumer<FixtureItemInsertEvent> veto=e -> { if(e.fixture==fixture) {
            h.assertTrue(fixture.extract(0,1,false).isEmpty(),"Reentrant extraction is blocked during preflight");e.setCanceled(true);
        }};
        NeoForge.EVENT_BUS.addListener(veto);
        try { h.assertTrue(!ArmorSwap.swap(fixture,player),"Individual slot veto cancels entire loadout"); }
        finally { NeoForge.EVENT_BUS.unregister(veto); }
        h.assertTrue(fixture.displayedStack(0).is(Items.IRON_HELMET)&&fixture.displayedStack(3).isEmpty()&&player.getItemBySlot(EquipmentSlot.FEET).is(Items.IRON_BOOTS)&&player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),"No partial swap or loss");
        Consumer<FixtureItemExtractEvent> extractVeto=e -> { if(e.fixture==fixture)e.setCanceled(true); };NeoForge.EVENT_BUS.addListener(extractVeto);
        try { h.assertTrue(fixture.extract(0,1,false).isEmpty()&&fixture.displayedStack(0).is(Items.IRON_HELMET),"Extraction veto preserves contents"); }
        finally { NeoForge.EVENT_BUS.unregister(extractVeto); }h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void upperHalfAndCreativeBreakConserveGear(GameTestHelper h) {
        var pos=new BlockPos(1,2,1);var block=RacksNStands.FIXTURES.get("armor_mannequin").get();h.setBlock(pos,block);h.setBlock(pos.above(),block.defaultBlockState().setValue(FixtureBlock.HALF,DoubleBlockHalf.UPPER));
        var fixture=(FixtureBlockEntity)h.getBlockEntity(pos);fixture.insert(0,new ItemStack(Items.DIAMOND_HELMET),false);
        var absolute=h.absolutePos(pos);h.getLevel().destroyBlock(absolute.above(),true);
        var dropped=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(absolute).inflate(2));
        h.assertTrue(dropped.stream().filter(e -> e.getItem().is(Items.DIAMOND_HELMET)).mapToInt(e -> e.getItem().getCount()).sum()==1,"Upper-half break drops gear once");
        h.assertTrue(dropped.stream().filter(e -> e.getItem().is(block.asItem())).mapToInt(e -> e.getItem().getCount()).sum()==1,"Upper-half break drops one fixture");
        h.assertTrue(h.getLevel().isEmptyBlock(absolute),"Upper-half removal removes storage half");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void waitingRepairDoesNotSendVisibleUpdates(GameTestHelper h) {
        var pos=new BlockPos(1,2,1);var block=RacksNStands.FIXTURES.get("generic_pedestal").get();h.setBlock(pos,block);var fixture=(FixtureBlockEntity)h.getBlockEntity(pos);
        var host=new ItemStack(block);host.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(RacksNStands.id("repairing")).orElseThrow(),1);fixture.initializeFrom(host);
        var stack=new ItemStack(Items.DIAMOND_PICKAXE);stack.setDamageValue(500);fixture.insert(0,stack,false);
        h.runAfterDelay(2,() -> { int revision=fixture.revision();
        h.runAfterDelay(45,() -> { h.assertTrue(fixture.revision()==revision,"Scheduled clock updates do not emit visible state changes");h.assertTrue(fixture.displayedStack(0).getDamageValue()==500,"Fractional progress has not reached a full durability point");h.succeed(); }); });
    }
}
