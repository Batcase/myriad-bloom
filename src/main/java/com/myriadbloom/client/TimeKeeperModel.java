package com.myriadbloom.client;

import com.myriadbloom.world.entity.TimeKeeper;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** 时间维护者：水晶机兵，层叠晶片装甲，头顶光环，右手水晶巨锤。 */
public class TimeKeeperModel extends EntityModel<TimeKeeper> {
    private final ModelPart root;
    private final ModelPart halo;
    private final ModelPart rightArm;
    private final ModelPart head;
    private final ModelPart hammer;

    public TimeKeeperModel(ModelPart root) {
        this.root = root;
        this.halo = root.getChild("halo");
        this.rightArm = root.getChild("r_arm");
        this.head = root.getChild("head");
        this.hammer = root.getChild("r_arm").getChild("hammer");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();


        root.addOrReplaceChild("l_foot", CubeListBuilder.create()
                        .texOffs(0, 48).addBox(-5.5F, -4.0F, -7.5F, 11, 4, 15)
                                                    .addBox(-4.5F, -6.0F, -7.5F, 9, 3, 5)
                                                    .addBox(-2.0F, -2.0F, -7.5F, 4, 2, 3)
                , PartPose.offset(8.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("l_shin", CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-4.5F, -15.0F, -4.5F, 9, 15, 9)
                                                    .addBox(-5.0F, -10.0F, -6.5F, 10, 5, 3)
                                                    .addBox(-5.0F, -16.0F, -5.0F, 10, 4, 10)
                , PartPose.offset(8.0F, 20.0F, 0.0F));
        root.getChild("l_shin").addOrReplaceChild("l_knee_plate", CubeListBuilder.create()
                        .texOffs(52, 32)
                        .addBox(-5.0F, 6.0F, -6.0F, 10, 3, 3)
                        .addBox(-4.0F, 3.0F, -6.8F, 8, 3, 2),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("l_knee", CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-4.0F, -4.0F, -2.0F, 8, 4, 4)
                , PartPose.offset(8.0F, 6.0F, -3.0F));
        root.addOrReplaceChild("l_thigh", CubeListBuilder.create()
                        .texOffs(24, 32).addBox(-5.5F, -14.0F, -5.5F, 11, 14, 11)
                                                    .addBox(-6.0F, -15.0F, -7.5F, 12, 5, 3)
                , PartPose.offset(8.0F, 4.0F, 0.0F));
        root.addOrReplaceChild("r_foot", CubeListBuilder.create()
                        .texOffs(0, 48).addBox(-5.5F, -4.0F, -7.5F, 11, 4, 15)
                                                    .addBox(-4.5F, -6.0F, -7.5F, 9, 3, 5)
                                                    .addBox(-2.0F, -2.0F, -7.5F, 4, 2, 3)
                , PartPose.offset(-8.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("r_shin", CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-4.5F, -15.0F, -4.5F, 9, 15, 9)
                                                    .addBox(-5.0F, -10.0F, -6.5F, 10, 5, 3)
                                                    .addBox(-5.0F, -16.0F, -5.0F, 10, 4, 10)
                , PartPose.offset(-8.0F, 20.0F, 0.0F));
        root.getChild("r_shin").addOrReplaceChild("r_knee_plate", CubeListBuilder.create()
                        .texOffs(52, 32)
                        .addBox(-5.0F, 6.0F, -6.0F, 10, 3, 3)
                        .addBox(-4.0F, 3.0F, -6.8F, 8, 3, 2),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("r_knee", CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-4.0F, -4.0F, -2.0F, 8, 4, 4)
                , PartPose.offset(-8.0F, 6.0F, -3.0F));
        root.addOrReplaceChild("r_thigh", CubeListBuilder.create()
                        .texOffs(24, 32).addBox(-5.5F, -14.0F, -5.5F, 11, 14, 11)
                                                    .addBox(-6.0F, -15.0F, -7.5F, 12, 5, 3)
                , PartPose.offset(-8.0F, 4.0F, 0.0F));
        root.addOrReplaceChild("pelvis", CubeListBuilder.create()
                        .texOffs(0, 20).addBox(-11.0F, -9.0F, -7.0F, 22, 9, 14)
                                                    .addBox(-4.0F, -8.0F, -9.5F, 8, 5, 3)
                , PartPose.offset(0.0F, -9.0F, 0.0F));
        root.addOrReplaceChild("skirt0", CubeListBuilder.create()
                        .texOffs(0, 56).addBox(-2.0F, -10.0F, -1.0F, 4, 10, 2)
                                                    .addBox(-1.5F, 2.0F, -1.0F, 3, 5, 2)
                , PartPose.offset(-8.0F, -12.0F, -7.0F));
        root.addOrReplaceChild("skirt1", CubeListBuilder.create()
                        .texOffs(0, 56).addBox(-2.0F, -10.0F, -1.0F, 4, 10, 2)
                                                    .addBox(-1.5F, 2.0F, -1.0F, 3, 5, 2)
                , PartPose.offset(-3.0F, -12.0F, -7.0F));
        root.addOrReplaceChild("skirt2", CubeListBuilder.create()
                        .texOffs(0, 56).addBox(-2.0F, -10.0F, -1.0F, 4, 10, 2)
                                                    .addBox(-1.5F, 2.0F, -1.0F, 3, 5, 2)
                , PartPose.offset(3.0F, -12.0F, -7.0F));
        root.addOrReplaceChild("skirt3", CubeListBuilder.create()
                        .texOffs(0, 56).addBox(-2.0F, -10.0F, -1.0F, 4, 10, 2)
                                                    .addBox(-1.5F, 2.0F, -1.0F, 3, 5, 2)
                , PartPose.offset(8.0F, -12.0F, -7.0F));
        root.addOrReplaceChild("torso", CubeListBuilder.create()
                        .texOffs(40, 0).addBox(-13.0F, -17.0F, -7.5F, 26, 17, 15)
                                                    .addBox(-7.0F, -18.0F, -9.5F, 14, 10, 3)
                                                    .addBox(-6.5F, -21.0F, -6.0F, 13, 5, 12)
                                                    .addBox(-14.0F, -18.0F, -5.5F, 4, 11, 11)
                                                    .addBox(10.0F, -18.0F, -5.5F, 4, 11, 11)
                , PartPose.offset(0.0F, -18.0F, 0.0F));
        root.addOrReplaceChild("core", CubeListBuilder.create()
                        .texOffs(0, 60).addBox(-4.0F, -8.0F, -1.5F, 8, 8, 3)
                                                    .addBox(-2.0F, -6.0F, -4.0F, 4, 4, 2)
                , PartPose.offset(0.0F, -23.0F, -9.0F));
        root.addOrReplaceChild("l_shoulder", CubeListBuilder.create()
                        .texOffs(52, 32).addBox(-7.5F, -11.0F, -9.0F, 15, 11, 18)
                                                    .addBox(-0.5F, -9.0F, -8.0F, 9, 8, 16)
                                                    .addBox(2.0F, -1.0F, -7.0F, 10, 5, 14)
                , PartPose.offset(18.0F, -23.0F, 0.0F));
        root.addOrReplaceChild("l_spike1", CubeListBuilder.create()
                        .texOffs(0, 56).addBox(-2.5F, -16.0F, -2.5F, 5, 16, 5)
                                                    .addBox(-1.5F, -20.0F, -1.5F, 3, 6, 3)
                , PartPose.offset(22.0F, -32.0F, -3.0F));
        root.addOrReplaceChild("l_spike2", CubeListBuilder.create()
                        .texOffs(0, 56).addBox(-2.0F, -11.0F, -2.0F, 4, 11, 4)
                , PartPose.offset(15.0F, -28.0F, 7.0F));
        root.addOrReplaceChild("r_shoulder", CubeListBuilder.create()
                        .texOffs(52, 32).addBox(-7.5F, -11.0F, -9.0F, 15, 11, 18)
                                                    .addBox(-8.5F, -9.0F, -8.0F, 9, 8, 16)
                                                    .addBox(-12.0F, -1.0F, -7.0F, 10, 5, 14)
                , PartPose.offset(-18.0F, -23.0F, 0.0F));
        root.addOrReplaceChild("r_spike1", CubeListBuilder.create()
                        .texOffs(0, 56).addBox(-2.5F, -16.0F, -2.5F, 5, 16, 5)
                                                    .addBox(-1.5F, -20.0F, -1.5F, 3, 6, 3)
                , PartPose.offset(-22.0F, -32.0F, -3.0F));
        root.addOrReplaceChild("r_spike2", CubeListBuilder.create()
                        .texOffs(0, 56).addBox(-2.0F, -11.0F, -2.0F, 4, 11, 4)
                , PartPose.offset(-15.0F, -28.0F, 7.0F));
        root.addOrReplaceChild("l_arm", CubeListBuilder.create()
                        .texOffs(40, 32).addBox(-4.5F, -15.0F, -4.5F, 9, 15, 9)
                                                    .addBox(-5.0F, 9.0F, -5.0F, 10, 5, 10)
                                                    .addBox(-4.5F, 6.0F, -4.5F, 9, 13, 9)
                                                    .addBox(-5.5F, 25.0F, -5.5F, 11, 6, 11)
                                                    .addBox(-5.0F, 29.0F, -5.0F, 10, 7, 10)
                , PartPose.offset(-21.0F, -24.0F, 0.0F));
        root.getChild("l_arm").addOrReplaceChild("l_finger0", CubeListBuilder.create()
                        .texOffs(0, 60).addBox(-1.5F, -6.0F, -1.5F, 3, 6, 3)
                                                    .addBox(-1.0F, 1.0F, -1.0F, 2, 4, 2),
                PartPose.offset(-4.0F, 36.0F, 3.0F));
        root.getChild("l_arm").addOrReplaceChild("l_finger1", CubeListBuilder.create()
                        .texOffs(0, 60).addBox(-1.5F, -6.0F, -1.5F, 3, 6, 3)
                                                    .addBox(-1.0F, 1.0F, -1.0F, 2, 4, 2),
                PartPose.offset(0.0F, 36.0F, 3.0F));
        root.getChild("l_arm").addOrReplaceChild("l_finger2", CubeListBuilder.create()
                        .texOffs(0, 60).addBox(-1.5F, -6.0F, -1.5F, 3, 6, 3)
                                                    .addBox(-1.0F, 1.0F, -1.0F, 2, 4, 2),
                PartPose.offset(4.0F, 36.0F, 3.0F));
        root.addOrReplaceChild("r_arm", CubeListBuilder.create()
                        .texOffs(40, 32).addBox(-4.5F, -15.0F, -4.5F, 9, 15, 9)
                                                    .addBox(-5.0F, 9.0F, -5.0F, 10, 5, 10)
                                                    .addBox(-4.5F, 6.0F, -4.5F, 9, 13, 9)
                                                    .addBox(-5.5F, 25.0F, -5.5F, 11, 6, 11)
                                                    .addBox(-5.0F, 29.0F, -5.0F, 10, 7, 10)
                , PartPose.offset(21.0F, -24.0F, 0.0F));
        root.getChild("r_arm").addOrReplaceChild("r_finger0", CubeListBuilder.create()
                        .texOffs(0, 60).addBox(-1.5F, -6.0F, -1.5F, 3, 6, 3)
                                                    .addBox(-1.0F, 1.0F, -1.0F, 2, 4, 2),
                PartPose.offset(-4.0F, 36.0F, 3.0F));
        root.getChild("r_arm").addOrReplaceChild("r_finger1", CubeListBuilder.create()
                        .texOffs(0, 60).addBox(-1.5F, -6.0F, -1.5F, 3, 6, 3)
                                                    .addBox(-1.0F, 1.0F, -1.0F, 2, 4, 2),
                PartPose.offset(0.0F, 36.0F, 3.0F));
        root.getChild("r_arm").addOrReplaceChild("r_finger2", CubeListBuilder.create()
                        .texOffs(0, 60).addBox(-1.5F, -6.0F, -1.5F, 3, 6, 3)
                                                    .addBox(-1.0F, 1.0F, -1.0F, 2, 4, 2),
                PartPose.offset(4.0F, 36.0F, 3.0F));
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(24, 20).addBox(-7.0F, -13.0F, -6.5F, 14, 13, 13)
                                                    .addBox(-9.0F, -10.0F, -6.0F, 2, 7, 12)
                                                    .addBox(7.0F, -10.0F, -6.0F, 2, 7, 12)
                                                    .addBox(-3.0F, -20.0F, -7.5F, 6, 7, 9)
                                                    .addBox(-5.5F, -11.0F, -10.0F, 11, 3, 4)
                                                    .addBox(-1.5F, -24.0F, -4.0F, 3, 6, 6)

                        // 面甲：眼窝（向内凹）+ 眉脊
                        .texOffs(0, 60).addBox(-4.5F, -9.0F, -7.8F, 3, 3, 2)
                        .texOffs(0, 60).addBox(1.5F, -9.0F, -7.8F, 3, 3, 2)
                        .texOffs(24, 20).addBox(-6.5F, -11.0F, -8.2F, 13, 2, 2)
                        .texOffs(0, 60).addBox(-1.0F, -5.0F, -7.8F, 2, 3, 2)
                , PartPose.offset(0.0F, -33.0F, 0.0F));


        // 躯干与肩部的阶梯收窄（增加斜切感）
        for (int side = 0; side < 2; side++) {
            float sx = side == 0 ? 18.0F : -18.0F;
            float dir = side == 0 ? 1.0F : -1.0F;
            root.addOrReplaceChild("step" + side, CubeListBuilder.create()
                            .texOffs(52, 32)
                            .addBox(-6.0F, -3.0F, -7.0F, 12, 3, 14)
                            .addBox(dir * 3.0F - 5.0F, -6.0F, -6.0F, 10, 3, 12)
                            .addBox(dir * 6.0F - 4.0F, -9.0F, -5.0F, 8, 3, 10),
                    PartPose.offset(sx, -18.0F, 0.0F));
        }

        PartDefinition halo = root.addOrReplaceChild("halo", CubeListBuilder.create(),
                PartPose.offset(0.0F, -44.0F, 4.0F));
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2.0 * i / 16.0;
            halo.addOrReplaceChild("seg" + i, CubeListBuilder.create()
                            .texOffs(60, 20).addBox(-1.0F, -1.5F, -1.0F, 9, 3, 2),
                    PartPose.offsetAndRotation((float) (Math.cos(angle) * 24.0D), (float) (Math.sin(angle) * 24.0D),
                            0.0F, 0.0F, 0.0F, (float) angle));
        }

        PartDefinition arm = root.getChild("r_arm");
        arm.addOrReplaceChild("hammer", CubeListBuilder.create()
                        // 锤柄
                        .texOffs(24, 56).addBox(-2.0F, -2.0F, -2.0F, 4, 22, 4)
                        .texOffs(24, 56).addBox(-3.0F, -4.0F, -3.0F, 6, 3, 6)
                        // 锤颈（加粗）
                        .texOffs(36, 56).addBox(-4.0F, 19.0F, -4.0F, 8, 5, 8)
                        // 锤头主体：厚实的战锤块（沿锤柄方向很厚）
                        .texOffs(36, 56).addBox(-8.0F, 22.0F, -11.0F, 16, 22, 22)
                        // 上下加强环
                        .texOffs(0, 32).addBox(-9.5F, 22.0F, -12.5F, 19, 5, 25)
                        .texOffs(0, 32).addBox(-9.5F, 39.0F, -12.5F, 19, 5, 25)
                        // 两侧打击面（稍微外扩）
                        .texOffs(0, 60).addBox(-6.0F, 25.0F, -13.5F, 12, 16, 3)
                        .texOffs(0, 60).addBox(-6.0F, 25.0F, 10.5F, 12, 16, 3)
                        // 顶部晶刺
                        .texOffs(0, 56).addBox(-3.0F, 44.0F, -3.0F, 6, 8, 6)
                        .texOffs(0, 56).addBox(-2.0F, 51.0F, -2.0F, 4, 5, 4),
                PartPose.offsetAndRotation(0.0F, 30.0F, -2.0F, -1.95F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(TimeKeeper entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        int state = entity.animState();
        int tick = entity.animTickClient();
        this.halo.zRot = ageInTicks * 0.05F;
        this.root.y = Mth.sin(ageInTicks * 0.03F) * 0.4F;
        this.rightArm.xRot = Mth.sin(ageInTicks * 0.04F) * 0.05F;
        // 锤子默认斜举；砸地过程中先扬到高处，落点与地面平行
        float hammerRest = -1.95F;
        this.hammer.xRot = hammerRest;
        switch (state) {
            case TimeKeeper.ANIM_SLAM -> {
                if (tick < 50) {
                    float raise = Mth.clamp(tick / 25.0F, 0.0F, 1.0F);
                    this.rightArm.xRot = -0.2F - raise * 2.2F;
                    // 举到头顶时锤子竖起来
                    this.hammer.xRot = hammerRest + raise * 0.6F;
                } else {
                    float fall = Mth.clamp((tick - 50) / 6.0F, 0.0F, 1.0F);
                    this.rightArm.xRot = -2.4F + fall * 2.7F;
                    // 砸下后锤头平行地面
                    this.hammer.xRot = (hammerRest + 0.6F) - fall * (0.6F + 1.55F);
                }
            }
            case TimeKeeper.ANIM_SWEEP -> {
                float wind = Mth.clamp(tick / 14.0F, 0.0F, 1.0F);
                this.root.yRot = -wind * 0.8F;
                this.rightArm.xRot = -1.1F * wind;
            }
            case TimeKeeper.ANIM_STASIS -> {
                this.rightArm.xRot = -0.4F;
                this.halo.zRot = ageInTicks * 0.3F;
            }
            case TimeKeeper.ANIM_HEAL -> this.rightArm.xRot = 0.35F;
            default -> { }
        }
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,
                               com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, int overlay,
                               int colour) {
        this.root.render(pose, consumer, light, overlay, colour);
    }
}
