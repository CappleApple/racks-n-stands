package com.cappleapple.racksnstands.display.render;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** A scoped read-only equipment view for native armor-layer hooks. Never changes a player inventory or creates entities. */
public final class ArmorRenderContext implements AutoCloseable {
    private static final ThreadLocal<ArmorRenderContext> CURRENT=new ThreadLocal<>();
    private final ArmorRenderContext previous;
    private final Player player;
    private final EquipmentSlot slot;
    private final ItemStack stack;
    public ArmorRenderContext(Player player,EquipmentSlot slot,ItemStack stack) {
        previous=CURRENT.get();this.player=player;this.slot=slot;this.stack=stack;CURRENT.set(this);
    }
    public static ItemStack equipment(Player player,EquipmentSlot slot) {
        var context=CURRENT.get();
        return context!=null&&context.player==player?(context.slot==slot?context.stack:ItemStack.EMPTY):null;
    }
    public static Iterable<ItemStack> armor(Player player) {
        if(equipment(player,EquipmentSlot.HEAD)==null) return null;
        return List.of(equipment(player,EquipmentSlot.FEET),equipment(player,EquipmentSlot.LEGS),equipment(player,EquipmentSlot.CHEST),equipment(player,EquipmentSlot.HEAD));
    }
    @Override public void close() { if(previous==null) CURRENT.remove();else CURRENT.set(previous); }
}
