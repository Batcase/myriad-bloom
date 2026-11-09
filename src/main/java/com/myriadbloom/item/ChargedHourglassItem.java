package com.myriadbloom.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 已充能的时之沙漏：可用 20 次。
 *
 * <p>使用后冻结周围 50x50 内的所有实体 7 秒；在霜境对着冰山前的祭坛使用，则不消耗次数地终止风雪。</p>
 */
public class ChargedHourglassItem extends HourglassItem {
    /** 总使用次数。 */
    public static final int USES = 20;
    private static final int FREEZE_RADIUS = 25;
    private static final int FREEZE_TICKS = 140;

    public ChargedHourglassItem(Properties properties) {
        super(properties.durability(USES), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int left = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.add(Component.translatable("item.myriadbloom.hourglass.charged.uses", left)
                .withStyle(ChatFormatting.GOLD));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        ServerLevel server = (ServerLevel) level;
        com.myriadbloom.MyriadBloom.LOGGER.info("charged hourglass used by {} in {}",
                player.getGameProfile().getName(), level.dimension().location());

        // 对着祭坛使用：不消耗次数，终止风雪
        BlockPos altar = findAltar(server, player);
        if (altar != null) {
            server.setWeatherParameters(6000, 0, false, false);
            // 关闭暴风雪（霜境的风雪由 BlizzardMechanic 模拟，/weather 无法停止）
            try {
                com.myriadbloom.FrostConfig.BLIZZARD_ENABLED.set(false);
                com.myriadbloom.FrostConfig.BLIZZARD_ENABLED.save();
            } catch (Throwable ignored) {
                // 配置不可写时忽略，至少天气已放晴
            }
            server.playSound(null, altar, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 2.0F, 0.6F);
            if (player instanceof ServerPlayer serverPlayer) {
                com.myriadbloom.world.AdvancementAward.award(serverPlayer, "storm_clear", "done");
            }
            return InteractionResultHolder.success(stack);
        }

        // 普通使用：冻结周围实体 7 秒
        AABB area = player.getBoundingBox().inflate(FREEZE_RADIUS);
        var targets = server.getEntities(player, area, e -> e != player).stream()
                .filter(net.minecraft.world.entity.Entity::isAlive)
                .toList();
        com.myriadbloom.world.TimeStopMechanic.freeze(targets, FREEZE_TICKS);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.0D, player.getZ(), 120, 8.0D, 3.0D, 8.0D, 0.05D);
        server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 2.0F, 0.5F);

        // 消耗一次
        stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + 1));
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            player.setItemInHand(hand, new ItemStack(com.myriadbloom.BossContent.HOURGLASS.get()));
        }
        return InteractionResultHolder.success(stack);
    }

    private static BlockPos findAltar(ServerLevel level, Player player) {
        if (!com.myriadbloom.api.MyriadBloomApi.isMyriadBloom(level)) {
            return null;
        }
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos pos = player.blockPosition().offset(dx, dy, dz);
                    if (level.getBlockState(pos).is(com.myriadbloom.FrostedLandContent.TIME_ALTAR.get())) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
}
