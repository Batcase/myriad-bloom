package com.myriadbloom.client.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;

/**
 * A vanilla {@link EntityModel} driven by a Bedrock geometry and its animations.
 *
 * <p>One animation plays at a time, chosen from the entity state: attack while the arm is swinging, move
 * while the entity walks, idle otherwise. The switch is a clean cut; blending can be added later once the
 * animation sets are known.</p>
 */
public class BedrockEntityModel<T extends LivingEntity> extends EntityModel<T> {
    private final BedrockModels.Entry entry;
    private float time;
    private String playing = "";

    public BedrockEntityModel(BedrockModels.Entry entry) {
        this.entry = entry;
    }

    public BedrockModels.Entry entry() {
        return this.entry;
    }

    /** Name of the animation currently playing, for debugging. */
    public String playing() {
        return this.playing;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        BedrockAnimation animation = this.choose(entity, limbSwingAmount);
        if (animation == null) {
            return;
        }
        if (!animation.name().equals(this.playing)) {
            this.playing = animation.name();
            this.time = 0.0F;
        }
        this.time += 1.0F / 20.0F;
        float seconds = this.time;
        float length = animation.effectiveLength();
        if (length > 0.0F && !animation.loop() && seconds > length) {
            seconds = length;
            this.time = length;
        }

        final float sampleTime = seconds;
        this.entry.model().bones().forEach((bone, part) -> {
            float[] initial = this.entry.initialRotations().get(bone);
            BedrockAnimation.Pose pose = animation.sample(bone, sampleTime);
            part.xRot = initial[0] + (float) Math.toRadians(pose.rotation()[0]);
            part.yRot = initial[1] + (float) Math.toRadians(pose.rotation()[1]);
            part.zRot = initial[2] + (float) Math.toRadians(pose.rotation()[2]);
            part.x = initial[3] + pose.position()[0] * 16.0F;
            part.y = initial[4] - pose.position()[1] * 16.0F;
            part.z = initial[5] + pose.position()[2] * 16.0F;
            part.xScale = pose.scale()[0];
            part.yScale = pose.scale()[1];
            part.zScale = pose.scale()[2];
        });
    }

    private BedrockAnimation choose(T entity, float limbSwingAmount) {
        if (entity.swinging) {
            BedrockAnimation attack = this.entry.animationMatching("attack", "slam", "punch", "swing");
            if (attack != null) {
                return attack;
            }
        }
        if (limbSwingAmount > 0.05F) {
            BedrockAnimation move = this.entry.animationMatching("walk", "move", "run");
            if (move != null) {
                return move;
            }
        }
        return this.entry.animationMatching("idle", "rest", "breath");
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int colour) {
        this.entry.model().root().render(pose, buffer, light, overlay, colour);
    }
}
