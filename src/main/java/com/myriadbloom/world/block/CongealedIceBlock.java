package com.myriadbloom.world.block;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 凝结之冰 (congealed ice): the ground of the congealed land.
 *
 * <p>Obsidian hard, never melts near heat and ignores light. Only the snow covered pickaxe mines it
 * quickly - with that tool the block behaves like stone.</p>
 */
public class CongealedIceBlock extends Block {
    /** Obsidian tier: needs a diamond pickaxe and takes forever. */
    public static final float HARDNESS = 50.0F;

    public CongealedIceBlock(Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.ICE)
                .strength(HARDNESS, 1200.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.GLASS)
                .noOcclusion();
    }

    /** The snow covered pickaxe chews through it as if it were stone. */
    @Override
    public float getDestroyProgress(net.minecraft.world.level.block.state.BlockState state, Player player,
                                    net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        if (player.getMainHandItem().is(com.myriadbloom.EmberContent.FROST_PICKAXE.get())) {
            // stone hardness with the right tool
            float stone = 1.5F;
            return player.getDestroySpeed(state) / stone / (player.getDestroySpeed(state) > 1.0F ? 30.0F : 100.0F);
        }
        return super.getDestroyProgress(state, player, level, pos);
    }
}
