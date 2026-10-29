package com.myriadbloom.client;

import com.myriadbloom.ModAttachments;
import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.world.WarmthMechanic;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the warmth meter as a row of flame icons above the hunger bar.
 *
 * <p>It is drawn <b>only inside the frost realm</b>: the overworld and the candy dimension show
 * nothing (the candy dimension draws thirst instead). The row lifts above the armour and oxygen rows
 * when those are visible, see {@link HudLayout}.</p>
 *
 * <p>The icon textures are 9x9, so the nine argument {@code blit} overload is used with the real
 * texture size. The shorter overload assumes a 256x256 sheet and would sample only the left 3.5% of
 * the icon - which is almost entirely transparent, i.e. an invisible bar.</p>
 */
public final class WarmthOverlay implements LayeredDraw.Layer {
    private static final ResourceLocation FULL = MyriadBloomApi.id("textures/gui/warmth_full.png");
    private static final ResourceLocation EMPTY = MyriadBloomApi.id("textures/gui/warmth_empty.png");
    /** Vanilla food row sits at height-39 and grows right-to-left from width/2+82. */
    private static final int RIGHT_MARGIN = 82;
    private static final int ICON = 9;
    private static final int SPACING = 8;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        if (!MyriadBloomApi.isMyriadBloom(minecraft.player.level())) {
            return;
        }
        if (minecraft.player.isCreative() || minecraft.player.isSpectator()) {
            // like the vanilla health/hunger rows, the meter is hidden outside survival
            return;
        }
        Integer stored = minecraft.player.getData(ModAttachments.WARMTH.get());
        int warmth = stored == null ? WarmthMechanic.MAX_WARMTH : stored;
        int right = graphics.guiWidth() / 2 + RIGHT_MARGIN;
        int y = graphics.guiHeight() - HudLayout.rowFromBottom(minecraft.player);
        for (int i = 0; i < WarmthMechanic.MAX_WARMTH; i++) {
            int x = right - i * SPACING;
            graphics.blit(i < warmth ? FULL : EMPTY, x, y, 0, 0, ICON, ICON, ICON, ICON);
        }
    }
}
