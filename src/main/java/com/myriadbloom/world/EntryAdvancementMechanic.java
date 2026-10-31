package com.myriadbloom.world;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Advancement bookkeeping for the realms.
 *
 * <p>Vanilla only sends an advancement to the client when it is <em>visible</em>, and a root only
 * becomes visible once it is completed. That is why the mod awards its three page headers silently on
 * the first tick: it gives the advancement screen the tabs for 千花万境 / 冰霜的国度 / 糖果的世界
 * without waiting for the player to explore anything. The actual milestones (entering a realm) stay on
 * their child advancements and keep their toast.</p>
 *
 * <p>Entering a realm also works while standing in it, so teleports, portals and logging in already
 * inside the dimension all count.</p>
 */
public final class EntryAdvancementMechanic implements EnvironmentMechanic {
    private static final ResourceLocation ID = MyriadBloomApi.id("entry_advancements");
    /** Players whose page headers were already handed out in this session. */
    private static final Set<UUID> BOOTSTRAPPED = ConcurrentHashMap.newKeySet();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        if (BOOTSTRAPPED.add(player.getUUID())) {
            // the page headers: no toast, no announcement, they only exist to own the tabs
            AdvancementAward.award(player, "root", "done");
            AdvancementAward.award(player, "frost_root", "done");
            AdvancementAward.award(player, "candy_root", "done");
            MyriadBloom.LOGGER.info("Advancement page roots handed to {}",
                    player.getGameProfile().getName());
        }
        // once a second is plenty for an entry achievement
        if (player.tickCount % 20 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (MyriadBloomApi.isMyriadBloom(level)) {
            grant(player, "realm_frost", "entered");
        } else if (MyriadBloomApi.isCandy(level)) {
            grant(player, "realm_candy", "entered");
        }
    }

    private static void grant(ServerPlayer player, String path, String criterion) {
        if (AdvancementAward.award(player, path, criterion)) {
            MyriadBloom.LOGGER.info("Entry advancement granted: {} -> {}", path,
                    player.getGameProfile().getName());
        }
    }

    @Override
    public void forgetPlayer(UUID playerId) {
        BOOTSTRAPPED.remove(playerId);
    }

    @Override
    public String debugState() {
        return "entry_advancements.bootstrapped=" + BOOTSTRAPPED.size();
    }
}
