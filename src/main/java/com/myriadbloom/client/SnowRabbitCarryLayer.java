package com.myriadbloom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.myriadbloom.ModAttachments;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.world.entity.SnowRabbit;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.RabbitModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the snow rabbit sitting on the player's head while it is being carried.
 *
 * <p>The rabbit entity is removed when it is picked up, so this layer is the only thing that renders
 * it: a vanilla rabbit model, scaled down, sitting above the head and bobbing slightly.</p>
 */
public class SnowRabbitCarryLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/snow_rabbit.png");
    private final RabbitModel<SnowRabbit> rabbit;
    /** Throwaway entity used only to drive the model's animation. */
    private SnowRabbit dummy;

    public SnowRabbitCarryLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                                EntityModelSet models) {
        super(parent);
        this.rabbit = new RabbitModel<>(models.bakeLayer(ModelLayers.RABBIT));
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        Integer ticks = player.getData(ModAttachments.SNOW_RABBIT_CARRY.get());
        if (ticks == null || ticks <= 0) {
            return;
        }
        pose.pushPose();
        this.getParentModel().head.translateAndRotate(pose);
        // above the head, small, and nodding with the player's head
        pose.translate(0.0D, -0.62D, -0.05D);
        pose.mulPose(Axis.YP.rotationDegrees(netHeadYaw * 0.6F));
        pose.scale(0.42F, 0.42F, 0.42F);
        pose.mulPose(Axis.XP.rotationDegrees(180.0F));
        pose.translate(0.0D, -0.35D + Math.sin(ageInTicks * 0.15F) * 0.03D, 0.0D);

        if (this.dummy == null || this.dummy.level() != player.level()) {
            this.dummy = new com.myriadbloom.world.entity.SnowRabbit(
                    com.myriadbloom.FrostMobs.SNOW_RABBIT.get(), player.level());
        }
        this.dummy.setPos(player.getX(), player.getY(), player.getZ());
        this.rabbit.setupAnim(this.dummy, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        var buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.rabbit.renderToBuffer(pose, buffer, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
