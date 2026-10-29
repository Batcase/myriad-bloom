package com.myriadbloom;

import com.myriadbloom.world.block.JellyPhysicsBlock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The sixteen candy blocks and sixteen jelly blocks, one registered block per colour.
 *
 * <p>They used to be a single block with a {@code color} block state, which meant the creative
 * inventory only offered one item and the colour was picked at random on placement - builders could
 * not get "the red one". Now every colour is a real block ({@code red_candy_block},
 * {@code light_blue_jelly_block}, ...) with its own item, so they can be picked and placed exactly.</p>
 *
 * <p>The old {@code candy_block} / {@code jelly_block} states are still registered (outside the
 * creative tab) so worlds generated before this change keep loading.</p>
 */
public final class CandyBlocks {
    public static final Map<DyeColor, DeferredBlock<Block>> CANDY = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, DeferredBlock<Block>> JELLY = new EnumMap<>(DyeColor.class);
    private static final List<DeferredItem<BlockItem>> TAB_ITEMS = new ArrayList<>();

    static {
        for (DyeColor color : DyeColor.values()) {
            String name = color.getName();

            DeferredBlock<Block> candy = MyriadBloom.BLOCKS.register(name + "_candy_block",
                    () -> new Block(BlockBehaviour.Properties.of()
                            .mapColor(color.getMapColor())
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.AMETHYST)));
            DeferredBlock<Block> jelly = MyriadBloom.BLOCKS.register(name + "_jelly_block",
                    () -> new JellyPhysicsBlock(BlockBehaviour.Properties.of()
                            .mapColor(color.getMapColor())
                            .strength(0.8F)
                            .friction(0.8F)
                            .sound(SoundType.SLIME_BLOCK)
                            .noOcclusion()));

            CANDY.put(color, candy);
            JELLY.put(color, jelly);
            TAB_ITEMS.add(MyriadBloom.ITEMS.registerSimpleBlockItem(name + "_candy_block", candy));
            TAB_ITEMS.add(MyriadBloom.ITEMS.registerSimpleBlockItem(name + "_jelly_block", jelly));
        }
    }

    /** The candy block state of one colour (used by world generation). */
    public static BlockState candy(DyeColor color) {
        return CANDY.get(color).get().defaultBlockState();
    }

    /** The jelly block state of one colour (used by world generation). */
    public static BlockState jelly(DyeColor color) {
        return JELLY.get(color).get().defaultBlockState();
    }

    public static void bootstrap() {
        // the legacy colour-state blocks keep their items too, so nothing is missing from creative
        ModItems.CREATIVE_TAB_ITEMS.add(ModBlocks.CANDY_BLOCK_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(ModBlocks.JELLY_BLOCK_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.addAll(TAB_ITEMS);
    }

    private CandyBlocks() {
    }
}
