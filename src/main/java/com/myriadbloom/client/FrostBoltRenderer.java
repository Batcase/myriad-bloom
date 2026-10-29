package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.world.entity.FrostBolt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Renders an ice bolt as a camera facing, fully lit quad with a radial icy gradient - the vanilla blaze
 * fireball look, but drawn from scratch in frost colours.
 */
public class FrostBoltRenderer extends EntityRenderer<FrostBolt> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/frost_bolt.png");
    private static final float HALF = 0.55F;

    public FrostBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FrostBolt bolt, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light) {
        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.scale(HALF, HALF, HALF);

        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        int fullBright = 0xF000F0;
        quad(consumer, matrix, -1.0F, -1.0F, 1.0F, 1.0F, fullBright);
        pose.popPose();
        super.render(bolt, yaw, partialTick, pose, buffer, light);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix,
                             float minX, float minY, float maxX, float maxY, int light) {
        vertex(consumer, matrix, minX, maxY, 0.0F, 0.0F, light);
        vertex(consumer, matrix, maxX, maxY, 1.0F, 0.0F, light);
        vertex(consumer, matrix, maxX, minY, 1.0F, 1.0F, light);
        vertex(consumer, matrix, minX, minY, 0.0F, 1.0F, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y,
                               float u, float v, int light) {
        consumer.addVertex(matrix, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0.0F, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(FrostBolt bolt) {
        return TEXTURE;
    }
}
