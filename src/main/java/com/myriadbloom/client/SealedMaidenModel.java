package com.myriadbloom.client;

import com.myriadbloom.world.entity.SealedMaiden;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** 少女的简易模型：头、长发、身体、裙摆、双臂。 */
public class SealedMaidenModel extends EntityModel<SealedMaiden> {
    private final ModelPart head;
    private final ModelPart body;

    public SealedMaidenModel(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4)
                        .texOffs(24, 16).addBox(-5.0F, 8.0F, -3.0F, 10, 10, 6),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8)
                        .texOffs(32, 0).addBox(-5.0F, -9.0F, 3.0F, 10, 12, 2),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(SealedMaiden entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        this.head.yRot = Mth.sin(ageInTicks * 0.01F) * 0.12F;
        this.body.y = 12.0F + Mth.sin(ageInTicks * 0.02F) * 0.4F;
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,
                               com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, int overlay,
                               int colour) {
        this.head.render(pose, consumer, light, overlay, colour);
        this.body.render(pose, consumer, light, overlay, colour);
    }
}
