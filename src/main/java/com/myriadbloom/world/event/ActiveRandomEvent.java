package com.myriadbloom.world.event;

import com.myriadbloom.ModMobEffects;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * One running random event.
 *
 * <p>The manager keeps at most one per level. Blizzard applies a 50% movement penalty to players inside
 * the radius (they can still walk, just slowly) and throws snow around; time stasis freezes entities, mob
 * AI, random block ticks and block entities inside the sphere and hands out the 时滞 effect for the HUD
 * icon.</p>
 */
public final class ActiveRandomEvent {
    private static final ResourceLocation BLIZZARD_SLOW_ID =
            ResourceLocation.fromNamespaceAndPath("myriadbloom", "blizzard_wind");
    private static final float BLIZZARD_SLOW = -0.5F;

    private final RandomEventType type;
    private final Vec3 centre;
    private final double radius;
    private final long endGameTime;
    /** Mobs whose AI we switched off, with the value to restore. */
    private final Map<UUID, Boolean> frozenMobs = new HashMap<>();
    private final Set<UUID> slowedPlayers = new HashSet<>();

    public ActiveRandomEvent(RandomEventType type, Vec3 centre, long endGameTime) {
        this.type = type;
        this.centre = centre;
        this.radius = type.radius();
        this.endGameTime = endGameTime;
    }

    public RandomEventType type() {
        return this.type;
    }

    public boolean isFinished(ServerLevel level) {
        return level.getGameTime() >= this.endGameTime;
    }

    /** Starts the event and announces it to everyone nearby. */
    public void start(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(this.centre) <= this.radius * this.radius) {
                player.displayClientMessage(Component.translatable(this.type.id().equals("blizzard")
                        ? "event.myriadbloom.blizzard.start"
                        : "event.myriadbloom.time_stasis.start").withStyle(ChatFormatting.AQUA), false);
            }
        }
        if (this.type == RandomEvents.TIME_STASIS) {
            StasisZone.set(level, this.centre, this.radius, this.endGameTime);
        }
        level.playSound(null, this.centre.x, this.centre.y, this.centre.z,
                this.type == RandomEvents.BLIZZARD ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.BELL_RESONATE,
                SoundSource.WEATHER, 1.4F, this.type == RandomEvents.BLIZZARD ? 0.7F : 0.5F);
    }

    /** Applies one tick of the event. */
    public void tick(ServerLevel level) {
        if (this.type == RandomEvents.BLIZZARD) {
            this.tickBlizzard(level);
        } else if (this.type == RandomEvents.TIME_STASIS) {
            this.tickStasis(level);
        }
    }

    // ------------------------------------------------------------- 暴风雪 ---------------
    private void tickBlizzard(ServerLevel level) {
        long remaining = this.endGameTime - level.getGameTime();
        this.slowedPlayers.removeIf(uuid -> {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player == null || player.level() != level) {
                return true;
            }
            if (player.position().distanceToSqr(this.centre) > this.radius * this.radius) {
                removeSlow(player);
                return true;
            }
            if (remaining <= 0) {
                removeSlow(player);
                return true;
            }
            applySlow(player);
            return false;
        });
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(this.centre) > this.radius * this.radius) {
                continue;
            }
            applySlow(player);
            this.slowedPlayers.add(player.getUUID());
        }

        // heavy snow, and the wind keeps pushing at the players a little
        double angle = level.getGameTime() * 0.05D;
        for (int i = 0; i < 26; i++) {
            double ox = (level.random.nextDouble() - 0.5D) * this.radius * 2.0D;
            double oy = (level.random.nextDouble() - 0.2D) * 8.0D;
            double oz = (level.random.nextDouble() - 0.5D) * this.radius * 2.0D;
            level.sendParticles(ParticleTypes.SNOWFLAKE, this.centre.x + ox, this.centre.y + oy, this.centre.z + oz,
                    1, Math.cos(angle) * 0.12D, -0.4D, Math.sin(angle) * 0.12D, 0.02D);
        }
        if (level.getGameTime() % 20L == 0L) {
            level.playSound(null, this.centre.x, this.centre.y, this.centre.z,
                    SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 1.0F, 0.6F);
        }
        // a barely noticeable drift, so the player still walks where they want to
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(this.centre) > this.radius * this.radius) {
                continue;
            }
            Vec3 wind = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle)).scale(0.012D);
            player.push(wind.x, 0.0D, wind.z);
            player.hurtMarked = true;
        }
    }

    private static void applySlow(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null || speed.getModifier(BLIZZARD_SLOW_ID) != null) {
            return;
        }
        speed.addTransientModifier(new AttributeModifier(BLIZZARD_SLOW_ID, BLIZZARD_SLOW,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeSlow(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(BLIZZARD_SLOW_ID);
        }
    }

    // ------------------------------------------------------------- 时间凝滞 -------------
    private void tickStasis(ServerLevel level) {
        long remaining = this.endGameTime - level.getGameTime();
        if (remaining <= 0) {
            return;
        }
        StasisZone.set(level, this.centre, this.radius, this.endGameTime);

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, this.bounds())) {
            if (entity instanceof ServerPlayer player) {
                // the effect is what draws the hourglass icon on the HUD
                player.addEffect(new MobEffectInstance(ModMobEffects.TIME_STASIS, (int) remaining, 0,
                        false, false, true));
                continue;
            }
            entity.setDeltaMovement(Vec3.ZERO);
            entity.hurtMarked = true;
            if (entity instanceof Mob mob) {
                mob.getNavigation().stop();
                this.frozenMobs.putIfAbsent(mob.getUUID(), mob.isNoAi());
                mob.setNoAi(true);
            }
        }
        // frozen falling blocks and loose items need their motion cleared as well
        for (Entity entity : level.getEntitiesOfClass(Entity.class, this.bounds())) {
            if (entity instanceof LivingEntity) {
                continue;
            }
            entity.setDeltaMovement(Vec3.ZERO);
            entity.hurtMarked = true;
        }

        if (level.getGameTime() % 5L == 0L) {
            for (int i = 0; i < 12; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2.0D;
                double r = this.radius * (0.4D + level.random.nextDouble() * 0.6D);
                level.sendParticles(ParticleTypes.END_ROD,
                        this.centre.x + Math.cos(angle) * r,
                        this.centre.y + (level.random.nextDouble() - 0.4D) * 6.0D,
                        this.centre.z + Math.sin(angle) * r, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private net.minecraft.world.phys.AABB bounds() {
        return new net.minecraft.world.phys.AABB(
                this.centre.x - this.radius, this.centre.y - this.radius, this.centre.z - this.radius,
                this.centre.x + this.radius, this.centre.y + this.radius, this.centre.z + this.radius);
    }

    /** Ends the event and restores everything it changed. */
    public void stop(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            removeSlow(player);
        }
        this.slowedPlayers.clear();

        if (this.type == RandomEvents.TIME_STASIS) {
            StasisZone.clear(level);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, this.bounds())) {
                if (entity instanceof Mob mob) {
                    Boolean previous = this.frozenMobs.get(mob.getUUID());
                    mob.setNoAi(previous != null && previous);
                }
                if (entity instanceof ServerPlayer player) {
                    player.removeEffect(ModMobEffects.TIME_STASIS);
                }
            }
            this.frozenMobs.clear();
        }
    }

    /** True when the given position is inside the affected sphere. */
    public boolean contains(Vec3 position) {
        return position.distanceToSqr(this.centre) <= this.radius * this.radius;
    }

    public double radius() {
        return this.radius;
    }

    public Vec3 centre() {
        return this.centre;
    }

    public long endGameTime() {
        return this.endGameTime;
    }
}
