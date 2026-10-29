package com.myriadbloom.client;

import com.myriadbloom.api.MyriadBloomApi;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SnowGolemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.SnowGolem;

/**
 * Renders the frenzied snow golem with the vanilla model but its own texture (red eyes).
 * The vanilla renderer hardcodes {@code textures/entity/snow_golem.png}, so only the texture
 * lookup is overridden.
 */
public class FrenziedSnowGolemRenderer extends SnowGolemRenderer {
    private static final ResourceLocation TEXTURE =
            MyriadBloomApi.id("textures/entity/frenzied_snow_golem.png");

    public FrenziedSnowGolemRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SnowGolem entity) {
        return TEXTURE;
    }
}
