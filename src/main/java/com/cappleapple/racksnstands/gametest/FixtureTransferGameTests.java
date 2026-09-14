package com.cappleapple.racksnstands.gametest;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.block.FixtureBlock;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.config.FixtureConfig;
import com.cappleapple.racksnstands.event.FixtureItemExtractEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;
import java.util.function.Consumer;

@GameTestHolder(RacksNStands.MOD_ID) @PrefixGameTestTemplate(false)
public final class FixtureTransferGameTests {
    private static final String EMPTY="bastion/mobs/empty";
    private static final BlockPos SOURCE=new BlockPos(1,2,1),DESTINATION=new BlockPos(3,2,1);
    private static final int MOVE_FLAGS=Block.UPDATE_ALL|Block.UPDATE_MOVE_BY_PISTON;
    private static FixtureBlockEntity fixture(GameTestHelper h,String id) {
        var block=RacksNStands.FIXTURES.get(id).get();h.setBlock(SOURCE,block);
        var fixture=(FixtureBlockEntity)h.getBlockEntity(SOURCE);var host=new ItemStack(block);
        host.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(RacksNStands.id("repairing")).orElseThrow(),2);
        block.setPlacedBy(h.getLevel(),fixture.getBlockPos(),fixture.getBlockState(),null,host);
        h.assertTrue(fixture.setMaterial("wood",Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X)),"Fixture accepts a material with block-state properties");
        return fixture;
    }
    private static ItemStack stack(Item item,int count,int index) {
        var stack=new ItemStack(item,count);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Transferred slot "+index));
        if(stack.isDamageableItem()) { stack.set(DataComponents.MAX_DAMAGE,2000+index);stack.setDamageValue(100+index); }
        return stack;
    }
    private static ItemStack[] fill(GameTestHelper h,FixtureBlockEntity fixture,boolean armor) {
        Item[] items=armor?new Item[]{Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS}
            :new Item[]{Items.DIAMOND_PICKAXE,Items.DIAMOND,Items.IRON_AXE,Items.EMERALD,Items.GOLDEN_SHOVEL,Items.REDSTONE};
        var expected=new ItemStack[items.length];
        for(int i=0;i<items.length;i++) {
            expected[i]=stack(items[i],!armor&&i%2==1?17+i:1,i);
            h.assertTrue(fixture.insert(i,expected[i],false).isEmpty(),"Every transfer slot is populated without leftovers");
        }
        return expected;
    }
    private static List<ItemEntity> drops(GameTestHelper h) {
        var bounds=new AABB(h.absolutePos(SOURCE)).minmax(new AABB(h.absolutePos(DESTINATION))).inflate(1).expandTowards(0,2,0);
        return h.getLevel().getEntitiesOfClass(ItemEntity.class,bounds);
    }
    private static void assertDrops(GameTestHelper h,ItemStack[] expected,Item fixtureItem) {
        var drops=drops(h);int total=1;
        for(var stack:expected) {
            int count=drops.stream().filter(e -> ItemStack.isSameItemSameComponents(stack,e.getItem())).mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(count==stack.getCount(),"Stored components and count drop exactly once: "+stack);total+=stack.getCount();
        }
        h.assertTrue(drops.stream().filter(e -> e.getItem().is(fixtureItem)).mapToInt(e -> e.getItem().getCount()).sum()==1,"Exactly one fixture item drops");
        h.assertTrue(drops.stream().mapToInt(e -> e.getItem().getCount()).sum()==total,"No extra inventory or fixture items drop");
    }
    private static FixtureBlockEntity transfer(GameTestHelper h,FixtureBlockEntity source,BlockPos destination,boolean upperFirst,boolean fullMetadata) {
        var level=h.getLevel();var state=source.getBlockState();var origin=source.getBlockPos();var materials=source.materials();int power=source.repairingLevel();
        CompoundTag saved=fullMetadata?source.saveWithFullMetadata(level.registryAccess()):source.saveWithoutMetadata(level.registryAccess());
        Clearable.tryClear(source);
        for(int i=0;i<source.slotCount();i++) h.assertTrue(source.displayedStack(i).isEmpty(),"Transfer clears every source slot");
        if(((FixtureBlock)state.getBlock()).kind().tall()) {
            var first=upperFirst?origin.above():origin;var second=upperFirst?origin:origin.above();var remaining=level.getBlockState(second);
            level.setBlock(first,Blocks.AIR.defaultBlockState(),MOVE_FLAGS);
            h.assertTrue(level.getBlockState(second).equals(remaining),"Moving the first half leaves its partner intact for transfer");
            level.setBlock(second,Blocks.AIR.defaultBlockState(),MOVE_FLAGS);
        } else level.setBlock(origin,Blocks.AIR.defaultBlockState(),MOVE_FLAGS);
        h.assertTrue(level.getBlockEntity(origin)==null,"Source block entity is removed after transfer");
        h.assertTrue(drops(h).isEmpty(),"Removing transferred source blocks creates no world drops");
        level.setBlock(destination,state,MOVE_FLAGS);
        if(((FixtureBlock)state.getBlock()).kind().tall()) level.setBlock(destination.above(),state.setValue(FixtureBlock.HALF,DoubleBlockHalf.UPPER),MOVE_FLAGS);
        var restored=(FixtureBlockEntity)level.getBlockEntity(destination);restored.loadWithComponents(saved,level.registryAccess());
        h.assertTrue(restored.materials().equals(materials)&&restored.repairingLevel()==power,"Transfer preserves appearance and fixture enchantments");
        var relocated=restored.saveWithFullMetadata(level.registryAccess());
        h.assertTrue(restored.getBlockPos().equals(destination)&&relocated.getInt("x")==destination.getX()&&relocated.getInt("y")==destination.getY()&&relocated.getInt("z")==destination.getZ(),"Restored metadata identifies the destination");
        return restored;
    }
    private static void assertContents(GameTestHelper h,FixtureBlockEntity fixture,ItemStack[] expected) {
        for(int i=0;i<expected.length;i++) h.assertTrue(ItemStack.matches(expected[i],fixture.displayedStack(i)),"Transfer preserves the full stack in slot "+i);
    }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void sixSlotRackRoundTripConservesFullStacksAndComponents(GameTestHelper h) {
        boolean fullStacks=FixtureConfig.FULL_STACKS.get();
        try {
            FixtureConfig.FULL_STACKS.set(true);var source=fixture(h,"tool_rack");var expected=fill(h,source,false);
            var destination=transfer(h,source,h.absolutePos(DESTINATION),false,true);assertContents(h,destination,expected);
            var returned=transfer(h,destination,h.absolutePos(SOURCE),false,false);assertContents(h,returned,expected);
            var item=returned.getBlockState().getBlock().asItem();h.getLevel().destroyBlock(returned.getBlockPos(),true);returned.dropContents();
            assertDrops(h,expected,item);
        } finally { FixtureConfig.FULL_STACKS.set(fullStacks); }
        h.succeed();
    }
    private static void mannequinTransfer(GameTestHelper h,boolean upperFirst) {
        var source=fixture(h,"armor_mannequin");var expected=fill(h,source,true);
        var destination=transfer(h,source,h.absolutePos(DESTINATION),upperFirst,true);assertContents(h,destination,expected);
        var returned=transfer(h,destination,h.absolutePos(SOURCE),!upperFirst,false);assertContents(h,returned,expected);
        var item=returned.getBlockState().getBlock().asItem();h.getLevel().destroyBlock(upperFirst?returned.getBlockPos().above():returned.getBlockPos(),true);
        h.assertTrue(h.getLevel().isEmptyBlock(returned.getBlockPos())&&h.getLevel().isEmptyBlock(returned.getBlockPos().above()),"Ordinary destruction removes both restored halves");
        assertDrops(h,expected,item);h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void mannequinTransfersUpperHalfFirst(GameTestHelper h) { mannequinTransfer(h,true); }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void mannequinTransfersLowerHalfFirst(GameTestHelper h) { mannequinTransfer(h,false); }
    private static void movingWithoutClear(GameTestHelper h,boolean upperFirst) {
        var source=fixture(h,"armor_mannequin");var expected=fill(h,source,true);var origin=source.getBlockPos();
        var first=upperFirst?origin.above():origin;var second=upperFirst?origin:origin.above();var remaining=h.getLevel().getBlockState(second);
        h.getLevel().setBlock(first,Blocks.AIR.defaultBlockState(),MOVE_FLAGS);
        h.assertTrue(h.getLevel().getBlockState(second).equals(remaining),"Movement cannot destroy the unprocessed companion half");
        assertContents(h,source,expected);h.assertTrue(drops(h).isEmpty(),"Movement never drops inventory, even without a prior clear");
        h.getLevel().setBlock(second,Blocks.AIR.defaultBlockState(),MOVE_FLAGS);
        h.assertTrue(h.getLevel().getBlockEntity(origin)==null&&drops(h).isEmpty(),"Moving both halves removes the source block entity without drops");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void movingUpperHalfLeavesUnclearedLowerHalfIntact(GameTestHelper h) { movingWithoutClear(h,true); }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void movingLowerHalfLeavesUpperHalfIntact(GameTestHelper h) { movingWithoutClear(h,false); }
    private static void normalMannequinBreak(GameTestHelper h,boolean upperFirst) {
        var fixture=fixture(h,"armor_mannequin");var expected=fill(h,fixture,true);var origin=fixture.getBlockPos();var item=fixture.getBlockState().getBlock().asItem();
        h.getLevel().destroyBlock(upperFirst?origin.above():origin,true);fixture.dropContents();
        h.assertTrue(h.getLevel().isEmptyBlock(origin)&&h.getLevel().isEmptyBlock(origin.above()),"Breaking either mannequin half removes its partner");
        assertDrops(h,expected,item);h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void ordinaryUpperHalfBreakDropsInventoryOnce(GameTestHelper h) { normalMannequinBreak(h,true); }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void ordinaryLowerHalfBreakDropsInventoryOnce(GameTestHelper h) { normalMannequinBreak(h,false); }
    private static ListTag repeated(int count,double value) { var list=new ListTag();for(int i=0;i<count;i++) list.add(DoubleTag.valueOf(value));return list; }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void clearBypassesExtractionVetoAndResetsOnlyInventoryState(GameTestHelper h) {
        var fixture=fixture(h,"tool_rack");var old=stack(Items.DIAMOND_PICKAXE,1,0);fixture.insert(0,old,false);
        var saved=fixture.saveWithoutMetadata(h.getLevel().registryAccess());saved.put("RepairFractions",repeated(fixture.slotCount(),.25));saved.put("RepairSoundProgress",repeated(fixture.slotCount(),.75));
        fixture.loadWithComponents(saved,h.getLevel().registryAccess());var materials=fixture.materials();int power=fixture.repairingLevel(),revision=fixture.revision();int[] vetoCalls={0};
        Consumer<FixtureItemExtractEvent> veto=event -> { if(event.fixture==fixture) { vetoCalls[0]++;event.setCanceled(true); } };
        boolean automation=FixtureConfig.AUTOMATION.get();NeoForge.EVENT_BUS.addListener(veto);
        try {
            FixtureConfig.AUTOMATION.set(false);
            h.assertTrue(fixture.automation().extractItem(0,1,false).isEmpty()&&fixture.extract(0,1,false).isEmpty()&&vetoCalls[0]==1,"Automation is disabled and direct extraction is vetoed");
            Clearable.tryClear(fixture);Clearable.tryClear(fixture);
            h.assertTrue(vetoCalls[0]==1&&fixture.comparator()==0&&fixture.revision()>revision,"Clear bypasses extraction events and updates visible inventory state");
            var cleared=fixture.saveWithoutMetadata(h.getLevel().registryAccess());
            for(int i=0;i<fixture.slotCount();i++) {
                h.assertTrue(fixture.displayedStack(i).isEmpty(),"Clear empties every slot");
                for(String key:List.of("RepairFractions","RepairRates","RepairSoundProgress")) h.assertTrue(cleared.getList(key,Tag.TAG_DOUBLE).getDouble(i)==0,"Clear resets "+key+" for slot "+i);
            }
            h.assertTrue(fixture.materials().equals(materials)&&fixture.repairingLevel()==power&&drops(h).isEmpty(),"Repeated clearing preserves appearance and enchantments without drops");
            var replacement=stack(Items.IRON_PICKAXE,1,99);h.assertTrue(fixture.insert(0,replacement,false).isEmpty(),"Cleared fixture remains usable");
            var item=fixture.getBlockState().getBlock().asItem();h.getLevel().destroyBlock(fixture.getBlockPos(),true);fixture.dropContents();
            assertDrops(h,new ItemStack[]{replacement},item);
        } finally { FixtureConfig.AUTOMATION.set(automation);NeoForge.EVENT_BUS.unregister(veto); }
        h.succeed();
    }
}
