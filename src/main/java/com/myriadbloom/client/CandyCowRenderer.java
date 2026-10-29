package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.world.entity.CandyCow;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the candy cow with a plain cow model (no mushroom layer - the lollipops are painted on its
 * back) and its own pink texture.
 */
public class CandyCowRenderer extends MobRenderer<CandyCow, CowModel<CandyCow>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/candy_cow.png");

    public CandyCowRenderer(EntityRendererProvider.Context context) {
        super(context, new CowModel<>(context.bakeLayer(ModelLayers.COW)), 0.7F);
        this.addLayer(new CandyCowLollipopLayer(this,
                context.bakeLayer(FrostClientModEvents.CANDY_COW_LOLLIPOPS)));
    }

    @Override
    public ResourceLocation getTextureLocation(CandyCow cow) {
        return TEXTURE;
    }
}
