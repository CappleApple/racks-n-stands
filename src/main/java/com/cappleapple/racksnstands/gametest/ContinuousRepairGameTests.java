package com.cappleapple.racksnstands.gametest;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.block.*;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.config.FixtureConfig;
import com.cappleapple.racksnstands.display.RepairTarget;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(RacksNStands.MOD_ID) @PrefixGameTestTemplate(false)
public final class ContinuousRepairGameTests {
    private static FixtureBlockEntity fixture(GameTestHelper h,String id) {
        var pos=new BlockPos(1,2,1);var block=RacksNStands.FIXTURES.get(id).get();h.setBlock(pos,block);
        var f=(FixtureBlockEntity)h.getBlockEntity(pos);var host=new ItemStack(block);
        host.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(RacksNStands.id("repairing")).orElseThrow(),1);f.initializeFrom(host);return f;
    }
    private static ItemStack damaged(int damage) { var stack=new ItemStack(Items.DIAMOND_PICKAXE);stack.setDamageValue(damage);return stack; }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void fractionalRepairSurvivesNewBlockEntityAndCatchUp(GameTestHelper h) {
        var f=fixture(h,"tool_rack");f.insert(0,damaged(1000),false);f.insert(1,damaged(500),false);
        var saved=f.saveWithoutMetadata(h.getLevel().registryAccess());var fractions=new ListTag();fractions.add(DoubleTag.valueOf(.75));fractions.add(DoubleTag.valueOf(.25));saved.put("RepairFractions",fractions);
        long now=h.getLevel().getGameTime();saved.putLong("LastRepairTime",now);saved.putLong("SavedAt",now);
        var copy=new FixtureBlockEntity(f.getBlockPos(),f.getBlockState());copy.loadWithComponents(saved,h.getLevel().registryAccess());copy.setLevel(h.getLevel());
        long original=now;boolean unloaded=FixtureConfig.UNLOADED.get();
        try {
            FixtureConfig.UNLOADED.set(true);((net.minecraft.world.level.storage.ServerLevelData)h.getLevel().getLevelData()).setGameTime(now+7200);
            copy.onLoad();h.assertTrue(copy.displayedStack(0).getDamageValue()==969&&copy.displayedStack(1).getDamageValue()==469,"Ten percent of the calculation period repairs proportionally on load");
            h.assertTrue(Math.abs(copy.credit(0)-.97)<1e-7&&Math.abs(copy.credit(1)-.47)<1e-7,"Independent sub-point credits survive reload");
            copy.onLoad();h.assertTrue(copy.displayedStack(0).getDamageValue()==969,"Repeated load cannot award the same elapsed time twice");
        } finally { copy.setRemoved();FixtureConfig.UNLOADED.set(unloaded);((net.minecraft.world.level.storage.ServerLevelData)h.getLevel().getLevelData()).setGameTime(original); }
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void loadedOnlyKeepsEarnedTimeBetweenLastUpdateAndSave(GameTestHelper h) {
        var f=fixture(h,"generic_pedestal");f.insert(0,damaged(1000),false);var saved=f.saveWithoutMetadata(h.getLevel().registryAccess());
        // A save may occur between repair updates. Those loaded ticks must not disappear.
        saved.putLong("LastRepairTime",100);saved.putLong("SavedAt",1100);
        var copy=new FixtureBlockEntity(f.getBlockPos(),f.getBlockState());copy.loadWithComponents(saved,h.getLevel().registryAccess());copy.setLevel(h.getLevel());
        long original=h.getLevel().getGameTime();boolean unloaded=FixtureConfig.UNLOADED.get();
        try {
            FixtureConfig.UNLOADED.set(false);((net.minecraft.world.level.storage.ServerLevelData)h.getLevel().getLevelData()).setGameTime(100000);
            copy.onLoad();h.assertTrue(copy.displayedStack(0).getDamageValue()==996,"Loaded-only accounts 1000 earned ticks, not the unloaded gap");
            h.assertTrue(Math.abs(copy.credit(0)-(1561*.2/72-4))<1e-7,"Saving does not discard fractional earned repair");
        } finally { copy.setRemoved();FixtureConfig.UNLOADED.set(unloaded);((net.minecraft.world.level.storage.ServerLevelData)h.getLevel().getLevelData()).setGameTime(original); }
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void legacyTicksMigrateAndNewItemsCannotInheritProgress(GameTestHelper h) {
        var f=fixture(h,"tool_rack");f.insert(0,damaged(1000),false);var tag=f.saveWithoutMetadata(h.getLevel().registryAccess());
        tag.remove("RepairFractions");tag.putLongArray("RepairCredit",new long[]{36000,36000,0,0,0,0});
        var copy=new FixtureBlockEntity(f.getBlockPos(),f.getBlockState());copy.loadWithComponents(tag,h.getLevel().registryAccess());copy.setLevel(h.getLevel());copy.onLoad();
        h.assertTrue(copy.displayedStack(0).getDamageValue()==844&&Math.abs(copy.credit(0)-.1)<1e-7,"Old elapsed-tick credits migrate even when load runs before setLevel");
        copy.insert(1,damaged(1000),false);h.assertTrue(copy.credit(1)==0,"An empty slot cannot transfer legacy credit to a new item");copy.setRemoved();h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty",batch="repair_cadence",timeoutTicks=80)
    public static void configuredCadenceAppliesPartialRepairsAndLiveReschedules(GameTestHelper h) {
        var f=fixture(h,"generic_pedestal");f.insert(0,damaged(1000),false);
        h.runAtTickTime(2,() -> {
            double period=FixtureConfig.CALCULATION_PERIOD_SECONDS.get(),percent=FixtureConfig.PERCENT.get();int interval=FixtureConfig.INTERVAL_TICKS.get();
            try {
                FixtureConfig.CALCULATION_PERIOD_SECONDS.set(15.61);FixtureConfig.PERCENT.set(.2);FixtureConfig.INTERVAL_TICKS.set(20);f.configChanged();
            } finally { FixtureConfig.CALCULATION_PERIOD_SECONDS.set(period);FixtureConfig.PERCENT.set(percent);FixtureConfig.INTERVAL_TICKS.set(interval); }
        });
        h.runAtTickTime(12,() -> h.assertTrue(f.displayedStack(0).getDamageValue()==1000,"No whole durability is applied before the update tick"));
        h.runAtTickTime(23,() -> {
            h.assertTrue(f.displayedStack(0).getDamageValue()<1000,"Partial repair is applied long before the calculation period ends");
            int interval=FixtureConfig.INTERVAL_TICKS.get();
            try { FixtureConfig.INTERVAL_TICKS.set(2);f.configChanged(); }
            finally { FixtureConfig.INTERVAL_TICKS.set(interval); }
            int before=f.displayedStack(0).getDamageValue();
            h.runAtTickTime(26,() -> { h.assertTrue(f.lastRepairTime()>=h.getLevel().getGameTime()-2,"Live cadence edit replaces the pending 20-tick schedule");h.assertTrue(f.displayedStack(0).getDamageValue()<=before,"No progress is undone by changing the rate");h.succeed(); });
        });
    }

    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void progressSnapshotUsesServerRateAndSelectedSlot(GameTestHelper h) {
        var f=fixture(h,"tool_rack");f.insert(0,damaged(1000),false);f.insert(1,damaged(500),false);
        var snapshot=f.getUpdateTag(h.getLevel().registryAccess());h.assertTrue(snapshot.getList("RepairRates",Tag.TAG_DOUBLE).getDouble(0)>0,"Snapshot includes authoritative rates");
        h.assertTrue(f.repairProgress(1,h.getLevel().getGameTime()).completion()>f.repairProgress(0,h.getLevel().getGameTime()).completion(),"Each slot has its own progress and ETA");
        for(int slot=0;slot<2;slot++) {
            var local=f.profile().slots().get(slot).bounds().getCenter();var hit=new BlockHitResult(local.add(Vec3.atLowerCornerOf(f.getBlockPos())),Direction.NORTH,f.getBlockPos(),false);
            var target=RepairTarget.find(h.getLevel(),hit);h.assertTrue(target!=null&&target.slot()==slot,"Tooltip selects the looked-at rack slot");
        }
        var armor=fixture(h,"armor_mannequin");h.setBlock(new BlockPos(1,3,1),armor.getBlockState().setValue(FixtureBlock.HALF,DoubleBlockHalf.UPPER));
        var head=armor.profile().slots().getFirst().bounds().getCenter().add(Vec3.atLowerCornerOf(armor.getBlockPos()));
        h.assertTrue(RepairTarget.find(h.getLevel(),new BlockHitResult(head,Direction.NORTH,armor.getBlockPos().above(),false)).slot()==0,"Upper armor stand resolves the lower inventory's head slot");
        h.succeed();
    }
}
