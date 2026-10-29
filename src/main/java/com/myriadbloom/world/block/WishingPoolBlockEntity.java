package com.myriadbloom.world.block;

import com.myriadbloom.FrostMobs;
import com.myriadbloom.SnowBeastContent;
import com.myriadbloom.ModAttachments;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 许愿池 (wishing pool) block entity.
 *
 * <p>An item of the six accepted ores dropped onto the pool is consumed, a lightning bolt strikes and a
 * reward matching the ore's rarity pops out. Three wishes are allowed; after that the pool turns into
 * blue ice and stops working.</p>
 *
 * <p>Order of value, rarest first: diamond, ember crystal, lapis, gold, frost ingot, iron ingot.</p>
 */
public class WishingPoolBlockEntity extends BlockEntity {
    public static final int MAX_USES = 3;

    /** Accepted offerings, rarest first. */
    public static final List<Item> OFFERINGS = List.of(
            Items.DIAMOND, com.myriadbloom.EmberContent.EMBER_CRYSTAL.get(), Items.LAPIS_LAZULI,
            Items.GOLD_INGOT, com.myriadbloom.ModItems.FROST_INGOT.get(), Items.IRON_INGOT);

    private int uses;
    /** Ticks during which offerings are ignored, so rewards are not swallowed again. */
    private int cooldown;
    /** Ticks each offering has spent in the water, keyed by entity id. */
    private final java.util.Map<java.util.UUID, Integer> soak = new java.util.HashMap<>();
    /** Ticks an offering has to sit in the water before the wish is granted. */
    private static final int SOAK_TICKS = 60;

    public WishingPoolBlockEntity(BlockPos pos, BlockState state) {
        super(com.myriadbloom.FrostedLandContent.WISHING_POOL_ENTITY.get(), pos, state);
    }

    public int uses() {
        return this.uses;
    }

    @Override
    public void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.uses = tag.getInt("Wishes");
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Wishes", this.uses);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    /** Called by the block's ticker: looks for a dropped offering above the pool. */
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        // the wish water never freezes and is restored if somebody empties it
        if (this.uses < MAX_USES) {
            for (int dy = 1; dy <= 2; dy++) {
                BlockPos waterPos = pos.above(dy);
                net.minecraft.world.level.block.state.BlockState waterState = level.getBlockState(waterPos);
                if (!waterState.is(net.minecraft.world.level.block.Blocks.WATER)) {
                    level.setBlockAndUpdate(waterPos, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
                }
            }
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        if (this.uses >= MAX_USES) {
            return;
        }
        // the water column is two blocks deep and items float at its very top, so the box reaches the
        // top face of the upper water block
        AABB area = new AABB(pos.getX() + 0.05D, pos.getY() + 1.0D, pos.getZ() + 0.05D,
                pos.getX() + 0.95D, pos.getY() + 3.05D, pos.getZ() + 0.95D);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area,
                entity -> entity.isAlive() && entity.isInWater()
                        && OFFERINGS.contains(entity.getItem().getItem()));
        if (items.isEmpty()) {
            this.soak.clear();
            return;
        }
        // only offerings that have been under water long enough count
        ItemEntity ripe = null;
        for (ItemEntity candidate : items) {
            int ticks = this.soak.getOrDefault(candidate.getUUID(), 0) + 1;
            this.soak.put(candidate.getUUID(), ticks);
            if (ticks >= SOAK_TICKS) {
                ripe = candidate;
                break;
            }
        }
        if (ripe == null) {
            return;
        }
        ItemEntity chosen = ripe;
        this.soak.remove(chosen.getUUID());
        ItemEntity offering = chosen;
        int rarity = rarityOf(offering.getItem().getItem());
        offering.getItem().shrink(1);
        if (offering.getItem().isEmpty()) {
            offering.discard();
        }
        this.uses++;
        // rewards pop out right above the pool; ignore items for two seconds so they are not swallowed
        this.cooldown = 40;
        this.setChanged();

        // a lightning bolt announces the wish, then the reward pops out
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2F, 1.4F);

        // the pool spits the rewards out: each one flies up and outwards, then lands on the platform
        net.minecraft.util.RandomSource random = level.random;
        for (ItemStack reward : WishingPoolLoot.roll(level, rarity, random)) {
            // spawn outside the 3x3 rim so a reward can never fall back into the wish water
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 1.9D + random.nextDouble() * 0.6D;
            ItemEntity dropped = new ItemEntity(level, pos.getX() + 0.5D + Math.cos(angle) * radius,
                    pos.getY() + 2.6D, pos.getZ() + 0.5D + Math.sin(angle) * radius, reward);
            dropped.setDeltaMovement(Math.cos(angle) * 0.05D, 0.10D, Math.sin(angle) * 0.05D);
            dropped.setDefaultPickUpDelay();
            level.addFreshEntity(dropped);
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,
                pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, 30, 0.35D, 0.2D, 0.35D, 0.15D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, 8, 0.25D, 0.15D, 0.25D, 0.06D);

        if (this.uses >= MAX_USES) {
            // the pool is spent: the whole well freezes over for good
            for (int dy = 1; dy <= 2; dy++) {
                level.setBlockAndUpdate(pos.above(dy), net.minecraft.world.level.block.Blocks.BLUE_ICE.defaultBlockState());
            }
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.BLUE_ICE.defaultBlockState());
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                    pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 40, 0.4D, 0.4D, 0.4D, 0.05D);
        }
    }

    private static int rarityOf(Item item) {
        int index = OFFERINGS.indexOf(item);
        return index < 0 ? OFFERINGS.size() : index;   // 0 = diamond, 5 = iron
    }

    public static Component usage(int uses) {
        return Component.translatable("block.myriadbloom.wishing_pool.uses", uses, MAX_USES);
    }

    private static NonNullList<ItemStack> unused() {
        return NonNullList.create();
    }
}