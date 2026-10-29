package com.myriadbloom.world;

import com.myriadbloom.FrostMobs;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.MyriadBloomApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * Spawn rules of the frost realm.
 *
 * <p>Only three vanilla mobs are allowed there: zombie, stray and goat. Zombies and strays are
 * further restricted to <b>caves</b> - a block has to sit within the five blocks above the spawn
 * position - and half of their spawn attempts are thrown away.</p>
 *
 * <p>The data driven spawner tables in the biome jsons already keep everything else out; this hook
 * enforces the same rules for anything that bypasses them (mob spawners, modded spawners) and applies
 * the cave/height rule, which a biome json cannot express.</p>
 */
@EventBusSubscriber(modid = MyriadBloom.MODID)
public final class FrostSpawnRules {
    /** Cave ceiling searched above a spawn position. */
    private static final int CEILING_SEARCH = 5;

    /**
     * Lazy check instead of a static set: our own entity types must not be resolved during class
     * initialisation, the registry is still unbound at that point.
     */
    private static boolean isAllowed(EntityType<?> type) {
        return type == EntityType.FOX
                || type == EntityType.ZOMBIE
                || type == EntityType.STRAY
                || type == EntityType.GOAT
                || type == FrostMobs.CARNIVOROUS_RABBIT.get()
                || type == FrostMobs.FROST_BLAZE.get()
                || type == FrostMobs.ORIGIN_APE.get()
                || type == FrostMobs.SNOW_SPRITE.get()
                || type == FrostMobs.SNOW_BEAST.get();
    }

    /**
     * The congealed land is deliberately empty: nothing spawns there yet. Its own spirit mob will be
     * whitelisted here once it exists.
     */
    private static boolean isCongealedLand(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).is(CongealedLandMechanic.BIOME);
    }

    /** Spawn kinds that are always allowed: explicit commands, spawn eggs and breeding. */
    private static boolean isExemptSpawnType(MobSpawnType spawnType) {
        return spawnType == MobSpawnType.COMMAND
                || spawnType == MobSpawnType.SPAWN_EGG
                || spawnType == MobSpawnType.BREEDING;
    }

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        ServerLevelAccessor accessor = event.getLevel();
        if (!(accessor.getLevel() instanceof ServerLevel level) || !MyriadBloomApi.isMyriadBloom(level)) {
            return;
        }
        Mob mob = event.getEntity();
        if (isCongealedLand(level, mob.blockPosition()) && !isExemptSpawnType(event.getSpawnType())) {
            // the congealed land only allows its own resident, the snow sprite
            if (mob.getType() != FrostMobs.SNOW_SPRITE.get()) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                return;
            }
        }
        if (!isAllowed(mob.getType()) && !isExemptSpawnType(event.getSpawnType())) {
            // includes monster spawners, chunk generation, patrols, reinforcements and
            // structure spawns, so leftovers from old dungeons cannot appear either
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
            return;
        }
        if (event.getSpawnType() != MobSpawnType.NATURAL) {
            // denied types are unreachable above; here only the cave rule is skipped for the
            // remaining spawn kinds (commands, spawn eggs, breeding, chunk loading, ...)
            return;
        }
        if (mob.getType() != EntityType.ZOMBIE && mob.getType() != EntityType.STRAY) {
            return;
        }
        if (!hasCeiling(level, mob.blockPosition()) || !rollHalf(level.random)) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    /** True when a non air block sits within {@value #CEILING_SEARCH} blocks above the position. */
    private static boolean hasCeiling(ServerLevel level, BlockPos pos) {
        for (int dy = 1; dy <= CEILING_SEARCH; dy++) {
            if (!level.getBlockState(pos.above(dy)).isAir()) {
                return true;
            }
        }
        return false;
    }

    private static boolean rollHalf(RandomSource random) {
        return random.nextInt(2) == 0;
    }

    private FrostSpawnRules() {
    }
}
