package com.myriadbloom.world.gen;

import com.myriadbloom.FrostMobs;
import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.world.entity.FrenziedSnowGolem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * Natural spawning of the frenzied snow golem.
 *
 * <p>The spec asks for a spawn rule that cannot be expressed with a biome spawn list: "four blocks of
 * air above the spot" and "ignore light level". So the attempts are rolled here instead, at a rate
 * comparable to vanilla zombies (a handful of attempts per player every half second, about one in ten
 * succeeding). No light level is ever checked.</p>
 */
public final class FrenziedSnowGolemMechanic implements EnvironmentMechanic {
    private static final ResourceLocation ID = MyriadBloomApi.id("frenzied_snow_golem_spawn");
    private static final int INTERVAL = 10;
    private static final int ATTEMPTS_PER_PLAYER = 3;
    private static final float SPAWN_CHANCE = 0.1F;
    private static final double HORIZONTAL_RANGE = 40.0D;
    private static final int VERTICAL_RANGE = 10;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void onLevelTick(ServerLevel level) {
        if (!MyriadBloomApi.isMyriadBloom(level) || level.players().isEmpty()) {
            return;
        }
        if (level.getGameTime() % INTERVAL != 0L) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            for (int attempt = 0; attempt < ATTEMPTS_PER_PLAYER; attempt++) {
                trySpawn(level, player);
            }
        }
    }

    private static void trySpawn(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.random;
        BlockPos feet = BlockPos.containing(
                player.getX() + (random.nextDouble() - 0.5D) * HORIZONTAL_RANGE,
                player.getY() + (double) (random.nextInt(2 * VERTICAL_RANGE + 1) - VERTICAL_RANGE),
                player.getZ() + (random.nextDouble() - 0.5D) * HORIZONTAL_RANGE);
        if (!level.isLoaded(feet)) {
            return;
        }
        BlockPos floor = feet.below();
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
            return;
        }
        // the spec's condition: the golem needs four free blocks of head room
        for (int dy = 0; dy <= 3; dy++) {
            if (!level.getBlockState(feet.above(dy)).isAir()) {
                return;
            }
        }
        // deliberately no light level check
        if (random.nextFloat() > SPAWN_CHANCE) {
            return;
        }
        FrenziedSnowGolem golem = FrostMobs.FRENZIED_SNOW_GOLEM.get().create(level);
        if (golem == null) {
            return;
        }
        golem.moveTo(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D,
                random.nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(golem)) {
            return;
        }
        level.addFreshEntity(golem);
    }
}
