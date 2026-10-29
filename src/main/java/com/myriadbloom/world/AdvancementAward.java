package com.myriadbloom.world;

import com.myriadbloom.MyriadBloom;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Grants advancements that cannot be expressed with a vanilla trigger.
 *
 * <p>A few achievements of the frost realm depend on mod state - the warmth stat, or a piece of armour
 * actually being worn - which the data driven criteria cannot read. Those advancement files use the
 * {@code minecraft:impossible} trigger and are awarded from code through this helper.</p>
 */
public final class AdvancementAward {
    private AdvancementAward() {
    }

    public static void award(ServerPlayer player, String path) {
        award(player, path, "done");
    }

    /** Grants one named criterion of an advancement. Returns true when progress was actually made. */
    public static boolean award(ServerPlayer player, String path, String criterionKey) {
        AdvancementHolder holder = holder(player, path);
        return holder != null && player.getAdvancements().award(holder, criterionKey);
    }

    /** Looks up one of the mod's advancements, or {@code null} when it is not loaded. */
    public static AdvancementHolder holder(ServerPlayer player, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, path);
        return player.server.getAdvancements().get(id);
    }
}
