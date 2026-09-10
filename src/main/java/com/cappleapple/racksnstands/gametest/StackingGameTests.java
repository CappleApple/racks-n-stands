package com.cappleapple.racksnstands.gametest;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.material.MaterialPalette;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(RacksNStands.MOD_ID) @PrefixGameTestTemplate(false)
public final class StackingGameTests {
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void furnitureStacksTo64AndPreservesComponentDifferences(GameTestHelper h) {
        var inventory=h.makeMockPlayer(GameType.SURVIVAL).getInventory();
        for(var block:RacksNStands.FIXTURES.values()) {
            inventory.clearContent();var first=new ItemStack(block.get(),32);var second=first.copy();
            h.assertTrue(first.getMaxStackSize()==64,"Every current and legacy fixture stacks to 64");
            inventory.add(first);inventory.add(second);
            h.assertTrue(inventory.getItem(0).getCount()==64&&inventory.getItem(1).isEmpty(),"Matching furniture combines into one stack");
        }
        var enchantment=h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(RacksNStands.id("repairing")).orElseThrow();
        var a=new ItemStack(RacksNStands.FIXTURES.get("sword_floor_stand").get(),32);a.enchant(enchantment,1);
        a.set(RacksNStands.MATERIALS.get(),MaterialPalette.EMPTY.with("dark",Blocks.BRICKS.defaultBlockState()));
        inventory.clearContent();inventory.add(a.copy());inventory.add(a.copy());
        h.assertTrue(inventory.getItem(0).getCount()==64,"Identically enchanted and textured furniture still stacks");
        inventory.clearContent();inventory.add(a.copy());var b=a.copy();b.enchant(enchantment,2);inventory.add(b);
        h.assertTrue(inventory.getItem(0).getCount()==32&&inventory.getItem(1).getCount()==32,"Different enchantments do not merge");
        inventory.clearContent();inventory.add(a.copy());b=a.copy();b.set(RacksNStands.MATERIALS.get(),MaterialPalette.EMPTY.with("dark",Blocks.GOLD_BLOCK.defaultBlockState()));inventory.add(b);
        h.assertTrue(inventory.getItem(0).getCount()==32&&inventory.getItem(1).getCount()==32,"Different sampled materials do not merge");
        h.succeed();
    }
}
