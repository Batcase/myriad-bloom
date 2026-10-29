package com.myriadbloom;

import com.myriadbloom.world.block.GlacierIceBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Glacier ice (极冰), the block the snow fortress is built from.
 *
 * <p>Registered here rather than in {@link ModBlocks} to keep the fortress content together.</p>
 */
public final class GlacierContent {
    /** Glacier ice: blue ice tuned towards blue-green, slippery, silk-touch only, melts near lava. */
    public static final DeferredBlock<GlacierIceBlock> GLACIER_ICE = MyriadBloom.BLOCKS.register(
            "glacier_ice",
            () -> new GlacierIceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.5F)
                    .friction(0.989F)
                    .randomTicks()
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> GLACIER_ICE_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("glacier_ice", GLACIER_ICE);

    public static void bootstrap() {
        ModItems.CREATIVE_TAB_ITEMS.add(GLACIER_ICE_ITEM);
    }

    private GlacierContent() {
    }
}
