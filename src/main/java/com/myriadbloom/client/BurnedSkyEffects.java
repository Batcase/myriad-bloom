package com.myriadbloom.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 燃尽之地的天空：没有太阳，整体是夜空般的深色，地平线附近有淡淡的红光；
 * 夜晚在天空中挂一轮红色的月亮。
 */
public class BurnedSkyEffects extends DimensionSpecialEffects {
    public BurnedSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return new Vec3(0.36D, 0.09D, 0.06D);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f projectionMatrix,
                             Camera camera, Matrix4f modelViewMatrix, boolean isFoggy, Runnable setupFog) {
        Matrix4f matrix = new Matrix4f(projectionMatrix).mul(modelViewMatrix);
        Vec3 cameraPos = camera.getPosition();
        float time = (level.getDayTime() % 24000L) + partialTick;
        // 夜间程度：0 = 白天（天仍然很暗），1 = 深夜
        float night = Math.max(0.0F, Math.min(1.0F,
                Math.abs(time - 18000.0F) < 6000.0F ? 1.0F : 0.35F));

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        double ox = cameraPos.x;
        double oy = cameraPos.y;
        double oz = cameraPos.z;
        float size = 256.0F;
        float top = 200.0F;
        float bottom = -40.0F;

        // 天顶：接近黑色；地平线：暗红
        float[] topColor = {0.012F, 0.004F, 0.004F};
        float[] bottomColor = {0.11F + 0.05F * night, 0.028F, 0.022F};
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI / 2.0 * i;
            float x0 = (float) Math.cos(angle) * size;
            float z0 = (float) Math.sin(angle) * size;
            float x1 = (float) Math.cos(angle + Math.PI / 2.0) * size;
            float z1 = (float) Math.sin(angle + Math.PI / 2.0) * size;
            addVertex(buffer, matrix, (float) (ox + x0), (float) (oy + top), (float) (oz + z0), topColor, 1.0F);
            addVertex(buffer, matrix, (float) (ox + x1), (float) (oy + top), (float) (oz + z1), topColor, 1.0F);
            addVertex(buffer, matrix, (float) (ox + x1), (float) (oy + bottom), (float) (oz + z1), bottomColor, 1.0F);
            addVertex(buffer, matrix, (float) (ox + x0), (float) (oy + bottom), (float) (oz + z0), bottomColor, 1.0F);
        }
        // 红月：像原版一样沿天空绕行（日落后升起，午夜最高，日出前落下）
        double phase = ((time - 12000.0D) % 24000.0D + 24000.0D) % 24000.0D / 24000.0D;
        double elevation = Math.sin(phase * Math.PI * 2.0D);
        if (elevation > 0.03D) {
            double horizontal = Math.cos(phase * Math.PI * 2.0D);
            double dist = 140.0D;
            float mx = (float) (ox + horizontal * dist);
            float mz = (float) (oz - 60.0D);
            float my = (float) (oy + elevation * dist * 0.9D + 20.0D);
            // 朝向相机的广告牌
            float dirX = mx - (float) ox, dirY = my - (float) oy, dirZ = mz - (float) oz;
            float len = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
            dirX /= len; dirY /= len; dirZ /= len;
            float rx = dirZ, rz = -dirX;                       // cross(dir, up)
            float rlen = (float) Math.sqrt(rx * rx + rz * rz);
            rx /= rlen; rz /= rlen;
            float ux = dirY * rz, uy = dirZ * rx - dirX * rz, uz = -dirY * rx;
            float r = 16.0F;
            float[] moonColor = {0.92F, 0.20F, 0.12F};
            float[] glowColor = {0.34F, 0.06F, 0.04F};
            // 光晕
            addBillboard(buffer, matrix, mx, my, mz, rx, rz, ux, uy, uz, r * 2.2F, glowColor, 0.45F);
            // 月面（正反两面）
            addBillboard(buffer, matrix, mx, my, mz, rx, rz, ux, uy, uz, r, moonColor, 1.0F);
            addBillboard(buffer, matrix, mx, my, mz, -rx, -rz, -ux, -uy, -uz, r, moonColor, 1.0F);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        return true;
    }


    /** 画一个朝向相机的方形广告牌。 */
    private static void addBillboard(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float cz,
                                     float rx, float rz, float ux, float uy, float uz, float r,
                                     float[] colour, float alpha) {
        for (int i = 0; i < 2; i++) {
            float s = i == 0 ? 1.0F : -1.0F;
            addVertex(buffer, matrix, cx - rx * r, cy - uy * r, cz - rz * r, colour, alpha);
            addVertex(buffer, matrix, cx - rx * r * s, cy + uy * r, cz - rz * r * s, colour, alpha);
            addVertex(buffer, matrix, cx + rx * r * s, cy + uy * r, cz + rz * r * s, colour, alpha);
            addVertex(buffer, matrix, cx + rx * r, cy - uy * r, cz + rz * r, colour, alpha);
        }
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z,
                                  float[] colour, float alpha) {
        buffer.addVertex(matrix, x, y, z).setColor(colour[0], colour[1], colour[2], alpha);
    }
}
