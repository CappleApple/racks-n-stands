package com.cappleapple.racksnstands.block;
import net.minecraft.world.item.*;
/** Enchantments are real ItemEnchantments components, including anvil/book support. */
public final class FixtureItem extends BlockItem {
    public FixtureItem(FixtureBlock block,Properties properties) { super(block,properties); }
    @Override public int getEnchantmentValue() { return 1; }
    @Override public boolean isEnchantable(ItemStack stack) { return !stack.isEnchanted(); }
}
