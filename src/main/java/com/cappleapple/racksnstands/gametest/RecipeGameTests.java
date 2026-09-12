package com.cappleapple.racksnstands.gametest;

import com.cappleapple.racksnstands.RacksNStands;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(RacksNStands.MOD_ID) @PrefixGameTestTemplate(false)
public final class RecipeGameTests {
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void everyFixtureRecipeHasExactlyOneCraftingResult(GameTestHelper h) {
        var manager=h.getLevel().getRecipeManager();
        var recipes=manager.getAllRecipesFor(RecipeType.CRAFTING).stream().filter(r -> r.id().getNamespace().equals(RacksNStands.MOD_ID)).toList();
        h.assertTrue(recipes.size()==15,"13 furniture recipes and two appearance recipes are registered");
        recipes=recipes.stream().filter(r -> r.value() instanceof ShapedRecipe).toList();
        h.assertTrue(recipes.size()==13,"All 13 furniture recipes remain distinct shaped recipes");
        for(var holder:recipes) {
            h.assertTrue(holder.value() instanceof ShapedRecipe,"Fixture recipes are shaped");
            var recipe=(ShapedRecipe)holder.value();int width=recipe.getWidth(),height=recipe.getHeight();
            for(boolean mirrored:new boolean[]{false,true}) for(int ox=0;ox<=3-width;ox++) for(int oy=0;oy<=3-height;oy++) {
                var grid=new ArrayList<>(Collections.nCopies(9,ItemStack.EMPTY));
                for(int y=0;y<height;y++) for(int x=0;x<width;x++) {
                    var ingredient=recipe.getIngredients().get(y*width+(mirrored?width-x-1:x));
                    if(!ingredient.isEmpty()) grid.set((y+oy)*3+x+ox,ingredient.getItems()[0].copyWithCount(1));
                }
                var input=CraftingInput.of(3,3,grid);var matches=manager.getRecipesFor(RecipeType.CRAFTING,input,h.getLevel());
                h.assertTrue(matches.size()==1&&matches.getFirst().id().equals(holder.id()),"Unique normal/mirrored crafting result for "+holder.id()+": "+matches.stream().map(RecipeHolder::id).toList());
                h.assertTrue(recipe.assemble(input,h.getLevel().registryAccess()).is(recipe.getResultItem(h.getLevel().registryAccess()).getItem()),"Crafting returns the declared fixture");
                // Exercise every loaded tag member at each position, including mixed materials.
                for(int y=0;y<height;y++) for(int x=0;x<width;x++) {
                    var ingredient=recipe.getIngredients().get(y*width+(mirrored?width-x-1:x));
                    int slot=(y+oy)*3+x+ox;var original=grid.get(slot);
                    for(var alternative:ingredient.getItems()) {
                        grid.set(slot,alternative.copyWithCount(1));
                        assertCrafts(h,holder.id().getPath(),grid);
                    }
                    grid.set(slot,original);
                }
            }
        }
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void taggedMaterialsCanBeMixedWithinRecipes(GameTestHelper h) {
        var centers=Map.of("armor_mannequin",Items.ARMOR_STAND,"helmet_stand",Items.CARVED_PUMPKIN,
            "chestplate_stand",Items.NETHERITE_CHESTPLATE,"leggings_stand",Items.DIAMOND_LEGGINGS,
            "boots_stand",Items.IRON_BOOTS,"generic_wall_display",Items.ITEM_FRAME,
            "generic_tabletop_display",Items.WARPED_SLAB,"curio_cabinet",Items.TRAPPED_CHEST);
        centers.forEach((id,center) -> assertCrafts(h,id,List.of(
            new ItemStack(Items.CRIMSON_PLANKS),ItemStack.EMPTY,new ItemStack(Items.BAMBOO_PLANKS),
            ItemStack.EMPTY,new ItemStack(center),ItemStack.EMPTY,
            new ItemStack(Items.CHERRY_PLANKS),new ItemStack(Items.STICK),new ItemStack(Items.WARPED_PLANKS))));
        assertCrafts(h,"generic_pedestal",List.of(
            new ItemStack(Items.MOSSY_STONE_BRICKS),ItemStack.EMPTY,new ItemStack(Items.CRACKED_STONE_BRICKS),
            ItemStack.EMPTY,new ItemStack(Items.IRON_INGOT),ItemStack.EMPTY,
            new ItemStack(Items.CHISELED_STONE_BRICKS),new ItemStack(Items.SMOOTH_STONE),new ItemStack(Items.STONE_BRICKS)));
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void recipeTagsDoNotIntroduceAmbiguousLayouts(GameTestHelper h) {
        var recipes=h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
            .filter(r -> r.id().getNamespace().equals(RacksNStands.MOD_ID)&&r.value() instanceof ShapedRecipe).toList();
        for(int a=0;a<recipes.size();a++) for(int b=a+1;b<recipes.size();b++)
            for(var left:layouts((ShapedRecipe)recipes.get(a).value())) for(var right:layouts((ShapedRecipe)recipes.get(b).value())) {
                boolean overlap=true;
                for(int slot=0;slot<9&&overlap;slot++) {
                    var x=left.get(slot);var y=right.get(slot);
                    overlap=x.isEmpty()?y.isEmpty():!y.isEmpty()&&Arrays.stream(x.getItems()).anyMatch(y::test);
                }
                h.assertTrue(!overlap,"No possible tagged inputs match both "+recipes.get(a).id()+" and "+recipes.get(b).id());
            }
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void recipeBookUnlocksWithAlternativeBuildingMaterials(GameTestHelper h) {
        for(var material:List.of(Items.BIRCH_PLANKS,Items.WARPED_PLANKS,Items.MOSSY_STONE_BRICKS)) {
            var player=TestPlayers.create(h);
            try {
                var stack=new ItemStack(material);player.getInventory().setItem(0,stack);
                CriteriaTriggers.INVENTORY_CHANGED.trigger(player,player.getInventory(),stack);
                for(var kind:com.cappleapple.racksnstands.registry.FixtureCatalog.ALL) if(kind.available()) {
                    var advancement=h.getLevel().getServer().getAdvancements().get(RacksNStands.id("recipes/"+kind.id()));
                    h.assertTrue(advancement!=null,"Recipe advancement loaded for "+kind.id());
                    boolean expected=kind.id().equals("generic_pedestal")== (material==Items.MOSSY_STONE_BRICKS);
                    h.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone()==expected,"Correct tagged unlock for "+kind.id()+" using "+material);
                }
            } finally { player.discard(); }
        }
        h.succeed();
    }
    private static void assertCrafts(GameTestHelper h,String id,List<ItemStack> grid) {
        var input=CraftingInput.of(3,3,grid);
        var matches=h.getLevel().getRecipeManager().getRecipesFor(RecipeType.CRAFTING,input,h.getLevel());
        h.assertTrue(matches.size()==1&&matches.getFirst().id().equals(RacksNStands.id(id)),"Unique result for "+id+" using "+grid+": "+matches.stream().map(RecipeHolder::id).toList());
    }
    private static List<List<Ingredient>> layouts(ShapedRecipe recipe) {
        List<List<Ingredient>> layouts=new ArrayList<>();int width=recipe.getWidth(),height=recipe.getHeight();
        for(boolean mirror:new boolean[]{false,true}) for(int ox=0;ox<=3-width;ox++) for(int oy=0;oy<=3-height;oy++) {
            var grid=new ArrayList<>(Collections.nCopies(9,Ingredient.EMPTY));
            for(int y=0;y<height;y++) for(int x=0;x<width;x++)
                grid.set((y+oy)*3+x+ox,recipe.getIngredients().get(y*width+(mirror?width-x-1:x)));
            layouts.add(grid);
        }
        return layouts;
    }

}
