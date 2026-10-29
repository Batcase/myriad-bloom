package com.myriadbloom.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/**
 * Frost creeping in from the screen edges while the player stands in the 凝结之地 - the same idea as
 * the powder snow outline, drawn as four soft bands so it needs no texture.
 */
public final class CongealedOverlay implements LayeredDraw.Layer {
    private static final int EDGE = 60;                 // thickness of the band in pixels
    private static final int TINT_NEAR = 0x66000000;    // icy white-blue at the very edge
    private static final int TINT_FAR = 0x00C8E8FF;     // fades to fully transparent

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        if (!CongealedClientEvents.isCameraInside()) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        // four bands, darker towards the edge
        for (int i = 0; i < EDGE; i += 2) {
            int alpha = 26 - (i * 26 / EDGE);
            int tint = (Math.max(0, alpha) << 24) | 0xB8E4FF;
            graphics.fill(i, 0, i + 2, height, tint);                 // left
            graphics.fill(width - i - 2, 0, width - i, height, tint);  // right
            graphics.fill(0, i, width, i + 2, tint);                    // top
            graphics.fill(0, height - i - 2, width, height - i, tint);  // bottom
        }
    }
}
