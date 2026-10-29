package com.myriadbloom.world;

import com.myriadbloom.FrostMobs;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Keeps the frost realm free of mobs that are not on its whitelist.
 *
 * <p>{@link FrostSpawnRules} already denies them at spawn time, but a few paths bypass it: monster
 * spawners in chunks generated before the rules existed, chunk-generation spawns, patrols, and
 * anything spawned by another mod. This sweep runs once a second and removes leftover <b>hostile</b>
 * mobs, so a stray skeleton from an old dungeon cannot survive.</p>
 *
 * <p>Only {@link Enemy} mobs are touched - animals, pets, villagers, items and players are left alone.
 * The mod's own mobs and the whitelisted vanilla ones (zombie, stray, goat, fox) are skipped.</p>
 *
 * <p>Anything a player has made their own is left alone as well: name tagged mobs, leashed mobs, mobs
 * spawned from an egg or bucket (which set persistence), tamed pets and anything the mod marks as
 * protected. Without this a player's caged zombie or mob farm could be wiped while they walk past.</p>
 */
public class FrostMobCleanupMechanic implements EnvironmentMechanic {
    @Override
    public ResourceLocation id() {
        return MyriadBloomApi.id("mob_cleanup");
    }

    @Override
    public void onLevelTick(ServerLevel level) {
        if (!MyriadBloomApi.isMyriadBloom(level) || level.getGameTime() % 20L != 0L) {
            return;
        }
        int seen = 0;
        int removed = 0;
        for (Entity entity : level.getAllEntities()) {
            seen++;
            if (!(entity instanceof Mob mob) || !(mob instanceof Enemy)) {
                continue;
            }
            if (isWhitelisted(mob.getType()) || isPlayerOwned(mob)) {
                continue;
            }
            mob.discard();
            removed++;
        }
        if (removed > 0) {
            MyriadBloom.LOGGER.info("Frost Realm mob cleanup: removed {} of {} entities", removed, seen);
        }
    }

    /**
     * True when a player clearly owns this mob, so the sweep must not delete it.
     *
     * <p>{@code isPersistenceRequired()} covers name tags and spawn eggs/buckets, which is the vanilla
     * way of saying "the player put this here".</p>
     */
    private static boolean isPlayerOwned(Mob mob) {
        if (mob.hasCustomName() || mob.isLeashed() || mob.isPersistenceRequired()) {
            return true;
        }
        if (mob instanceof net.minecraft.world.entity.TamableAnimal tamable && tamable.getOwnerUUID() != null) {
            return true;
        }
        return mob instanceof net.minecraft.world.entity.OwnableEntity ownable && ownable.getOwnerUUID() != null;
    }

    private static boolean isWhitelisted(EntityType<?> type) {
        return type == EntityType.ZOMBIE
                || type == EntityType.STRAY
                || type == EntityType.GOAT
                || type == EntityType.FOX
                || type == FrostMobs.FRENZIED_SNOW_GOLEM.get()
                || type == FrostMobs.CARNIVOROUS_RABBIT.get()
                || type == FrostMobs.CANDY_COW.get()
                || type == FrostMobs.FROST_BLAZE.get()
                || type == FrostMobs.ORIGIN_APE.get()
                || type == FrostMobs.SNOW_SPRITE.get()
                || type == FrostMobs.SNOW_BEAST.get()
                || type == FrostMobs.GLACIAL_CONGLOMERATION.get();
    }
}
