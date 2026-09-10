package com.cappleapple.racksnstands.gametest;

import com.cappleapple.racksnstands.RacksNStands;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(RacksNStands.MOD_ID) @PrefixGameTestTemplate(false)
public final class RecipeGameTests {
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void everyFixtureRecipeHasExactlyOneCraftingResult(GameTestHelper h) {
        var manager=h.getLevel().getRecipeManager();
        var recipes=manager.getAllRecipesFor(RecipeType.CRAFTING).stream().filter(r -> r.id().getNamespace().equals(RacksNStands.MOD_ID)).toList();
        h.assertTrue(recipes.size()==13,"All 13 active fixture recipes are registered");
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
            }
        }
        h.succeed();
    }
}
