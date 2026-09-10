package com.cappleapple.racksnstands.gametest;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.config.FixtureConfig;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder(RacksNStands.MOD_ID) @PrefixGameTestTemplate(false)
public final class RepairSoundGameTests {
    private static final class Settings implements AutoCloseable {
        final double period=FixtureConfig.CALCULATION_PERIOD_SECONDS.get(),percent=FixtureConfig.PERCENT.get(),threshold=FixtureConfig.SOUND_INTERVAL_PERCENT.get(),volume=FixtureConfig.SOUND_VOLUME.get(),pitch=FixtureConfig.SOUND_PITCH.get();
        final boolean enabled=FixtureConfig.SOUND.get(),particles=FixtureConfig.PARTICLES.get(),unloaded=FixtureConfig.UNLOADED.get();
        Settings() {
            FixtureConfig.CALCULATION_PERIOD_SECONDS.set(10.0);FixtureConfig.PERCENT.set(.2);
            FixtureConfig.SOUND_INTERVAL_PERCENT.set(1.0);FixtureConfig.SOUND_VOLUME.set(.15);FixtureConfig.SOUND_PITCH.set(1.7);
            FixtureConfig.SOUND.set(true);FixtureConfig.PARTICLES.set(false);FixtureConfig.UNLOADED.set(true);
        }
        public void close() {
            FixtureConfig.CALCULATION_PERIOD_SECONDS.set(period);FixtureConfig.PERCENT.set(percent);
            FixtureConfig.SOUND_INTERVAL_PERCENT.set(threshold);FixtureConfig.SOUND_VOLUME.set(volume);FixtureConfig.SOUND_PITCH.set(pitch);
            FixtureConfig.SOUND.set(enabled);FixtureConfig.PARTICLES.set(particles);FixtureConfig.UNLOADED.set(unloaded);
        }
    }
    private static FixtureBlockEntity fixture(GameTestHelper h) {
        var pos=new BlockPos(1,2,1);var block=RacksNStands.FIXTURES.get("tool_rack").get();h.setBlock(pos,block);
        var f=(FixtureBlockEntity)h.getBlockEntity(pos);var host=new ItemStack(block);
        host.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(RacksNStands.id("repairing")).orElseThrow(),1);
        f.initializeFrom(host);f.insert(0,item(),false);return f;
    }
    private static ItemStack item() {
        var stack=new ItemStack(Items.DIAMOND_PICKAXE);stack.set(DataComponents.MAX_DAMAGE,1000);stack.setDamageValue(900);return stack;
    }
    private static void advance(FixtureBlockEntity f,long ticks) {
        var level=f.getLevel();((ServerLevelData)level.getLevelData()).setGameTime(level.getGameTime()+ticks);f.scheduledRepair();
    }
    private static Consumer<PlayLevelSoundEvent.AtPosition> capture(FixtureBlockEntity f,List<PlayLevelSoundEvent.AtPosition> sounds) {
        Consumer<PlayLevelSoundEvent.AtPosition> listener=e -> {
            if(e.getLevel()==f.getLevel()&&BlockPos.containing(e.getPosition()).equals(f.getBlockPos())&&e.getSound()!=null&&e.getSound().value()==SoundEvents.ENCHANTMENT_TABLE_USE) sounds.add(e);
        };
        NeoForge.EVENT_BUS.addListener(listener);return listener;
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void percentMilestonesUseLiveSoundVolumePitchAndToggle(GameTestHelper h) {
        long clock=h.getLevel().getGameTime();FixtureBlockEntity f=null;Consumer<PlayLevelSoundEvent.AtPosition> listener=null;
        try(var settings=new Settings()) {
            f=fixture(h);var sounds=new ArrayList<PlayLevelSoundEvent.AtPosition>();listener=capture(f,sounds);
            advance(f,9);h.assertTrue(sounds.isEmpty(),"No sound before 1% of maximum durability is restored");
            advance(f,1);h.assertTrue(sounds.size()==1,"Exactly one sound at 1%, even with particles disabled");
            h.assertTrue(Math.abs(sounds.getFirst().getOriginalVolume()-.15f)<1e-6&&Math.abs(sounds.getFirst().getOriginalPitch()-1.7f)<1e-6,"Existing default volume and pitch are preserved");
            advance(f,1);FixtureConfig.SOUND_INTERVAL_PERCENT.set(2.0);advance(f,18);
            h.assertTrue(sounds.size()==1,"The new 2% threshold takes effect immediately");
            FixtureConfig.SOUND_VOLUME.set(.35);FixtureConfig.SOUND_PITCH.set(.75);advance(f,1);
            h.assertTrue(sounds.size()==2&&Math.abs(sounds.getLast().getOriginalVolume()-.35f)<1e-6&&Math.abs(sounds.getLast().getOriginalPitch()-.75f)<1e-6,"Live volume and pitch reach the real server sound event");
            FixtureConfig.SOUND.set(false);advance(f,20);FixtureConfig.SOUND.set(true);advance(f,1);
            h.assertTrue(sounds.size()==2,"Disabled sounds do not replay as a backlog when enabled");
            FixtureConfig.SOUND_VOLUME.set(0.0);advance(f,19);h.assertTrue(sounds.size()==2,"Zero volume suppresses sound dispatch");
            FixtureConfig.SOUND_VOLUME.set(.35);advance(f,20);h.assertTrue(sounds.size()==3,"Repair sound resumes at the next milestone");
        } finally {
            if(listener!=null) NeoForge.EVENT_BUS.unregister(listener);if(f!=null) f.setRemoved();((ServerLevelData)h.getLevel().getLevelData()).setGameTime(clock);
        }
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void savedSoundProgressAndCatchUpEmitAtMostOneSound(GameTestHelper h) {
        long clock=h.getLevel().getGameTime();FixtureBlockEntity f=null,copy=null;Consumer<PlayLevelSoundEvent.AtPosition> listener=null;
        try(var settings=new Settings()) {
            f=fixture(h);f.insert(1,item(),false);advance(f,9);
            var tag=f.saveWithoutMetadata(h.getLevel().registryAccess());h.assertTrue(Math.abs(tag.getList("RepairSoundProgress",Tag.TAG_DOUBLE).getDouble(0)-.9)<1e-7,"Partial sound milestone is persisted per slot");
            copy=new FixtureBlockEntity(f.getBlockPos(),f.getBlockState());copy.loadWithComponents(tag,h.getLevel().registryAccess());copy.setLevel(h.getLevel());
            var sounds=new ArrayList<PlayLevelSoundEvent.AtPosition>();listener=capture(copy,sounds);advance(copy,1);
            h.assertTrue(sounds.size()==1,"Two slots crossing the saved milestone share one stand sound");
            var saved=copy.saveWithoutMetadata(h.getLevel().registryAccess());copy.loadWithComponents(saved,h.getLevel().registryAccess());
            ((ServerLevelData)h.getLevel().getLevelData()).setGameTime(h.getLevel().getGameTime()+100);copy.onLoad();
            h.assertTrue(sounds.size()==2,"Ten catch-up milestones on two items emit one sound");
            copy.onLoad();h.assertTrue(sounds.size()==2,"Repeated loading cannot replay milestones");
        } finally {
            if(listener!=null) NeoForge.EVENT_BUS.unregister(listener);if(f!=null) f.setRemoved();if(copy!=null) copy.setRemoved();((ServerLevelData)h.getLevel().getLevelData()).setGameTime(clock);
        }
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void replacementGearCannotInheritSoundProgress(GameTestHelper h) {
        long clock=h.getLevel().getGameTime();FixtureBlockEntity f=null;Consumer<PlayLevelSoundEvent.AtPosition> listener=null;
        try(var settings=new Settings()) {
            f=fixture(h);advance(f,9);f.extract(0,1,false);f.insert(0,item(),false);
            var sounds=new ArrayList<PlayLevelSoundEvent.AtPosition>();listener=capture(f,sounds);advance(f,1);
            h.assertTrue(sounds.isEmpty(),"Replacement items do not inherit the old item's 0.9% sound progress");
            advance(f,9);h.assertTrue(sounds.size()==1,"Replacement earns its own full 1% milestone");
        } finally {
            if(listener!=null) NeoForge.EVENT_BUS.unregister(listener);if(f!=null) f.setRemoved();((ServerLevelData)h.getLevel().getLevelData()).setGameTime(clock);
        }
        h.succeed();
    }
}
