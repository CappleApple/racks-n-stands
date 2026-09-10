package com.cappleapple.racksnstands.display;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.api.*;
import com.cappleapple.racksnstands.compat.curios.CuriosBridge;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Classification {
    private static final Set<Class<?>> WARNED=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final List<DisplayCategoryProvider> PROVIDERS = new CopyOnWriteArrayList<>();
    public static void register(DisplayCategoryProvider provider) { PROVIDERS.add(Objects.requireNonNull(provider)); }
    public static boolean tagged(ItemStack stack, ResourceLocation id) { return stack.is(TagKey.create(Registries.ITEM, id)); }
    public static Set<ResourceLocation> categories(ItemStack stack, Level level) {
        Set<ResourceLocation> out = new HashSet<>();
        for (String name : List.of("tools","swords","weapons","bows","shields","staffs","polearms","helmets","chestplates","leggings","boots"))
            if (tagged(stack, RacksNStands.id(name))) out.add(RacksNStands.id(name));
        if (stack.has(net.minecraft.core.component.DataComponents.TOOL) || stack.canPerformAction(ItemAbilities.PICKAXE_DIG)
            || stack.canPerformAction(ItemAbilities.AXE_DIG) || stack.canPerformAction(ItemAbilities.SHOVEL_DIG) || stack.canPerformAction(ItemAbilities.HOE_DIG)) out.add(RacksNStands.id("tools"));
        if (stack.canPerformAction(ItemAbilities.SWORD_SWEEP)) { out.add(RacksNStands.id("swords")); out.add(RacksNStands.id("weapons")); }
        if (stack.getUseAnimation() == UseAnim.BOW || stack.getUseAnimation() == UseAnim.CROSSBOW) out.add(RacksNStands.id("bows"));
        if (stack.canPerformAction(ItemAbilities.SHIELD_BLOCK)) out.add(RacksNStands.id("shields"));
        Profiles.categories(level.isClientSide()).forEach((id, filter) -> { if (matchesPositive(stack, level, filter, Set.of())) out.add(id); });
        for (var provider : PROVIDERS) {
            try { provider.classify(stack,level,out); }
            catch(RuntimeException e) { if(WARNED.add(provider.getClass())) RacksNStands.LOGGER.warn("Classification provider {} failed: {}",provider.getClass().getName(),e.toString()); }
        }
        return Set.copyOf(out);
    }
    public static boolean accepts(DisplaySlotDefinition slot, ItemStack stack, Level level) {
        if (stack.isEmpty()) return true;
        if (slot.equipment().isPresent()) {
            EquipmentSlot target = slot.equipment().get();
            EquipmentSlot actual = stack.getEquipmentSlot();
            if (actual == null && Equipable.get(stack) != null) actual = Equipable.get(stack).getEquipmentSlot();
            if (actual != target) return false;
        }
        return matchesPositive(stack, level, slot.filter(), categories(stack, level));
    }
    private static boolean matchesPositive(ItemStack stack, Level level, DisplayFilter f, Set<ResourceLocation> categories) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (tagged(stack, RacksNStands.id("display_denied")) || f.deniedItems().contains(id) || f.deniedTags().stream().anyMatch(t -> tagged(stack,t))) return false;
        if (f.predicate().isPresent() && !f.predicate().get().test(stack)) return false;
        boolean constrained = !f.categories().isEmpty() || !f.acceptedTags().isEmpty() || !f.allowedItems().isEmpty() || !f.curioSlots().isEmpty() || f.anyCurio();
        if (!constrained || f.allowedItems().contains(id) || f.acceptedTags().stream().anyMatch(t -> tagged(stack,t)) || f.categories().stream().anyMatch(categories::contains)) return true;
        if (f.anyCurio() || !f.curioSlots().isEmpty()) {
            var compatible = CuriosBridge.slots(stack, level);
            return f.anyCurio() ? !compatible.isEmpty() : f.curioSlots().stream().anyMatch(compatible::contains);
        }
        return false;
    }
}
