package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * 极冰战利品的头部模型：一小块棱晶，摆件和戴在头上时共用这个形状。
 *
 * <p>模型原点在头部中心，尺寸 8x8x8 单位（半格），和始源人头骨一致。</p>
 */
public final class GlacialTrophyLayer {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "glacial_trophy"), "main");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/glacial_trophy_head.png");

    private GlacialTrophyLayer() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 5, 8)
                        .texOffs(0, 14).addBox(-3.0F, -3.0F, -3.0F, 6, 2, 6)
                        .texOffs(24, 14).addBox(-2.0F, -1.0F, -2.0F, 4, 1, 4),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }
}
