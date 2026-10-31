package com.myriadbloom.world.entity;

import com.myriadbloom.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 雪兔 (snow rabbit): the round white rabbit of the storm woodland.
 *
 * <p>Neutral and harmless, it hops exactly like a vanilla rabbit but stays in the air longer and
 * falls slowly. Feeding it a carrot tames it for a while; right clicking a tamed rabbit puts it on the
 * player's head, where it grants a little jump boost and slow falling. The rabbit stays there for five
 * minutes and then jumps off and returns to the wild.</p>
 */
public class SnowRabbit extends Rabbit {
    /** How long a carrot keeps the rabbit tame, in ticks. */
    public static final int TAME_TICKS = 6000;
    /** How long the rabbit sits on the player's head, in ticks (five minutes). */
    public static final int CARRY_TICKS = 6000;

    private boolean tamed;
    private int tameTicksLeft;

    public SnowRabbit(EntityType<? extends Rabbit> type, Level level) {
        super(type, level);
        this.xpReward = 2;
    }

    public static AttributeSupplier.Builder snowRabbitAttributes() {
        return Rabbit.createAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 10.0D);
    }

    public boolean isTamed() {
        return this.tamed;
    }

    // ------------------------------------------------------------------ behaviour

    /** A higher hop than a vanilla rabbit, which keeps it airborne for longer. */
    @Override
    public float getJumpPower() {
        return super.getJumpPower() + 0.16F;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.tamed && this.tameTicksLeft > 0) {
            this.tameTicksLeft--;
            if (this.tameTicksLeft == 0) {
                this.tamed = false;
            }
        }
        // slow falling: the descent is capped instead of accelerating
        Vec3 motion = this.getDeltaMovement();
        if (!this.onGround() && motion.y < -0.12D) {
            this.setDeltaMovement(motion.x, -0.12D, motion.z);
        }
        if (this.level().isClientSide) {
            return;
        }
        if (this.tamed && this.tickCount % 10 == 0 && this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 0.4D, this.getZ(),
                    1, 0.15D, 0.15D, 0.15D, 0.01D);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.CARROT)) {
            if (!this.level().isClientSide) {
                this.tamed = true;
                this.tameTicksLeft = TAME_TICKS;
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                this.level().playSound(null, this.blockPosition(), SoundEvents.RABBIT_AMBIENT,
                        SoundSource.NEUTRAL, 1.0F, 1.4F);
                if (this.level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 0.35D, this.getZ(),
                            3, 0.25D, 0.25D, 0.25D, 0.01D);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.tamed && !player.isShiftKeyDown() && !player.isPassenger()) {
            if (!this.level().isClientSide) {
                // the rabbit rides on the head: the entity goes away, a synced timer takes over
                player.setData(ModAttachments.SNOW_RABBIT_CARRY.get(), CARRY_TICKS);
                this.level().playSound(null, this.blockPosition(), SoundEvents.RABBIT_JUMP,
                        SoundSource.NEUTRAL, 1.0F, 1.3F);
                this.discard();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /** Used by the mechanic when the five minutes are over: drop the rabbit back to the ground. */
    public static void releaseNear(ServerLevel level, Player player) {
        SnowRabbit rabbit = com.myriadbloom.FrostMobs.SNOW_RABBIT.get().create(level);
        if (rabbit == null) {
            return;
        }
        BlockPos pos = player.blockPosition().above(2);
        rabbit.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot(), 0.0F);
        rabbit.setDeltaMovement((level.random.nextDouble() - 0.5D) * 0.35D, 0.55D,
                (level.random.nextDouble() - 0.5D) * 0.35D);
        rabbit.tamed = false;
        rabbit.tameTicksLeft = 0;
        level.addFreshEntity(rabbit);
        level.playSound(null, rabbit.blockPosition(), SoundEvents.RABBIT_JUMP, SoundSource.NEUTRAL,
                1.0F, 1.2F);
        level.sendParticles(ParticleTypes.SNOWFLAKE, rabbit.getX(), rabbit.getY(), rabbit.getZ(),
                12, 0.3D, 0.3D, 0.3D, 0.02D);
    }

    // ------------------------------------------------------------------------ nbt

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("MyriadTamed", this.tamed);
        tag.putInt("MyriadTameTicks", this.tameTicksLeft);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.tamed = tag.getBoolean("MyriadTamed");
        this.tameTicksLeft = tag.getInt("MyriadTameTicks");
    }
}
