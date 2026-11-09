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
 * 时之虚空的天空：蓝色到紫色的渐变，像置身外太空，四周散布星光。
 */
public class TimeVoidSkyEffects extends DimensionSpecialEffects {
    public TimeVoidSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return new Vec3(0.09D, 0.06D, 0.20D);
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
        double ox = cameraPos.x, oy = cameraPos.y, oz = cameraPos.z;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // 天空穹顶：顶部深紫，越往下越蓝
        float size = 320.0F;
        float top = 260.0F;
        float bottom = -120.0F;
        float[] topColor = {0.16F, 0.09F, 0.36F};
        float[] bottomColor = {0.10F, 0.22F, 0.55F};
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI / 2.0 * i;
            float x0 = (float) Math.cos(angle) * size;
            float z0 = (float) Math.sin(angle) * size;
            float x1 = (float) Math.cos(angle + Math.PI / 2.0) * size;
            float z1 = (float) Math.sin(angle + Math.PI / 2.0) * size;
            addVertex(buffer, matrix, (float) (ox + x0), (float) (oy + top), (float) (oz + z0), topColor);
            addVertex(buffer, matrix, (float) (ox + x1), (float) (oy + top), (float) (oz + z1), topColor);
            addVertex(buffer, matrix, (float) (ox + x1), (float) (oy + bottom), (float) (oz + z1), bottomColor);
            addVertex(buffer, matrix, (float) (ox + x0), (float) (oy + bottom), (float) (oz + z0), bottomColor);
        }

        // 星空：固定分布的星点，朝向相机的小方块
        var upVec = camera.getUpVector();
        var leftVec = camera.getLeftVector();
        float starSize = 0.9F;
        for (int i = 0; i < 260; i++) {
            double hash = (i * 2654435761L & 0x7FFFFFFF) / (double) 0x7FFFFFFF;
            double hash2 = (i * 40503L + 7919L & 0x7FFFFFFF) / (double) 0x7FFFFFFF;
            double angle = hash * Math.PI * 2.0;
            double elevation = 0.05 + hash2 * 1.25;
            double radius = 240.0;
            float sx = (float) (ox + Math.cos(angle) * Math.cos(elevation) * radius);
            float sy = (float) (oy + Math.sin(elevation) * radius);
            float sz = (float) (oz + Math.sin(angle) * Math.cos(elevation) * radius);
            float brightness = 0.55F + (float) (hash2 * 0.45F);
            float[] colour = {brightness, brightness, Math.min(1.0F, brightness + 0.15F)};
            float ux = upVec.x * starSize, uy = upVec.y * starSize, uz = upVec.z * starSize;
            float lx = leftVec.x * starSize, ly = leftVec.y * starSize, lz = leftVec.z * starSize;
            addVertex(buffer, matrix, sx - lx - ux, sy - ly - uy, sz - lz - uz, colour);
            addVertex(buffer, matrix, sx - lx + ux, sy - ly + uy, sz - lz + uz, colour);
            addVertex(buffer, matrix, sx + lx + ux, sy + ly + uy, sz + lz + uz, colour);
            addVertex(buffer, matrix, sx + lx - ux, sy + ly - uy, sz + lz - uz, colour);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        return true;
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float[] c) {
        buffer.addVertex(matrix, x, y, z).setColor(c[0], c[1], c[2], 1.0F);
    }
}
