package com.myriadbloom.world.gen;

import com.myriadbloom.MyriadBloom;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Structure types of the mod. */
public final class FrostStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, MyriadBloom.MODID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, MyriadBloom.MODID);

    /** Snow fortress: registered so {@code /locate structure} and structure placement work. */
    public static final DeferredHolder<StructureType<?>, StructureType<SnowFortressStructure>> SNOW_FORTRESS =
            STRUCTURE_TYPES.register("snow_fortress", () -> () -> SnowFortressStructure.CODEC);

    /** Its (single) piece, needed so the built structure can be saved with the chunk. */
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SNOW_FORTRESS_PIECE =
            STRUCTURE_PIECES.register("snow_fortress", () -> (context, tag) -> SnowFortressPiece.fromTag(tag));

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        STRUCTURE_TYPES.register(bus);
        STRUCTURE_PIECES.register(bus);
    }

    private FrostStructures() {
    }
}
