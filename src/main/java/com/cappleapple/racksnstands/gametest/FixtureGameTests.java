package com.cappleapple.racksnstands.gametest;

import com.cappleapple.racksnstands.*;
import com.cappleapple.racksnstands.api.*;
import com.cappleapple.racksnstands.block.*;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.compat.curios.CuriosBridge;
import com.cappleapple.racksnstands.config.FixtureConfig;
import com.cappleapple.racksnstands.datagen.FixtureData;
import com.cappleapple.racksnstands.display.*;
import com.cappleapple.racksnstands.event.*;
import com.cappleapple.racksnstands.repairing.RepairMath;
import com.cappleapple.racksnstands.interaction.*;
import com.cappleapple.racksnstands.registry.FixtureCatalog;
import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder(RacksNStands.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FixtureGameTests {
    private static final String EMPTY="bastion/mobs/empty";
    private static final BlockPos POS=new BlockPos(1,2,1);
    private static FixtureBlockEntity fixture(GameTestHelper h,String name,int power) {
        var block=RacksNStands.FIXTURES.get(name).get();h.setBlock(POS,block);
        var fixture=(FixtureBlockEntity)h.getBlockEntity(POS);var host=new ItemStack(block);
        if(power>0) host.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(RacksNStands.id("repairing")).orElseThrow(),power);
        fixture.initializeFrom(host);return fixture;
    }
    private static ListTag fractionList(double... values) { var result=new ListTag();for(double value:values) result.add(DoubleTag.valueOf(value));return result; }
    private static ItemStack damaged(Item item,int damage) { var stack=new ItemStack(item);stack.setDamageValue(damage);return stack; }
    private static void bank(FixtureBlockEntity fixture,long intervals) {
        var tag=fixture.saveWithoutMetadata(fixture.getLevel().registryAccess());
        var fractions=new ListTag();
        for(int i=0;i<fixture.slotCount();i++) fractions.add(DoubleTag.valueOf(fixture.credit(i)+RepairMath.rate(fixture.displayedStack(i).getMaxDamage(),FixtureConfig.PERCENT.get(),fixture.repairingLevel(),FixtureConfig.calculationTicks())*FixtureConfig.calculationTicks()*intervals));
        tag.put("RepairFractions",fractions);tag.putLong("LastRepairTime",fixture.getLevel().getGameTime());
        fixture.loadWithComponents(tag,fixture.getLevel().registryAccess());fixture.scheduledRepair();
    }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void repairingLevelsIndependentSlotsAndClamp(GameTestHelper h) {
        var fixture=fixture(h,"tool_rack",1);
        for(int i=0;i<6;i++) fixture.insert(i,damaged(Items.DIAMOND_PICKAXE,1000),false);
        bank(fixture,1);
        for(int i=0;i<6;i++) h.assertTrue(fixture.displayedStack(i).getDamageValue()==688,"Each item receives the full 20% amount");
        bank(fixture,2);
        for(int i=0;i<6;i++) h.assertTrue(fixture.displayedStack(i).getDamageValue()==64,"Multiple intervals accumulate independently");
        var host=new ItemStack(fixture.getBlockState().getBlock());host.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(RacksNStands.id("repairing")).orElseThrow(),10);fixture.initializeFrom(host);
        bank(fixture,1);for(int i=0;i<6;i++) h.assertTrue(fixture.displayedStack(i).getDamageValue()==0,"Repairing X clamps to full");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void noRepairForUnenchantedOrNonDurable(GameTestHelper h) {
        var fixture=fixture(h,"generic_pedestal",0);fixture.insert(0,damaged(Items.IRON_SWORD,100),false);bank(fixture,100);
        h.assertTrue(fixture.displayedStack(0).getDamageValue()==100,"Unenchanted host cannot repair");
        fixture.extract(0,1,false);fixture.insert(0,new ItemStack(Items.DIAMOND),false);bank(fixture,100);
        h.assertTrue(fixture.displayedStack(0).is(Items.DIAMOND)&&!fixture.displayedStack(0).isDamaged(),"Non-durable item preserved");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void unloadedCatchUpAndLoadedOnly(GameTestHelper h) {
        var fixture=fixture(h,"generic_pedestal",1);fixture.insert(0,damaged(Items.DIAMOND_PICKAXE,1000),false);
        var saved=fixture.saveWithoutMetadata(h.getLevel().registryAccess());long now=h.getLevel().getGameTime();
        // Move the fixture's persisted clock into the past without changing global world time.
        saved.putLong("LastRepairTime",now);fixture.loadWithComponents(saved,h.getLevel().registryAccess());
        long original=h.getLevel().getGameTime();
        // Use pre-earned credits for loaded-only; catch-up path is tested with a positive simulated clock below.
        saved.put("RepairFractions",FixtureGameTests.fractionList(1561*.2*2));
        boolean before=FixtureConfig.UNLOADED.get();
        try {
            FixtureConfig.UNLOADED.set(false);fixture.loadWithComponents(saved,h.getLevel().registryAccess());fixture.onLoad();
            h.assertTrue(fixture.displayedStack(0).getDamageValue()==376,"Loaded-only preserves earned progress on reload");
            var noCredit=fixture.saveWithoutMetadata(h.getLevel().registryAccess());noCredit.put("RepairFractions",fractionList(0));noCredit.putLong("LastRepairTime",0);noCredit.putLong("SavedAt",0);
            fixture.loadWithComponents(noCredit,h.getLevel().registryAccess());fixture.onLoad();
            h.assertTrue(fixture.displayedStack(0).getDamageValue()==376,"Loaded-only excludes unloaded clock delta");
            FixtureConfig.UNLOADED.set(true);
            ((net.minecraft.world.level.storage.ServerLevelData)h.getLevel().getLevelData()).setGameTime(original+FixtureConfig.calculationTicks()*3);
            noCredit.putLong("LastRepairTime",original);fixture.loadWithComponents(noCredit,h.getLevel().registryAccess());fixture.onLoad();
            h.assertTrue(fixture.displayedStack(0).getDamageValue()==0,"Unloaded catch-up accounts three intervals");
        } finally { FixtureConfig.UNLOADED.set(before);((net.minecraft.world.level.storage.ServerLevelData)h.getLevel().getLevelData()).setGameTime(original); }
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void newInsertCannotInheritOldCredit(GameTestHelper h) {
        var fixture=fixture(h,"tool_rack",1);fixture.insert(0,damaged(Items.IRON_PICKAXE,100),false);
        var tag=fixture.saveWithoutMetadata(h.getLevel().registryAccess());tag.put("RepairFractions",fractionList(.95,.95,0,0,0,0));fixture.loadWithComponents(tag,h.getLevel().registryAccess());
        fixture.insert(1,damaged(Items.IRON_PICKAXE,100),false);h.assertTrue(fixture.credit(1)==0,"New item starts its own clock");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void allArmorLoadoutCombinations(GameTestHelper h) {
        var fixture=fixture(h,"armor_mannequin",0);var player=h.makeMockPlayer(GameType.SURVIVAL);
        EquipmentSlot[] slots={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
        Item[] iron={Items.IRON_HELMET,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS};
        Item[] diamond={Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS};
        // 256 combinations include full, empty and every partial set in both directions.
        for(int a=0;a<16;a++) for(int b=0;b<16;b++) {
            for(int n=0;n<4;n++) { fixture.extract(n,1,false);player.setItemSlot(slots[n],(a&(1<<n))==0?ItemStack.EMPTY:new ItemStack(iron[n]));if((b&(1<<n))!=0) fixture.insert(n,new ItemStack(diamond[n]),false); }
            h.assertTrue(ArmorSwap.swap(fixture,player),"Valid loadout must swap atomically");
            for(int n=0;n<4;n++) {
                h.assertTrue((a&(1<<n))==0?fixture.displayedStack(n).isEmpty():fixture.displayedStack(n).is(iron[n]),"Fixture conserves player armor");
                h.assertTrue((b&(1<<n))==0?player.getItemBySlot(slots[n]).isEmpty():player.getItemBySlot(slots[n]).is(diamond[n]),"Player conserves fixture armor");
            }
        }
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void cancellationBindingAndInvalidArmor(GameTestHelper h) {
        var fixture=fixture(h,"armor_mannequin",0);var player=h.makeMockPlayer(GameType.SURVIVAL);
        fixture.insert(0,new ItemStack(Items.IRON_HELMET),false);player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
        Consumer<ArmorLoadoutSwapEvent> cancel=e -> { if(e.fixture==fixture)e.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(cancel);
        try { h.assertTrue(!ArmorSwap.swap(fixture,player),"Canceled loadout rejected"); }
        finally { NeoForge.EVENT_BUS.unregister(cancel); }
        h.assertTrue(fixture.displayedStack(0).is(Items.IRON_HELMET)&&player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE),"Cancellation changes neither side");
        var bound=new ItemStack(Items.IRON_BOOTS);bound.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE),1);player.setItemSlot(EquipmentSlot.FEET,bound);
        h.assertTrue(!ArmorSwap.swap(fixture,player),"Binding prevents removal");player.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Items.DIAMOND));h.assertTrue(!ArmorSwap.swap(fixture,player),"Invalid source rejects whole transaction");
        h.assertTrue(fixture.displayedStack(0).is(Items.IRON_HELMET),"Invalid swap preserves fixture");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void automationFiltersSimulationAndCancellation(GameTestHelper h) {
        var fixture=fixture(h,"helmet_stand",0);var handler=fixture.automation();
        h.assertTrue(!handler.insertItem(0,new ItemStack(Items.IRON_BOOTS),false).isEmpty(),"Helmet stand rejects boots");
        h.assertTrue(handler.insertItem(0,new ItemStack(Items.IRON_HELMET),true).isEmpty()&&fixture.displayedStack(0).isEmpty(),"Simulation never changes storage");
        Consumer<FixtureItemInsertEvent> cancel=e -> { if(e.fixture==fixture)e.setCanceled(true); };NeoForge.EVENT_BUS.addListener(cancel);
        try { h.assertTrue(!handler.insertItem(0,new ItemStack(Items.IRON_HELMET),false).isEmpty(),"Insert cancellation respected"); }
        finally { NeoForge.EVENT_BUS.unregister(cancel); }
        handler.insertItem(0,new ItemStack(Items.IRON_HELMET),false);
        boolean before=FixtureConfig.AUTOMATION.get();try { FixtureConfig.AUTOMATION.set(false);h.assertTrue(handler.extractItem(0,1,false).isEmpty(),"Cached capability respects disabled automation"); } finally { FixtureConfig.AUTOMATION.set(before); }
        h.assertTrue(fixture.comparator()==15,"Occupied single fixture is full");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void saveReloadAndEnchantedLoot(GameTestHelper h) {
        var fixture=fixture(h,"generic_pedestal",5);var stack=damaged(Items.DIAMOND_SWORD,100);stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Keepsake"));fixture.insert(0,stack,false);
        var saved=fixture.saveWithoutMetadata(h.getLevel().registryAccess());fixture.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(ItemStack.matches(stack,fixture.displayedStack(0))&&fixture.repairingLevel()==5,"Full components and real enchantment survive reload");
        var loot=Block.getDrops(fixture.getBlockState(),h.getLevel(),fixture.getBlockPos(),fixture);
        h.assertTrue(loot.size()==1&&loot.getFirst().isEnchanted(),"Loot contains one genuinely enchanted fixture");
        h.assertTrue(!loot.getFirst().has(net.minecraft.core.component.DataComponents.CONTAINER),"Fixture loot cannot also carry the dropped inventory");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void breakingDropsStoredGearOnce(GameTestHelper h) {
        var fixture=fixture(h,"generic_pedestal",0);fixture.insert(0,new ItemStack(Items.DIAMOND_SWORD),false);
        var pos=fixture.getBlockPos();fixture.dropContents();h.getLevel().destroyBlock(pos,true);
        int total=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1)).stream().filter(e -> e.getItem().is(Items.DIAMOND_SWORD)).mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(total==1,"Gear drops exactly once across explicit drop and block removal");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void categoriesExplicitDenyAndGenericFallback(GameTestHelper h) {
        var fixture=fixture(h,"generic_pedestal",0);
        h.assertTrue(fixture.accepts(0,new ItemStack(Items.COMMAND_BLOCK)),"Generic fallback accepts arbitrary items");
        h.assertTrue(Classification.categories(new ItemStack(Items.IRON_PICKAXE),h.getLevel()).contains(RacksNStands.id("tools")),"Tool metadata and tags classify tools");
        h.assertTrue(Classification.categories(new ItemStack(Items.MACE),h.getLevel()).contains(RacksNStands.id("weapons")),"Weapon classification is not SwordItem inheritance");
        var json=FixtureData.object("index",0,"filter",FixtureData.object("allowed_items",FixtureData.array("minecraft:diamond"),"denied_items",FixtureData.array("minecraft:diamond")),"transform",FixtureData.object(),"interaction_bounds",FixtureData.array(0,0,0,1,1,1));
        var slot=DisplaySlotDefinition.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow();
        h.assertTrue(!Classification.accepts(slot,new ItemStack(Items.DIAMOND),h.getLevel()),"Explicit deny wins over explicit allow");
        json.getAsJsonObject("filter").remove("denied_items");slot=DisplaySlotDefinition.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow();
        h.assertTrue(Classification.accepts(slot,new ItemStack(Items.DIAMOND),h.getLevel()),"Explicit allow works");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void visibleSlotsAndMalformedProfiles(GameTestHelper h) {
        var profile=BuiltinProfiles.create(FixtureCatalog.get("tool_rack"));
        for(int n=0;n<6;n++) h.assertTrue(FixtureInteraction.slotAt(profile,profile.slots().get(n).bounds().getCenter())==n,"Every visible slot has its own interaction region");
        for(Direction facing:Direction.Plane.HORIZONTAL) {
            Vec3 p=new Vec3(.25,.7,.4);double a=Math.toRadians(180-facing.toYRot()),c=Math.cos(a),s=Math.sin(a);double x=p.x-.5,z=p.z-.5;
            var transformed=FixtureInteraction.local(new Vec3(c*x+s*z+.5,p.y,-s*x+c*z+.5),facing);
            h.assertTrue(transformed.distanceTo(p)<.0001,"Target transform matches rotated fixtures");
        }
        var parsed=Profiles.decode(Map.of(RacksNStands.id("displays/tool_rack"),JsonParser.parseString("{\"slots\":[]}")));
        h.assertTrue(parsed.displays().get(RacksNStands.id("tool_rack")).slots().size()==6,"Broken definition falls back independently");
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template=EMPTY)
    public static void curiosOptionalAndSlotMetadata(GameTestHelper h) {
        var fixture=fixture(h,"ring_stand",1);
        if(!CuriosBridge.available()) {
            h.assertTrue(CuriosBridge.slots(new ItemStack(Items.FEATHER),h.getLevel()).isEmpty(),"Absent Curios safely returns no metadata");
            h.assertTrue(fixture.accepts(0,new ItemStack(Items.FEATHER)),"Legacy furniture remains usable without Curios");h.succeed();return;
        }
        // The external test datapack assigns real Curios slot validators to vanilla test items.
        h.assertTrue(fixture.accepts(0,new ItemStack(Items.FEATHER)),"Ring metadata accepts feather fixture test item");
        h.assertTrue(fixture.accepts(0,new ItemStack(Items.STRING)),"Legacy furniture accepts arbitrary items");
        fixture.insert(0,damaged(Items.IRON_SWORD,200),false);bank(fixture,1);
        h.assertTrue(fixture.displayedStack(0).getDamageValue()==150,"Curios use ordinary durability repair");
        var all=BuiltinProfiles.create(FixtureCatalog.get("generic_curio_stand"));
        h.assertTrue(Classification.accepts(all.slots().getFirst(),new ItemStack(Items.PAPER),h.getLevel()),"Generic stand accepts custom Curios slot");
        var custom=FixtureData.object("index",0,"filter",FixtureData.object("accepted_curio_slots",FixtureData.array("sigil")),"transform",FixtureData.object(),"interaction_bounds",FixtureData.array(0,0,0,1,1,1));
        var slot=DisplaySlotDefinition.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,custom).getOrThrow();
        h.assertTrue(Classification.accepts(slot,new ItemStack(Items.PAPER),h.getLevel()),"Arbitrary registered string slot identifier works");
        var cabinet=BuiltinProfiles.create(FixtureCatalog.get("curio_cabinet"));
        h.assertTrue(Classification.accepts(cabinet.slots().get(1),new ItemStack(Items.STRING),h.getLevel())&&Classification.accepts(cabinet.slots().get(0),new ItemStack(Items.STRING),h.getLevel()),"Every shelf slot accepts arbitrary items");h.succeed();
    }
}
