package com.myriadbloom.world.block;

import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 冰台阶 (ice step): the stair block of the storm woodland maze.
 *
 * <p>It behaves like a packed ice stair: cold, glassy, cheap to break. The maze builder places it as
 * the spiral descent of the four entrances.</p>
 */
public class IceStepBlock extends StairBlock {
    public IceStepBlock(BlockState baseState, BlockBehaviour.Properties properties) {
        super(baseState, properties);
    }
}
