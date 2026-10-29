package com.myriadbloom.world;

import com.myriadbloom.BossContent;
import com.myriadbloom.FrostMobs;
import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.world.entity.OriginApe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Wakes the snow fortress boss up.
 *
 * <p>Fortress generation leaves a {@code boss_spawner} marker inside the dome and records it in the
 * {@link RegionLedger}. The first time a player comes within {@value #TRIGGER_RANGE} blocks the ape
 * spawns on top of it, the spawner itself is destroyed, and a roar plus a burst of snow announce the
 * fight.</p>
 */
public class OriginApeSpawnerMechanic implements EnvironmentMechanic {
    private static final double TRIGGER_RANGE = 24.0D;

    @Override
    public ResourceLocation id() {
        return MyriadBloomApi.id("origin_ape_spawner");
    }

    @Override
    public void onLevelTick(ServerLevel level) {
        if (!MyriadBloomApi.isMyriadBloom(level) || level.getGameTime() % 20L != 0L) {
            return;
        }
        for (RegionLedger.Entry entry : RegionLedger.of(level).allEntries("boss_spawner")) {
            BlockPos pos = entry.pos();
            if (!level.isLoaded(pos) || !level.getBlockState(pos).is(BossContent.BOSS_SPAWNER.get())) {
                continue;
            }
            Player nearby = level.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    TRIGGER_RANGE, false);
            if (nearby == null) {
                continue;
            }
            OriginApe ape = FrostMobs.ORIGIN_APE.get().create(level);
            if (ape != null) {
                ape.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                        level.random.nextFloat() * 360.0F, 0.0F);
                ape.setPersistenceRequired();
                level.addFreshEntity(ape);
            }
            // the spawner shatters as the boss breaks out
            level.removeBlock(pos, false);
            level.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    60, 1.5D, 1.5D, 1.5D, 0.05D);
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                    SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0F, 0.7F);
        }
    }
}
