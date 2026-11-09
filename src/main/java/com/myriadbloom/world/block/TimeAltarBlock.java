package com.myriadbloom.world.block;

import com.myriadbloom.BossContent;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.world.TimeVoidArenaMechanic;
import com.myriadbloom.world.entity.TimeKeeper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 时之祭坛：1.5 格高的祭坛，放在凝结之地中央冰山前。
 *
 * <p>把未充能的时之沙漏放上去，就会把玩家拉进时之虚空，并唤醒时间维护者。</p>
 */
public class TimeAltarBlock extends Block {
    /** 1.5 格高 = 24/16。 */
    private static final VoxelShape SHAPE = Block.box(0.5D, 0.0D, 0.5D, 15.5D, 24.0D, 15.5D);
    private static final VoxelShape TOP_SHAPE = Block.box(0.0D, 16.0D, 0.0D, 16.0D, 24.0D, 16.0D);

    public TimeAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos,
                                     CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return TOP_SHAPE;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(BossContent.HOURGLASS.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ServerLevel voidLevel = serverPlayer.server.getLevel(TimeVoidArenaMechanic.TIME_VOID);
        if (voidLevel == null) {
            return ItemInteractionResult.FAIL;
        }
        // 记下返回位置
        var data = serverPlayer.getPersistentData();
        data.putString("myriadbloom_return_dim", serverPlayer.serverLevel().dimension().location().toString());
        data.putDouble("myriadbloom_return_x", serverPlayer.getX());
        data.putDouble("myriadbloom_return_y", serverPlayer.getY());
        data.putDouble("myriadbloom_return_z", serverPlayer.getZ());
        data.putFloat("myriadbloom_return_yaw", serverPlayer.getYRot());
        data.putFloat("myriadbloom_return_pitch", serverPlayer.getXRot());
        data.putLong("myriadbloom_altar_pos", pos.asLong());
        data.putString("myriadbloom_altar_dim", serverPlayer.serverLevel().dimension().location().toString());

        stack.shrink(1);
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.5F, 0.6F);
        ((ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                pos.getX() + 0.5D, pos.getY() + 1.6D, pos.getZ() + 0.5D, 80, 0.6D, 1.2D, 0.6D, 0.12D);

        serverPlayer.teleportTo(voidLevel, 0.5D, 66.0D, 0.5D, serverPlayer.getYRot(), serverPlayer.getXRot());
        // 唤醒时间维护者（若场地里还没有）
        if (voidLevel.getEntitiesOfClass(TimeKeeper.class, new net.minecraft.world.phys.AABB(-40, 40, -40, 40, 120, 120)).isEmpty()) {
            TimeKeeper keeper = com.myriadbloom.FrostMobs.TIME_KEEPER.get().create(voidLevel);
            if (keeper != null) {
                keeper.moveTo(0.5D, 65.0D, 0.5D, 0.0F, 0.0F);
                voidLevel.addFreshEntity(keeper);
            }
        }
        MyriadBloom.LOGGER.info("Time keeper awakened for {}", serverPlayer.getGameProfile().getName());
        return ItemInteractionResult.SUCCESS;
    }
}
