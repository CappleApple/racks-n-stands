package com.cappleapple.racksnstands.gametest;
import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.block.FixtureBlock;
import com.cappleapple.racksnstands.registry.FixtureCatalog;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(RacksNStands.MOD_ID) @PrefixGameTestTemplate(false)
public final class GeometryGameTests {
    private static boolean solid(VoxelShape shape,double x,double y,double z) { return shape.toAabbs().stream().anyMatch(b -> b.contains(x,y,z)); }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void furnitureShapesFollowSolidsAndFacing(GameTestHelper h) {
        var pos=new BlockPos(1,2,1);
        var stand=RacksNStands.FIXTURES.get("sword_floor_stand").get().defaultBlockState();h.setBlock(pos,stand);
        var shape=stand.getShape(h.getLevel(),h.absolutePos(pos));
        h.assertTrue(solid(shape,.28125,.4,.5)&&solid(shape,.71875,.4,.5),"Two centered supports are selectable and solid");
        h.assertTrue(!solid(shape,.5,.4,.5)&&!solid(shape,.1,.5,.1),"Support gap and empty corners have no full-block hitbox");
        var table=RacksNStands.FIXTURES.get("generic_tabletop_display").get().defaultBlockState();
        h.assertTrue(table.getShape(h.getLevel(),h.absolutePos(pos)).max(Direction.Axis.Y)<.25,"Tabletop has a low furniture shape");
        var shelf=RacksNStands.FIXTURES.get("curio_cabinet").get().defaultBlockState();
        h.assertTrue(FixtureCatalog.get("curio_cabinet").wall(),"Shelf uses wall-face placement");
        for(var direction:Direction.Plane.HORIZONTAL) {
            var wall=shelf.setValue(FixtureBlock.FACING,direction);var s=wall.getShape(h.getLevel(),h.absolutePos(pos));
            double x=direction==Direction.EAST?.02:direction==Direction.WEST?.98:.5;
            double z=direction==Direction.NORTH?.98:direction==Direction.SOUTH?.02:.5;
            h.assertTrue(solid(s,x,.3,z),"Shelf backing is flush with the wall in every facing");
            h.assertTrue(!solid(s,1-x,.3,1-z),"Space in front of the shelf remains empty");
        }
        var upper=RacksNStands.FIXTURES.get("armor_mannequin").get().defaultBlockState().setValue(FixtureBlock.HALF,DoubleBlockHalf.UPPER);
        var top=upper.getShape(h.getLevel(),h.absolutePos(pos));
        h.assertTrue(solid(top,.3,1.1,.5)&&solid(top,.5,.75,.5)&&!solid(top,.1,.75,.1),"Mannequin upper hitbox follows the head instead of a full cube");
        h.succeed();
    }
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void displayMountsOnEverySurfaceAndPreservesStorage(GameTestHelper h) {
        var display=(com.cappleapple.racksnstands.block.SurfaceDisplayBlock)RacksNStands.FIXTURES.get("generic_tabletop_display").get();
        var pos=new BlockPos(1,2,1);h.setBlock(pos,display.defaultBlockState());
        var fixture=(com.cappleapple.racksnstands.blockentity.FixtureBlockEntity)h.getBlockEntity(pos);
        fixture.insert(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.APPLE),false);
        for(var clicked:Direction.values()) {
            var state=display.placement(clicked,Direction.EAST);h.setBlock(pos,state);
            var face=state.getValue(com.cappleapple.racksnstands.block.SurfaceDisplayBlock.FACE);var box=state.getShape(h.getLevel(),h.absolutePos(pos)).bounds();
            switch(clicked) {
                case UP -> h.assertTrue(face==net.minecraft.world.level.block.state.properties.AttachFace.FLOOR&&box.minY==0&&box.maxY<.25,"Floor display rests on its surface");
                case DOWN -> h.assertTrue(face==net.minecraft.world.level.block.state.properties.AttachFace.CEILING&&box.maxY==1&&box.minY>.75,"Ceiling display sits against its surface");
                case NORTH -> h.assertTrue(box.maxZ==1&&box.minZ>.75,"North wall attachment is flush");
                case SOUTH -> h.assertTrue(box.minZ==0&&box.maxZ<.25,"South wall attachment is flush");
                case EAST -> h.assertTrue(box.minX==0&&box.maxX<.25,"East wall attachment is flush");
                case WEST -> h.assertTrue(box.maxX==1&&box.minX>.75,"West wall attachment is flush");
            }
            var point=new net.minecraft.world.phys.Vec3(.2,.23,.7);
            var mounted=com.cappleapple.racksnstands.block.SurfaceDisplayBlock.mount(face,point);
            h.assertTrue(com.cappleapple.racksnstands.block.SurfaceDisplayBlock.unmount(face,mounted).distanceTo(point)<.00001,"Mounted item and targeting coordinates round-trip");
            h.assertTrue(((com.cappleapple.racksnstands.blockentity.FixtureBlockEntity)h.getBlockEntity(pos)).displayedStack(0).is(net.minecraft.world.item.Items.APPLE),"Changing attachment keeps the stored item");
        }
        h.succeed();
    }

    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void armorPlacementAndExpandedChestTargets(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var pos=h.absolutePos(new BlockPos(1,2,1));
        for(var direction:Direction.Plane.HORIZONTAL) {
            player.setYRot(direction.toYRot());
            for(String id:java.util.List.of("helmet_stand","chestplate_stand","leggings_stand","boots_stand","armor_mannequin")) {
                var block=(FixtureBlock)RacksNStands.FIXTURES.get(id).get();
                var context=new net.minecraft.world.item.context.BlockPlaceContext(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(block),new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false));
                var state=block.getStateForPlacement(context);
                h.assertTrue(state.getValue(FixtureBlock.FACING)==direction.getOpposite(),"Armor stand placement points toward the player");
                var transform=com.cappleapple.racksnstands.display.BuiltinProfiles.create(block.kind()).slots().getFirst().transform();
                var front=transform.quaternion().transform(new org.joml.Vector3f(0,0,1));
                new org.joml.Quaternionf().rotationY((float)Math.toRadians(180-state.getValue(FixtureBlock.FACING).toYRot())).transform(front);
                h.assertTrue(front.dot(new org.joml.Vector3f(direction.getOpposite().getStepX(),0,direction.getOpposite().getStepZ()))>.999,"Worn armor faces the placing player in all directions");
            }
            var block=(FixtureBlock)RacksNStands.FIXTURES.get("armor_mannequin").get();
            var upper=block.defaultBlockState().setValue(FixtureBlock.FACING,direction).setValue(FixtureBlock.HALF,DoubleBlockHalf.UPPER);
            h.assertTrue(upper.getShape(h.getLevel(),pos).toAabbs().stream().anyMatch(b -> b.getXsize()>.45&&b.getZsize()>.45),"Expanded torso includes broad chest and sleeves");
        }
        var profile=com.cappleapple.racksnstands.display.BuiltinProfiles.create(FixtureCatalog.get("armor_mannequin"));
        h.assertTrue(com.cappleapple.racksnstands.interaction.FixtureInteraction.slotAt(profile,new net.minecraft.world.phys.Vec3(.2,1.6,.3))==1,"Upper chest targets chest equipment rather than the helmet");
        h.assertTrue(com.cappleapple.racksnstands.interaction.FixtureInteraction.slotAt(profile,new net.minecraft.world.phys.Vec3(.5,1.85,.5))==0,"Head remains separately selectable");
        h.assertTrue(com.cappleapple.racksnstands.interaction.FixtureInteraction.slotAt(profile,new net.minecraft.world.phys.Vec3(.5,.6,.5))==2,"Leggings retain their selection region");
        h.succeed();
    }

}
