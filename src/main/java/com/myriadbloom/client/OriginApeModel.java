package com.myriadbloom.client;

import com.myriadbloom.world.entity.OriginApe;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Model of the 始源人: a hunched, heavy gorilla.
 *
 * <p>Built in model units (16 = one block). The silhouette is gorilla first: a huge shoulder girdle on
 * a narrow waist, long heavy arms that hang past the knees, short thick legs and a low head set between
 * the shoulders under a heavy brow.</p>
 */
public class OriginApeModel extends EntityModel<OriginApe> {
    private final ModelPart hips;
    private final ModelPart chest;
    private final ModelPart head;
    private final ModelPart brow;
    private final ModelPart jaw;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public OriginApeModel(ModelPart root) {
        this.hips = root.getChild("hips");
        this.chest = root.getChild("chest");
        this.head = root.getChild("head");
        this.brow = root.getChild("brow");
        this.jaw = root.getChild("jaw");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(0, 200).addBox(-15.0F, -34.0F, -15.0F, 30, 34, 30),
                PartPose.offset(-18F, 24F, 2F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(0, 200).addBox(-15.0F, -34.0F, -15.0F, 30, 34, 30),
                PartPose.offset(18F, 24F, 2F));

        root.addOrReplaceChild("hips", CubeListBuilder.create()
                        .texOffs(150, 190).addBox(-30.0F, -28.0F, -22.0F, 60, 28, 44),
                PartPose.offset(0F, -10F, 0F));

        root.addOrReplaceChild("chest", CubeListBuilder.create()
                        .texOffs(150, 0).addBox(-40.0F, -58.0F, -28.0F, 80, 58, 56)
                        .texOffs(0, 120).addBox(-30.0F, -66.0F, -22.0F, 60, 12, 44),
                PartPose.offsetAndRotation(0F, -34F, 0F, 0.22F, 0.0F, 0.0F));

        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-26.0F, -36.0F, -28.0F, 52, 36, 48),
                PartPose.offsetAndRotation(0F, -76F, -10F, 0.12F, 0.0F, 0.0F));
        root.addOrReplaceChild("brow", CubeListBuilder.create()
                        .texOffs(0, 88).addBox(-28.0F, -6.0F, -6.0F, 56, 12, 12),
                PartPose.offset(0F, -76F, -34F));
        root.addOrReplaceChild("jaw", CubeListBuilder.create()
                        .texOffs(120, 120).addBox(-20.0F, 0.0F, -22.0F, 40, 16, 30),
                PartPose.offset(0F, -54F, -22F));

        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(0, 60).addBox(-16.0F, 0.0F, -15.0F, 32, 78, 30)
                        .texOffs(300, 60).addBox(-19.0F, 78.0F, -19.0F, 38, 22, 38),
                PartPose.offset(-44F, -80F, -2F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(0, 60).addBox(-16.0F, 0.0F, -15.0F, 32, 78, 30)
                        .texOffs(300, 60).addBox(-19.0F, 78.0F, -19.0F, 38, 22, 38),
                PartPose.offset(44F, -80F, -2F));

        return LayerDefinition.create(mesh, 512, 256);
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer,
                               int light, int overlay, int colour) {
        this.hips.render(pose, buffer, light, overlay, colour);
        this.chest.render(pose, buffer, light, overlay, colour);
        this.head.render(pose, buffer, light, overlay, colour);
        this.brow.render(pose, buffer, light, overlay, colour);
        this.jaw.render(pose, buffer, light, overlay, colour);
        this.leftArm.render(pose, buffer, light, overlay, colour);
        this.rightArm.render(pose, buffer, light, overlay, colour);
        this.leftLeg.render(pose, buffer, light, overlay, colour);
        this.rightLeg.render(pose, buffer, light, overlay, colour);
    }

    @Override
    public void setupAnim(OriginApe ape, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        int anim = ape.getAnimState();
        float raise = anim == 2 ? 1.0F : (anim == 1 || anim == 3 ? 0.85F : 0.0F);
        boolean punch = anim == 4;
        if (anim == 2) {
            raise += Mth.sin(ageInTicks * 1.4F) * 0.05F;
        }

        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.5F;
        this.head.xRot = 0.12F + headPitch * ((float) Math.PI / 180.0F) * 0.4F;
        this.brow.xRot = this.head.xRot;
        this.jaw.xRot = this.head.xRot + (raise > 0.5F ? 0.25F : 0.0F);

        // knuckle dragging walk: the long arms swing opposite the legs
        this.rightArm.xRot = -raise * 2.3F + Mth.cos(limbSwing * 0.45F) * 0.45F * limbSwingAmount;
        this.leftArm.xRot = -raise * 2.3F - Mth.cos(limbSwing * 0.45F) * 0.45F * limbSwingAmount;
        this.rightArm.zRot = -0.16F - raise * 0.3F;
        this.leftArm.zRot = 0.16F + raise * 0.3F;
        if (punch) {
            this.rightArm.xRot = -1.8F;
            this.rightArm.zRot = -0.05F;
            this.leftArm.xRot = 0.3F;
            this.chest.yRot = 0.16F;
        } else {
            this.chest.yRot = 0.0F;
        }

        this.rightLeg.xRot = Mth.cos(limbSwing * 0.45F) * 0.55F * limbSwingAmount;
        this.leftLeg.xRot = -Mth.cos(limbSwing * 0.45F) * 0.55F * limbSwingAmount;

        // hunched, breathing heavily
        this.chest.xRot = 0.22F + Mth.sin(ageInTicks * 0.05F) * 0.03F;
        this.hips.yRot = Mth.sin(ageInTicks * 0.04F) * 0.03F;
    }
}
