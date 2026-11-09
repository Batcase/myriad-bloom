package com.myriadbloom.world;

import com.myriadbloom.FrostedLandContent;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 在凝结之地的中心放一座时之祭坛。
 *
 * <p>玩家进入凝结之地后，这台机制会在同一次生成的中心点上方放一块 {@code time_altar}；已经放好就跳过。</p>
 */
public final class AltarSiteMechanic implements EnvironmentMechanic {
    private static final ResourceLocation ID = MyriadBloomApi.id("altar_site");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void onLevelTick(ServerLevel level) {
        if (!MyriadBloomApi.isMyriadBloom(level)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            // 拿到骑士之誓就授予「骑士的誓言」
            for (var stack : player.getInventory().items) {
                if (stack.is(com.myriadbloom.BossContent.KNIGHT_OATH.get())) {
                    AdvancementAward.award(player, "knight", "done");
                    break;
                }
            }
            boolean inBiome = level.getBiome(player.blockPosition()).is(CongealedLandMechanic.BIOME);
            MyriadBloom.LOGGER.info("altar check: player={} biome={} inPatch={}",
                    player.getGameProfile().getName(),
                    level.getBiome(player.blockPosition()).unwrapKey().map(k -> k.location().toString()).orElse("?"),
                    inBiome);
            if (!inBiome) {
                continue;
            }
            int[] centre = com.myriadbloom.world.gen.CongealedLandFeature.patchCentre(
                    player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);
            if (!level.hasChunkAt(new BlockPos(centre[0], 64, centre[1]))) {
                continue;
            }
            // 祭坛放在冰山脚下的正前方：以中心为基准，沿四个正方向中的一个外移
            int offset = 12;
            int[][] directions = {{0, -offset}, {0, offset}, {-offset, 0}, {offset, 0}};
            int pick = Math.floorMod(centre[0] * 31 + centre[1] * 17, directions.length);
            int altarX = centre[0] + directions[pick][0];
            int altarZ = centre[1] + directions[pick][1];
            BlockPos column = new BlockPos(altarX, 0, altarZ);
            if (!level.hasChunkAt(column)) {
                continue;
            }
            int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, altarX, altarZ);
            boolean alreadyThere = false;
            for (int y = groundY - 8; y <= groundY + 8; y++) {
                if (level.getBlockState(new BlockPos(altarX, y, altarZ))
                        .is(FrostedLandContent.TIME_ALTAR.get())) {
                    alreadyThere = true;
                    break;
                }
            }
            if (alreadyThere) {
                continue;
            }
            BlockPos pos = new BlockPos(altarX, groundY, altarZ);
            // 若地表被清空（冰山被移除后），向下找实心地面
            for (int i = 0; i < 10 && !level.getBlockState(pos.below()).isSolid(); i++) {
                pos = pos.below();
            }
            boolean replaceable = level.getBlockState(pos).canBeReplaced();
            boolean solidBelow = level.getBlockState(pos.below()).isSolid();
            MyriadBloom.LOGGER.info("altar site: centre={},{} groundY={} pos={} replaceable={} solidBelow={}",
                    centre[0], centre[1], groundY, pos, replaceable, solidBelow);
            if (!replaceable || level.getBlockState(pos).is(Blocks.WATER) || !solidBelow) {
                continue;
            }
            level.setBlock(pos, FrostedLandContent.TIME_ALTAR.get().defaultBlockState(), 3);
            MyriadBloom.LOGGER.info("Time altar placed at {} {} {}", altarX, groundY, altarZ);
        }
    }

    @Override
    public void forgetPlayer(UUID playerId) {
    }

    @Override
    public String debugState() {
        return "altar_site";
    }
}
