package com.myriadbloom;

import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.world.BlizzardMechanic;
import com.myriadbloom.world.FrostPortalIgniter;
import com.myriadbloom.world.PortalCharge;
import com.myriadbloom.world.ThirstMechanic;
import com.myriadbloom.world.entity.FrenziedSnowGolem;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Server-side event entry point of the mod.
 *
 * <p>Everything here runs on the logical server only; nothing is registered on the client, which
 * is what keeps the mod usable on a dedicated server.</p>
 */
public final class FrostEvents {
    private final Set<ResourceKey<Level>> initialisedRealms = new HashSet<>();

    /**
     * Self-check instead of "recipe count went up by one": other mods also add recipes, so the total
     * is meaningless. Ask the recipe manager whether our own recipe id resolves.
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        boolean smelting = event.getServer().getRecipeManager()
                .byKey(MyriadBloomApi.id("frost_ingot_from_smelting"))
                .isPresent();
        MyriadBloom.LOGGER.info("Frost Realm: frost_ingot smelting recipe registered = {}", smelting);
    }

    /**
     * The frenzied golem throws plain vanilla snowballs (same physics, same visuals). The extra bite
     * is applied here: 1 damage plus a shove along the flight path.
     */
    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Snowball snowball)) {
            return;
        }
        if (!(snowball.getOwner() instanceof FrenziedSnowGolem)) {
            return;
        }
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)) {
            return;
        }
        if (!(hit.getEntity() instanceof LivingEntity target) || target == snowball.getOwner()) {
            return;
        }
        target.hurt(target.damageSources().thrown(snowball, snowball.getOwner()), 1.0F);
        // LivingEntity#knockback subtracts the given vector, so it must point from the victim
        // towards the shooter. Passing the snowball's flight direction (which points at the victim)
        // would drag the player towards the golem instead of shoving them away.
        Entity shooter = snowball.getOwner();
        double shoveX;
        double shoveZ;
        if (shooter != null) {
            shoveX = shooter.getX() - target.getX();
            shoveZ = shooter.getZ() - target.getZ();
        } else {
            Vec3 velocity = snowball.getDeltaMovement();
            shoveX = -velocity.x;
            shoveZ = -velocity.z;
        }
        if (shoveX * shoveX + shoveZ * shoveZ > 1.0E-4D) {
            // half of the original shove: 0.5 was strong enough to fling the player several blocks
            target.knockback(0.25D, shoveX, shoveZ);
        }
    }

    /** Thirst is refilled by drinking and eating (candy dimension only). */    @SubscribeEvent
    public void onItemUseFinished(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ThirstMechanic.onConsumed(player, event.getItem());
        }
    }

    /**
     * The floor of a frozen ocean arena is sealed while 极冰聚合物 is alive: the congealed ice of the
     * platform simply refuses to break until the boss dies.
     */
    @SubscribeEvent
    public void onBlockBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        if (!event.getState().is(com.myriadbloom.FrostedLandContent.CONGEALED_ICE.get())) {
            return;
        }
        if (com.myriadbloom.world.ArenaGuard.of(level).isSealed(event.getPos())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("block.myriadbloom.boss_summoner.sealed"),
                    true);
        }
    }

    /**
     * Per-player caches (warmth, thirst, portal charge) are keyed by UUID and live as long as the
     * server: without this they keep every player that ever logged in.
     */
    @SubscribeEvent
    public void onPlayerLoggedOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        EnvironmentMechanic.Registry.playerLeft(event.getEntity().getUUID());
        com.myriadbloom.world.PortalCharge.cancel(event.getEntity());
    }

    /**
     * A portal charge entry must not outlive its entity: without this, every mob that ever touched a
     * portal stays in the static map until the LRU pushes it out.
     */
    @SubscribeEvent
    public void onEntityLeaveLevel(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel) {
            com.myriadbloom.world.PortalCharge.cancel(event.getEntity());
        }
    }

    /** Static worldgen buffers must not survive a world reload inside the same JVM. */
    @SubscribeEvent
    public void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        com.myriadbloom.world.gen.FallenLogFeature.clearCaches();
        com.myriadbloom.world.LakePopulator.clearCaches();
        com.myriadbloom.world.PortalCharge.clearCaches();
        com.myriadbloom.world.event.StasisZone.clearAll();
        com.myriadbloom.world.event.StasisHandler.clearAnchors();
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // Environment mechanics run in every dimension the mod owns: warmth gates itself on the frost
        // realm, thirst on the candy dimension, so dispatching for both is what makes them tick.
        if (MyriadBloomApi.isMyriadBloom(level) || MyriadBloomApi.isCandy(level)) {
            if (this.initialisedRealms.add(level.dimension())) {
                MyriadBloom.LOGGER.info("Frost Realm level ready: {} ({} environment mechanics active)",
                        level.dimension().location(), EnvironmentMechanic.Registry.all().size());
                EnvironmentMechanic.Registry.levelLoaded(level);
            }
            EnvironmentMechanic.Registry.tickLevel(level);
            for (ServerPlayer player : level.players()) {
                EnvironmentMechanic.Registry.tickPlayer(player);
            }
        }

        // Portal ignition works in any dimension so frames built in the overworld or the frost
        // realm both react to a thrown diamond.
        FrostPortalIgniter.tickLevel(level);
    }

    /**
     * The flat portal sheet is flush with the ground, so entities stand <i>on top</i> of it instead
     * of inside it: {@code Block#entityInside} never fires there. This handler does the whole
     * portal lifecycle:
     *
     * <ul>
     *   <li>staying on a portal builds up a charge ({@code portal_transition_ticks}, default 60
     *       ticks = 3 s) while snowflakes gather and swell around the entity;</li>
     *   <li>stepping off cancels the charge;</li>
     *   <li>after arriving, the entity is "disarmed" until it steps off, so it is not bounced
     *       straight back.</li>
     * </ul>
     */
    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.isRemoved() || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!FrostConfig.ENABLE_PORTAL.get()) {
            return;
        }
        BlockPos inside = entity.blockPosition();
        BlockPos portalPos = portalPosition(level, entity, inside);
        if (portalPos == null) {
            PortalCharge.cancel(entity);
            return;
        }

        PortalCharge.State charge = PortalCharge.get(entity);
        if (!charge.armed) {
            // just arrived: wait until the entity steps off before it can charge again
            return;
        }
        if (entity.getPortalCooldown() > 0) {
            return;
        }

        int total = Math.max(1, FrostConfig.PORTAL_TRANSITION_TICKS.get());
        charge.ticks++;
        spawnChargeParticles(level, entity, charge.ticks, total, portalPos);

        if (charge.ticks >= total) {
            PortalCharge.cancel(entity);
            BlockState state = level.getBlockState(portalPos);
            if (state.getBlock() instanceof Portal portal) {
                entity.setAsInsidePortal(portal, portalPos);
            }
        }
    }

    /**
     * The portal block the entity is currently touching, or {@code null}.
     *
     * <p>The flat frost pool sits flush with the ground, so an entity stands <i>on</i> it: its own
     * block and the one below are checked. The candy portal is a standing sheet, so any portal block
     * overlapping the entity's bounding box counts - that is the vertical equivalent of vanilla's
     * {@code entityInside}.</p>
     */
    private static BlockPos portalPosition(ServerLevel level, Entity entity, BlockPos inside) {
        if (isFrostPortal(level, inside)) {
            return inside;
        }
        BlockPos below = inside.below();
        if (isFrostPortal(level, below)) {
            return below;
        }
        AABB box = entity.getBoundingBox();
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(pos).is(ModBlocks.CANDY_PORTAL.get())) {
                return pos.immutable();
            }
        }
        return null;
    }

    /** Snowflakes gather around the entity at a frost portal, petals at a candy portal. */
    private static void spawnChargeParticles(ServerLevel level, Entity entity, int ticks, int total,
                                            BlockPos portalPos) {
        boolean candy = level.getBlockState(portalPos).is(ModBlocks.CANDY_PORTAL.get());
        ParticleOptions particle = candy ? ParticleTypes.CHERRY_LEAVES : ParticleTypes.SNOWFLAKE;
        float progress = Math.min(1.0F, ticks / (float) total);
        double x = entity.getX();
        double y = entity.getY() + 1.0D;
        double z = entity.getZ();
        if (ticks % 2 == 0) {
            double radius = 0.35D + progress * 1.05D;
            int count = 2 + (int) (progress * 6.0F);
            level.sendParticles(particle, x, y, z, count, radius, 0.45D, radius, 0.0D);
        }
        if (ticks == total) {
            level.sendParticles(particle, x, y, z, 40, 1.2D, 0.9D, 1.2D, 0.06D);
            level.playSound(null, entity.blockPosition(), SoundEvents.PORTAL_TRIGGER,
                    SoundSource.BLOCKS, 1.0F, candy ? 1.4F : 1.25F);
        }
    }

    private static boolean isFrostPortal(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() == ModBlocks.FROST_PORTAL.get();
    }
}
