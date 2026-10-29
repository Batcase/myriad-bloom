package com.myriadbloom;

import com.myriadbloom.world.block.MarshmallowBlock;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 棉花糖 (marshmallow) cloud blocks of the candy dimension.
 *
 * <p>Five colours only: white, yellow, cyan, sky blue and lilac. Each cloud that generates is a single
 * colour, see {@code MarshmallowCloudFeature}.</p>
 */
public final class MarshmallowContent {
    /** Colour name -> map colour, in the order they appear in the creative tab. */
    public static final Map<String, MapColor> COLORS = new LinkedHashMap<>();

    static {
        COLORS.put("white", MapColor.SNOW);
        COLORS.put("yellow", MapColor.COLOR_YELLOW);
        COLORS.put("cyan", MapColor.COLOR_CYAN);
        COLORS.put("light_blue", MapColor.COLOR_LIGHT_BLUE);
        COLORS.put("purple", MapColor.COLOR_PURPLE);
    }

    public static final Map<String, DeferredBlock<MarshmallowBlock>> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> ITEMS = new LinkedHashMap<>();

    static {
        for (Map.Entry<String, MapColor> entry : COLORS.entrySet()) {
            String name = entry.getKey();
            MapColor mapColor = entry.getValue();
            DeferredBlock<MarshmallowBlock> block = MyriadBloom.BLOCKS.register(
                    name + "_marshmallow",
                    () -> new MarshmallowBlock(BlockBehaviour.Properties.of()
                            .mapColor(mapColor)
                            .strength(0.8F)
                            .sound(MarshmallowBlock.SILENT_STEP)
                            .noOcclusion()
                            .randomTicks()));
            BLOCKS.put(name, block);
            ITEMS.put(name, MyriadBloom.ITEMS.registerSimpleBlockItem(name + "_marshmallow", block));
        }
    }

    /** A random marshmallow state, used by the cloud feature (one colour per cloud). */
    public static net.minecraft.world.level.block.state.BlockState randomCloud(RandomSource random) {
        List<DeferredBlock<MarshmallowBlock>> blocks = List.copyOf(BLOCKS.values());
        return blocks.get(random.nextInt(blocks.size())).get().defaultBlockState();
    }

    public static void bootstrap() {
        ITEMS.values().forEach(ModItems.CREATIVE_TAB_ITEMS::add);
    }

    private MarshmallowContent() {
    }
}
