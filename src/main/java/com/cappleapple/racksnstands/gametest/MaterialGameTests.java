package com.cappleapple.racksnstands.gametest;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.block.*;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.interaction.FixtureInteraction;
import com.cappleapple.racksnstands.material.*;
import com.cappleapple.racksnstands.registry.FixtureCatalog;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(RacksNStands.MOD_ID) @PrefixGameTestTemplate(false)
public final class MaterialGameTests {
    private static FixtureBlockEntity fixture(GameTestHelper h,String id) {
        var pos=new BlockPos(1,2,1);h.setBlock(pos,RacksNStands.FIXTURES.get(id).get());return (FixtureBlockEntity)h.getBlockEntity(pos);
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void matchingPartsShareMaterialWithoutTouchingItems(GameTestHelper h) {
        var f=fixture(h,"sword_floor_stand");var p=h.makeMockPlayer(GameType.SURVIVAL);
        f.insert(0,new ItemStack(Items.DIAMOND_SWORD),false);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GOLD_BLOCK,32));
        var left=new Vec3(4.5/16,.5,7.25/16);var right=new Vec3(11.5/16,.5,7.25/16);
        h.assertTrue(MaterialCustomization.groupAt(FixtureCatalog.get("sword_floor_stand"),left).equals(MaterialCustomization.groupAt(FixtureCatalog.get("sword_floor_stand"),right)),"Both supports address the same original material");
        MaterialCustomization.apply(f,p,InteractionHand.MAIN_HAND,left);
        h.assertTrue(f.materials().materials().size()==1&&f.materials().materials().get("metal").is(Blocks.GOLD_BLOCK),"Only the metal material is replaced");
        h.assertTrue(p.getMainHandItem().getCount()==32&&f.displayedStack(0).is(Items.DIAMOND_SWORD),"Sampling consumes nothing and preserves displayed gear");
        var revision=f.revision();MaterialCustomization.apply(f,p,InteractionHand.MAIN_HAND,right);
        h.assertTrue(revision==f.revision(),"Applying the same material twice is idempotent");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BRICKS));MaterialCustomization.apply(f,p,InteractionHand.MAIN_HAND,new Vec3(.5,.125,.25));
        h.assertTrue(f.materials().materials().get("dark").is(Blocks.BRICKS)&&f.materials().materials().get("metal").is(Blocks.GOLD_BLOCK),"Changing the base preserves the supports");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void materialsPersistInNetworkLootAndReplacement(GameTestHelper h) {
        var f=fixture(h,"armor_mannequin");var log=Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X);
        f.setMaterial("wood",log);f.setMaterial("dark",Blocks.BLUE_TERRACOTTA.defaultBlockState());var expected=f.materials();
        var saved=f.saveWithoutMetadata(h.getLevel().registryAccess());f.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(expected.equals(f.materials()),"Save and load preserve block state properties and independent material groups");
        var network=f.getUpdateTag(h.getLevel().registryAccess());
        h.assertTrue(expected.equals(MaterialPalette.CODEC.parse(NbtOps.INSTANCE,network.get("Materials")).getOrThrow()),"Client update contains the full material palette");
        var loot=Block.getDrops(f.getBlockState(),h.getLevel(),f.getBlockPos(),f);
        h.assertTrue(loot.size()==1&&expected.equals(loot.getFirst().get(RacksNStands.MATERIALS.get())),"Dropped fixture carries its appearance");
        var serialized=ItemStack.parseOptional(h.getLevel().registryAccess(),(CompoundTag)loot.getFirst().save(h.getLevel().registryAccess()));
        f.initializeFrom(new ItemStack(f.getBlockState().getBlock()));h.assertTrue(f.materials().equals(MaterialPalette.EMPTY),"Plain placement clears previous customization");
        f.initializeFrom(serialized);h.assertTrue(f.materials().equals(expected),"Replaced fixture recovers all material groups");
        h.assertTrue(!f.setMaterial("invalid",log)&&!f.setMaterial("metal",log),"Unknown and absent material groups are rejected");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void heldBlockOverridesSneakEquipmentSwapUsingServerRay(GameTestHelper h) {
        var f=fixture(h,"helmet_stand");var p=h.makeMockPlayer(GameType.SURVIVAL);
        f.insert(0,new ItemStack(Items.DIAMOND_HELMET),false);p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));
        var held=new ItemStack(Items.OAK_LOG,7);held.set(DataComponents.BLOCK_STATE,BlockItemStateProperties.EMPTY.with(RotatedPillarBlock.AXIS,Direction.Axis.Z));p.setItemInHand(InteractionHand.MAIN_HAND,held);
        p.setShiftKeyDown(true);var origin=Vec3.atLowerCornerOf(f.getBlockPos());p.setPos(origin.x+.5,origin.y+1,origin.z-1.5);
        var target=origin.add(.5,.8,5.25/16);var delta=target.subtract(p.getEyePosition());
        p.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));p.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));
        p.setYHeadRot(p.getYRot());
        var actual=p.pick(p.blockInteractionRange(),1,false);
        h.assertTrue(actual instanceof BlockHitResult ray&&ray.getBlockPos().equals(f.getBlockPos()),"Server ray must hit fixture; actual="+actual.getLocation()+" origin="+origin+" eye="+p.getEyePosition()+" target="+target);
        FixtureInteraction.use(f,p,InteractionHand.MAIN_HAND,new BlockHitResult(origin,Direction.UP,f.getBlockPos(),false));
        h.assertTrue(f.materials().materials().get("wood")!=null&&f.materials().materials().get("wood").getValue(RotatedPillarBlock.AXIS)==Direction.Axis.Z,"Server ray selects wood and honors held block state: "+f.materials()+" hit="+actual.getLocation().subtract(origin)+" shift="+p.isShiftKeyDown()+" build="+p.mayBuild());
        h.assertTrue(f.displayedStack(0).is(Items.DIAMOND_HELMET)&&p.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)&&p.getMainHandItem().getCount()==7,"Texture interaction takes precedence over armor swap");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);FixtureInteraction.use(f,p,InteractionHand.MAIN_HAND,new BlockHitResult(origin,Direction.UP,f.getBlockPos(),false));
        h.assertTrue(f.displayedStack(0).is(Items.IRON_HELMET)&&p.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET),"Empty-hand sneak equipment swap still works");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void rotatedMountedAndUpperPartsTargetOriginalMaterial(GameTestHelper h) {
        var kind=FixtureCatalog.get("generic_tabletop_display");var floor=new Vec3(.5,3.5/16,.5);
        for(var face:AttachFace.values()) for(var direction:Direction.Plane.HORIZONTAL) {
            var mounted=SurfaceDisplayBlock.mount(face,floor);int turns=switch(direction) {case EAST -> 1;case SOUTH -> 2;case WEST -> 3;default -> 0;};
            var rotated=mounted;for(int n=0;n<turns;n++) rotated=new Vec3(1-rotated.z,rotated.y,rotated.x);
            var local=SurfaceDisplayBlock.unmount(face,FixtureInteraction.local(rotated,direction));
            h.assertTrue("cloth".equals(MaterialCustomization.groupAt(kind,local)),"Floor, wall and ceiling material targeting agrees with rotated geometry");
        }
        h.assertTrue("wood".equals(MaterialCustomization.groupAt(FixtureCatalog.get("armor_mannequin"),new Vec3(.5,34.8/16,.5))),"Mannequin head above the upper block shares the wood material");
        h.assertTrue(MaterialCustomization.groupAt(FixtureCatalog.get("sword_floor_stand"),new Vec3(.5,.5,.5))==null,"Empty gap is not an editable part");h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void pedestalBaseBodyAndTopCustomizeIndependently(GameTestHelper h) {
        var f=fixture(h,"generic_pedestal");var kind=FixtureCatalog.get("generic_pedestal");
        h.assertTrue("base".equals(MaterialCustomization.groupAt(kind,new Vec3(.5,0,.5))),"Pedestal bottom has a separate material group");
        h.assertTrue("stone".equals(MaterialCustomization.groupAt(kind,new Vec3(3.0/16,.5,.5))),"Pedestal body retains its stone group");
        h.assertTrue("trim".equals(MaterialCustomization.groupAt(kind,new Vec3(.2,1,.2))),"Pedestal top has its own cap material");
        f.setMaterial("base",Blocks.GOLD_BLOCK.defaultBlockState());f.setMaterial("stone",Blocks.BRICKS.defaultBlockState());f.setMaterial("trim",Blocks.QUARTZ_BLOCK.defaultBlockState());
        h.assertTrue(f.materials().materials().get("base").is(Blocks.GOLD_BLOCK)&&f.materials().materials().get("stone").is(Blocks.BRICKS)&&f.materials().materials().get("trim").is(Blocks.QUARTZ_BLOCK),"All three pedestal selections remain independent");h.succeed();
    }
}
