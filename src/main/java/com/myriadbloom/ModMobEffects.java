package com.myriadbloom;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's mob effects.
 *
 * <p>时滞 (time stasis) is cosmetic on the client: it is what draws the hourglass icon in the buff row
 * while a time stasis event freezes the world. The actual freezing lives in {@code StasisZone}.</p>
 */
public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, MyriadBloom.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> TIME_STASIS = MOB_EFFECTS.register(
            "time_stasis", TimeStasis::new);

    /** 风暴 (storm wind): the prism's passive, a flat 20% slow while the storm rages. */
    public static final DeferredHolder<MobEffect, MobEffect> STORM_WIND = MOB_EFFECTS.register(
            "storm_wind", StormWind::new);

    private static final class StormWind extends MobEffect {
        StormWind() {
            super(MobEffectCategory.HARMFUL, 0x9FE4FF);
            this.addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            MyriadBloom.MODID, "storm_wind_slow"),
                    -0.20D,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }

    /** Cosmetic-only effect: it exists so the HUD shows an hourglass while time is frozen. */
    private static final class TimeStasis extends MobEffect {
        TimeStasis() {
            super(MobEffectCategory.NEUTRAL, 0x8FD4FF);
        }
    }

    private ModMobEffects() {
    }
}
