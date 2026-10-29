package com.myriadbloom.world.event;

import com.myriadbloom.api.MyriadBloomApi;
import java.util.List;
import java.util.Set;

/** The catalogue of random events. Everything dimension specific lives in these entries. */
public final class RandomEvents {
    /** 暴风雪: heavy snow plus a wind that halves walking speed across a wide area. */
    public static final RandomEventType BLIZZARD = new RandomEventType(
            "blizzard", Set.of(MyriadBloomApi.FROST_REALM), 3, 48.0D, 240, 400);

    /** 时间凝滞: everything inside the sphere stops, the player included, for 3 to 5 seconds. */
    public static final RandomEventType TIME_STASIS = new RandomEventType(
            "time_stasis", Set.of(MyriadBloomApi.FROST_REALM), 1, 24.0D, 60, 100);

    public static final List<RandomEventType> ALL = List.of(BLIZZARD, TIME_STASIS);

    private RandomEvents() {
    }
}
