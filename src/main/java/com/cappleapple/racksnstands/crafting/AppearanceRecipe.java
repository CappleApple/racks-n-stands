package com.cappleapple.racksnstands.crafting;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.block.FixtureItem;
import com.cappleapple.racksnstands.material.MaterialPalette;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Component-only appearance operations. Matching and previews never mutate the input grid. */
public final class AppearanceRecipe extends CustomRecipe {
    private final boolean copy;
    public AppearanceRecipe(CraftingBookCategory category,boolean copy) { super(category);this.copy=copy; }
    public record Plan(int target,int template,ItemStack result) {}
    public Plan plan(CraftingInput input) {
        int customized=-1,plain=-1;
        for(int i=0;i<input.size();i++) {
            var stack=input.getItem(i);if(stack.isEmpty()) continue;
            if(!(stack.getItem() instanceof FixtureItem)||stack.getCount()>stack.getMaxStackSize()) return null;
            if(stack.getOrDefault(RacksNStands.MATERIALS.get(),MaterialPalette.EMPTY).materials().isEmpty()) {
                if(plain>=0) return null;plain=i;
            } else {
                if(customized>=0) return null;customized=i;
            }
        }
        if(customized<0) return null;
        if(copy) {
            if(plain<0||input.getItem(plain).getItem()!=input.getItem(customized).getItem()) return null;
            var output=input.getItem(plain).copy();
            output.set(RacksNStands.MATERIALS.get(),input.getItem(customized).get(RacksNStands.MATERIALS.get()));
            return new Plan(plain,customized,output);
        }
        if(plain>=0) return null;
        var output=input.getItem(customized).copy();output.remove(RacksNStands.MATERIALS.get());
        return new Plan(customized,-1,output);
    }
    public static Plan selectedPlan(CraftingInput input,Level level) {
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElse(null);
        return recipe!=null&&recipe.value() instanceof AppearanceRecipe appearance?appearance.plan(input):null;
    }
    @Override public boolean matches(CraftingInput input,Level level) { return plan(input)!=null; }
    @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider registries) {
        var plan=plan(input);return plan==null?ItemStack.EMPTY:plan.result();
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var result=NonNullList.withSize(input.size(),ItemStack.EMPTY);var plan=plan(input);
        if(plan!=null&&plan.template()>=0) result.set(plan.template(),input.getItem(plan.template()).copyWithCount(1));
        return result;
    }
    @Override public boolean canCraftInDimensions(int width,int height) { return width*height>=(copy?2:1); }
    @Override public RecipeSerializer<?> getSerializer() { return (copy?RacksNStands.COPY_APPEARANCE:RacksNStands.RESET_APPEARANCE).get(); }
}
