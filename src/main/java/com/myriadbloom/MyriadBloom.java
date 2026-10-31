package com.myriadbloom;

import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.world.BlizzardMechanic;
import com.myriadbloom.world.CandyBiomeSource;
import com.myriadbloom.world.FrostBiomeSource;
import com.myriadbloom.world.ThirstMechanic;
import com.myriadbloom.world.WarmthMechanic;
import com.myriadbloom.world.gen.CandyFloorFeature;
import com.myriadbloom.world.gen.CandyTerrainFeature;
import com.myriadbloom.world.gen.LollipopFeature;
import com.myriadbloom.world.gen.FrenziedSnowGolemMechanic;
import com.myriadbloom.world.gen.FrostFloorFeature;
import com.myriadbloom.world.gen.FrostyVillageProcessor;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.biome.BiomeSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * Entry point of Frost Realm.
 *
 * <p>Content lives in per-feature holder classes ({@link ModBlocks}, {@link ModItems},
 * {@link ModCreativeTabs}) so that addon-style extensions can be added without touching
 * this class. Cross-content extension points are exposed through
 * {@link com.myriadbloom.api.MyriadBloomApi}.</p>
 */
@Mod(MyriadBloom.MODID)
public final class MyriadBloom {
    public static final String MODID = "myriadbloom";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    /** Entity types for future mobs. Empty for now; wired so new content needs no plumbing. */
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    /**
     * The biome source registry holds {@link com.mojang.serialization.MapCodec}s
     * (not {@code Codec}s), because {@code BiomeSource#codec()} is a MapCodec.
     */
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, MODID);

    public static final String FROST_BIOME_SOURCE_ID = "frost_biome_source";

    /** Structure processors used by the village template pool overrides. */
    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<?>>
            STRUCTURE_PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, MODID);

    /** Custom worldgen features (the realm's void floor). */
    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, MODID);

    public MyriadBloom(IEventBus modEventBus, ModContainer modContainer) {
        // The syrup fluid objects must exist before the blocks (syrup block) and items (syrup bucket)
        // that hold a reference to them; the fluid instances themselves are created eagerly.
        ModFluids.bootstrap();
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        BIOME_SOURCES.register(modEventBus);
        STRUCTURE_PROCESSORS.register(modEventBus);
        com.myriadbloom.world.gen.FrostStructures.register(modEventBus);
        EmberContent.ARMOR_MATERIALS.register(modEventBus);
        BossContent.BLOCK_ENTITIES.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        FEATURES.register(modEventBus);

        BIOME_SOURCES.register(FROST_BIOME_SOURCE_ID, () -> FrostBiomeSource.CODEC);
        BIOME_SOURCES.register("candy_biome_source", () -> CandyBiomeSource.CODEC);
        STRUCTURE_PROCESSORS.register("frosty_village", () -> FrostyVillageProcessor.TYPE);
        FEATURES.register("frost_floor", () -> new FrostFloorFeature());
        FEATURES.register("lollipop", () -> new LollipopFeature());
        FEATURES.register("candy_floor", () -> new CandyFloorFeature());
        FEATURES.register("candy_terrain", () -> new CandyTerrainFeature());
        FEATURES.register("fallen_log", () -> new com.myriadbloom.world.gen.FallenLogFeature());
        FEATURES.register("frozen_lake", () -> new com.myriadbloom.world.gen.FrozenLakeFeature());
        FEATURES.register("wild_crops", () -> new com.myriadbloom.world.gen.WildCropFeature());
        FEATURES.register("marshmallow_cloud", () -> new com.myriadbloom.world.gen.MarshmallowCloudFeature());
        FEATURES.register("congealed_land", () -> new com.myriadbloom.world.gen.CongealedLandFeature());
        FEATURES.register("wishing_pool", () -> new com.myriadbloom.world.gen.WishingPoolFeature());
        FEATURES.register("ancient_tree", () -> new com.myriadbloom.world.gen.AncientTreeFeature());
        FEATURES.register("storm_entrance", () -> new com.myriadbloom.world.gen.StormEntranceFeature());
        // The snow fortress is a structure now (see FrostStructures), not a placed feature.

        // Force class initialisation so all holders above are populated before registration.
        ModBlocks.bootstrap();
        ModItems.bootstrap();
        ModCreativeTabs.bootstrap();
        CandyContent.bootstrap();
        CandyBlocks.bootstrap();
        FrostMobs.bootstrap();
        FrostItems.bootstrap();
        GlacierContent.bootstrap();
        FrostedLandContent.bootstrap();
        EmberContent.bootstrap();
        MarshmallowContent.bootstrap();
        BossContent.bootstrap();
        SnowBeastContent.bootstrap();
        StormWoodlandContent.bootstrap();

        modContainer.registerConfig(ModConfig.Type.COMMON, FrostConfig.SPEC);

        // Server-side gameplay hooks (portal ignition, environment mechanics dispatch).
        NeoForge.EVENT_BUS.register(new FrostEvents());

        // Built-in environment mechanics; addons can register their own through
        // MyriadBloomApi#registerMechanic without touching this class.
        MyriadBloomApi.registerMechanic(new BlizzardMechanic());
        MyriadBloomApi.registerMechanic(new WarmthMechanic());
        MyriadBloomApi.registerMechanic(new ThirstMechanic());
        MyriadBloomApi.registerMechanic(new FrenziedSnowGolemMechanic());
        MyriadBloomApi.registerMechanic(new com.myriadbloom.world.ThinIceMechanic());
        MyriadBloomApi.registerMechanic(new com.myriadbloom.world.LakePopulator());
        MyriadBloomApi.registerMechanic(new com.myriadbloom.world.FrostMobCleanupMechanic());
        MyriadBloomApi.registerMechanic(new com.myriadbloom.world.OriginApeSpawnerMechanic());
        MyriadBloomApi.registerMechanic(new com.myriadbloom.world.CongealedLandMechanic());
        MyriadBloomApi.registerMechanic(new com.myriadbloom.world.CongealedBlizzardMechanic());
        MyriadBloomApi.registerMechanic(new com.myriadbloom.world.IceOceanArenaMechanic());
        MyriadBloomApi.registerMechanic(new com.myriadbloom.world.event.RandomEventManager());
        MyriadBloomApi.registerMechanic(new com.myriadbloom.world.EntryAdvancementMechanic());
        MyriadBloomApi.registerMechanic(new com.myriadbloom.world.StormWoodlandMechanic());
        MyriadBloomApi.registerMechanic(new com.myriadbloom.world.StormMazeMechanic());

        LOGGER.info("Frost Realm initialised (dimension: {})", MyriadBloomApi.FROST_REALM.location());
    }
}
