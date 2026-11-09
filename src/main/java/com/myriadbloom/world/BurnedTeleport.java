package com.myriadbloom.world;

import com.myriadbloom.api.MyriadBloomApi;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/** 通往燃尽之地的传送逻辑。 */
public final class BurnedTeleport {
    public static final ResourceKey<Level> BURNED = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION, MyriadBloomApi.id("burned"));

    private BurnedTeleport() {
    }

    /** 把玩家送到燃尽之地的地表（避开岩浆）。 */
    public static void send(ServerPlayer player, ServerLevel target) {
        int x = player.getBlockX();
        int z = player.getBlockZ();
        int y = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y + 1, z);
        // 往四周找一块不是岩浆的落脚点
        for (int attempt = 0; attempt < 24; attempt++) {
            BlockPos candidate = pos.offset((attempt % 5 - 2) * 3, 0, (attempt / 5 - 2) * 3);
            int surface = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    candidate.getX(), candidate.getZ());
            BlockPos feet = new BlockPos(candidate.getX(), surface, candidate.getZ());
            if (!target.getBlockState(feet.below()).is(net.minecraft.world.level.block.Blocks.LAVA)
                    && !target.getBlockState(feet).is(net.minecraft.world.level.block.Blocks.LAVA)) {
                pos = feet;
                break;
            }
        }
        player.teleportTo(target, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
    }
}
