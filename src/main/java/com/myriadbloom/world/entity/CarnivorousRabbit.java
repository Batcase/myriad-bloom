package com.myriadbloom.world.entity;

import com.myriadbloom.FrostMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/**
 * 食人兔 (carnivorous rabbit).
 *
 * <p>A rabbit that hunts: it attacks every non-rabbit living thing and the player, has ten health and
 * hits for 1.5. Carrots do nothing (there is no taming path at all), and it drops rabbit meat about
 * half of the time. It only spawns in the frosted land.</p>
 */
public class CarnivorousRabbit extends Rabbit {
    public CarnivorousRabbit(EntityType<? extends Rabbit> entityType, Level level) {
        super(entityType, level);
    }

    /** Ten health, 1.5 attack damage, vanilla rabbit movement. */
    public static AttributeSupplier.Builder carnivorousAttributes() {
        return Rabbit.createAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.5D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // vanilla rabbits both avoid players and panic when hurt; a carnivorous rabbit must do
        // neither: the avoid goal made it run instead of attacking, and the panic goal made it flee
        // for a few seconds every time the player hit it - it looked like a hit-and-run bug
        this.goalSelector.removeAllGoals(goal -> goal instanceof AvoidEntityGoal<?>);
        this.goalSelector.removeAllGoals(goal -> goal instanceof PanicGoal);
        // hunt: whoever hurt it first, then anything that is not a rabbit
        this.targetSelector.removeAllGoals(goal -> true);
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                candidate -> !(candidate instanceof Rabbit) && !candidate.isSpectator()));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3D, true));
    }

    /** Never eats carrots: no breeding food is registered for this entity. */
    @Override
    public boolean isFood(net.minecraft.world.item.ItemStack stack) {
        return false;
    }

    /** Rabbit meat about half of the time, see {@code data/myriadbloom/loot_table/entities/carnivorous_rabbit.json}. */
    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        return ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath("myriadbloom", "entities/carnivorous_rabbit"));
    }

    /** Kept for the future map/ledger: this mob is not recorded, it just has to spawn on snow. */
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                 MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        BlockPos pos = this.blockPosition();
        if (level.getBlockState(pos.below()).is(Blocks.SNOW_BLOCK)
                || level.getBlockState(pos.below()).is(Blocks.SNOW)) {
            this.setVariant(Rabbit.Variant.WHITE);
        }
        return super.finalizeSpawn(level, difficulty, reason, spawnData);
    }
}
