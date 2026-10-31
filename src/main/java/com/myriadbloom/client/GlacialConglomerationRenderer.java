package com.myriadbloom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.world.entity.GlacialConglomeration;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders 极冰聚合物: an eight block cube of ice. */
public class GlacialConglomerationRenderer
        extends MobRenderer<GlacialConglomeration, net.minecraft.client.model.EntityModel<GlacialConglomeration>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/glacial_conglomeration.png");

    public GlacialConglomerationRenderer(EntityRendererProvider.Context context) {
        super(context, pickModel(context), 2.0F);
    }

    /**
     * Prefers the Bedrock model in {@code assets/myriadbloom/bedrock/glacial_conglomeration.geo.json} and
     * falls back to the hand written cube when that file is absent.
     */
    private static net.minecraft.client.model.EntityModel<GlacialConglomeration> pickModel(
            EntityRendererProvider.Context context) {
        com.myriadbloom.client.bedrock.BedrockModels.Entry entry =
                com.myriadbloom.client.bedrock.BedrockModels.get("glacial_conglomeration");
        if (entry != null) {
            return new com.myriadbloom.client.bedrock.BedrockEntityModel<>(entry);
        }
        return new GlacialConglomerationModel(context.bakeLayer(FrostClientModEvents.GLACIAL_CONGLOMERATION));
    }

    /** One model unit is 1/16 block, so an eightfold scale turns the cube into 8x8x8 blocks. */
    @Override
    protected void scale(GlacialConglomeration boss, PoseStack pose, float partialTick) {
        pose.scale(4.0F, 4.0F, 4.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(GlacialConglomeration boss) {
        return TEXTURE;
    }
}
