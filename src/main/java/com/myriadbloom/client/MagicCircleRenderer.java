package com.myriadbloom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.world.entity.MagicCircle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 时间魔法阵：六芒星 + 双环 + 中央时钟。
 *
 * <p>六芒星由两个等边三角形叠成（旋转 60 度），外环顺时针、内环逆时针，中央的时针分针跟随世界时间。</p>
 */
public class MagicCircleRenderer extends EntityRenderer<MagicCircle> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/magic_circle.png");

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
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Matrix4f m = pose.last().pose();

        // 外环
        ring(consumer, m, radius, radius * 0.022F, 0.55F, 0.85F, 0.95F, 1.0F, 0.85F);
        // 内环
        ring(consumer, m, radius * 0.72F, radius * 0.016F, 0.45F, 0.80F, 0.92F, 1.0F, 0.7F);
        // 六芒星：两个三角形
        float starRadius = radius * 0.66F;
        float spin = age * 0.1F;
        triangle(consumer, m, starRadius, spin, radius * 0.018F);
        triangle(consumer, m, starRadius, spin + 60.0F, radius * 0.018F);
        // 中央时钟
        ring(consumer, m, radius * 0.2F, radius * 0.012F, 0.3F, 0.95F, 0.99F, 1.0F, 0.9F);
        long time = circle.level().getDayTime() % 24000L;
        float minuteAngle = (time % 1000L) / 1000.0F * 360.0F;
        float hourAngle = (time / 1000.0F) * 30.0F;
        hand(consumer, m, radius * 0.15F, hourAngle, radius * 0.024F, 0.95F, 0.98F, 1.0F, 1.0F);
        hand(consumer, m, radius * 0.19F, minuteAngle, radius * 0.018F, 0.75F, 0.9F, 1.0F, 1.0F);
    }

    private static void ring(VertexConsumer c, Matrix4f m, float radius, float width, float alpha,
                             float r, float g, float b, float a) {
        int segments = Math.max(64, (int) (radius * 3));
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0 * i / segments;
            double a1 = Math.PI * 2.0 * (i + 1) / segments;
            float x0 = (float) Math.cos(a0) * radius, z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius, z1 = (float) Math.sin(a1) * radius;
            float ix0 = (float) Math.cos(a0) * (radius - width), iz0 = (float) Math.sin(a0) * (radius - width);
            float ix1 = (float) Math.cos(a1) * (radius - width), iz1 = (float) Math.sin(a1) * (radius - width);
            quad(c, m, x0, z0, x1, z1, ix1, iz1, ix0, iz0, r, g, b, a);
        }
    }

    private static void triangle(VertexConsumer c, Matrix4f m, float radius, float spinDegrees, float width) {
        for (int corner = 0; corner < 3; corner++) {
            double a0 = Math.toRadians(spinDegrees + corner * 120.0);
            double a1 = Math.toRadians(spinDegrees + (corner + 1) * 120.0);
            float x0 = (float) Math.cos(a0) * radius, z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius, z1 = (float) Math.sin(a1) * radius;
            line(c, m, x0, z0, x1, z1, width, 0.85F, 0.93F, 1.0F, 0.95F);
        }
    }

    private static void hand(VertexConsumer c, Matrix4f m, float length, float angleDegrees, float width,
                             float r, float g, float b, float a) {
        double rad = Math.toRadians(angleDegrees);
        line(c, m, 0.0F, 0.0F, (float) Math.cos(rad) * length, (float) Math.sin(rad) * length,
                width, r, g, b, a);
    }

    private static void line(VertexConsumer c, Matrix4f m, float x0, float z0, float x1, float z1,
                             float width, float r, float g, float b, float a) {
        float dx = x1 - x0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-4F) {
            return;
        }
        float nx = -dz / len * width, nz = dx / len * width;
        quad(c, m, x0 + nx, z0 + nz, x1 + nx, z1 + nz, x1 - nx, z1 - nz, x0 - nx, z0 - nz, r, g, b, a);
    }

    private static void quad(VertexConsumer c, Matrix4f m,
                             float x0, float z0, float x1, float z1,
                             float x2, float z2, float x3, float z3,
                             float r, float g, float b, float a) {
        vertex(c, m, x0, z0, r, g, b, a);
        vertex(c, m, x1, z1, r, g, b, a);
        vertex(c, m, x2, z2, r, g, b, a);
        vertex(c, m, x3, z3, r, g, b, a);
    }

    private static void vertex(VertexConsumer c, Matrix4f m, float x, float z,
                               float r, float g, float b, float a) {
        c.addVertex(m, x, 0.0F, z).setColor(r, g, b, a).setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0.0F, 1.0F, 0.0F);
    }
}
