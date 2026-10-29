package com.myriadbloom;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Fluids of Frost Realm: the candy dimension's syrup.
 *
 * <p>Syrup is a pale pink, slow flowing "water" that replaces every water block inside the candy
 * dimension (see {@code CandyTerrainFeature}). It is an ordinary NeoForge fluid: a {@link FluidType}
 * (which defines how entities move inside it, how thick it is and what sounds it makes) plus a
 * source and a flowing {@link BaseFlowingFluid}.</p>
 *
 * <p>The fluid objects are created inside their registry suppliers, not eagerly: a {@link Fluid}
 * constructor allocates an intrusive holder in the fluid registry, which is only writable while the
 * register events run. NeoForge fires the registry events in vanilla order, and vanilla registers
 * {@code minecraft:fluid} before {@code minecraft:block} and {@code minecraft:item}, so the block
 * ({@code myriadbloom:syrup}) and bucket item can safely call {@link #SYRUP_SOURCE} during their own
 * factories.</p>
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MyriadBloom.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, MyriadBloom.MODID);

    /** Physical properties of syrup: thicker and denser than water, still swimmable. */
    public static final DeferredHolder<FluidType, FluidType> SYRUP_TYPE = FLUID_TYPES.register("syrup",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.myriadbloom.syrup")
                    .density(1500)
                    .viscosity(6000)
                    .motionScale(0.007D)
                    .canDrown(true)
                    .canSwim(true)
                    .canConvertToSource(false)
                    .supportsBoating(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    /** The still (source) syrup. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SYRUP_SOURCE =
            FLUIDS.register("syrup", () -> new BaseFlowingFluid.Source(properties()));

    /** The flowing syrup. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> SYRUP_FLOWING =
            FLUIDS.register("flowing_syrup", () -> new BaseFlowingFluid.Flowing(properties()));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(SYRUP_TYPE, () -> SYRUP_SOURCE.get(), () -> SYRUP_FLOWING.get())
                .block(() -> ModBlocks.SYRUP.get())
                .bucket(() -> ModItems.SYRUP_BUCKET.get())
                .slopeFindDistance(3)
                .levelDecreasePerBlock(2)
                .tickRate(15)
                .explosionResistance(100.0F);
    }

    /** Forces static initialisation. Called from the mod constructor before the blocks/items. */
    public static void bootstrap() {
        // Touching the class is enough; the holders above are the registration.
    }

    private ModFluids() {
    }
}
