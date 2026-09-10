package com.cappleapple.racksnstands.block;
import com.cappleapple.racksnstands.registry.FixtureCatalog;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;
import java.util.List;

/** A single-item display attached to any of the six block faces. */
public final class SurfaceDisplayBlock extends FixtureBlock {
    public static final EnumProperty<AttachFace> FACE=BlockStateProperties.ATTACH_FACE;
    private final VoxelShape[][] mountedShapes=new VoxelShape[3][4];
    public SurfaceDisplayBlock(FixtureCatalog.Kind kind,Properties properties) {
        super(kind,properties);registerDefaultState(defaultBlockState().setValue(FACE,AttachFace.FLOOR));
        for(var face:AttachFace.values()) for(var facing:Direction.Plane.HORIZONTAL)
            mountedShapes[face.ordinal()][facing.get2DDataValue()]=FixtureGeometry.shape(parts(kind,face),facing,false);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { super.createBlockStateDefinition(builder);builder.add(FACE); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var clicked=context.getClickedFace();
        return placement(clicked,context.getHorizontalDirection());
    }
    public BlockState placement(Direction clicked,Direction playerFacing) {
        var face=clicked==Direction.UP?AttachFace.FLOOR:clicked==Direction.DOWN?AttachFace.CEILING:AttachFace.WALL;
        return defaultBlockState().setValue(FACE,face).setValue(FACING,face==AttachFace.WALL?clicked:playerFacing.getOpposite());
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return mountedShapes[state.getValue(FACE).ordinal()][state.getValue(FACING).get2DDataValue()];
    }
    public static Vec3 mount(AttachFace face,Vec3 point) {
        return switch(face) { case WALL -> new Vec3(point.x,point.z,1-point.y);case CEILING -> new Vec3(point.x,1-point.y,1-point.z);default -> point; };
    }
    public static Vec3 unmount(AttachFace face,Vec3 point) {
        return face==AttachFace.WALL?new Vec3(point.x,1-point.z,point.y):mount(face,point);
    }
    public static List<FixtureGeometry.Part> parts(FixtureCatalog.Kind kind,AttachFace face) {
        return FixtureGeometry.parts(kind).stream().map(p -> {
            var a=mount(face,new Vec3(p.x()/16,p.y()/16,p.z()/16));var b=mount(face,new Vec3(p.xx()/16,p.yy()/16,p.zz()/16));
            return new FixtureGeometry.Part(Math.min(a.x,b.x)*16,Math.min(a.y,b.y)*16,Math.min(a.z,b.z)*16,
                Math.max(a.x,b.x)*16,Math.max(a.y,b.y)*16,Math.max(a.z,b.z)*16,p.texture());
        }).toList();
    }
}
