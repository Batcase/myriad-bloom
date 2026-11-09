package com.myriadbloom.client;

import com.myriadbloom.world.entity.TimeKeeper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * 时间维护者的两条血条：上面紫色（生命），下面白色（护盾），间距 1 像素。
 *
 * <p>两条都取自**同一只 BOSS、同一帧**的数据，并直接画在原版 BOSS 条的位置上，
 * 所以既紧凑又不会出现两条不一致。原版 BOSS 条已关闭。</p>
 */
public final class ShieldBarOverlay {
    private static final ResourceLocation HEALTH_BG =
            ResourceLocation.withDefaultNamespace("boss_bar/purple_background");
    private static final ResourceLocation HEALTH_FG =
            ResourceLocation.withDefaultNamespace("boss_bar/purple_progress");
    private static final ResourceLocation SHIELD_BG =
            ResourceLocation.withDefaultNamespace("boss_bar/white_background");
    private static final ResourceLocation SHIELD_FG =
            ResourceLocation.withDefaultNamespace("boss_bar/white_progress");

    private static final int WIDTH = 182;
    private static final int HEIGHT = 5;
    private static final int NAME_Y = 2;
    private static final int HEALTH_Y = 12;
    /** 紧贴生命条下沿 1 像素。 */
    private static final int SHIELD_Y = HEALTH_Y + HEIGHT + 1;

    private ShieldBarOverlay() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        TimeKeeper keeper = nearest(player);
        if (keeper == null) {
            return;
        }
        int x = graphics.guiWidth() / 2 - WIDTH / 2;

        graphics.drawCenteredString(minecraft.font, keeper.getDisplayName(),
                graphics.guiWidth() / 2, NAME_Y, 0xFFFFFF);

        drawBar(graphics, HEALTH_BG, HEALTH_FG, x, HEALTH_Y,
                keeper.getHealth() / Math.max(1.0F, keeper.getMaxHealth()));
        drawBar(graphics, SHIELD_BG, SHIELD_FG, x, SHIELD_Y,
                keeper.shield() / (float) TimeKeeper.MAX_SHIELD);
    }

    private static void drawBar(GuiGraphics graphics, ResourceLocation background,
                                ResourceLocation progress, int x, int y, float ratio) {
        float clamped = Math.max(0.0F, Math.min(1.0F, ratio));
        graphics.blitSprite(background, x, y, WIDTH, HEIGHT);
        int filled = Math.round(WIDTH * clamped);
        if (filled > 0) {
            graphics.enableScissor(x, y, x + filled, y + HEIGHT);
            graphics.blitSprite(progress, x, y, WIDTH, HEIGHT);
            graphics.disableScissor();
        }
    }

    private static TimeKeeper nearest(LocalPlayer player) {
        TimeKeeper best = null;
        double bestDistance = Double.MAX_VALUE;
        for (var entity : player.level().getEntities(player, player.getBoundingBox().inflate(64.0D))) {
            if (entity instanceof TimeKeeper candidate) {
                double distance = candidate.distanceToSqr(player);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
        }
        return best;
    }
}
