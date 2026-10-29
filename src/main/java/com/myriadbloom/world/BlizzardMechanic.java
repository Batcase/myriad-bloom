package com.myriadbloom.world;

import com.myriadbloom.FrostConfig;
import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The realm is permanently caught in a blizzard.
 *
 * <p>Minecraft's rain/thunder state is global (every dimension shares the overworld's weather), so a
 * per-dimension storm cannot be done with {@code /weather}. Instead the blizzard is simulated where
 * it matters: a constant, dense snowflake fall around everyone inside the realm, paired with the
 * short fog and dark tint applied on the client ({@code FrostClientGameEvents}). Because it is not
 * the vanilla weather system, {@code /weather clear} cannot end it.</p>
 */
public final class BlizzardMechanic implements EnvironmentMechanic {
    private static final ResourceLocation ID = MyriadBloomApi.id("blizzard");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void onLevelTick(ServerLevel level) {
        if (!FrostConfig.BLIZZARD_ENABLED.get()) {
            return;
        }
        if (level.getGameTime() % 4L != 0L) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            Vec3 pos = player.position();
            double x = pos.x + (level.random.nextDouble() - 0.5D) * 20.0D;
            double y = pos.y + 4.0D + level.random.nextDouble() * 8.0D;
            double z = pos.z + (level.random.nextDouble() - 0.5D) * 20.0D;
            level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 3, 6.0D, 3.0D, 6.0D, 0.01D);
        }
    }
}
