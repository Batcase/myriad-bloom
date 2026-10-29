package com.myriadbloom.world.block;

import com.myriadbloom.FrostConfig;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.world.CandyPortalBuilder;
import com.myriadbloom.world.CandyPortalFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The candy portal surface: a <b>vertical</b> 2x3 sheet of animated pink/white stripes.
 *
 * <p>Unlike the flat frost pool ({@link FrostPortalBlock}) this is a doorway you walk into, built
 * exactly like a nether portal (interior 2 wide x 3 tall, one-block obsidian-style ring). The
 * surface is a thin plane whose axis is horizontal, so the portal faces north/south or east/west.</p>
 *
 * <p>The block itself carries no teleport logic: the server-side charging in
 * {@code FrostEvents#onEntityTick} drives {@code PortalCharge} exactly as it does for the frost
 * pool, and vanilla's {@link Portal} interface handles the actual dimension change (players and
 * mobs, travel sound, cooldown). Only the destination differs: candy portals connect the Frost
 * Realm and the Candy Dimension, never the overworld.</p>
 */
public class CandyPortalBlock extends Block implements Portal {
    /** Horizontal plane normal; {@code Y} is impossible for a standing doorway. */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    /** Plane in the YZ plane (thin along X). */
    private static final VoxelShape SHAPE_X = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);
    /** Plane in the XY plane (thin along Z). */
    private static final VoxelShape SHAPE_Z = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);

    public CandyPortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Z));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    // ------------------------------------------------------------------ frame integrity

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        collapseIfBroken(level, pos, state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        collapseIfBroken(level, pos, state);
    }

    /** A portal surface only exists as part of a complete frame; without it the sheet vanishes. */
    private static void collapseIfBroken(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide || CandyPortalFrame.isEditing()) {
            return;
        }
        if (CandyPortalFrame.find(level, pos, state.getValue(AXIS)).isEmpty()) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    // ------------------------------------------------------------------ portal behaviour

    @Override
    public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        if (!FrostConfig.ENABLE_PORTAL.get()) {
            return null;
        }
        ResourceKey<Level> target = MyriadBloomApi.candyPortalDestination(level.dimension());
        if (target == null) {
            // A candy portal outside the frost realm/candy dimension never travels.
            return null;
        }
        ServerLevel destination = level.getServer().getLevel(target);
        if (destination == null) {
            MyriadBloom.LOGGER.warn("Candy portal at {} has no destination level ({} not loaded?)", pos, target);
            return null;
        }
        Direction.Axis axis = level.getBlockState(pos).getValue(AXIS);
        if (FrostConfig.LOG_PORTAL_EVENTS.get()) {
            MyriadBloom.LOGGER.info("Candy portal transfer: {} -> {} at {}",
                    entity.getName().getString(), destination.dimension().location(), pos);
        }
        return CandyPortalBuilder.buildDestination(entity, destination, axis);
    }

    @Override
    public Portal.Transition getLocalTransition() {
        // Same on-screen distortion the nether portal uses.
        return Portal.Transition.CONFUSION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(120) == 0) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5F,
                    random.nextFloat() * 0.4F + 1.0F, false);
        }
        for (int i = 0; i < 3; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            // Sugar petals drift through the sheet instead of snowflakes.
            level.addParticle(ParticleTypes.CHERRY_LEAVES, x, y, z,
                    (random.nextDouble() - 0.5D) * 0.04D,
                    0.01D + random.nextDouble() * 0.02D,
                    (random.nextDouble() - 0.5D) * 0.04D);
        }
    }
}
