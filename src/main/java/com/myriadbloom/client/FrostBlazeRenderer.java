package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.world.entity.FrostBlaze;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Blaze;

/** Renders the frost blaze with an ice blue texture instead of the fiery one. */
public class FrostBlazeRenderer extends BlazeRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/frost_blaze.png");

    public FrostBlazeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Blaze blaze) {
        return TEXTURE;
    }
}
