package com.myriadbloom.world.block;

import com.myriadbloom.BossContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 摆件的方块实体：只存位置，类型与朝向都在方块状态里。 */
public class TrophyBlockEntity extends BlockEntity {
    public TrophyBlockEntity(BlockPos pos, BlockState state) {
        super(BossContent.TROPHY_ENTITY.get(), pos, state);
    }
}
