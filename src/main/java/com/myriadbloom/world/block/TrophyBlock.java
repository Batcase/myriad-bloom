package com.myriadbloom.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 战利品摆件 (trophy): a boss head, placed on the ground or hung on a wall like a vanilla skull.
 *
 * <p>Floor placement uses the sixteen skull rotations; wall placement stores the horizontal facing.
 * The head itself is drawn by {@code TrophyRenderer}.</p>
 */
public class TrophyBlock extends BaseEntityBlock {
    public static final MapCodec<TrophyBlock> CODEC = simpleCodec(TrophyBlock::new);
    public static final BooleanProperty GLACIAL = BooleanProperty.create("glacial");
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 15);
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    public static final DirectionProperty FACING = DirectionProperty.create("facing",
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final VoxelShape FLOOR_SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 8.0D, 12.0D);

    public TrophyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(GLACIAL, false).setValue(ROTATION, 0).setValue(WALL, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GLACIAL, ROTATION, WALL, FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrophyBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** 贴墙时判定箱贴在墙面上，落地时只有头那么大。 */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(WALL)) {
            return switch (state.getValue(FACING)) {
                case SOUTH -> Block.box(4.0D, 4.0D, 0.0D, 12.0D, 12.0D, 8.0D);
                case EAST -> Block.box(0.0D, 4.0D, 4.0D, 8.0D, 12.0D, 12.0D);
                case WEST -> Block.box(8.0D, 4.0D, 4.0D, 16.0D, 12.0D, 12.0D);
                default -> Block.box(4.0D, 4.0D, 8.0D, 12.0D, 12.0D, 16.0D);
            };
        }
        return FLOOR_SHAPE;
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return this.getShape(state, level, pos, context);
    }

    /** 点方块的侧面就挂墙，点顶面就落地。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        if (clicked.getAxis().isHorizontal()) {
            return this.defaultBlockState()
                    .setValue(WALL, true)
                    .setValue(FACING, clicked);
        }
        int rotation = Math.floorMod(Math.round(context.getRotation() * 16.0F / 360.0F), 16);
        return this.defaultBlockState().setValue(WALL, false).setValue(ROTATION, rotation);
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        if (state.getValue(WALL)) {
            BlockPos behind = pos.relative(state.getValue(FACING));
            return level.getBlockState(behind).isSolidRender(level, behind);
        }
        BlockPos below = pos.below();
        return level.getBlockState(below).isSolidRender(level, below);
    }
}
