package com.myriadbloom.world;

import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Grants the dimension entry advancements while the player stands in one of the mod's dimensions.
 *
 * <p>The vanilla {@code changed_dimension} trigger turned out to be unreliable for our levels: it did not
 * fire for command teleports and never fired for players who log in already inside the realm. Doing the
 * check here instead covers every way of arriving - portal, teleport, respawn, rejoin - and it is
 * idempotent because {@code PlayerAdvancements} ignores criteria that are already granted.</p>
 */
public final class EntryAdvancementMechanic implements EnvironmentMechanic {
    private static final ResourceLocation ID = MyriadBloomApi.id("entry_advancements");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        // once a second is plenty for an entry achievement
        if (player.tickCount % 20 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (MyriadBloomApi.isMyriadBloom(level)) {
            // the hub root accepts "entered" as one of its two alternatives
            grant(player, "root", "entered");
            grant(player, "realm_frost", "entered");
            grant(player, "frost_root", "arrived");
        } else if (MyriadBloomApi.isCandy(level)) {
            grant(player, "realm_candy", "entered");
            grant(player, "candy_root", "entered");
        }
    }

    private static void grant(ServerPlayer player, String path, String criterion) {
        if (AdvancementAward.award(player, path, criterion)) {
            com.myriadbloom.MyriadBloom.LOGGER.info("Entry advancement granted: {} -> {}",
                    path, player.getGameProfile().getName());
        }
    }

    @Override
    public String debugState() {
        return "entry_advancements=on";
    }
}
