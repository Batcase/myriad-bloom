package com.myriadbloom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.world.entity.TimeKeeper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 时间维护者：水晶半透明渲染（方块模型）。 */
public class TimeKeeperRenderer extends MobRenderer<TimeKeeper, TimeKeeperModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MyriadBloom.MODID, "textures/entity/time_keeper.png");

    public TimeKeeperRenderer(EntityRendererProvider.Context context) {
        super(context, new TimeKeeperModel(context.bakeLayer(FrostClientModEvents.TIME_KEEPER)), 1.4F);
        this.shadowStrength = 0.4F;
    }

    @Override
    public ResourceLocation getTextureLocation(TimeKeeper entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(TimeKeeper entity, PoseStack pose, float partialTick) {
        // 模型高约 74 单位，缩放到 6 格
        pose.scale(1.3F, 1.3F, 1.3F);
    }
}
