package com.myriadbloom.world;

import com.myriadbloom.FrostConfig;
import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Blizzard and snow tornadoes of the 凝结之地.
 *
 * <p>While a player stands in the congealed land the air is filled with driving snow and a low wind,
 * and now and then a snow tornado (雪龙卷风) forms on the ground nearby: a spinning column of snow that
 * drifts with the wind and drags anything close towards its core. Everything is particle based, so no
 * extra entity, renderer or packet type is needed.</p>
 */
public class CongealedBlizzardMechanic implements EnvironmentMechanic {
    private static int particlesPerPlayer() {
        return FrostConfig.CONGEALED_BLIZZARD_PARTICLES.get();
    }
    /** Chance per player per second to spawn a tornado. */
    private static float tornadoChance() {
        return (float) (double) FrostConfig.CONGEALED_TORNADO_CHANCE.get();
    }
    private static int tornadoLife() {
        return FrostConfig.CONGEALED_TORNADO_LIFETIME.get();
    }
    private static final double TORNADO_PULL_RADIUS = 6.0D;
    private static int maxTornadoes() {
        return FrostConfig.CONGEALED_TORNADO_MAX.get();
    }

    private final List<Tornado> tornadoes = new ArrayList<>();

    private static final class Tornado {
        double x;
        double y;
        double z;
        int life;
        final double driftX;
        final double driftZ;

        Tornado(double x, double y, double z, int life, double driftX, double driftZ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.life = life;
            this.driftX = driftX;
            this.driftZ = driftZ;
        }
    }

    @Override
    public ResourceLocation id() {
        return MyriadBloomApi.id("congealed_blizzard");
    }

    @Override
    public void onLevelTick(ServerLevel level) {
        if (!MyriadBloomApi.isMyriadBloom(level)) {
            return;
        }
        long time = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            if (!CongealedLandMechanic.isInside(player)
                    || player.isCreative() || player.isSpectator()) {
                // the storm itself is drawn for everybody, but it only pushes survival players
                continue;
            }
            // the storm shoves you along: a slow, steady wind from one direction
            if (time % 2L == 0L) {
                double windAngle = (level.getGameTime() % 2400L) / 2400.0D * Math.PI * 2.0D;
                player.push(Math.cos(windAngle) * 0.012D, 0.0D, Math.sin(windAngle) * 0.012D);
                player.hurtMarked = true;
            }
            if (time % 5L == 0L) {
                Vec3 eye = player.position().add(0.0D, 1.0D, 0.0D);
                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        eye.x, eye.y, eye.z, particlesPerPlayer(),
                        9.0D, 5.0D, 9.0D, 0.35D);
                if (player.getRandom().nextFloat() < 0.12F) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.5F, 0.55F);
                }
            }
            if (time % 20L == 0L && FrostConfig.CONGEALED_TORNADOES_ENABLED.get()
                    && this.tornadoes.size() < maxTornadoes()
                    && player.getRandom().nextFloat() < tornadoChance()) {
                this.spawnTornado(level, player);
            }
        }
        this.tickTornadoes(level);
    }

    private void spawnTornado(ServerLevel level, ServerPlayer player) {
        float angle = player.getRandom().nextFloat() * ((float) Math.PI * 2.0F);
        double distance = 10.0D + player.getRandom().nextDouble() * 10.0D;
        double x = player.getX() + Math.cos(angle) * distance;
        double z = player.getZ() + Math.sin(angle) * distance;
        double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) Math.floor(x), (int) Math.floor(z));
        double wind = Math.PI * 2.0D * player.getRandom().nextDouble();
        this.tornadoes.add(new Tornado(x, y, z, tornadoLife(),
                Math.cos(wind) * 0.06D, Math.sin(wind) * 0.06D));
        level.playSound(null, x, y, z, SoundEvents.AMBIENT_CAVE.value(), SoundSource.WEATHER, 1.1F, 0.5F);
    }

    private void tickTornadoes(ServerLevel level) {
        if (this.tornadoes.isEmpty()) {
            return;
        }
        this.tornadoes.removeIf(tornado -> --tornado.life <= 0);
        for (Tornado tornado : this.tornadoes) {
            tornado.x += tornado.driftX;
            tornado.z += tornado.driftZ;
            tornado.y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (int) Math.floor(tornado.x), (int) Math.floor(tornado.z));
            this.drawTornado(level, tornado);
            AABB area = new AABB(tornado.x - TORNADO_PULL_RADIUS, tornado.y - 2.0D, tornado.z - TORNADO_PULL_RADIUS,
                    tornado.x + TORNADO_PULL_RADIUS, tornado.y + 14.0D, tornado.z + TORNADO_PULL_RADIUS);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
                if (victim instanceof net.minecraft.world.entity.player.Player p
                        && (p.isCreative() || p.isSpectator())) {
                    continue;
                }
                double dx = tornado.x - victim.getX();
                double dz = tornado.z - victim.getZ();
                double length = Math.max(0.6D, Math.sqrt(dx * dx + dz * dz));
                victim.push(dx / length * 0.11D, 0.07D, dz / length * 0.11D);
                victim.hurtMarked = true;
            }
        }
    }

    private void drawTornado(ServerLevel level, Tornado tornado) {
        int rotation = tornado.life * 14;
        for (int i = 0; i < 26; i++) {
            double angle = Math.toRadians(rotation + i * 24.0D);
            double radius = 1.2D + (i % 6) * 0.55D;
            double y = tornado.y + 0.2D + i * 0.55D;
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    tornado.x + Math.cos(angle) * radius, y, tornado.z + Math.sin(angle) * radius,
                    1, 0.05D, 0.05D, 0.05D, 0.01D);
        }
        level.sendParticles(ParticleTypes.CLOUD, tornado.x, tornado.y + 0.2D, tornado.z, 2, 0.6D, 0.1D, 0.6D, 0.05D);
    }
}
