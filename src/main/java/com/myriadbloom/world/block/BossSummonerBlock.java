package com.myriadbloom.world.block;

import com.myriadbloom.BossContent;
import com.myriadbloom.FrostMobs;
import com.myriadbloom.world.ArenaGuard;
import com.myriadbloom.world.entity.GlacialConglomeration;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 极冰聚合物召唤器 (boss summoner): a stone pedestal with a recess on top.
 *
 * <p>Placing the 始源人战利品 into the recess consumes both the trophy and the pedestal - neither drops
 * anything - and wakes 极冰聚合物 on the spot, which seals the arena floor until the boss dies.</p>
 */
public class BossSummonerBlock extends Block {
    public static final MapCodec<BossSummonerBlock> CODEC = simpleCodec(BossSummonerBlock::new);
    /** Radius of the congealed ice floor that is sealed while the boss lives. */
    public static final int ARENA_RADIUS = 40;

    public BossSummonerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, net.minecraft.world.InteractionHand hand,
                                              BlockHitResult hit) {
        if (!stack.is(BossContent.ORIGIN_APE_TROPHY.get())) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("block.myriadbloom.boss_summoner.hint"), true);
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel server) {
            // the trophy and the pedestal are both consumed, and neither drops an item
            stack.shrink(1);
            ignite(server, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Consumes the pedestal, wakes 极冰聚合物 and seals the arena floor. Shared by the block and the
     * {@code /myriadbloom summon_boss} debug command.
     */
    public static void ignite(ServerLevel server, BlockPos pos) {
        server.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());

        GlacialConglomeration boss = FrostMobs.GLACIAL_CONGLOMERATION.get().create(server);
        if (boss != null) {
            Vec3 centre = Vec3.atBottomCenterOf(pos);
            boss.moveTo(centre.x, centre.y, centre.z, server.random.nextFloat() * 360.0F, 0.0F);
            boss.setPersistenceRequired();
            server.addFreshEntity(boss);
            ArenaGuard.of(server).seal(pos, ARENA_RADIUS);
        }

        server.sendParticles(ParticleTypes.SNOWFLAKE,
                pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D, 120, 2.0D, 1.2D, 2.0D, 0.08D);
        server.sendParticles(ParticleTypes.EXPLOSION,
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 8, 1.5D, 0.5D, 1.5D, 0.02D);
        server.playSound(null, pos, SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0F, 0.6F);
    }
}
