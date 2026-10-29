package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RabbitRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Rabbit;

/**
 * Renders the carnivorous rabbit with its own pale, red-eyed texture instead of the vanilla variants.
 */
public class CarnivorousRabbitRenderer extends RabbitRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/carnivorous_rabbit.png");

    public CarnivorousRabbitRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** Slightly bigger than a normal rabbit. */
    @Override
    protected void scale(Rabbit rabbit, com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
        pose.scale(1.2F, 1.2F, 1.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(Rabbit rabbit) {
        return TEXTURE;
    }
}
