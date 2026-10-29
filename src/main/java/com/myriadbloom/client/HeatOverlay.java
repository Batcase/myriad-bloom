package com.myriadbloom.client;

import com.myriadbloom.EmberContent;
import com.myriadbloom.ModAttachments;
import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.world.WarmthMechanic;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

/**
 * Heat reserve bar (热源值), drawn just above the warmth row.
 *
 * <p>Only visible while the whole 极霜 armour set is worn - the set is what grants the reserve. The
 * bar is orange with a gold frame; its fill follows the synced {@link ModAttachments#HEAT} value
 * (0..100 %).</p>
 */
public final class HeatOverlay implements LayeredDraw.Layer {
    private static final int WIDTH = 81;
    private static final int HEIGHT = 5;
    /** Same right margin as the warmth row so the two meters line up. */
    private static final int RIGHT_MARGIN = 82;

    private static final int FRAME = 0xFFFFC94A;
    private static final int BACKGROUND = 0xC0201A12;
    private static final int FILL = 0xFFF08A24;
    private static final int FILL_HIGHLIGHT = 0xFFFFC061;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        if (!MyriadBloomApi.isMyriadBloom(player.level())) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (!hasFullSet(player)) {
            return;
        }
        int heat = ModAttachments.HEAT.get() == null ? 0 : player.getData(ModAttachments.HEAT.get());
        int right = graphics.guiWidth() / 2 + RIGHT_MARGIN;
        int left = right - WIDTH;
        int y = graphics.guiHeight() - HudLayout.rowFromBottom(player) - HudLayout.ROW + 2;

        // gold frame, dark inside
        graphics.fill(left - 1, y - 1, right + 1, y + HEIGHT + 1, FRAME);
        graphics.fill(left, y, right, y + HEIGHT, BACKGROUND);
        int filled = Math.round(WIDTH * (heat / (float) WarmthMechanic.MAX_HEAT));
        if (filled > 0) {
            graphics.fill(left, y, left + filled, y + HEIGHT, FILL);
            graphics.fill(left, y, left + filled, y + 1, FILL_HIGHLIGHT);
        }
    }

    /** True when all four armour slots hold the frost armour pieces. */
    private static boolean hasFullSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(EmberContent.FROST_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(EmberContent.FROST_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(EmberContent.FROST_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(EmberContent.FROST_BOOTS.get());
    }
}
