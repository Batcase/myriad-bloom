package com.myriadbloom.world.event;

import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Definition of one random event.
 *
 * <p>Random events are meant to be reusable: the definition only says which dimensions it may happen in,
 * how likely it is relative to the others, how far it reaches and how long it lasts. Adding an event is
 * one entry in {@link RandomEvents#ALL} plus one branch in {@link ActiveRandomEvent}.</p>
 *
 * @param id         registry style id used in messages and logs
 * @param dimensions dimensions the event may start in
 * @param weight     relative chance against the other events of the same dimension
 * @param radius     radius in blocks the event affects
 * @param minTicks   shortest duration
 * @param maxTicks   longest duration
 */
public record RandomEventType(String id, Set<ResourceKey<Level>> dimensions, int weight, double radius,
                              int minTicks, int maxTicks) {
    public int randomDuration(RandomSource random) {
        return this.minTicks + random.nextInt(Math.max(1, this.maxTicks - this.minTicks + 1));
    }

    public static RandomEventType pick(List<RandomEventType> candidates, ResourceKey<Level> dimension,
                                       RandomSource random) {
        int total = 0;
        for (RandomEventType type : candidates) {
            if (type.dimensions().contains(dimension)) {
                total += type.weight();
            }
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (RandomEventType type : candidates) {
            if (!type.dimensions().contains(dimension)) {
                continue;
            }
            roll -= type.weight();
            if (roll < 0) {
                return type;
            }
        }
        return null;
    }
}
