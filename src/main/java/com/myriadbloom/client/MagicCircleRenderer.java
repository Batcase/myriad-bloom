package com.myriadbloom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.world.entity.MagicCircle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 时间魔法阵：双环 + 一个六芒星 + 12 刻度 + 中央时钟。
 *
 * <p>线条是几何体，双面绘制，所以从地面上仰视、从高空俯视都能看清。整体缓慢自转，
 * 指针跟随世界时间；亮度有极轻微的呼吸，避免看起来像一张死图。</p>
 */
public class MagicCircleRenderer extends EntityRenderer<MagicCircle> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/magic_circle.png");
    private static final float U = 0.015F;
    private static final float V = 0.015F;
    /** 六芒星顶点所在半径（相对法阵半径）。 */
    private static final float STAR_RADIUS = 0.78F;

    public MagicCircleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(MagicCircle entity) {
        return TEXTURE;
    }

    @Override
    public void render(MagicCircle circle, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float radius = circle.radius();
        float age = circle.tickCount + partialTick;
        VertexConsumer c = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));

        pose.pushPose();
        pose.translate(0.0D, 0.03D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(age * 0.05F));
        Matrix4f m = pose.last().pose();

        float line = radius * 0.011F;
        float brightness = 0.92F + 0.08F * Mth.sin(age * 0.04F);

        // 外环与内环（细）
        ring(c, m, radius, line * 1.4F, brightness);
        ring(c, m, radius * 0.90F, line, brightness * 0.85F);
        // 六芒星
        triangle(c, m, radius * STAR_RADIUS, 90.0F, line * 1.3F, brightness);
        triangle(c, m, radius * STAR_RADIUS, 270.0F, line * 1.3F, brightness);
        // 12 个刻度，落在双环之间
        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30.0);
            float r0 = radius * 0.90F;
            float r1 = radius * 0.98F;
            line(c, m,
                    (float) Math.cos(angle) * r0, (float) Math.sin(angle) * r0,
                    (float) Math.cos(angle) * r1, (float) Math.sin(angle) * r1,
                    line, brightness * 0.9F);
        }
        // 中央表盘
        ring(c, m, radius * 0.14F, line * 0.9F, brightness * 0.8F);
        long time = circle.level().getDayTime() % 24000L;
        // 负号：让指针从上方俯视时顺时针走
        float minuteAngle = -(time % 1000L) / 1000.0F * 360.0F;
        float hourAngle = -(time / 1000.0F) * 30.0F;
        line(c, m, 0.0F, 0.0F,
                (float) Math.cos(Math.toRadians(hourAngle)) * radius * 0.10F,
                (float) Math.sin(Math.toRadians(hourAngle)) * radius * 0.10F,
                line * 1.5F, brightness);
        line(c, m, 0.0F, 0.0F,
                (float) Math.cos(Math.toRadians(minuteAngle)) * radius * 0.12F,
                (float) Math.sin(Math.toRadians(minuteAngle)) * radius * 0.12F,
                line, brightness);
        pose.popPose();
    }

    private static void ring(VertexConsumer c, Matrix4f m, float radius, float width, float brightness) {
        int segments = Math.max(128, (int) radius);
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0 * i / segments;
            double a1 = Math.PI * 2.0 * (i + 1) / segments;
            line(c, m,
                    (float) Math.cos(a0) * radius, (float) Math.sin(a0) * radius,
                    (float) Math.cos(a1) * radius, (float) Math.sin(a1) * radius,
                    width, brightness);
        }
    }

    private static void triangle(VertexConsumer c, Matrix4f m, float radius, float startDegrees,
                                 float width, float brightness) {
        for (int corner = 0; corner < 3; corner++) {
            double a0 = Math.toRadians(startDegrees + corner * 120.0);
            double a1 = Math.toRadians(startDegrees + (corner + 1) * 120.0);
            line(c, m,
                    (float) Math.cos(a0) * radius, (float) Math.sin(a0) * radius,
                    (float) Math.cos(a1) * radius, (float) Math.sin(a1) * radius,
                    width, brightness);
        }
    }

    private static void line(VertexConsumer c, Matrix4f m, float x0, float z0, float x1, float z1,
                             float width, float brightness) {
        float dx = x1 - x0;
        float dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-5F) {
            return;
        }
        float nx = -dz / len * width;
        float nz = dx / len * width;
        quad(c, m, x0 + nx, z0 + nz, x1 + nx, z1 + nz, x1 - nx, z1 - nz, x0 - nx, z0 - nz, brightness);
        quad(c, m, x0 - nx, z0 - nz, x1 - nx, z1 - nz, x1 + nx, z1 + nz, x0 + nx, z0 + nz, brightness);
    }

    private static void quad(VertexConsumer c, Matrix4f m, float x0, float z0, float x1, float z1,
                             float x2, float z2, float x3, float z3, float brightness) {
        vertex(c, m, x0, z0, brightness);
        vertex(c, m, x1, z1, brightness);
        vertex(c, m, x2, z2, brightness);
        vertex(c, m, x3, z3, brightness);
    }

    private static void vertex(VertexConsumer c, Matrix4f m, float x, float z, float brightness) {
        c.addVertex(m, x, 0.0F, z)
                .setColor(0.86F * brightness, 0.96F * brightness, 1.0F, 1.0F)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0x00F000F0)
                .setNormal(0.0F, 1.0F, 0.0F);
    }
}
