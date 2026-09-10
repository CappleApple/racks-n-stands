package com.cappleapple.racksnstands.block;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.interaction.FixtureInteraction;
import com.cappleapple.racksnstands.registry.FixtureCatalog;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public class FixtureBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    private final FixtureCatalog.Kind kind;
    private final VoxelShape[][] shapes=new VoxelShape[2][4];
    public FixtureBlock(FixtureCatalog.Kind kind,Properties properties) {
        super(properties);this.kind=kind;
        var geometry=FixtureGeometry.selectionParts(kind);
        for(var facing:Direction.Plane.HORIZONTAL) for(int half=0;half<2;half++) shapes[half][facing.get2DDataValue()]=FixtureGeometry.shape(geometry,facing,half==1);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(HALF,DoubleBlockHalf.LOWER));
    }
    public FixtureCatalog.Kind kind() { return kind; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return MapCodec.unit(this); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(FACING,HALF); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return state.getValue(HALF)==DoubleBlockHalf.UPPER?null:new FixtureBlockEntity(pos,state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
        if(kind.tall()&&(c.getClickedPos().getY()>=c.getLevel().getMaxBuildHeight()-1||!c.getLevel().getBlockState(c.getClickedPos().above()).canBeReplaced(c))) return null;
        Direction facing=c.getHorizontalDirection().getOpposite();
        if(kind.wall()&&c.getClickedFace().getAxis().isHorizontal()) facing=c.getClickedFace();
        return defaultBlockState().setValue(FACING,facing);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
        if(kind.tall()) level.setBlock(pos.above(),state.setValue(HALF,DoubleBlockHalf.UPPER),3);
        if(level.getBlockEntity(pos) instanceof FixtureBlockEntity fixture) fixture.initializeFrom(stack);
    }
    public static BlockPos base(BlockPos pos,BlockState state) { return state.getValue(HALF)==DoubleBlockHalf.UPPER?pos.below():pos; }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(level.isClientSide) return ItemInteractionResult.SUCCESS;
        if(level.getBlockEntity(base(pos,state)) instanceof FixtureBlockEntity fixture) FixtureInteraction.use(fixture,player,hand,hit);
        return ItemInteractionResult.CONSUME;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(!level.isClientSide && level.getBlockEntity(base(pos,state)) instanceof FixtureBlockEntity fixture) FixtureInteraction.use(fixture,player,InteractionHand.MAIN_HAND,hit);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(level.getBlockEntity(pos) instanceof FixtureBlockEntity fixture) fixture.scheduledRepair();
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock())) {
            if(!level.isClientSide) {
                if(level.getBlockEntity(pos) instanceof FixtureBlockEntity fixture) fixture.dropContents();
                if(kind.tall()) {
                    BlockPos other=state.getValue(HALF)==DoubleBlockHalf.LOWER?pos.above():pos.below();
                    if(level.getBlockState(other).is(this)) {
                        if(state.getValue(HALF)==DoubleBlockHalf.UPPER) level.destroyBlock(other,true);
                        else level.removeBlock(other,false);
                    }
                }
                level.updateNeighbourForOutputSignal(pos,this);
            }
            super.onRemove(state,level,pos,next,moving);
        }
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player) {
        if(!level.isClientSide&&player.isCreative()&&kind.tall()&&state.getValue(HALF)==DoubleBlockHalf.UPPER) level.destroyBlock(pos.below(),false);
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos) {
        return level.getBlockEntity(base(pos,state)) instanceof FixtureBlockEntity fixture?fixture.comparator():0;
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return shapes[state.getValue(HALF)==DoubleBlockHalf.UPPER?1:0][state.getValue(FACING).get2DDataValue()];
    }
    @Override protected BlockState rotate(BlockState state,Rotation rotation) { return state.setValue(FACING,rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state,Mirror mirror) { return rotate(state,mirror.getRotation(state.getValue(FACING))); }
}
