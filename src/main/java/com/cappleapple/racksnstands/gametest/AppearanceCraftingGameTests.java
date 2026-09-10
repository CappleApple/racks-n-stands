package com.cappleapple.racksnstands.gametest;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.crafting.AppearanceRecipe;
import com.cappleapple.racksnstands.material.MaterialPalette;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(RacksNStands.MOD_ID) @PrefixGameTestTemplate(false)
public final class AppearanceCraftingGameTests {
    private static ItemStack furniture(GameTestHelper h,String id,int count,int enchantment,boolean customized) {
        var stack=new ItemStack(RacksNStands.FIXTURES.get(id).get(),count);
        if(enchantment>0) stack.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(RacksNStands.id("repairing")).orElseThrow(),enchantment);
        stack.set(DataComponents.CUSTOM_NAME,Component.literal(customized?"Original template":"Target stack"));
        if(customized) stack.set(RacksNStands.MATERIALS.get(),MaterialPalette.EMPTY.with("wood",Blocks.GOLD_BLOCK.defaultBlockState()).with("dark",Blocks.BRICKS.defaultBlockState()));
        return stack;
    }
    private static CraftingRecipe recipe(GameTestHelper h,CraftingInput input) {
        var recipes=h.getLevel().getRecipeManager().getRecipesFor(RecipeType.CRAFTING,input,h.getLevel());
        h.assertTrue(recipes.size()==1,"Exactly one recipe matches appearance operation");return recipes.getFirst().value();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void resettingAllFixturesPreservesEnchantmentsAndCounts(GameTestHelper h) {
        for(String id:RacksNStands.FIXTURES.keySet()) for(int count:new int[]{1,17,64}) {
            var original=furniture(h,id,count,3,true);var before=original.copy();var expected=original.copy();expected.remove(RacksNStands.MATERIALS.get());
            var input=CraftingInput.of(2,2,List.of(ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY,original));
            var recipe=recipe(h,input);
            h.assertTrue(ItemStack.matches(recipe.assemble(input,h.getLevel().registryAccess()),expected),"Only sampled appearance is reset: "+id);
            h.assertTrue(ItemStack.matches(original,before),"Preview leaves the source intact");
            h.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),"Reset returns no extra items");
        }
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void copyingAllFixturesPreservesTemplateAndTargetComponents(GameTestHelper h) {
        for(String id:RacksNStands.FIXTURES.keySet()) for(int count:new int[]{1,17,64}) for(boolean enchanted:new boolean[]{false,true}) {
            var template=furniture(h,id,1,5,true);var blank=furniture(h,id,count,enchanted?2:0,false);
            var original=template.copy();var expected=blank.copy();expected.set(RacksNStands.MATERIALS.get(),template.get(RacksNStands.MATERIALS.get()));
            var input=CraftingInput.of(2,1,List.of(template,blank));var recipe=recipe(h,input);
            h.assertTrue(ItemStack.matches(expected,recipe.assemble(input,h.getLevel().registryAccess())),"Copy keeps target count, name and enchantments: "+id);
            h.assertTrue(ItemStack.matches(original,template)&&ItemStack.matches(original,recipe.getRemainingItems(input).getFirst()),"Template is returned unchanged");
            h.assertTrue(!blank.has(RacksNStands.MATERIALS.get()),"Preview does not mutate blank inputs");
        }
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void invalidAppearanceInputsNeverMatch(GameTestHelper h) {
        var template=furniture(h,"tool_rack",1,5,true);var blank=furniture(h,"tool_rack",1,0,false);
        for(var list:List.of(List.of(blank),List.of(blank,blank.copy()),List.of(template,template.copy()),
            List.of(template,furniture(h,"weapon_rack",1,0,false)),List.of(template,blank,new ItemStack(Items.DIAMOND)),List.of(template,blank.copyWithCount(65)))) {
            var input=CraftingInput.of(list.size(),1,list);
            h.assertTrue(h.getLevel().getRecipeManager().getRecipesFor(RecipeType.CRAFTING,input,h.getLevel()).stream().noneMatch(r -> r.value() instanceof AppearanceRecipe),"Invalid/ambiguous appearance grid is rejected");
        }
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty",timeoutTicks=100)
    public static void manualGridsConsumeWholeStacksAndKeepReusableTemplate(GameTestHelper h) {
        var player=TestPlayers.create(h);
        try {
            for(int cells:new int[]{4,9}) for(int count:new int[]{1,17,64}) for(int click:new int[]{0,1,2}) {
                player.getInventory().clearContent();
                AbstractContainerMenu menu=cells==4?new InventoryMenu(player.getInventory(),true,player):new CraftingMenu(1,player.getInventory(),ContainerLevelAccess.create(h.getLevel(),h.absolutePos(BlockPos.ZERO)));
                player.containerMenu=menu;
                var template=furniture(h,"tool_rack",3,5,true);var blank=furniture(h,"tool_rack",count,2,false);
                var expected=blank.copy();expected.set(RacksNStands.MATERIALS.get(),template.get(RacksNStands.MATERIALS.get()));
                menu.getSlot(cells).set(template.copy());menu.getSlot(cells-1).set(blank.copy());
                h.assertTrue(ItemStack.matches(menu.getSlot(0).getItem(),expected),"Result preview contains the whole batch");
                menu.clicked(0,click==1?1:0,click==2?ClickType.QUICK_MOVE:ClickType.PICKUP,player);
                h.assertTrue(menu.getSlot(cells-1).getItem().isEmpty(),"Every blank input was consumed exactly once");
                h.assertTrue(ItemStack.matches(menu.getSlot(cells).getItem(),template),"Click and shift-click retain all original templates");
                int total=menu.getCarried().isEmpty()?0:menu.getCarried().getCount();
                for(var stack:player.getInventory().items) if(!stack.isEmpty()) {
                    h.assertTrue(ItemStack.isSameItemSameComponents(stack,expected),"No template enchantments copied into inventory");total+=stack.getCount();
                }
                h.assertTrue(total==count,"Crafting conserves the complete batch for click mode "+click);
                if(click!=2) h.assertTrue(ItemStack.matches(menu.getCarried(),expected),"Cursor receives exact component-preserving output");
            }
        } finally { player.discard(); }
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty",timeoutTicks=100)
    public static void shiftClickResetConsumesOnlyTheCustomizedStack(GameTestHelper h) {
        var player=TestPlayers.create(h);
        try {
            var menu=player.inventoryMenu;player.containerMenu=menu;
            var original=furniture(h,"generic_pedestal",64,7,true);var expected=original.copy();expected.remove(RacksNStands.MATERIALS.get());
            menu.getSlot(4).set(original);menu.clicked(0,0,ClickType.QUICK_MOVE,player);
            h.assertTrue(menu.getSlot(4).getItem().isEmpty()&&menu.getSlot(0).getItem().isEmpty(),"Reset consumes its entire source stack");
            int total=0;for(var item:player.getInventory().items) if(!item.isEmpty()) { h.assertTrue(ItemStack.isSameItemSameComponents(item,expected),"Reset preserves other components");total+=item.getCount(); }
            h.assertTrue(total==64,"Exactly 64 reset fixtures are delivered");
        } finally { player.discard(); }
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty",timeoutTicks=100)
    public static void partialInventoryShiftClickKeepsEveryOutputAndTemplate(GameTestHelper h) {
        var player=TestPlayers.create(h);
        try {
            var template=furniture(h,"tool_rack",1,5,true);var blank=furniture(h,"tool_rack",64,2,false);
            var expected=blank.copy();expected.set(RacksNStands.MATERIALS.get(),template.get(RacksNStands.MATERIALS.get()));
            for(int slot=0;slot<36;slot++) player.getInventory().setItem(slot,new ItemStack(Items.STONE,64));
            player.getInventory().setItem(0,expected.copyWithCount(59));
            var menu=player.inventoryMenu;player.containerMenu=menu;
            menu.getSlot(3).set(template.copy());menu.getSlot(4).set(blank);
            menu.clicked(0,0,ClickType.QUICK_MOVE,player);
            h.assertTrue(ItemStack.matches(menu.getSlot(3).getItem(),template)&&menu.getSlot(4).getItem().isEmpty(),"Partial insertion still consumes the batch once and retains its template");
            int produced=player.getInventory().getItem(0).getCount()-59;
            for(var entity:h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(3)))
                if(ItemStack.isSameItemSameComponents(entity.getItem(),expected)) produced+=entity.getItem().getCount();
            h.assertTrue(produced==64,"Inventory plus overflow drops contain all 64 crafted fixtures");
        } finally { player.discard(); }
        h.succeed();
    }

    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty",timeoutTicks=100)
    public static void automatedCrafterConsumesBatchAndReturnsTemplate(GameTestHelper h) {
        var pos=new BlockPos(1,2,1);h.setBlock(pos,Blocks.CRAFTER);
        var crafter=(net.minecraft.world.level.block.entity.CrafterBlockEntity)h.getBlockEntity(pos);
        var template=furniture(h,"tool_rack",1,5,true);var blank=furniture(h,"tool_rack",64,2,false);
        var expected=blank.copy();expected.set(RacksNStands.MATERIALS.get(),template.get(RacksNStands.MATERIALS.get()));
        crafter.setItem(7,template.copy());crafter.setItem(8,blank.copy());
        var absolute=h.absolutePos(pos);var state=h.getLevel().getBlockState(absolute);
        state.tick(h.getLevel(),absolute,h.getLevel().random);
        h.assertTrue(crafter.getItems().stream().allMatch(ItemStack::isEmpty),"Crafter consumes the whole target and returns the template through output");
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(absolute).inflate(3));
        int copied=0,templates=0;for(var entity:drops) {
            var item=entity.getItem();if(ItemStack.isSameItemSameComponents(item,expected)) copied+=item.getCount();
            if(ItemStack.isSameItemSameComponents(item,template)) templates+=item.getCount();
        }
        h.assertTrue(copied==64&&templates==1,"Automated output conserves 64 copies and one unchanged template");h.succeed();
    }
}
