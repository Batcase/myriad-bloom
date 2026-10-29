package com.myriadbloom.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/**
 * The screen edges darken and pulse while the glacial conglomeration is still alive in the ice ocean.
 *
 * <p>Same four soft bands as {@code CongealedOverlay}, but cold blue and driven by the shared fog pulse,
 * so the darkening breathes in step with the fog itself.</p>
 */
public final class IceOceanOverlay implements LayeredDraw.Layer {
    private static final int EDGE = 96;
    private static final int MAX_ALPHA = 78;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        if (!IceOceanClientEvents.isFogActive()) {
            return;
        }
        float pulse = IceOceanClientEvents.pulse();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int peak = (int) (MAX_ALPHA * (0.55F + 0.45F * pulse));
        for (int i = 0; i < EDGE; i += 2) {
            int alpha = peak - (i * peak / EDGE);
            int tint = (Math.max(0, alpha) << 24) | 0x5A8FC8;
            graphics.fill(i, 0, i + 2, height, tint);                  // left
            graphics.fill(width - i - 2, 0, width - i, height, tint);   // right
            graphics.fill(0, i, width, i + 2, tint);                    // top
            graphics.fill(0, height - i - 2, width, height - i, tint);  // bottom
        }
    }
}
