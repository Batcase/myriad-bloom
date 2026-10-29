package com.myriadbloom.api;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.world.FrostBiomeMap;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Public facade of the mod. Everything an addon (or a future feature of this mod) needs to hook
 * into Frost Realm goes through here, so internals can be refactored freely.
 */
public final class MyriadBloomApi {
    /** Key of the frost dimension, as declared by {@code data/myriadbloom/dimension/permafrost.json}. */
    public static final ResourceKey<Level> FROST_REALM =
            ResourceKey.create(Registries.DIMENSION, id("permafrost"));

    /** Key of the candy dimension, as declared by {@code data/myriadbloom/dimension/candy.json}. */
    public static final ResourceKey<Level> CANDY =
            ResourceKey.create(Registries.DIMENSION, id("candy"));

    /** Registry used for new blocks. Register your entries during mod construction. */
    public static DeferredRegister.Blocks blocks() {
        return MyriadBloom.BLOCKS;
    }

    /** Registry used for new items. Register your entries during mod construction. */
    public static DeferredRegister.Items items() {
        return MyriadBloom.ITEMS;
    }

    /** Registry used for extra creative tabs. */
    public static DeferredRegister<CreativeModeTab> creativeTabs() {
        return MyriadBloom.CREATIVE_TABS;
    }

    /**
     * Registry used for mobs. Register the {@link net.minecraft.world.entity.EntityType}, then wire
     * attributes ({@code EntityAttributeCreationEvent}) and {@code SpawnPlacements} as usual.
     */
    public static DeferredRegister<EntityType<?>> entityTypes() {
        return MyriadBloom.ENTITY_TYPES;
    }

    /** Registers the entry into the mod's own creative tab. Call after creating the holder. */
    public static void addToCreativeTab(Supplier<? extends Item> item) {
        com.myriadbloom.ModItems.CREATIVE_TAB_ITEMS.add(item);
    }

    /** Registers a server-side environment rule. Safe to call during mod construction. */
    public static void registerMechanic(EnvironmentMechanic mechanic) {
        EnvironmentMechanic.Registry.register(mechanic);
    }

    /**
     * Adds/replaces a biome remap used by the Frost Realm's biome source.
     * Vanilla overworld biomes already have sensible defaults; this is for modded biomes.
     */
    public static void registerBiomeRemap(ResourceLocation from, ResourceLocation to) {
        FrostBiomeMap.register(from, to);
    }

    /** {@code true} when the given level is the Frost Realm itself. */
    public static boolean isMyriadBloom(Level level) {
        return level.dimension().equals(FROST_REALM);
    }

    /** {@code true} when the given level is the Candy Dimension itself. */
    public static boolean isCandy(Level level) {
        return level.dimension().equals(CANDY);
    }

    /** The Frost Realm level, or {@code null} when the dimension is not loaded. */
    public static ServerLevel frostRealm(MinecraftServer server) {
        return server.getLevel(FROST_REALM);
    }

    /** The Candy Dimension level, or {@code null} when the dimension is not loaded. */
    public static ServerLevel candy(MinecraftServer server) {
        return server.getLevel(CANDY);
    }

    /**
     * The dimension a candy portal in {@code from} leads to.
     *
     * <p>Candy portals only exist between the Frost Realm and the Candy Dimension: a candy frame
     * built anywhere else (the overworld, another mod's dimension) does not light up, and a candy
     * portal that somehow ends up there does not travel.</p>
     *
     * @return the destination key, or {@code null} when candy portals do not work in {@code from}
     */
    public static ResourceKey<Level> candyPortalDestination(ResourceKey<Level> from) {
        if (from.equals(FROST_REALM)) {
            return CANDY;
        }
        if (from.equals(CANDY)) {
            return FROST_REALM;
        }
        return null;
    }

    /** The dimension a portal in {@code level} leads to. */
    public static ServerLevel otherSide(MinecraftServer server, Level level) {
        return server.getLevel(otherSideKey(level));
    }

    /** Key of the dimension a portal in {@code level} leads to. */
    public static ResourceKey<Level> otherSideKey(Level level) {
        return isMyriadBloom(level) ? Level.OVERWORLD : FROST_REALM;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, path);
    }

    private MyriadBloomApi() {
    }
}
