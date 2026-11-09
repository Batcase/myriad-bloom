package com.myriadbloom.world;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * 时间停止：被冻结的实体在持续时间内每 tick 都被强制静止（AI 关闭、速度归零）。
 *
 * <p>不依赖 {@code setNoAi} 一次设置，避免某些生物的 AI 绕过。</p>
 */
public final class TimeStopMechanic implements EnvironmentMechanic {
    private static final ResourceLocation ID = MyriadBloomApi.id("time_stop");
    private static final List<Frozen> FROZEN = new CopyOnWriteArrayList<>();

    private record Frozen(Entity entity, long until) {
    }

    /** 冻结若干实体若干 tick。 */
    public static void freeze(List<? extends Entity> entities, long ticks) {
        for (Entity entity : entities) {
            FROZEN.add(new Frozen(entity, entity.level().getGameTime() + ticks));
            entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
            if (entity instanceof Mob mob) {
                mob.setNoAi(true);
            }
        }
        MyriadBloom.LOGGER.info("time stop: {} entities frozen for {} ticks", entities.size(), ticks);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void onLevelTick(ServerLevel level) {
        if (FROZEN.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (Frozen frozen : List.copyOf(FROZEN)) {
            Entity entity = frozen.entity();
            if (entity.level() != level) {
                continue;
            }
            if (!entity.isAlive() || now >= frozen.until()) {
                if (entity instanceof Mob mob) {
                    mob.setNoAi(false);
                }
                FROZEN.remove(frozen);
                continue;
            }
            entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
            entity.hurtMarked = true;
            if (now % 4 == 0) { drawClock(level, entity); }
            if (entity instanceof Mob mob) {
                mob.setNoAi(true);
                mob.getNavigation().stop();
                mob.setTarget(null);
            }
        }
    }

    @Override
    public void forgetPlayer(UUID playerId) {
    }

    @Override
    public String debugState() {
        return "time_stop:" + FROZEN.size();
    }

    /** 用粒子在目标脚底画一个不动的时钟：外圈 + 刻度 + 两根固定指针。 */
    private static void drawClock(ServerLevel level, Entity target) {
        double cx = target.getX();
        double cy = target.getY() + 0.06D;
        double cz = target.getZ();
        double radius = 1.15D;
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2.0D * i / 24.0D;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    cx + Math.cos(angle) * radius, cy, cz + Math.sin(angle) * radius, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2.0D * i / 12.0D;
            double r = i % 3 == 0 ? 0.92D : 1.02D;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    cx + Math.cos(angle) * r, cy, cz + Math.sin(angle) * r, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        double hourAngle = Math.toRadians(-60.0D);
        double minuteAngle = Math.toRadians(35.0D);
        for (int i = 1; i <= 5; i++) {
            double r = 0.55D * i / 5.0D;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    cx + Math.cos(hourAngle) * r, cy + 0.01D, cz + Math.sin(hourAngle) * r, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        for (int i = 1; i <= 8; i++) {
            double r = 0.85D * i / 8.0D;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    cx + Math.cos(minuteAngle) * r, cy + 0.01D, cz + Math.sin(minuteAngle) * r, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}