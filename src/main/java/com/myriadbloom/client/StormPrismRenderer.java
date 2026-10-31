package com.myriadbloom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.world.entity.StormPrism;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 风暴棱镜渲染器。
 *
 * <p>不再使用方块拼装：直接写三角形顶点，生成一颗六角双锥晶体（肩部偏上、下半部拉长）、一个全亮的
 * 八面体核心和四片环绕的碎晶。半透明材质让它看起来像玻璃而不是冰块。</p>
 */
public class StormPrismRenderer extends EntityRenderer<StormPrism> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/storm_prism.png");
    /** 肩部高度（格，相对中心）。 */
    private static final float SHOULDER_Y = 0.9F;
    /** 肩部半径。 */
    private static final float SHOULDER_R = 2.6F;
    /** 顶尖高度。 */
    private static final float TOP_Y = 3.0F;
    /** 底尖高度（比上面长，和参考图一致）。 */
    private static final float BOTTOM_Y = -3.0F;
    /** 晶体中心离脚底的高度（碰撞箱 6 格高）。 */
    private static final float CENTRE_Y = 3.0F;

    public StormPrismRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(StormPrism entity) {
        return TEXTURE;
    }

    @Override
    public void render(StormPrism entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0D, CENTRE_Y, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        float age = entity.tickCount + partialTick;
        pose.translate(0.0D, Mth.sin(age * 0.08F) * 0.12D, 0.0D);

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Matrix4f matrix = pose.last().pose();

        // ---- 晶体：六角双锥 ------------------------------------------------------
        int sides = 6;
        Vector3f[] ring = new Vector3f[sides];
        for (int i = 0; i < sides; i++) {
            double angle = Math.PI * 2.0D * i / sides + Math.toRadians(age * 0.15D);
            ring[i] = new Vector3f((float) (Math.cos(angle) * SHOULDER_R), SHOULDER_Y,
                    (float) (Math.sin(angle) * SHOULDER_R));
        }
        Vector3f top = new Vector3f(0.0F, TOP_Y, 0.0F);
        Vector3f bottom = new Vector3f(0.0F, BOTTOM_Y, 0.0F);
        for (int i = 0; i < sides; i++) {
            Vector3f a = ring[i];
            Vector3f b = ring[(i + 1) % sides];
            triangle(consumer, matrix, a, b, top, 0, 0, 32, 20, 0.72F, light, true);
            triangle(consumer, matrix, b, a, bottom, 32, 0, 32, 24, 0.82F, light, false);
        }

        // ---- 核心：八面体，全亮 ------------------------------------------------
        Vector3f coreTop = new Vector3f(0.0F, 1.1F, 0.0F);
        Vector3f coreBottom = new Vector3f(0.0F, -1.1F, 0.0F);
        Vector3f[] coreRing = new Vector3f[4];
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI * 0.5D * i - Math.toRadians(age * 0.4D);
            coreRing[i] = new Vector3f((float) (Math.cos(angle) * 0.8F), 0.0F,
                    (float) (Math.sin(angle) * 0.8F));
        }
        int fullBright = 0x00F000F0;
        for (int i = 0; i < 4; i++) {
            Vector3f a = coreRing[i];
            Vector3f b = coreRing[(i + 1) % 4];
            triangle(consumer, matrix, a, b, coreTop, 32, 32, 16, 16, 0.95F, fullBright, true);
            triangle(consumer, matrix, b, a, coreBottom, 32, 32, 16, 16, 0.95F, fullBright, false);
        }

        // ---- 环绕的四片碎晶 ------------------------------------------------------
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI * 0.5D * i + Math.toRadians(age * 0.9D);
            float radius = 3.3F;
            float x = (float) (Math.cos(angle) * radius);
            float z = (float) (Math.sin(angle) * radius);
            float y = Mth.sin(age * 0.12F + i) * 0.6F;
            shard(consumer, matrix, x, y, z, 0.34F, (float) angle, light);
        }
        pose.popPose();
    }

    /** 环绕的碎晶：上下两个尖的细长晶体。 */
    private static void shard(VertexConsumer consumer, Matrix4f matrix, float cx, float cy, float cz,
                              float size, float spin, int light) {
        Vector3f[] ring = new Vector3f[4];
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI * 0.5D * i + spin;
            ring[i] = new Vector3f(cx + (float) (Math.cos(angle) * size), cy,
                    cz + (float) (Math.sin(angle) * size));
        }
        Vector3f top = new Vector3f(cx, cy + size * 1.8F, cz);
        Vector3f bottom = new Vector3f(cx, cy - size * 1.8F, cz);
        for (int i = 0; i < 4; i++) {
            Vector3f a = ring[i];
            Vector3f b = ring[(i + 1) % 4];
            triangle(consumer, matrix, a, b, top, 32, 0, 16, 8, 0.66F, light, true);
            triangle(consumer, matrix, b, a, bottom, 32, 8, 16, 8, 0.66F, light, false);
        }
    }

    /** 写一个三角形：法线取自顶点顺序，UV 取自贴图的一块矩形。 */
    private static void triangle(VertexConsumer consumer, Matrix4f matrix, Vector3f a, Vector3f b,
                                 Vector3f c, float u0, float v0, float width, float height,
                                 float alpha, int light, boolean outward) {
        Vector3f normal = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
        if (normal.lengthSquared() < 1.0E-6F) {
            return;
        }
        normal.normalize();
        if (outward) {
            normal.negate();
        }
        float uu0 = u0 / 64.0F;
        float vv0 = v0 / 64.0F;
        float uu1 = (u0 + width) / 64.0F;
        float vv1 = (v0 + height) / 64.0F;
        vertex(consumer, matrix, a, uu0, vv1, normal, alpha, light);
        vertex(consumer, matrix, b, uu1, vv1, normal, alpha, light);
        vertex(consumer, matrix, c, (uu0 + uu1) * 0.5F, vv0, normal, alpha, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vector3f pos, float u, float v,
                               Vector3f normal, float alpha, int light) {
        consumer.addVertex(matrix, pos.x, pos.y, pos.z)
                .setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(normal.x, normal.y, normal.z);
    }
}
