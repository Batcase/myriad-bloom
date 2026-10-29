package com.myriadbloom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myriadbloom.world.entity.SnowSprite;
import net.minecraft.client.model.AllayModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Model adapter: the vanilla allay model is typed to {@code Allay}, so it cannot be used directly as
 * the model of our {@code SnowSprite} subclass. This thin wrapper forwards both animation and drawing
 * to it, which keeps the vanilla wing and hovering animation for free.
 */
public class SnowSpriteModel extends EntityModel<SnowSprite> {
    private final AllayModel inner;

    public SnowSpriteModel(ModelPart root) {
        this.inner = new AllayModel(root);
    }

    @Override
    public void setupAnim(SnowSprite sprite, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        this.inner.setupAnim(sprite, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int colour) {
        this.inner.renderToBuffer(pose, buffer, light, overlay, colour);
    }
}
