package com.myriadbloom;

import com.myriadbloom.world.entity.CandyCow;
import com.myriadbloom.world.entity.FrostBlaze;
import com.myriadbloom.world.entity.GlacialConglomeration;
import com.myriadbloom.world.entity.OriginApe;
import com.myriadbloom.world.entity.SnowBeast;
import com.myriadbloom.world.entity.SnowSprite;
import com.myriadbloom.world.entity.FrostBolt;
import com.myriadbloom.world.entity.CarnivorousRabbit;
import com.myriadbloom.world.entity.FrenziedSnowGolem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Entities of the Frost Realm.
 *
 * <p>Registered in their own holder so the mob code stays out of {@link MyriadBloom}. The attribute
 * event runs on the mod bus, which is why this class subscribes to it itself.</p>
 */
@EventBusSubscriber(modid = MyriadBloom.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class FrostMobs {
    /** The hostile snow golem. */
    public static final DeferredHolder<EntityType<?>, EntityType<FrenziedSnowGolem>> FRENZIED_SNOW_GOLEM =
            MyriadBloom.ENTITY_TYPES.register("frenzied_snow_golem",
                    () -> EntityType.Builder.of(FrenziedSnowGolem::new, MobCategory.MONSTER)
                            .sized(0.7F, 1.9F)
                            .clientTrackingRange(8)
                            .build("frenzied_snow_golem"));

    /** Spawn egg: white base with red spots, like the golem's red eyes. */
    public static final DeferredItem<SpawnEggItem> FRENZIED_SNOW_GOLEM_SPAWN_EGG =
            MyriadBloom.ITEMS.register("frenzied_snow_golem_spawn_egg",
                    () -> new SpawnEggItem(FRENZIED_SNOW_GOLEM.get(), 0xF2F5FA, 0xD8323A,
                            new Item.Properties()));

    /** The hostile rabbit of the frosted land. */
    public static final DeferredHolder<EntityType<?>, EntityType<CarnivorousRabbit>> CARNIVOROUS_RABBIT =
            MyriadBloom.ENTITY_TYPES.register("carnivorous_rabbit",
                    () -> EntityType.Builder.of(CarnivorousRabbit::new, MobCategory.CREATURE)
                            .sized(0.5F, 0.6F)
                            .clientTrackingRange(8)
                            .build("carnivorous_rabbit"));

    public static final DeferredItem<SpawnEggItem> CARNIVOROUS_RABBIT_SPAWN_EGG =
            MyriadBloom.ITEMS.register("carnivorous_rabbit_spawn_egg",
                    () -> new SpawnEggItem(CARNIVOROUS_RABBIT.get(), 0xE8E4DE, 0xB3262C,
                            new Item.Properties()));

    /** The candy cow of the candy dimension. */
    public static final DeferredHolder<EntityType<?>, EntityType<CandyCow>> CANDY_COW =
            MyriadBloom.ENTITY_TYPES.register("candy_cow",
                    () -> EntityType.Builder.of(CandyCow::new, MobCategory.CREATURE)
                            .sized(0.9F, 1.4F)
                            .clientTrackingRange(10)
                            .build("candy_cow"));

    public static final DeferredItem<SpawnEggItem> CANDY_COW_SPAWN_EGG =
            MyriadBloom.ITEMS.register("candy_cow_spawn_egg",
                    () -> new SpawnEggItem(CANDY_COW.get(), 0xF08AB4, 0xFFF4F8, new Item.Properties()));

    /** 寒霜神: the ice blaze. */
    public static final DeferredHolder<EntityType<?>, EntityType<FrostBlaze>> FROST_BLAZE =
            MyriadBloom.ENTITY_TYPES.register("frost_blaze",
                    () -> EntityType.Builder.of(FrostBlaze::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(8)
                            .build("frost_blaze"));

    /** Its ice bolt projectile. */
    public static final DeferredHolder<EntityType<?>, EntityType<FrostBolt>> FROST_BOLT =
            MyriadBloom.ENTITY_TYPES.register("frost_bolt",
                    () -> EntityType.Builder.<FrostBolt>of(FrostBolt::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F)
                            .clientTrackingRange(6)
                            .updateInterval(10)
                            .build("frost_bolt"));

    public static final DeferredItem<SpawnEggItem> FROST_BLAZE_SPAWN_EGG =
            MyriadBloom.ITEMS.register("frost_blaze_spawn_egg",
                    () -> new SpawnEggItem(FROST_BLAZE.get(), 0xBFE8FF, 0x2E6FA8, new Item.Properties()));

    /** 始源人: the snow fortress boss, ten blocks tall. */
    public static final DeferredHolder<EntityType<?>, EntityType<OriginApe>> ORIGIN_APE =
            MyriadBloom.ENTITY_TYPES.register("origin_ape",
                    () -> EntityType.Builder.of(OriginApe::new, MobCategory.MONSTER)
                            .sized(2.1F, 6.0F)
                            .clientTrackingRange(12)
                            .build("origin_ape"));

    public static final DeferredItem<SpawnEggItem> ORIGIN_APE_SPAWN_EGG =
            MyriadBloom.ITEMS.register("origin_ape_spawn_egg",
                    () -> new SpawnEggItem(ORIGIN_APE.get(), 0xF2F6FA, 0xC0242C, new Item.Properties()));

    /** 雪精灵: the flying hostile spirit of the congealed land. */
    public static final DeferredHolder<EntityType<?>, EntityType<SnowSprite>> SNOW_SPRITE =
            MyriadBloom.ENTITY_TYPES.register("snow_sprite",
                    () -> EntityType.Builder.of(SnowSprite::new, MobCategory.MONSTER)
                            .sized(0.6F, 0.6F)
                            .clientTrackingRange(8)
                            .build("snow_sprite"));

    public static final DeferredItem<SpawnEggItem> SNOW_SPRITE_SPAWN_EGG =
            MyriadBloom.ITEMS.register("snow_sprite_spawn_egg",
                    () -> new SpawnEggItem(SNOW_SPRITE.get(), 0xC9E9FF, 0x7FB6E8, new Item.Properties()));

    /** The sprite may spawn at any light level: the congealed land is always dark and stormy. */
    @SubscribeEvent
    public static void registerSpawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
        event.register(SNOW_SPRITE.get(), net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) -> true,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // animals that appear in the realm biome spawn lists still need an explicit placement rule,
        // otherwise NeoForge logs an error on every world start
        for (net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, ? extends EntityType<?>> animal
                : java.util.List.of(SNOW_BEAST, CANDY_COW, CARNIVOROUS_RABBIT)) {
            event.register((EntityType<?>) animal.get(),
                    net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, spawnType, pos, random) -> true,
                    net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    /** 雪兽: the white yak like neutral animal of the frost realm. */
    public static final DeferredHolder<EntityType<?>, EntityType<SnowBeast>> SNOW_BEAST =
            MyriadBloom.ENTITY_TYPES.register("snow_beast",
                    () -> EntityType.Builder.of(SnowBeast::new, MobCategory.CREATURE)
                            .sized(1.6F, 1.9F)
                            .clientTrackingRange(10)
                            .build("snow_beast"));

    public static final DeferredItem<SpawnEggItem> SNOW_BEAST_SPAWN_EGG =
            MyriadBloom.ITEMS.register("snow_beast_spawn_egg",
                    () -> new SpawnEggItem(SNOW_BEAST.get(), 0xF4F7FA, 0x8FD0F0, new Item.Properties()));

    /** 极冰聚合物: boss of the frozen ocean arena. */
    public static final DeferredHolder<EntityType<?>, EntityType<GlacialConglomeration>> GLACIAL_CONGLOMERATION =
            MyriadBloom.ENTITY_TYPES.register("glacial_conglomeration",
                    () -> EntityType.Builder.of(GlacialConglomeration::new, MobCategory.MONSTER)
                            .sized(2.4F, 3.6F)
                            // the whole ice ocean patch must know the boss exists: its presence drives
                            // the fog and the vignette while it is alive
                            .clientTrackingRange(16)
                            .build("glacial_conglomeration"));

    @SubscribeEvent
    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(FRENZIED_SNOW_GOLEM.get(), FrenziedSnowGolem.frenziedAttributes().build());
        event.put(CARNIVOROUS_RABBIT.get(), CarnivorousRabbit.carnivorousAttributes().build());
        event.put(CANDY_COW.get(), CandyCow.createAttributes().build());
        event.put(FROST_BLAZE.get(), Blaze.createAttributes().build());
        event.put(ORIGIN_APE.get(), OriginApe.originApeAttributes().build());
        event.put(SNOW_SPRITE.get(), SnowSprite.spriteAttributes().build());
        event.put(SNOW_BEAST.get(), SnowBeast.beastAttributes().build());
        event.put(GLACIAL_CONGLOMERATION.get(), GlacialConglomeration.glacialAttributes().build());
    }

    public static void bootstrap() {
        ModItems.CREATIVE_TAB_ITEMS.add(FRENZIED_SNOW_GOLEM_SPAWN_EGG);
        ModItems.CREATIVE_TAB_ITEMS.add(CARNIVOROUS_RABBIT_SPAWN_EGG);
        ModItems.CREATIVE_TAB_ITEMS.add(CANDY_COW_SPAWN_EGG);
        ModItems.CREATIVE_TAB_ITEMS.add(FROST_BLAZE_SPAWN_EGG);
        ModItems.CREATIVE_TAB_ITEMS.add(ORIGIN_APE_SPAWN_EGG);
        ModItems.CREATIVE_TAB_ITEMS.add(SNOW_SPRITE_SPAWN_EGG);
        ModItems.CREATIVE_TAB_ITEMS.add(SNOW_BEAST_SPAWN_EGG);
    }

    private FrostMobs() {
    }
}
