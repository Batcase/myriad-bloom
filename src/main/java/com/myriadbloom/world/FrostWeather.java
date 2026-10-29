package com.myriadbloom.world;

import com.myriadbloom.FrostConfig;
import com.myriadbloom.api.MyriadBloomApi;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Tells whether a level is the Frost Realm and should be treated as being inside a blizzard.
 *
 * <p>Minecraft's weather is global: every dimension shares the overworld's {@code LevelData}, so
 * {@code /weather} always affects the whole server. The two queries that everything else funnels
 * through - {@code Level#getRainLevel} and {@code Level#getThunderLevel} - are patched by
 * {@code LevelWeatherMixin} to answer "1.0" for this dimension only. Because {@code isRaining()},
 * {@code isThundering()}, sky darkening, precipitation ticking and the client's rain renderer all
 * derive from those two values, the realm gets a real, permanent snowstorm while the overworld keeps
 * its own weather.</p>
 */
public final class FrostWeather {
    private static final ResourceKey<Level> REALM = MyriadBloomApi.FROST_REALM;

    /** True when this level is the realm and the blizzard is enabled. */
    public static boolean isBlizzard(Level level) {
        if (level == null || !level.dimension().equals(REALM)) {
            return false;
        }
        try {
            return FrostConfig.BLIZZARD_ENABLED.get();
        } catch (IllegalStateException notLoadedYet) {
            // config not read yet during very early startup: stay vanilla until it is
            return false;
        }
    }

    private FrostWeather() {
    }
}
