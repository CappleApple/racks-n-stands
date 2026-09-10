package com.cappleapple.racksnstands.api;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;
@FunctionalInterface
public interface DisplayCategoryProvider { void classify(ItemStack stack, Level level, Set<ResourceLocation> categories); }
