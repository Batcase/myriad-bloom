package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RabbitRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Rabbit;

/** The snow rabbit: a small round white rabbit with red eyes. */
public class SnowRabbitRenderer extends RabbitRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/snow_rabbit.png");

    public SnowRabbitRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void scale(Rabbit rabbit, com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
        // smaller than a vanilla rabbit, and rounder looking
        pose.scale(0.85F, 0.85F, 0.85F);
    }

    @Override
    public ResourceLocation getTextureLocation(Rabbit rabbit) {
        return TEXTURE;
    }
}
