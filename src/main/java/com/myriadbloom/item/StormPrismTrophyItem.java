package com.myriadbloom.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** 风暴棱镜战利品：和始源人战利品同类的可佩戴摆件。 */
public class StormPrismTrophyItem extends Item {
    public StormPrismTrophyItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }
}
