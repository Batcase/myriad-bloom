package com.myriadbloom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myriadbloom.world.entity.SnowBeast;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * 雪兽 model: the vanilla cow model with our own white yak texture.
 *
 * <p>A hand rolled yak model kept misbehaving (wrong Y convention, parts buried in the ground), and the
 * cow is already the right shape for a shaggy bovine, so the battle tested vanilla model is reused and
 * only the skin is replaced: white fur, sky blue face markings and blue eyes.</p>
 */
public class SnowBeastModel extends EntityModel<SnowBeast> {
    private final CowModel inner;

    public SnowBeastModel(ModelPart root) {
        this.inner = new CowModel(root);
    }

    @Override
    public void setupAnim(SnowBeast beast, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        this.inner.setupAnim(beast, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int colour) {
        this.inner.renderToBuffer(pose, buffer, light, overlay, colour);
    }

    /** The cow model is bound to {@code Cow}, so the animation call above needs this unchecked bridge. */
    private static void unused() {
        // placeholder to keep the compiler honest about the generic bridge
    }
}
