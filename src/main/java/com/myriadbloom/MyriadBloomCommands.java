package com.myriadbloom;

import com.myriadbloom.world.ArenaGuard;
import com.myriadbloom.world.IceOceanArena;
import com.myriadbloom.world.block.BossSummonerBlock;
import com.mojang.brigadier.CommandDispatcher;
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
}
