package com.cappleapple.racksnstands.interaction;
import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.api.DisplayProfile;
import com.cappleapple.racksnstands.block.FixtureBlock;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.config.FixtureConfig;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

public final class FixtureInteraction {
    @FunctionalInterface public interface Behavior { boolean use(FixtureBlockEntity fixture,Player player,InteractionHand hand,BlockHitResult hit); }
    private static final Map<ResourceLocation,Behavior> BEHAVIORS=new ConcurrentHashMap<>();
    public static void registerBehavior(ResourceLocation id,Behavior behavior) { if(BEHAVIORS.putIfAbsent(id,behavior)!=null) throw new IllegalArgumentException("Duplicate behavior "+id); }
    public static Vec3 local(Vec3 hit,Direction facing) {
        double a=Math.toRadians(180-facing.toYRot()),c=Math.cos(a),s=Math.sin(a),x=hit.x-.5,z=hit.z-.5;
        return new Vec3(c*x-s*z+.5,hit.y,s*x+c*z+.5);
    }
    public static int slotAt(DisplayProfile profile,Vec3 hit) {
        for(var slot:profile.slots()) if(slot.bounds().inflate(.002).contains(hit)) return slot.index();
        int closest=0;double distance=Double.MAX_VALUE;
        for(var slot:profile.slots()) {
            var box=slot.bounds();double dx=Math.max(Math.max(box.minX-hit.x,0),hit.x-box.maxX),dy=Math.max(Math.max(box.minY-hit.y,0),hit.y-box.maxY);
            double d=dx*dx+dy*dy;
            if(d<distance) { distance=d;closest=slot.index(); }
        }
        return closest;
    }
    public static void use(FixtureBlockEntity fixture,Player player,InteractionHand hand,BlockHitResult supplied) {
        if(player.isSpectator()||!player.mayBuild()||fixture.getLevel()==null||fixture.getLevel().isClientSide) return;
        // Resolve targeting from the server player's eye and look, not a client-supplied slot index.
        var ray=player.pick(player.blockInteractionRange(),1,false);
        if(!(ray instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK) return;
        var state=fixture.getLevel().getBlockState(hit.getBlockPos());
        if(!(state.getBlock() instanceof FixtureBlock)||!FixtureBlock.base(hit.getBlockPos(),state).equals(fixture.getBlockPos())) return;
        Behavior custom=BEHAVIORS.get(fixture.profile().behavior());
        if(custom!=null&&custom.use(fixture,player,hand,hit)) return;
        var localHit=local(hit.getLocation().subtract(Vec3.atLowerCornerOf(fixture.getBlockPos())),state.getValue(FixtureBlock.FACING));
        if(state.hasProperty(com.cappleapple.racksnstands.block.SurfaceDisplayBlock.FACE)) localHit=com.cappleapple.racksnstands.block.SurfaceDisplayBlock.unmount(state.getValue(com.cappleapple.racksnstands.block.SurfaceDisplayBlock.FACE),localHit);
        if(player.isShiftKeyDown()&&com.cappleapple.racksnstands.material.MaterialCustomization.apply(fixture,player,hand,localHit)) return;
        int slot=slotAt(fixture.profile(),localHit);
        if(player.isShiftKeyDown()) {
            if(hand!=InteractionHand.MAIN_HAND) return;
            if(player.getMainHandItem().isEmpty()&&!com.cappleapple.racksnstands.compat.curios.CuriosBridge.slots(fixture.displayedStack(slot),fixture.getLevel()).isEmpty()) {
                com.cappleapple.racksnstands.compat.curios.CuriosBridge.swap(fixture,slot,player);
            } else if(fixture.profile().slots().stream().allMatch(s -> s.equipment().isPresent())) {
                if(FixtureConfig.SWAP.get()) ArmorSwap.swap(fixture,player);
            } else swapEquipped(fixture,slot,player);
        } else swapHeld(fixture,slot,player,hand);

    }
    public static boolean swapHeld(FixtureBlockEntity fixture,int slot,Player player,InteractionHand hand) {
        if(fixture.slotCount()>1&&fixture.profile().slots().stream().allMatch(s -> s.equipment().isPresent())) {
            var held=player.getItemInHand(hand);var equipment=held.getEquipmentSlot();
            if(equipment==null&&net.minecraft.world.item.Equipable.get(held)!=null) equipment=net.minecraft.world.item.Equipable.get(held).getEquipmentSlot();
            if(equipment!=null) for(var bound:fixture.profile().slots()) if(bound.equipment().orElseThrow()==equipment) { slot=bound.index();break; }
        }
        return exchange(fixture,slot,player,hand==InteractionHand.MAIN_HAND?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND);
    }
    public static boolean swapEquipped(FixtureBlockEntity fixture,int slot,Player player) {
        if(player.getMainHandItem().isEmpty()&&!com.cappleapple.racksnstands.compat.curios.CuriosBridge.slots(fixture.displayedStack(slot),fixture.getLevel()).isEmpty())
            return com.cappleapple.racksnstands.compat.curios.CuriosBridge.swap(fixture,slot,player);
        var candidate=fixture.displayedStack(slot).isEmpty()?player.getMainHandItem():fixture.displayedStack(slot);
        var target=candidate.getEquipmentSlot();
        if(target==null && net.minecraft.world.item.Equipable.get(candidate)!=null) target=net.minecraft.world.item.Equipable.get(candidate).getEquipmentSlot();
        if(target==null||target==net.minecraft.world.entity.EquipmentSlot.BODY) target=net.minecraft.world.entity.EquipmentSlot.MAINHAND;
        return exchange(fixture,slot,player,target);
    }
    private static boolean exchange(FixtureBlockEntity fixture,int slot,Player player,net.minecraft.world.entity.EquipmentSlot target) {
        if(player.isSpectator()||!player.mayBuild()||!fixture.beginTransaction()) return false;
        try {
            fixture.settleForInteraction();
            var held=player.getItemBySlot(target).copy();var stored=fixture.displayedStack(slot).copy();
            if(held.isEmpty()&&stored.isEmpty()) return false;
            int count=held.isEmpty()?0:Math.min(held.getCount(),fixture.itemLimit(slot,held));
            var incoming=held.copyWithCount(count);
            if(!fixture.accepts(slot,incoming)) return false;
            if(target.getType()==net.minecraft.world.entity.EquipmentSlot.Type.HUMANOID_ARMOR) {
                if(held.getCount()>1||stored.getCount()>1||!stored.isEmpty()&&!stored.canEquip(target,player)) return false;
                if(!player.isCreative()&&net.minecraft.world.item.enchantment.EnchantmentHelper.has(held,net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) return false;
            }
            if(!incoming.isEmpty()&&net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.cappleapple.racksnstands.event.FixtureItemInsertEvent(fixture,slot,incoming)).isCanceled()) return false;
            if(!stored.isEmpty()&&net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.cappleapple.racksnstands.event.FixtureItemExtractEvent(fixture,slot,stored)).isCanceled()) return false;
            if(fixture.isRemoved()||!net.minecraft.world.item.ItemStack.matches(player.getItemBySlot(target),held)
                    ||!net.minecraft.world.item.ItemStack.matches(fixture.displayedStack(slot),stored)
                    ||!net.minecraft.world.item.ItemStack.matches(incoming,held.copyWithCount(count))) return false;
            fixture.commitSlot(slot,incoming);
            var remainder=held.copyWithCount(Math.max(0,held.getCount()-count));
            // Empty slots consume the configured amount. Occupied slots put the old display
            // in hand, returning any held-stack remainder to inventory without loss.
            player.setItemSlot(target,stored.isEmpty()?remainder:stored);
            if(!stored.isEmpty()&&!remainder.isEmpty()&&!player.getInventory().add(remainder)) player.drop(remainder,false);
            player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();player.containerMenu.broadcastChanges();
            return true;
        } finally { fixture.endTransaction(); }
    }

}
