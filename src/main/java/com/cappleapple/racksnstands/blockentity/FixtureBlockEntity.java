package com.cappleapple.racksnstands.blockentity;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.material.MaterialPalette;
import com.cappleapple.racksnstands.api.*;
import com.cappleapple.racksnstands.block.FixtureBlock;
import com.cappleapple.racksnstands.config.FixtureConfig;
import com.cappleapple.racksnstands.display.*;
import com.cappleapple.racksnstands.event.*;
import com.cappleapple.racksnstands.repairing.RepairMath;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.*;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.*;

public final class FixtureBlockEntity extends BlockEntity implements DisplayFixture,RepairingHost,Clearable {
    private final ItemStack[] items;
    private volatile MaterialPalette materials=MaterialPalette.EMPTY;
    public MaterialPalette materials() { return materials; }
    public boolean setMaterial(String group,BlockState sample) {
        if(!MaterialPalette.GROUPS.contains(group)||sample.isAir()||!com.cappleapple.racksnstands.block.FixtureGeometry.parts(((FixtureBlock)getBlockState().getBlock()).kind()).stream().anyMatch(p -> p.texture().equals(group))||!beginTransaction()) return false;
        try {
            var next=materials.with(group,sample);
            if(next.equals(materials)) return false;
            materials=next;visibleChanged();return true;
        } finally { endTransaction(); }
    }
    private void refreshMaterials() {
        if(level==null||!level.isClientSide) return;
        requestModelDataUpdate();
        level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_ALL);
        if(((FixtureBlock)getBlockState().getBlock()).kind().tall()) {
            var above=worldPosition.above();var state=level.getBlockState(above);
            level.sendBlockUpdated(above,state,state,Block.UPDATE_ALL);
        }
    }
    public void configChanged() {
        if(!beginTransaction()) return;
        try { settle(level.getGameTime());refreshRepairRates();visibleChanged();
            if(level instanceof ServerLevel server) server.getBlockTicks().clearArea(new net.minecraft.world.level.levelgen.structure.BoundingBox(worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),worldPosition.getX(),worldPosition.getY(),worldPosition.getZ()));
            schedule(); } finally { endTransaction(); }
    }
    @Override public void setRemoved() { com.cappleapple.racksnstands.config.LiveConfig.untrack(this);super.setRemoved(); }
    private net.minecraft.world.phys.AABB renderBox;
    private DisplayProfile renderProfile;
    private BlockState renderState;
    private DisplayTransform[] resolvedTransforms;
    private Profiles.Snapshot transformSnapshot;
    private int transformRevision=-1;
    private final double[] credit,repairRates,soundProgress;
    private long savedAt=-1;
    private long[] legacyCredit;
    private int repairCadence=20;
    private long lastRepairTime=-1;
    private ItemEnchantments enchantments=ItemEnchantments.EMPTY;
    private boolean transaction, dropped;
    private int revision;
    private final IItemHandler automation=new IItemHandler() {
        public int getSlots() { return slotCount(); }
        public ItemStack getStackInSlot(int slot) { return displayedStack(slot); }
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) { return FixtureConfig.AUTOMATION.get()?insert(slot,stack,simulate):stack; }
        public ItemStack extractItem(int slot,int amount,boolean simulate) { return FixtureConfig.AUTOMATION.get()?extract(slot,amount,simulate):ItemStack.EMPTY; }
        public int getSlotLimit(int slot) { return slotLimit(slot); }
        public boolean isItemValid(int slot,ItemStack stack) { return FixtureConfig.AUTOMATION.get()&&accepts(slot,stack); }
    };
    public FixtureBlockEntity(BlockPos pos,BlockState state) {
        this(RacksNStands.FIXTURE_ENTITY.get(),pos,state);
    }
    public FixtureBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state) {
        super(type,pos,state);
        renderBox=new net.minecraft.world.phys.AABB(pos).inflate(1).expandTowards(0,1,0);
        items=new ItemStack[((FixtureBlock)state.getBlock()).kind().slots()];Arrays.fill(items,ItemStack.EMPTY);credit=new double[items.length];repairRates=new double[items.length];soundProgress=new double[items.length];
    }
    @Override public ResourceLocation profileId() { return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()); }
    @Override public DisplayProfile profile() { return Profiles.get(profileId(),level!=null&&level.isClientSide); }
    @Override public ItemStack displayedStack(int slot) { return items[slot]; }
    @Override public int slotCount() { return items.length; }
    public long lastRepairTime() { return lastRepairTime; }
    public double credit(int slot) { return credit[slot]; }
    public com.cappleapple.racksnstands.repairing.RepairProgress repairProgress(int slot,long now) {
        var stack=items[slot];
        return com.cappleapple.racksnstands.repairing.RepairProgress.of(stack.getDamageValue(),stack.getMaxDamage(),credit[slot],repairRates[slot],RepairMath.elapsed(now,lastRepairTime));
    }
    public void settleForInteraction() { if(!transaction) throw new IllegalStateException("No transaction");settle(level.getGameTime()); }
    private void migrateLegacyCredit() {
        if(legacyCredit==null) return;
        for(int i=0;i<Math.min(legacyCredit.length,credit.length);i++) credit[i]=Math.max(0,legacyCredit[i])*repairRates[i];
        legacyCredit=null;
    }
    private void refreshRepairRates() {
        if(level==null||level.isClientSide) return;
        int power=repairingLevel();repairCadence=FixtureConfig.INTERVAL_TICKS.get();
        for(int i=0;i<items.length;i++) repairRates[i]=items[i].isDamageableItem()?RepairMath.rate(items[i].getMaxDamage(),FixtureConfig.PERCENT.get(),power,FixtureConfig.calculationTicks()):0;
    }
    public int revision() { return revision; }
    public boolean fitsCompartment() {
        return switch(profileId().getPath()) { case "tool_rack","weapon_rack","polearm_rack","curio_cabinet" -> true;default -> false; };
    }
    private void resolveTransforms() {
        var snapshot=level!=null&&level.isClientSide?Profiles.CLIENT:Profiles.SERVER;
        var current=profile();
        if(transformSnapshot==snapshot&&transformRevision==revision&&renderProfile==current&&renderState==getBlockState()) return;
        resolvedTransforms=new DisplayTransform[slotCount()];
        for(int n=0;n<slotCount();n++) {
            var transform=current.slots().get(n).transform();
            java.util.Set<ResourceLocation> categories=java.util.Set.of();
            if(!current.slots().get(n).wornArmor()&&!items[n].isEmpty()&&level!=null) {
                categories=com.cappleapple.racksnstands.display.Classification.categories(items[n],level);
                boolean hand=false;for(String category:List.of("tools","weapons","swords","staffs","polearms","bows")) hand|=categories.contains(RacksNStands.id(category));
                if(hand&&transform.context==net.minecraft.world.item.ItemDisplayContext.FIXED)
                    transform=new DisplayTransform(List.of(transform.x(),transform.y(),transform.z()),transform.rotation(),List.of((double)transform.sx(),(double)transform.sy(),(double)transform.sz()),net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,transform.modelTranslation());
            }
            boolean matched=false;
            for(var rule:snapshot.orderedRules()) if(rule.matches(items[n],profileId())) { transform=rule.apply(transform);matched=true;break; }
            if(!matched&&transform.context==net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND&&!categories.isEmpty()) {
                double turn=items[n].is(net.minecraft.world.item.Items.TRIDENT)||categories.contains(RacksNStands.id("bows"))?0:
                    items[n].canPerformAction(net.neoforged.neoforge.common.ItemAbilities.AXE_DIG)?-45:
                    categories.contains(RacksNStands.id("weapons"))||categories.contains(RacksNStands.id("swords"))||categories.contains(RacksNStands.id("staffs"))||categories.contains(RacksNStands.id("polearms"))?135:-45;
                if(profileId().equals(RacksNStands.id("sword_floor_stand"))) turn=45;
                var angles=transform.rotation();transform=new DisplayTransform(List.of(transform.x(),transform.y(),transform.z()),List.of(angles.get(0),angles.get(1),angles.get(2)+turn),List.of((double)transform.sx(),(double)transform.sy(),(double)transform.sz()),transform.context,transform.modelTranslation());
            }
            resolvedTransforms[n]=transform;
        }
        transformSnapshot=snapshot;transformRevision=revision;renderProfile=current;renderState=getBlockState();
        renderBox=null;
    }
    public DisplayTransform resolvedTransform(int slot) { resolveTransforms();return resolvedTransforms[slot]; }
    public net.minecraft.world.phys.AABB renderBounds() {
        resolveTransforms();var current=profile();
        if(renderBox==null) {
            var box=new net.minecraft.world.phys.AABB(0,0,0,1,2,1);
            for(var slot:current.slots()) {
                var t=resolvedTransforms[slot.index()];double extent=Math.max(t.sx(),Math.max(t.sy(),t.sz()))*2;
                var center=new net.minecraft.world.phys.Vec3(t.x(),t.y(),t.z());
                if(getBlockState().hasProperty(com.cappleapple.racksnstands.block.SurfaceDisplayBlock.FACE)) center=com.cappleapple.racksnstands.block.SurfaceDisplayBlock.mount(getBlockState().getValue(com.cappleapple.racksnstands.block.SurfaceDisplayBlock.FACE),center);
                box=box.minmax(new net.minecraft.world.phys.AABB(center.x-extent,center.y-extent,center.z-extent,center.x+extent,center.y+extent,center.z+extent));
            }
            double radius=Math.max(Math.max(Math.abs(box.minX-.5),Math.abs(box.maxX-.5)),Math.max(Math.abs(box.minZ-.5),Math.abs(box.maxZ-.5)))*1.415;
            renderBox=new net.minecraft.world.phys.AABB(.5-radius,box.minY,.5-radius,.5+radius,box.maxY,.5+radius).move(worldPosition);
            renderProfile=current;
        }
        return renderBox;
    }
    public IItemHandler automation() { return automation; }
    public boolean accepts(int slot,ItemStack stack) {
        if(level==null||slot<0||slot>=items.length) return false;
        return Classification.accepts(profile().slots().get(slot),stack,level);
    }
    public boolean beginTransaction() { if(transaction||isRemoved()||level==null||level.isClientSide) return false;transaction=true;return true; }
    public void endTransaction() { transaction=false; }
    public int slotLimit(int slot) {
        return FixtureConfig.FULL_STACKS.get()&&!((FixtureBlock)getBlockState().getBlock()).kind().armor()&&!profile().slots().get(slot).wornArmor()?64:1;
    }
    public int itemLimit(int slot,ItemStack stack) { return Math.min(slotLimit(slot),stack.getMaxStackSize()); }
    public ItemStack insert(int slot,ItemStack stack,boolean simulate) {
        if(transaction||isRemoved()||level==null||level.isClientSide||stack.isEmpty()||!accepts(slot,stack)) return stack;
        if(!items[slot].isEmpty()&&!ItemStack.isSameItemSameComponents(items[slot],stack)) return stack;
        int amount=Math.min(stack.getCount(),Math.max(0,itemLimit(slot,stack)-items[slot].getCount()));
        if(amount==0) return stack;
        var remainder=stack.copyWithCount(stack.getCount()-amount);
        if(simulate) return remainder;
        if(!beginTransaction()) return stack;
        try {
            if(NeoForge.EVENT_BUS.post(new FixtureItemInsertEvent(this,slot,stack.copyWithCount(amount))).isCanceled()||isRemoved()) return stack;
            settle(level.getGameTime());items[slot]=stack.copyWithCount(items[slot].getCount()+amount);credit[slot]=0;soundProgress[slot]=0;changed();
            return remainder;
        } finally { endTransaction(); }
    }
    public ItemStack extract(int slot,int amount,boolean simulate) {
        if(transaction||isRemoved()||level==null||level.isClientSide||slot<0||slot>=items.length||amount<=0||items[slot].isEmpty()) return ItemStack.EMPTY;
        int taken=Math.min(amount,items[slot].getCount());
        if(simulate) return items[slot].copyWithCount(taken);
        if(!beginTransaction()) return ItemStack.EMPTY;
        try {
            if(NeoForge.EVENT_BUS.post(new FixtureItemExtractEvent(this,slot,items[slot].copyWithCount(taken))).isCanceled()||isRemoved()) return ItemStack.EMPTY;
            settle(level.getGameTime());var result=items[slot].copyWithCount(taken);items[slot]=items[slot].copyWithCount(items[slot].getCount()-taken);
            if(items[slot].isEmpty()) { credit[slot]=0;soundProgress[slot]=0; }
            changed();return result;
        } finally { endTransaction(); }
    }
    /** Commit a preflighted single-slot exchange while holding the transaction lock. */
    public void commitSlot(int slot,ItemStack replacement) {
        if(!transaction||slot<0||slot>=items.length) throw new IllegalStateException("No slot transaction");
        settle(level.getGameTime());items[slot]=replacement.copy();credit[slot]=0;soundProgress[slot]=0;changed();
    }
    /** Caller owns a successful transaction and has validated the complete replacement. */
    public void commitLoadout(ItemStack[] replacement) {
        if(!transaction||replacement.length!=items.length) throw new IllegalStateException("No loadout transaction");
        for(int i=0;i<items.length;i++) { items[i]=replacement[i];credit[i]=0;soundProgress[i]=0; }
        lastRepairTime=level.getGameTime();changed();
    }
    public void initializeFrom(ItemStack host) {
        materials=host.getOrDefault(RacksNStands.MATERIALS.get(),MaterialPalette.EMPTY);
        enchantments=host.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);
        lastRepairTime=level==null?-1:level.getGameTime();changed();
    }
    @Override public int repairingLevel() {
        if(level==null||!Classification.tagged(new ItemStack(getBlockState().getBlock()),RacksNStands.id("repairing_hosts"))) return 0;
        var holder=level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(RacksNStands.id("repairing"));
        return holder.map(h -> Math.min(FixtureConfig.MAX_LEVEL.get(),enchantments.getLevel(h))).orElse(0);
    }
    private boolean eligible(ItemStack stack) { return !stack.isEmpty()&&stack.isDamageableItem()&&(!FixtureConfig.DAMAGED_ONLY.get()||stack.isDamaged()); }
    public void scheduledRepair() {
        if(level==null||level.isClientSide||transaction) return;
        if(beginTransaction()) { try { settle(level.getGameTime()); } finally { endTransaction(); } }
        schedule();
    }
    /** Convert elapsed ticks to durability, retaining sub-point credit separately for each item. */
    private void settle(long now) {
        long elapsed=RepairMath.elapsed(now,lastRepairTime);lastRepairTime=now;
        List<Integer> repaired=null;boolean soundDue=false;
        for(int i=0;i<items.length;i++) {
            ItemStack stack=items[i];
            if(!stack.isDamageableItem()||repairRates[i]<=0) { credit[i]=0;continue; }
            var result=RepairMath.advance(stack.getDamageValue(),credit[i],repairRates[i],elapsed);
            credit[i]=result.fraction();
            if(result.damage()!=stack.getDamageValue()) {
                var sound=com.cappleapple.racksnstands.repairing.RepairSoundProgress.advance(soundProgress[i],stack.getDamageValue()-result.damage(),stack.getMaxDamage(),FixtureConfig.SOUND_INTERVAL_PERCENT.get());
                soundProgress[i]=result.damage()==0?0:sound.percent();soundDue|=sound.soundDue();
                stack.setDamageValue(result.damage());if(repaired==null) repaired=new ArrayList<>();repaired.add(i);
            }
        }
        if(elapsed>0) setChanged();
        if(repaired!=null) {
            visibleChanged();NeoForge.EVENT_BUS.post(new FixtureRepairEvent(this,repaired));
            if(level instanceof ServerLevel server) {
                if(FixtureConfig.PARTICLES.get()) server.sendParticles(ParticleTypes.ENCHANT,worldPosition.getX()+.5,worldPosition.getY()+.8,worldPosition.getZ()+.5,6,.25,.25,.25,.03);
                if(soundDue&&FixtureConfig.SOUND.get()&&FixtureConfig.SOUND_VOLUME.get()>0) server.playSound(null,worldPosition,SoundEvents.ENCHANTMENT_TABLE_USE,SoundSource.BLOCKS,FixtureConfig.SOUND_VOLUME.get().floatValue(),FixtureConfig.SOUND_PITCH.get().floatValue());
            }
        }
    }
    private void schedule() {
        if(level==null||level.isClientSide||isRemoved()) return;
        for(int i=0;i<items.length;i++) if(repairRates[i]>0&&eligible(items[i])) {
            level.scheduleTick(worldPosition,getBlockState().getBlock(),repairCadence);return;
        }
    }
    @Override public void onLoad() {
        super.onLoad();refreshMaterials();
        if(level!=null&&!level.isClientSide) {
            com.cappleapple.racksnstands.config.LiveConfig.track(this);
            refreshRepairRates();migrateLegacyCredit();
            if(lastRepairTime<0) lastRepairTime=level.getGameTime();
            else if(!FixtureConfig.UNLOADED.get()) lastRepairTime=level.getGameTime()-RepairMath.elapsed(savedAt,lastRepairTime);
            scheduledRepair();visibleChanged();
        }
    }
    private void visibleChanged() {
        revision++;setChanged();
        if(level!=null&&!level.isClientSide) {
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());
        }
    }
    private void changed() { refreshRepairRates();visibleChanged();schedule(); }
    public int comparator() { int occupied=0;for(var stack:items) if(!stack.isEmpty()) occupied++;return occupied==0?0:Math.max(1,15*occupied/items.length); }
    /** Sable and vanilla block transfers clear the source inventory after saving its data. */
    @Override public void clearContent() {
        Arrays.fill(items,ItemStack.EMPTY);
        Arrays.fill(credit,0);Arrays.fill(repairRates,0);Arrays.fill(soundProgress,0);
        legacyCredit=null;
        lastRepairTime=level==null?-1:level.getGameTime();savedAt=lastRepairTime;
        visibleChanged();
    }
    public void dropContents() {
        if(dropped||level==null||level.isClientSide) return;
        dropped=true;
        for(int i=0;i<items.length;i++) { ItemStack stack=items[i];items[i]=ItemStack.EMPTY;if(!stack.isEmpty()) Block.popResource(level,worldPosition,stack); }
        setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider) {
        super.saveAdditional(tag,provider);writeVisible(tag,provider);
        tag.putLong("SavedAt",level==null?lastRepairTime:level.getGameTime());
        tag.put("RepairSoundProgress",doubles(soundProgress));
    }
    private static ListTag doubles(double[] values) {
        var list=new ListTag();for(double value:values) list.add(DoubleTag.valueOf(value));return list;
    }
    private void writeVisible(CompoundTag tag,HolderLookup.Provider provider) {
        tag.put("RepairFractions",doubles(credit));tag.put("RepairRates",doubles(repairRates));
        tag.putLong("LastRepairTime",lastRepairTime);tag.putInt("RepairCadence",repairCadence);
        tag.put("Materials",MaterialPalette.CODEC.encodeStart(NbtOps.INSTANCE,materials).getOrThrow());
        ListTag list=new ListTag();
        for(int i=0;i<items.length;i++) if(!items[i].isEmpty()) { CompoundTag entry=new CompoundTag();entry.putInt("Slot",i);entry.put("Stack",items[i].save(provider));list.add(entry); }
        tag.put("Items",list);tag.put("Enchantments",ItemEnchantments.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE),enchantments).getOrThrow());
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider) {
        super.loadAdditional(tag,provider);
        var previousMaterials=materials;
        materials=tag.contains("Materials")?MaterialPalette.CODEC.parse(NbtOps.INSTANCE,tag.get("Materials")).result().orElse(MaterialPalette.EMPTY):MaterialPalette.EMPTY;
        if(!materials.equals(previousMaterials)) refreshMaterials();
        revision++;Arrays.fill(items,ItemStack.EMPTY);Arrays.fill(credit,0);Arrays.fill(repairRates,0);Arrays.fill(soundProgress,0);
        for(Tag entry:tag.getList("Items",Tag.TAG_COMPOUND)) { CompoundTag e=(CompoundTag)entry;int slot=e.getInt("Slot");if(slot>=0&&slot<items.length) items[slot]=ItemStack.parseOptional(provider,e.getCompound("Stack")); }
        enchantments=ItemEnchantments.EMPTY;
        if(tag.contains("Enchantments")) enchantments=ItemEnchantments.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE),tag.get("Enchantments")).result().orElse(ItemEnchantments.EMPTY);
        var sounds=tag.getList("RepairSoundProgress",Tag.TAG_DOUBLE);
        var fractions=tag.getList("RepairFractions",Tag.TAG_DOUBLE);var rates=tag.getList("RepairRates",Tag.TAG_DOUBLE);
        for(int i=0;i<items.length;i++) {
            if(i<sounds.size()) { double value=sounds.getDouble(i);soundProgress[i]=Double.isFinite(value)?Math.clamp(value,0,100):0; }
            if(i<fractions.size()) { double value=fractions.getDouble(i);credit[i]=Double.isFinite(value)?Math.max(0,value):0; }
            if(i<rates.size()) { double value=rates.getDouble(i);repairRates[i]=Double.isFinite(value)?Math.max(0,value):0; }
        }
        if(level!=null&&!level.isClientSide) refreshRepairRates();
        legacyCredit=!tag.contains("RepairFractions")&&tag.contains("RepairCredit")?tag.getLongArray("RepairCredit"):null;
        if(level!=null&&!level.isClientSide) migrateLegacyCredit();
        if(level==null||level.isClientSide) repairCadence=Math.max(1,tag.getInt("RepairCadence"));
        lastRepairTime=tag.contains("LastRepairTime")?tag.getLong("LastRepairTime"):-1;
        savedAt=tag.contains("SavedAt")?tag.getLong("SavedAt"):lastRepairTime;
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder builder) { super.collectImplicitComponents(builder);builder.set(DataComponents.ENCHANTMENTS,enchantments);builder.set(RacksNStands.MATERIALS.get(),materials); }
    @Override protected void applyImplicitComponents(DataComponentInput input) { super.applyImplicitComponents(input);materials=input.getOrDefault(RacksNStands.MATERIALS.get(),MaterialPalette.EMPTY);enchantments=input.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY); }
    @Override public void removeComponentsFromTag(CompoundTag tag) { super.removeComponentsFromTag(tag);tag.remove("Enchantments");tag.remove("Materials"); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { CompoundTag tag=new CompoundTag();writeVisible(tag,provider);return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
