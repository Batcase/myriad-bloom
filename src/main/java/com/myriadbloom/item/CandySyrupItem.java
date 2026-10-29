package com.myriadbloom.item;

import com.myriadbloom.FrostConfig;
import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.world.CandyPortalFrame;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Candy syrup: the candy portal's "flint and steel".
 *
 * <p>It is used (right click) on the air block <b>inside</b> a frost honey frame: the six interior
 * blocks turn into an animated candy portal and the frame becomes unbreakable candy rock. The item
 * is consumed. A frame built anywhere except the Frost Realm does nothing, which is how the spec's
 * "only from the frost realm" rule is enforced - the overworld copy of the frame stays inert.</p>
 *
 * <p>The item deliberately has no block placement: despite its bucket-like look it is not a bucket,
 * so it can never be spilled on the ground. The syrup fluid is placed and picked up with
 * {@code myriadbloom:syrup_bucket} instead.</p>
 */
public class CandySyrupItem extends Item {
    public CandySyrupItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return tryIgnite(context.getLevel(), context.getPlayer(), context.getItemInHand(),
                context.getClickedPos(), context.getClickedFace());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        InteractionResult result = tryIgnite(level, player, stack, hit.getBlockPos(), hit.getDirection());
        return result.consumesAction()
                ? InteractionResultHolder.success(stack)
                : InteractionResultHolder.pass(stack);
    }

    /**
     * Tries to light a candy portal around the clicked block. The three candidates are the block the
     * item would be placed into (the usual case: clicking the inner face of the frame), the clicked
     * block itself and the block behind the clicked face, so clicking a frame block from either side
     * of the doorway works.
     */
    private static InteractionResult tryIgnite(Level level, @Nullable Player player, ItemStack stack,
                                              BlockPos clicked, Direction face) {
        if (!FrostConfig.ENABLE_PORTAL.get() || !MyriadBloomApi.isMyriadBloom(level)) {
            return InteractionResult.PASS;
        }
        Optional<CandyPortalFrame.Frame> frame = findFrame(level, clicked, face);
        if (frame.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        CandyPortalFrame.ignite(level, frame.get(), player);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    private static Optional<CandyPortalFrame.Frame> findFrame(Level level, BlockPos clicked, Direction face) {
        BlockPos[] candidates = {
                clicked.relative(face),
                clicked,
                clicked.relative(face.getOpposite())
        };
        for (BlockPos candidate : candidates) {
            // Only air can be the interior of an unlit frame, so skip cheaply first.
            BlockState state = level.getBlockState(candidate);
            if (!state.isAir()) {
                continue;
            }
            Optional<CandyPortalFrame.Frame> frame = CandyPortalFrame.findIgnitable(level, candidate);
            if (frame.isPresent()) {
                return frame;
            }
        }
        return Optional.empty();
    }
}
