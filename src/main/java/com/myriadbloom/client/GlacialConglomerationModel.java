package com.myriadbloom.client;

import com.myriadbloom.world.entity.GlacialConglomeration;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 极冰聚合物 model: a single giant cube of ice in the spirit of the slime, plus a smaller inner core.
 *
 * <p>Feet on the ground follow the vanilla convention (pivot at {@code y = 24} units, box rising above it),
 * so the renderer only has to scale it up eight times to match the 8x8 hitbox.</p>
 */
public class GlacialConglomerationModel extends EntityModel<GlacialConglomeration> {
    private final ModelPart body;
    private final ModelPart core;

    public GlacialConglomerationModel(ModelPart root) {
        this.body = root.getChild("body");
        this.core = root.getChild("core");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16, 16, 16),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("core", CubeListBuilder.create()
                        .texOffs(0, 34).addBox(-4.0F, -13.0F, -4.0F, 8, 8, 8),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer,
                               int light, int overlay, int colour) {
        this.body.render(pose, buffer, light, overlay, colour);
        this.core.render(pose, buffer, light, overlay, colour);
    }

    @Override
    public void setupAnim(GlacialConglomeration boss, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // a slow breathing wobble, so the cube does not look frozen in time
        float wobble = Mth.sin(ageInTicks * 0.15F) * 0.02F;
        this.body.yScale = 1.0F + wobble;
        this.body.xScale = 1.0F - wobble * 0.5F;
        this.body.zScale = 1.0F - wobble * 0.5F;
        this.core.yScale = this.body.yScale;
    }
}
