package com.myriadbloom.world.entity;

import com.myriadbloom.CandyContent;
import com.myriadbloom.FrostMobs;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

/**
 * 糖果牛 (candy cow): a mooshroom lookalike that lives in the candy dimension.
 *
 * <ul>
 *   <li>pink fur with white candy patches, and small lollipops growing on its back instead of mushrooms</li>
 *   <li>shears on its back give 1-3 lollipops and leave it <b>bare</b>: shearing again does nothing
 *       until the lollipops grow back after five minutes (the sheared state is saved and synced, and
 *       the renderer shows a bare back while it lasts)</li>
 *   <li>it never turns into a normal cow, unlike a vanilla mooshroom</li>
 *   <li>killed it drops beef like a vanilla cow</li>
 *   <li>breeds with plain candy</li>
 * </ul>
 */
public class CandyCow extends MushroomCow {
    private static final EntityDataAccessor<Boolean> DATA_SHEARED =
            SynchedEntityData.defineId(CandyCow.class, EntityDataSerializers.BOOLEAN);
    /** Five minutes until the lollipops grow back. */
    private static final int REGROW_TICKS = 6000;

    private int regrowTimer;

    public CandyCow(EntityType<? extends MushroomCow> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SHEARED, false);
    }

    /** True while the back is bare and no lollipops can be sheared. */
    public boolean isSheared() {
        return this.entityData.get(DATA_SHEARED);
    }

    private void setSheared(boolean sheared) {
        this.entityData.set(DATA_SHEARED, sheared);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("CandySheared", this.isSheared());
        tag.putInt("CandyRegrow", this.regrowTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setSheared(tag.getBoolean("CandySheared"));
        this.regrowTimer = tag.getInt("CandyRegrow");
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        if (this.isSheared() && ++this.regrowTimer >= REGROW_TICKS) {
            this.regrowTimer = 0;
            this.setSheared(false);
        }
    }

    /** Candy is its breeding food. */
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(CandyContent.CANDY.get());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.SHEARS)) {
            if (this.isSheared()) {
                // already bare: no infinite lollipops
                return InteractionResult.PASS;
            }
            if (!this.level().isClientSide) {
                int count = 1 + this.random.nextInt(3);
                for (int i = 0; i < count; i++) {
                    this.spawnAtLocation(new ItemStack(CandyContent.CANDY_LOLLIPOP.get()));
                }
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                this.setSheared(true);
                this.regrowTimer = 0;
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /** Baby candy cows. */
    @Override
    public @Nullable MushroomCow getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return new CandyCow(FrostMobs.CANDY_COW.get(), level);
    }

    /** Beef like a vanilla cow, see the loot table. */
    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        return ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath("myriadbloom", "entities/candy_cow"));
    }
}
