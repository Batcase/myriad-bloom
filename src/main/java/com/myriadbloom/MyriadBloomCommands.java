package com.myriadbloom;

import com.myriadbloom.world.ArenaGuard;
import com.myriadbloom.world.IceOceanArena;
import com.myriadbloom.world.block.BossSummonerBlock;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Small operator commands that help testing the boss encounters. */
@net.neoforged.fml.common.EventBusSubscriber(modid = MyriadBloom.MODID)
public final class MyriadBloomCommands {
    private MyriadBloomCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("myriadbloom")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("summon_boss")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> summonBoss(context, BlockPosArgument.getBlockPos(context, "pos")))))
                .then(Commands.literal("build_arena")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> buildArena(context,
                                        BlockPosArgument.getBlockPos(context, "pos")))))
                .then(Commands.literal("cache_info")
                        .executes(MyriadBloomCommands::cacheInfo))
                .then(Commands.literal("random_event")
                        .then(Commands.literal("start")
                                .then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .executes(context -> startRandomEvent(context,
                                                com.mojang.brigadier.arguments.StringArgumentType
                                                        .getString(context, "id")))))
                        .then(Commands.literal("roll")
                                .executes(MyriadBloomCommands::rollRandomEvent))
                        .then(Commands.literal("stop")
                                .executes(MyriadBloomCommands::stopRandomEvent)))
                .then(Commands.literal("pool_info")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(MyriadBloomCommands::poolInfo)))
                .then(Commands.literal("biome_scan")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(16, 20000))
                                .then(Commands.argument("step", IntegerArgumentType.integer(16, 2048))
                                        .executes(MyriadBloomCommands::biomeScan))))
                .then(Commands.literal("maze_info")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> mazeInfo(context,
                                        BlockPosArgument.getBlockPos(context, "pos")))))
                .then(Commands.literal("maze_build")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> mazeBuild(context,
                                        BlockPosArgument.getBlockPos(context, "pos"), 260))))
                .then(Commands.literal("block_at")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> blockAt(context,
                                        BlockPosArgument.getBlockPos(context, "pos")))))
                .then(Commands.literal("grow_sapling")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> growSapling(context,
                                        BlockPosArgument.getBlockPos(context, "pos")))))
                .then(Commands.literal("storm_info")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> stormInfo(context,
                                        BlockPosArgument.getBlockPos(context, "pos")))))
                .then(Commands.literal("arena_info")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> arenaInfo(context, BlockPosArgument.getBlockPos(context, "pos"))))));
    }

    private static int summonBoss(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                                  BlockPos pos) {
        ServerLevel level = context.getSource().getLevel();
        BossSummonerBlock.ignite(level, pos);
        context.getSource().sendSuccess(
                () -> Component.literal("Awoke the glacial conglomeration at " + pos), true);
        return 1;
    }

    private static int buildArena(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                                  BlockPos pos) {
        ServerLevel level = context.getSource().getLevel();
        int[] centre = IceOceanArena.centreOf(pos.getX(), pos.getZ());
        boolean built = IceOceanArena.build(level, centre[0], centre[1]);
        context.getSource().sendSuccess(
                () -> Component.literal("arena build at " + centre[0] + "," + centre[1] + " -> " + built), true);
        return built ? 1 : 0;
    }

    /** Reports every cache the mod keeps, so runaway growth is visible before it becomes an OOM. */
    private static int cacheInfo(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        StringBuilder report = new StringBuilder("portal.states=")
                .append(com.myriadbloom.world.PortalCharge.size())
                .append(" | ").append(com.myriadbloom.world.LakePopulator.cacheReport())
                .append(" | ").append(com.myriadbloom.world.gen.FallenLogFeature.cacheReport());
        for (com.myriadbloom.api.EnvironmentMechanic mechanic
                : com.myriadbloom.api.EnvironmentMechanic.Registry.all()) {
            String state = mechanic.debugState();
            if (!state.isEmpty()) {
                report.append(" | ").append(state);
            }
        }
        String text = report.toString();
        context.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    /** Starts one random event at the command position, for testing and for admins. */
    private static int startRandomEvent(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                                        String id) {
        ServerLevel level = context.getSource().getLevel();
        com.myriadbloom.world.event.RandomEventType type = com.myriadbloom.world.event.RandomEvents.ALL.stream()
                .filter(candidate -> candidate.id().equals(id)).findFirst().orElse(null);
        if (type == null) {
            context.getSource().sendFailure(Component.literal("unknown random event: " + id));
            return 0;
        }
        var centre = context.getSource().getPosition();
        int duration = type.randomDuration(level.random);
        var event = new com.myriadbloom.world.event.ActiveRandomEvent(type, centre,
                level.getGameTime() + duration);
        event.start(level);
        com.myriadbloom.world.event.RandomEventManager.setRunning(level, event);
        context.getSource().sendSuccess(() -> Component.literal(
                "started " + id + " for " + duration + " ticks"), true);
        return 1;
    }

    private static int rollRandomEvent(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        boolean rolled = com.myriadbloom.world.event.RandomEventManager.rollNow(level,
                context.getSource().getPosition());
        context.getSource().sendSuccess(() -> Component.literal("rolled=" + rolled), true);
        return rolled ? 1 : 0;
    }

    private static int stopRandomEvent(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        boolean stopped = com.myriadbloom.world.event.RandomEventManager.stopRunning(level);
        context.getSource().sendSuccess(() -> Component.literal("stopped=" + stopped), false);
        return stopped ? 1 : 0;
    }

    private static int arenaInfo(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                                 BlockPos pos) {
        ServerLevel level = context.getSource().getLevel();
        ArenaGuard guard = ArenaGuard.of(level);
        boolean sealed = guard.isSealed(pos);
        boolean built = guard.isBuilt(IceOceanArena.cellKey(pos.getX(), pos.getZ()));
        context.getSource().sendSuccess(
                () -> Component.literal("arena sealed=" + sealed + " built=" + built), false);
        return 1;
    }

    /** Debug helper for the storm woodland: prints the biome, the cross test and the four entrances. */
    private static int stormInfo(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                                 BlockPos pos) {
        ServerLevel level = context.getSource().getLevel();
        var biomeKey = level.getBiome(pos).unwrapKey().map(key -> key.location().toString()).orElse("?");
        boolean inside = com.myriadbloom.world.gen.StormCross.insideCross(pos.getX(), pos.getZ());
        int[] centre = com.myriadbloom.world.gen.CongealedLandFeature.patchCentre(pos.getX() >> 4, pos.getZ() >> 4);
        StringBuilder text = new StringBuilder();
        text.append("biome=").append(biomeKey)
                .append(" insideCross=").append(inside)
                .append(" congealedCentre=").append(centre[0]).append(",").append(centre[1]);
        for (int arm = 0; arm < 4; arm++) {
            int[] at = com.myriadbloom.world.gen.StormCross.spiralCentre(pos.getX(), pos.getZ(), arm);
            text.append(" | arm").append(arm).append("=").append(at[0]).append(",").append(at[1]);
        }
        int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                pos.getX(), pos.getZ());
        int wood = 0;
        int leaves = 0;
        for (int dz = -8; dz <= 8; dz++) {
            for (int dx = -8; dx <= 8; dx++) {
                for (int y = surface - 4; y <= surface + 16; y++) {
                    BlockPos p = new BlockPos(pos.getX() + dx, y, pos.getZ() + dz);
                    var state = level.getBlockState(p);
                    if (state.is(com.myriadbloom.StormWoodlandContent.ANCIENT_WOOD.get())) {
                        wood++;
                    } else if (state.is(com.myriadbloom.StormWoodlandContent.ANCIENT_LEAVES.get())) {
                        leaves++;
                    }
                }
            }
        }
        int shaftAir = 0;
        int shaftIce = 0;
        int chamber = surface;
        for (int y = surface - 1; y >= surface - com.myriadbloom.world.gen.StormCross.SPIRAL_DEPTH - 8; y--) {
            BlockPos p = new BlockPos(pos.getX(), y, pos.getZ());
            var state = level.getBlockState(p);
            if (state.isAir()) {
                shaftAir++;
                chamber = Math.min(chamber, y);
            } else if (state.is(com.myriadbloom.FrostedLandContent.CONGEALED_ICE.get())) {
                shaftIce++;
            }
        }
        text.append(" | shaftAir=").append(shaftAir).append(" shaftIce=").append(shaftIce)
                .append(" lowestAir=").append(chamber);
        text.append(" | surfaceY=").append(surface).append(" ancientWood=").append(wood)
                .append(" ancientLeaves=").append(leaves)
                .append(" shaftChunk=").append(com.myriadbloom.world.gen.StormCross.spiralArmAt(
                        pos.getX(), pos.getZ()));
        String message = text.toString();
        com.myriadbloom.MyriadBloom.LOGGER.info("storm_info {}", message);
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    /** Force-grows a sapling, so the tree generator can be tested without waiting for a random tick. */
    private static int growSapling(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                                   BlockPos pos) {
        ServerLevel level = context.getSource().getLevel();
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.SaplingBlock sapling)) {
            context.getSource().sendFailure(Component.literal("no sapling at " + pos + " (found "
                    + state.getBlock() + ")"));
            return 0;
        }
        boolean grown = com.myriadbloom.world.block.AncientSaplingBlock.grow(level, pos, level.random);
        context.getSource().sendSuccess(() -> Component.literal(
                (grown ? "grew sapling at " : "could not grow sapling at ") + pos), false);
        return grown ? 1 : 0;
    }

    /** Prints (and logs) the block state at a position: the reliable channel when RCON is slow. */
    private static int blockAt(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                              BlockPos pos) {
        ServerLevel level = context.getSource().getLevel();
        String state = level.getBlockState(pos).toString();
        String above = level.getBlockState(pos.above()).toString();
        String message = "block_at " + pos + " = " + state + " | above = " + above;
        com.myriadbloom.MyriadBloom.LOGGER.info(message);
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    /** Prints (and logs) the maze layout of the region that owns this position. */
    private static int mazeInfo(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                                BlockPos pos) {
        ServerLevel level = context.getSource().getLevel();
        String message = com.myriadbloom.world.StormMazeMechanic.describe(level, pos.getX(), pos.getZ());
        com.myriadbloom.MyriadBloom.LOGGER.info(message);
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    /** Forces the maze of this region to be built, one chunk at a time. */
    private static int mazeBuild(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                                 BlockPos pos, int limit) {
        ServerLevel level = context.getSource().getLevel();
        int[] centre = com.myriadbloom.world.gen.CongealedLandFeature.patchCentre(
                pos.getX() >> 4, pos.getZ() >> 4);
        com.myriadbloom.world.StormMazeMechanic.force(centre[0], centre[1]);
        String message = "maze_build queued region " + centre[0] + "," + centre[1] + " | "
                + com.myriadbloom.world.StormMazeMechanic.describe(level, pos.getX(), pos.getZ());
        com.myriadbloom.MyriadBloom.LOGGER.info(message);
        context.getSource().sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    /** 采样一片区域的群系分布，用来检查海洋占比（调试用）。 */
    private static int biomeScan(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int radius = IntegerArgumentType.getInteger(context, "radius");
        int step = Math.max(16, IntegerArgumentType.getInteger(context, "step"));
        CommandSourceStack source = context.getSource();
        var level = source.getLevel();
        var biomeSource = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
        int total = 0;
        for (int x = -radius; x <= radius; x += step) {
            for (int z = -radius; z <= radius; z += step) {
                var biome = biomeSource.getNoiseBiome(x >> 2, 20, z >> 2, sampler);
                String name = biome.unwrapKey().map(key -> key.location().getPath()).orElse("unknown");
                counts.merge(name, 1, Integer::sum);
                total++;
            }
        }
        final int totalSamples = total;
        StringBuilder text = new StringBuilder("biome_scan r=" + radius + " step=" + step
                + " total=" + totalSamples);
        counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(entry -> text.append(" | ").append(entry.getKey()).append(" ")
                        .append(entry.getValue() * 100 / Math.max(1, totalSamples)).append("%"));
        String message = text.toString();
        com.myriadbloom.MyriadBloom.LOGGER.info(message);
        source.sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    /** 打印许愿池的准确中心，方便验证它到底建在哪。 */
    private static int poolInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        int chunkX = net.minecraft.core.SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = net.minecraft.core.SectionPos.blockToSectionCoord(pos.getZ());
        int[] centre = com.myriadbloom.world.gen.WishingPoolFeature.patchCentre(chunkX, chunkZ);
        String message = "pool centre=" + centre[0] + "," + centre[1]
                + " thisChunk=" + chunkX + "," + chunkZ
                + " isCentreChunk=" + (chunkX == (centre[0] >> 4) && chunkZ == (centre[1] >> 4));
        com.myriadbloom.MyriadBloom.LOGGER.info(message);
        context.getSource().sendSuccess(() -> Component.literal(message), true);
        return 1;
    }
}
