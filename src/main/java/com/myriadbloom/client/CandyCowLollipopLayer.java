package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.world.entity.CandyCow;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The little lollipops growing on a candy cow's back.
 *
 * <p>They are a real 3D model layer (not painted on the body texture), so they read from every angle.
 * The layer is skipped while the cow is sheared - shearing only removes the lollipops, the cow stays a
 * candy cow.</p>
 */
public class CandyCowLollipopLayer extends RenderLayer<CandyCow, CowModel<CandyCow>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/candy_cow_lollipops.png");

    private final ModelPart lollipops;

    public CandyCowLollipopLayer(RenderLayerParent<CandyCow, CowModel<CandyCow>> parent, ModelPart lollipops) {
        super(parent);
        this.lollipops = lollipops;
    }

    /**
     * Four lollipops standing on the cow's back. In the vanilla cow model the body occupies model
     * y 2..12 (y grows downwards), so y = 2 is the back surface and the sticks grow up from it.
     */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // the cow body's top surface sits at y = 2 model units (y grows downwards), so the sticks
        // start there and grow up into negative y
        float[][] spots = {
                {-4.0F, 2.0F, -4.0F},
                {1.5F, 2.0F, -1.5F},
                {-2.0F, 2.0F, 1.5F},
                {3.5F, 2.0F, -4.5F},
        };
        for (int i = 0; i < spots.length; i++) {
            root.addOrReplaceChild("lollipop" + i, CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-0.5F, -5.0F, -0.5F, 1, 5, 1)
                            .texOffs(0, 8).addBox(-1.5F, -6.0F, -1.5F, 3, 1, 3),
                    PartPose.offset(spots[i][0], spots[i][1], spots[i][2]));
        }
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, CandyCow cow,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (cow.isSheared() || cow.isBaby()) {
            return;
        }
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.lollipops.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
    }
}
