package com.cappleapple.racksnstands.gametest;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.config.FixtureConfig;
import com.cappleapple.racksnstands.interaction.FixtureInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(RacksNStands.MOD_ID) @PrefixGameTestTemplate(false)
public final class FullStackGameTests {
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void stacksConserveCountsAcrossConfigChangesAndAutomation(GameTestHelper h) {
        boolean old=FixtureConfig.FULL_STACKS.get(),automation=FixtureConfig.AUTOMATION.get();
        try {
            FixtureConfig.FULL_STACKS.set(false);FixtureConfig.AUTOMATION.set(true);
            var pos=new BlockPos(1,2,1);h.setBlock(pos,RacksNStands.FIXTURES.get("generic_tabletop_display").get());
            var f=(FixtureBlockEntity)h.getBlockEntity(pos);var handler=f.automation();
            h.assertTrue(handler.insertItem(0,new ItemStack(Items.APPLE,64),false).getCount()==63&&f.displayedStack(0).getCount()==1,"Default capacity is one");
            handler.extractItem(0,64,false);FixtureConfig.FULL_STACKS.set(true);
            h.assertTrue(handler.insertItem(0,new ItemStack(Items.APPLE,32),true).isEmpty()&&f.displayedStack(0).isEmpty(),"Insertion simulation has no side effects");
            handler.insertItem(0,new ItemStack(Items.APPLE,32),false);
            h.assertTrue(handler.insertItem(0,new ItemStack(Items.APPLE,40),false).getCount()==8&&f.displayedStack(0).getCount()==64,"Compatible automation stacks merge up to capacity");
            h.assertTrue(handler.insertItem(0,new ItemStack(Items.DIAMOND),false).getCount()==1,"Different items cannot merge");
            h.assertTrue(handler.extractItem(0,7,true).getCount()==7&&f.displayedStack(0).getCount()==64,"Extraction simulation preserves the stack");
            h.assertTrue(handler.extractItem(0,7,false).getCount()==7&&f.displayedStack(0).getCount()==57,"Partial extraction removes only the requested amount");
            FixtureConfig.FULL_STACKS.set(false);
            var saved=f.saveWithoutMetadata(h.getLevel().registryAccess());f.loadWithComponents(saved,h.getLevel().registryAccess());
            h.assertTrue(f.displayedStack(0).getCount()==57&&handler.insertItem(0,new ItemStack(Items.APPLE),false).getCount()==1,"Disabling full stacks preserves saved contents and prevents additions");
            h.assertTrue(handler.extractItem(0,64,false).getCount()==57,"Previously stored stacks remain fully retrievable");
            FixtureConfig.FULL_STACKS.set(true);
            h.assertTrue(handler.insertItem(0,new ItemStack(Items.ENDER_PEARL,32),false).getCount()==16&&f.displayedStack(0).getCount()==16,"Natural item stack limit still applies");
            handler.extractItem(0,64,false);
            var player=h.makeMockPlayer(GameType.SURVIVAL);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE,64));
            h.assertTrue(FixtureInteraction.swapHeld(f,0,player,InteractionHand.MAIN_HAND)&&player.getMainHandItem().isEmpty()&&f.displayedStack(0).getCount()==64,"Manual insertion transfers the full held stack");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND,27));
            h.assertTrue(FixtureInteraction.swapHeld(f,0,player,InteractionHand.MAIN_HAND)&&player.getMainHandItem().getCount()==64&&f.displayedStack(0).getCount()==27,"Occupied manual exchange swaps complete stacks");
            h.setBlock(pos,RacksNStands.FIXTURES.get("helmet_stand").get());f=(FixtureBlockEntity)h.getBlockEntity(pos);
            var helmets=new ItemStack(Items.DIAMOND_HELMET,3);helmets.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE,64);
            h.assertTrue(f.slotLimit(0)==1&&f.insert(0,helmets,false).getCount()==2&&f.displayedStack(0).getCount()==1,"Armor remains a single piece even with a stackable armor component");
            h.succeed();
        } finally { FixtureConfig.FULL_STACKS.set(old);FixtureConfig.AUTOMATION.set(automation); }
    }
}
