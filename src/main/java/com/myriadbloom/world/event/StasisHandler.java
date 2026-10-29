package com.myriadbloom.world.event;

import com.myriadbloom.MyriadBloom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Freezes everything inside a 时间凝滞 (time stasis) sphere, players included.
 *
 * <p>Frozen players are pinned to the spot they stood on when the event began: their motion is cleared
 * every tick and their position is pushed back, so walking, jumping, knockback and falling all stop. Their
 * actions are cancelled through the interaction and attack events. Mobs have their AI switched off by the
 * event itself; here only their motion is cleared.</p>
 */
@EventBusSubscriber(modid = MyriadBloom.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class StasisHandler {
    /** Where each frozen player must stay, in blocks. */
    private static final Map<UUID, Vec3> ANCHORS = new HashMap<>();

    private StasisHandler() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !StasisZone.isActive(level)) {
            return;
        }
        if (!StasisZone.isFrozen(level, entity.position())) {
            return;
        }
        if (entity instanceof ServerPlayer player) {
            ANCHORS.putIfAbsent(player.getUUID(), player.position());
            Vec3 anchor = ANCHORS.get(player.getUUID());
            player.setDeltaMovement(Vec3.ZERO);
            player.setPos(anchor.x, anchor.y, anchor.z);
            player.setSprinting(false);
            player.hurtMarked = true;
        } else {
            entity.setDeltaMovement(Vec3.ZERO);
            entity.hurtMarked = true;
        }
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || !StasisZone.isFrozen(level, player.position())) {
            ANCHORS.remove(player.getUUID());
            return;
        }
        Vec3 anchor = ANCHORS.get(player.getUUID());
        if (anchor != null) {
            // movement happens between Pre and Post, so pin the player again afterwards
            player.connection.teleport(anchor.x, anchor.y, anchor.z, player.getYRot(), player.getXRot());
            player.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static boolean frozen(ServerPlayer player) {
        return player.level() instanceof ServerLevel level
                && StasisZone.isFrozen(level, player.position());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && frozen(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && frozen(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && frozen(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && frozen(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player && frozen(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && frozen(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player && frozen(player)) {
            event.setCanceled(true);
        }
    }

    /** Clears the anchor cache when a level unloads or the server stops. */
    public static void clearAnchors() {
        ANCHORS.clear();
    }

    /** Kept for symmetry with the other handlers. */
    public static void onLevelUnload(ResourceKey<Level> dimension) {
        ANCHORS.clear();
    }
}
