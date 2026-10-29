package com.myriadbloom.client;

import com.myriadbloom.ModAttachments;
import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.world.ThirstMechanic;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the thirst meter where the warmth meter normally sits, but only inside the candy dimension:
 * the two stats never share the screen. The row lifts above the armour and oxygen rows when those
 * are visible, see {@link HudLayout}.
 *
 * <p>Same 9x9 texture note as {@link WarmthOverlay}: the nine argument {@code blit} overload is
 * required, the short one assumes a 256x256 sheet and draws nothing visible.</p>
 */
public final class ThirstOverlay implements LayeredDraw.Layer {
    private static final ResourceLocation FULL = MyriadBloomApi.id("textures/gui/thirst_full.png");
    private static final ResourceLocation EMPTY = MyriadBloomApi.id("textures/gui/thirst_empty.png");
    private static final int RIGHT_MARGIN = 82;
    private static final int ICON = 9;
    private static final int SPACING = 8;
    /** Half-flask units per icon. */
    private static final int UNITS_PER_ICON = 2;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        if (!MyriadBloomApi.isCandy(minecraft.player.level())) {
            return;
        }
        if (minecraft.player.isCreative() || minecraft.player.isSpectator()) {
            // like the vanilla health/hunger rows, the meter is hidden outside survival
            return;
        }
        Integer stored = minecraft.player.getData(ModAttachments.THIRST.get());
        int thirst = stored == null ? ThirstMechanic.MAX_THIRST : stored;
        int icons = ThirstMechanic.MAX_THIRST / UNITS_PER_ICON;
        int right = graphics.guiWidth() / 2 + RIGHT_MARGIN;
        int y = graphics.guiHeight() - HudLayout.rowFromBottom(minecraft.player);
        for (int i = 0; i < icons; i++) {
            // icon i is the i-th flask counting from the right
            boolean full = thirst >= (i + 1) * UNITS_PER_ICON;
            boolean half = !full && thirst >= i * UNITS_PER_ICON + 1;
            int x = right - i * SPACING;
            graphics.blit(full ? FULL : EMPTY, x, y, 0, 0, ICON, ICON, ICON, ICON);
            if (half) {
                // lower half filled: draw the full icon clipped to its bottom half
                graphics.blit(FULL, x, y + 4, 0, 4, ICON, 5, ICON, ICON);
            }
        }
    }
}
