package com.myriadbloom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.item.OriginApeTrophyItem;
import com.myriadbloom.ModItems;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the 始源人战利品 on the player's head, the same way vanilla draws a skull.
 *
 * <p>The model is a shrunken ape skull: white fur box, dark face plate and red eyes taken straight from
 * the texture, so no separate eye geometry is needed.</p>
 */
public class OriginApeTrophyLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "origin_ape_trophy"), "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/origin_ape_trophy.png");

    private final ModelPart head;

    public OriginApeTrophyLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                                net.minecraft.client.model.geom.EntityModelSet models) {
        super(parent);
        this.head = models.bakeLayer(LAYER);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // skull: 8 units wide, 8 tall, 9 deep - about half a player head, with a brow ridge
        root.addOrReplaceChild("skull", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.5F, 8, 8, 9)
                        .texOffs(0, 20).addBox(-4.5F, -9.0F, -4.0F, 9, 2, 8),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(stack.getItem() instanceof OriginApeTrophyItem)) {
            return;
        }
        pose.pushPose();
        this.getParentModel().head.translateAndRotate(pose);
        // sit the skull on top of the head and tilt it forward slightly, like a helmet
        pose.translate(0.0D, -0.12D, 0.0D);
        pose.mulPose(Axis.XP.rotationDegrees(4.0F));
        var buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.head.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, -1);
        pose.popPose();
    }
}
