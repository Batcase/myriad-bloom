package com.myriadbloom.world;

import com.myriadbloom.api.EnvironmentMechanic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Builds the 冰洋 arena the first time a player walks into an ice ocean patch.
 *
 * <p>Doing it in a worldgen feature crashed the server ("Requested chunk unavailable during world
 * generation") because the feature wanted to look at the biome of neighbouring chunks. As a mechanic it
 * runs on fully loaded chunks instead, so the platform, the ice floes and the boss summoner can be placed
 * freely.</p>
 */
public final class IceOceanArenaMechanic implements EnvironmentMechanic {
    private static final int CHECK_INTERVAL = 40;

    @Override
    public net.minecraft.resources.ResourceLocation id() {
        return com.myriadbloom.api.MyriadBloomApi.id("ice_ocean_arena");
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        int x = player.getBlockX();
        int z = player.getBlockZ();
        if (!IceOceanArena.insidePatch(x, z)) {
            return;
        }
        if (!level.getBiome(player.blockPosition()).is(FrostBiomeKeys.ICE_OCEAN)) {
            return;
        }
        int[] centre = IceOceanArena.centreOf(x, z);
        ArenaGuard guard = ArenaGuard.of(level);
        long cellKey = IceOceanArena.cellKey(x, z);
        if (guard.isBuilt(cellKey)) {
            return;
        }
        // an existing summoner means the patch already has its arena (older saves, world edits)
        if (level.getBlockState(new BlockPos(centre[0], level.getSeaLevel() + 1, centre[1]))
                .is(com.myriadbloom.ModBlocks.BOSS_SUMMONER.get())) {
            guard.markBuilt(cellKey);
            return;
        }
        // the platform spans ~9 chunks, and setBlock on an unloaded chunk is silently dropped, so the
        // whole square is force loaded for the moment it takes to write the arena
        int centreChunkX = centre[0] >> 4;
        int centreChunkZ = centre[1] >> 4;
        int reach = 4;
        for (int cx = -reach; cx <= reach; cx++) {
            for (int cz = -reach; cz <= reach; cz++) {
                level.setChunkForced(centreChunkX + cx, centreChunkZ + cz, true);
                // setBlock is silently dropped on a chunk that is not loaded yet, so load it now
                level.getChunk(centreChunkX + cx, centreChunkZ + cz);
            }
        }
        boolean built;
        try {
            built = IceOceanArena.build(level, centre[0], centre[1]);
        } finally {
            for (int cx = -reach; cx <= reach; cx++) {
                for (int cz = -reach; cz <= reach; cz++) {
                    level.setChunkForced(centreChunkX + cx, centreChunkZ + cz, false);
                }
            }
        }
        if (built) {
            com.myriadbloom.MyriadBloom.LOGGER.info("Ice ocean arena built at {}, {}", centre[0], centre[1]);
        }
    }
}
