package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.world.entity.OriginApe;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders the ten block tall origin ape; large shadow radius to sell the scale. */
public class OriginApeRenderer extends MobRenderer<OriginApe, OriginApeModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/origin_ape.png");

    public OriginApeRenderer(EntityRendererProvider.Context context) {
        super(context, new OriginApeModel(context.bakeLayer(FrostClientModEvents.ORIGIN_APE)), 1.8F);
    }

    /** The model is drawn 10 blocks tall; the ape is 6 blocks, so everything shrinks to 60%. */
    @Override
    protected void scale(OriginApe ape, com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
        pose.scale(0.6F, 0.6F, 0.6F);
    }

    @Override
    public ResourceLocation getTextureLocation(OriginApe ape) {
        return TEXTURE;
    }
}
