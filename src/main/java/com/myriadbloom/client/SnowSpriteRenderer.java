package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.world.entity.SnowSprite;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders the snow sprite with the allay model and a snowflake blue / ice crystal texture. */
public class SnowSpriteRenderer extends MobRenderer<SnowSprite, SnowSpriteModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/snow_sprite.png");

    public SnowSpriteRenderer(EntityRendererProvider.Context context) {
        super(context, new SnowSpriteModel(context.bakeLayer(ModelLayers.ALLAY)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(SnowSprite sprite) {
        return TEXTURE;
    }
}
