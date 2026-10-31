package com.myriadbloom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.world.block.TrophyBlock;
import com.myriadbloom.world.block.TrophyBlockEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * 摆件渲染器：用真正的头部模型（始源人头骨 / 极冰棱晶），支持落地和贴墙。
 */
public class TrophyRenderer implements BlockEntityRenderer<TrophyBlockEntity> {
    private static final ResourceLocation APE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/origin_ape_trophy.png");
    private static final ResourceLocation GLACIAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/glacial_trophy_head.png");

    private final ModelPart apeHead;
    private final ModelPart glacialHead;

    public TrophyRenderer(BlockEntityRendererProvider.Context context) {
        this.apeHead = context.getModelSet().bakeLayer(OriginApeTrophyLayer.LAYER);
        this.glacialHead = context.getModelSet().bakeLayer(GlacialTrophyLayer.LAYER);
    }

    @Override
    public void render(TrophyBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        boolean glacial = entity.getBlockState().getValue(TrophyBlock.GLACIAL);
        boolean wall = entity.getBlockState().getValue(TrophyBlock.WALL);
        ResourceLocation texture = glacial ? GLACIAL_TEXTURE : APE_TEXTURE;
        ModelPart head = glacial ? this.glacialHead : this.apeHead;

        pose.pushPose();
        if (wall) {
            Direction facing = entity.getBlockState().getValue(TrophyBlock.FACING);
            // 贴到墙面上，头朝外
            pose.translate(0.5D, 0.5D, 0.5D);
            pose.translate(facing.getStepX() * 0.45D, 0.0D, facing.getStepZ() * 0.45D);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        } else {
            int rotation = entity.getBlockState().getValue(TrophyBlock.ROTATION);
            pose.translate(0.5D, 0.0D, 0.5D);
            pose.mulPose(Axis.YP.rotationDegrees(rotation * 22.5F));
        }
        pose.scale(0.5F, 0.5F, 0.5F);
        head.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
