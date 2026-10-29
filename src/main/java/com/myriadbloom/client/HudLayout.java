package com.myriadbloom.client;

import net.minecraft.world.entity.player.Player;

/**
 * Shared HUD layout maths for the stat meters.
 *
 * <p>Vanilla stacks its rows bottom-up above the hotbar: hunger at {@code height - 39}, the armour row
 * and the air (oxygen) bubbles in the row above it. Horizontal placement matters as well:</p>
 *
 * <ul>
 *   <li>the armour row is drawn on the <b>left</b> half of the screen, while our meters sit on the
 *       right, so the meter can share the armour row without overlapping anything;</li>
 *   <li>the oxygen bubbles are drawn on the <b>right</b> half, exactly where the meter sits, so the
 *       meter has to move above them (and one row higher again when armour pushes the bubbles up).</li>
 * </ul>
 */
final class HudLayout {
    /** Height of one vanilla icon row. */
    static final int ROW = 10;
    /** Distance from the bottom of the screen to the row directly above the hunger bar. */
    static final int BASE_ROW = 49;

    /** Distance from the bottom of the screen for a meter row that clears every vanilla row. */
    static int rowFromBottom(Player player) {
        int distance = BASE_ROW;
        if (player.getAirSupply() < player.getMaxAirSupply()) {
            // bubbles occupy BASE_ROW, or one row higher when armour is worn
            distance += player.getArmorValue() > 0 ? 2 * ROW : ROW;
        }
        return distance;
    }

    private HudLayout() {
    }
}
