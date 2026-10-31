package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.world.entity.SealedMaiden;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 被封印的少女：站在冰山内部，微微浮动。 */
public class SealedMaidenRenderer extends MobRenderer<SealedMaiden, SealedMaidenModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/sealed_maiden.png");

    public SealedMaidenRenderer(EntityRendererProvider.Context context) {
        super(context, new SealedMaidenModel(context.bakeLayer(FrostClientModEvents.SEALED_MAIDEN)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(SealedMaiden entity) {
        return TEXTURE;
    }
}
