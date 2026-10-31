package com.myriadbloom;

import com.myriadbloom.world.WarmthMechanic;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Synced player data. The warmth value lives in a NeoForge data attachment, which keeps it
 * server-authoritative while still reaching the client for the HUD (no hand written packets).
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MyriadBloom.MODID);

    /** Warmth in whole "flame" units, 0..{@link WarmthMechanic#MAX_WARMTH}. */
    public static final Supplier<AttachmentType<Integer>> WARMTH = ATTACHMENT_TYPES.register("warmth",
            () -> AttachmentType.<Integer>builder(() -> WarmthMechanic.MAX_WARMTH)
                    .serialize(Codec.INT)
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    /** Heat reserve in percent, 0..{@link com.myriadbloom.world.WarmthMechanic#MAX_HEAT}; only used by the frost armour set. */
    public static final Supplier<AttachmentType<Integer>> HEAT = ATTACHMENT_TYPES.register("heat",
            () -> AttachmentType.<Integer>builder(() -> 0)
                    .serialize(Codec.INT)
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    /** Thirst in half-flask units, 0..{@link com.myriadbloom.world.ThirstMechanic#MAX_THIRST}. */
    public static final Supplier<AttachmentType<Integer>> THIRST = ATTACHMENT_TYPES.register("thirst",
            () -> AttachmentType.<Integer>builder(() -> com.myriadbloom.world.ThirstMechanic.MAX_THIRST)
                    .serialize(Codec.INT)
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    /** Ticks the snow rabbit still sits on the player's head; 0 means none. */
    public static final Supplier<AttachmentType<Integer>> SNOW_RABBIT_CARRY = ATTACHMENT_TYPES.register(
            "snow_rabbit_carry",
            () -> AttachmentType.<Integer>builder(() -> 0)
                    .serialize(Codec.INT)
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    private ModAttachments() {
    }
}
