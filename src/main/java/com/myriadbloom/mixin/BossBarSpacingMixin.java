package com.myriadbloom.mixin;

import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把原版 BOSS 条的竖直间距从 19 像素改成 6 像素，让血条和护盾条贴得更近。
 *
 * <p>条本身的绘制仍然完全交给原版（含掉血时的渐隐动画），只改了排布间距。</p>
 */
@Mixin(BossHealthOverlay.class)
public abstract class BossBarSpacingMixin {
    @Shadow
    @Final
    private Map<UUID, LerpingBossEvent> events;

    @Shadow
    protected abstract void drawBar(GuiGraphics guiGraphics, int x, int y, BossEvent bossEvent);

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void myriadbloom$compactBars(GuiGraphics guiGraphics, CallbackInfo info) {
        if (this.events.isEmpty()) {
            return;
        }
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int centerX = guiGraphics.guiWidth() / 2;
        int x = centerX - 91;          // 182 宽的条居中（与原版一致）
        int y = 12;
        for (BossEvent bossBar : this.events.values()) {
            // 条上面的名字（原版在 render 里画，这里被我们接管了）
            net.minecraft.network.chat.Component name = bossBar.getName();
            if (!name.getString().isEmpty()) {
                guiGraphics.drawCenteredString(font, name, centerX, y - 9, 0xFFFFFF);
            }
            this.drawBar(guiGraphics, x, y, bossBar);
            y += 6;
        }
        info.cancel();
    }
}
