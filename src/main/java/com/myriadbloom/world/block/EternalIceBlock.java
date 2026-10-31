package com.myriadbloom.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 永恒之冰 (eternal ice): almost transparent, unbreakable, seamless.
 *
 * <p>The texture is a single flat colour with no bevel, and the block is rendered without ambient
 * occlusion, so a wall of it reads as one body of ice instead of a grid of cubes.</p>
 */
public class EternalIceBlock extends Block {
    private static final VoxelShape FULL = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public EternalIceBlock(Properties properties) {
        super(properties);
    }

    /** 不能破坏：硬度 -1 已经足够，这里再兜一层。 */
    @Override
    public float getDestroyProgress(BlockState state, net.minecraft.world.entity.player.Player player,
                                    BlockGetter level, BlockPos pos) {
        return 0.0F;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        // 实心：谁都穿不过去
        return FULL;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
