package com.cappleapple.racksnstands.gametest;
import com.cappleapple.racksnstands.*;
import com.cappleapple.racksnstands.api.*;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.datagen.FixtureData;
import com.cappleapple.racksnstands.display.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(RacksNStands.MOD_ID) @PrefixGameTestTemplate(false)
public final class ItemTransformGameTests {
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void toolAndSwordOrientationAndCache(GameTestHelper h) {
        var pos=new BlockPos(1,2,1);h.setBlock(pos,RacksNStands.FIXTURES.get("generic_pedestal").get());var fixture=(FixtureBlockEntity)h.getBlockEntity(pos);
        fixture.insert(0,new ItemStack(Items.IRON_PICKAXE),false);var tool=fixture.resolvedTransform(0);
        h.assertTrue(tool.rotation().equals(List.of(0.0,0.0,-45.0)),"Tagged mining tools have upright orientation");
        h.assertTrue(fixture.resolvedTransform(0)==tool,"Repeated rendering reuses the resolved transform");
        fixture.extract(0,1,false);fixture.insert(0,new ItemStack(Items.IRON_SWORD),false);var sword=fixture.resolvedTransform(0);
        h.assertTrue(sword.rotation().equals(List.of(0.0,0.0,135.0)),"Sword points down");
        h.assertTrue(sword!=tool,"Visible inventory changes invalidate cached transforms");
        fixture.extract(0,1,false);fixture.insert(0,new ItemStack(Items.TRIDENT),false);
        h.assertTrue(fixture.resolvedTransform(0).rotation().equals(List.of(0.0,0.0,0.0)),"Trident points up");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void explicitItemTagRulesFixtureScopeAndReload(GameTestHelper h) {
        var pos=new BlockPos(1,2,1);h.setBlock(pos,RacksNStands.FIXTURES.get("generic_pedestal").get());var fixture=(FixtureBlockEntity)h.getBlockEntity(pos);fixture.insert(0,new ItemStack(Items.IRON_SWORD),false);
        var json=FixtureData.object("priority",100,"items",FixtureData.array("minecraft:iron_sword"),"fixtures",FixtureData.array("racksnstands:generic_pedestal"),"rotation",FixtureData.array(0,0,90),"scale",1.5,"offset",FixtureData.array(0,.25,0));
        var rule=ItemDisplayRule.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow();
        h.assertTrue(rule.matches(new ItemStack(Items.IRON_SWORD),fixture.profileId())&&!rule.matches(new ItemStack(Items.IRON_SWORD),RacksNStands.id("weapon_rack")),"Explicit fixture scope respected");
        var original=Profiles.SERVER;
        try {
            var rules=new HashMap<>(original.itemRules());rules.put(RacksNStands.id("test_override"),rule);Profiles.SERVER=new Profiles.Snapshot(original.displays(),original.categories(),rules);
            var t=fixture.resolvedTransform(0);
            h.assertTrue(t.rotation().get(2)==90&&Math.abs(t.sx()-1.65)<.001&&Math.abs(t.y()-1.30)<.001,"Higher-priority exact item rule controls rotation, size and offset");
        } finally { Profiles.SERVER=original; }
        h.assertTrue(fixture.resolvedTransform(0).rotation().get(2)==135,"Reload invalidates cached rules without item changes");
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void handModelsAxesAndTabletopPlane(GameTestHelper h) {
        var pos=new BlockPos(1,2,1);h.setBlock(pos,RacksNStands.FIXTURES.get("generic_tabletop_display").get());var fixture=(FixtureBlockEntity)h.getBlockEntity(pos);
        for(var item:List.of(Items.IRON_AXE,Items.IRON_PICKAXE,Items.MACE)) {
            fixture.insert(0,new ItemStack(item),false);var t=fixture.resolvedTransform(0);
            h.assertTrue(t.context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,"Tools use their hand model context");
            h.assertTrue(t.rotation().equals(List.of(90.0,0.0,-45.0)),"Axes and hammers remain upright relative to the tabletop plane");
            fixture.extract(0,1,false);
        }
        fixture.insert(0,new ItemStack(Items.IRON_SWORD),false);
        h.assertTrue(fixture.resolvedTransform(0).rotation().equals(List.of(90.0,0.0,135.0)),"Sword stays in the tabletop plane");
        fixture.extract(0,1,false);fixture.insert(0,new ItemStack(Items.APPLE),false);
        h.assertTrue(fixture.resolvedTransform(0).context==ItemDisplayContext.FIXED,"Ordinary items retain their normal fixed context");
        fixture.extract(0,1,false);fixture.insert(0,new ItemStack(Items.SHIELD),false);
        h.assertTrue(fixture.resolvedTransform(0).context==ItemDisplayContext.FIXED&&fixture.resolvedTransform(0).rotation().equals(List.of(90.0,0.0,0.0)),"Native 3D shield faces outward without inheriting a side-on hand grip");
        var json=FixtureData.object("items",FixtureData.array("minecraft:iron_sword"),"display_context","fixed","rotation",FixtureData.array(0,0,90));
        var rule=ItemDisplayRule.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow();
        h.assertTrue(rule.apply(fixture.resolvedTransform(0)).rotation().equals(List.of(0.0,0.0,90.0)),"Absolute pack overrides remain supported");h.succeed();
    }

    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void weaponFallbackAndCompanionRules(GameTestHelper h) {
        var pos=new BlockPos(1,2,1);h.setBlock(pos,RacksNStands.FIXTURES.get("generic_pedestal").get());var fixture=(FixtureBlockEntity)h.getBlockEntity(pos);
        fixture.insert(0,new ItemStack(Items.IRON_SWORD),false);var original=Profiles.SERVER;
        try {
            Profiles.SERVER=new Profiles.Snapshot(original.displays(),original.categories(),Map.of());
            h.assertTrue(fixture.resolvedTransform(0).rotation().get(2)==135,"Sword ability supplies a default when no tag rule matches");
        } finally { Profiles.SERVER=original; }
        fixture.extract(0,1,false);
        for(var entry:Map.of("simplyswords:iron_greataxe",-45.0,"simplyswords:iron_greathammer",45.0,"simplyswords:iron_longsword",135.0,
                "simplymore:iron_pernach",-45.0,"simplymore:iron_grandsword",135.0,"simplymore:earthshatter",-45.0).entrySet()) {
            var id=net.minecraft.resources.ResourceLocation.parse(entry.getKey());
            if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id)) continue;
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);fixture.insert(0,new ItemStack(item),false);
            h.assertTrue(fixture.resolvedTransform(0).context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,"Companion weapon uses its hand model: "+id);
            h.assertTrue(fixture.resolvedTransform(0).rotation().get(2).equals(entry.getValue()),"Companion orientation: "+id);
            fixture.extract(0,1,false);
        }
        h.succeed();
    }

    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void displayStandUsesHorizontalDefaults(GameTestHelper h) {
        var pos=new BlockPos(1,2,1);h.setBlock(pos,RacksNStands.FIXTURES.get("sword_floor_stand").get());var fixture=(FixtureBlockEntity)h.getBlockEntity(pos);
        for(var item:List.of(Items.DIAMOND_SWORD,Items.DIAMOND_AXE,Items.DIAMOND_PICKAXE,Items.BOW)) {
            fixture.insert(0,new ItemStack(item),false);var t=fixture.resolvedTransform(0);
            h.assertTrue(t.rotation().get(2)==45&&Math.abs(t.z()-.5)<.001,"Weapons and tools lie across centered stand supports");fixture.extract(0,1,false);
        }
        fixture.insert(0,new ItemStack(Items.TRIDENT),false);
        h.assertTrue(fixture.resolvedTransform(0).rotation().get(2)==90,"Native trident lies across supports");fixture.extract(0,1,false);
        var hammer=net.minecraft.resources.ResourceLocation.parse("simplyswords:iron_greathammer");
        if(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(hammer)) {
            fixture.insert(0,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(hammer)),false);
            h.assertTrue(fixture.resolvedTransform(0).rotation().get(2)==135,"Native 3D hammer uses its horizontal stand correction");
        }
        h.succeed();
    }

    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void compactDisplaysUseHandModelsAndOrientationRules(GameTestHelper h) {
        var pos=new BlockPos(1,2,1);
        for(String kind:List.of("tool_rack","curio_cabinet")) {
            h.setBlock(pos,RacksNStands.FIXTURES.get(kind).get());var fixture=(FixtureBlockEntity)h.getBlockEntity(pos);
            if(kind.equals("curio_cabinet")) {
                h.assertTrue(Math.abs(fixture.profile().slots().get(0).transform().y()-(8.5/16+.001))<1e-6&&Math.abs(fixture.profile().slots().get(3).transform().y()-(2.0/16+.001))<1e-6,"Shelf anchors are on the two shelf surfaces");
            }
            for(var item:List.of(Items.DIAMOND_SWORD,Items.BOW,Items.TRIDENT,Items.IRON_AXE)) {
                fixture.insert(0,new ItemStack(item),false);
                h.assertTrue(fixture.fitsCompartment()&&fixture.resolvedTransform(0).context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,"Rack and shelf use fitted hand geometry");
                double expected=item==Items.DIAMOND_SWORD?135:item==Items.IRON_AXE?-45:0;
                h.assertTrue(fixture.resolvedTransform(0).rotation().get(2)==expected,"Compact displays preserve item orientation rules");
                fixture.extract(0,64,false);
            }
            fixture.insert(0,new ItemStack(Items.STONE),false);
            h.assertTrue(fixture.fitsCompartment()&&fixture.resolvedTransform(0).context==ItemDisplayContext.FIXED,"Displayed blocks are also fitted while retaining their normal block model");
        }
        var json=FixtureData.object("items",FixtureData.array("minecraft:iron_sword"),"model_translation",FixtureData.array(.5,-.25,1.5));
        var rule=ItemDisplayRule.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow();
        var base=BuiltinProfiles.create(com.cappleapple.racksnstands.registry.FixtureCatalog.get("generic_pedestal")).slots().getFirst().transform();
        var shifted=rule.apply(base);
        h.assertTrue(shifted.x()==base.x()&&shifted.modelTranslation().equals(List.of(.5,-.25,1.5)),"Model-local centering does not move the fixture attachment point");
        h.succeed();
    }

}
