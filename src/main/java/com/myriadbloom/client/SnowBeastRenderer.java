package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.world.entity.SnowBeast;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders the white yak-like snow beast on the goat model. */
public class SnowBeastRenderer extends MobRenderer<SnowBeast, SnowBeastModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/snow_beast.png");

    public SnowBeastRenderer(EntityRendererProvider.Context context) {
        super(context, new SnowBeastModel(context.bakeLayer(ModelLayers.COW)), 0.8F);
    }

    /** The beast is a big yak: the cow model is drawn 30% larger than vanilla. */
    @Override
    protected void scale(SnowBeast beast, com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
        pose.scale(1.3F, 1.3F, 1.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(SnowBeast beast) {
        return TEXTURE;
    }
}
