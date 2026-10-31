package com.myriadbloom.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 风暴棱镜的召唤核心 (storm prism spawner): a crystal socket in the floor of the central boss hall.
 *
 * <p>The block does nothing on its own - the maze mechanic watches it and, once a player steps into the
 * hall, wakes the boss and shatters the socket.</p>
 */
public class StormPrismSpawnerBlock extends Block {
    /** True while the socket is still waiting: the mechanic uses it to avoid double spawns. */
    public static final BooleanProperty ARMED = BooleanProperty.create("armed");

    public StormPrismSpawnerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ARMED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ARMED);
    }

    /** Keeps the socket from being pushed by pistons: it is part of the arena. */
    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, net.minecraft.world.level.BlockGetter level,
                                             BlockPos pos) {
        return false;
    }
}
