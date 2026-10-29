package com.myriadbloom;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common (server-authoritative) configuration.
 *
 * <p>Everything that changes world behaviour lives here rather than in a hardcoded constant, so
 * server owners can tune the realm without touching the jar. The file is generated on first run at
 * {@code config/myriadbloom-common.toml}.</p>
 */
public final class FrostConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_PORTAL = BUILDER
            .comment("Allow frost portals to be lit and used.")
            .define("enable_portal", true);

    public static final ModConfigSpec.BooleanValue CONSUME_DIAMOND = BUILDER
            .comment("Consume the thrown diamond when a portal lights up (false keeps it).")
            .define("consume_diamond", true);

    public static final ModConfigSpec.IntValue PORTAL_TRANSITION_TICKS = BUILDER
            .comment("Ticks a portal needs to charge before it teleports (60 = 3 seconds).",
                    "Snowflakes gather around the entity while it charges; stepping off cancels it.")
            .defineInRange("portal_transition_ticks", 60, 0, 400);

    public static final ModConfigSpec.BooleanValue BUILD_DESTINATION_PORTAL = BUILDER
            .comment("Build a matching snow portal at the arrival column when none exists.",
                    "Disabling this is only sensible if you prefab portals on both sides; otherwise",
                    "travellers arrive in a plain column of terrain.")
            .define("build_destination_portal", true);

    public static final ModConfigSpec.BooleanValue LOG_PORTAL_EVENTS = BUILDER
            .comment("Log portal ignition, portal construction and dimension changes (debug aid).")
            .define("log_portal_events", false);

    public static final ModConfigSpec.BooleanValue WARMTH_ENABLED = BUILDER
            .comment("Enable the warmth stat (HUD meter, slowdown, freezing damage).")
            .define("warmth_enabled", true);

    public static final ModConfigSpec.IntValue HEAT_RADIUS = BUILDER
            .comment("Radius (blocks) in which a heat source refills warmth.")
            .defineInRange("heat_radius", 8, 1, 32);

    public static final ModConfigSpec.IntValue WARMUP_TICKS = BUILDER
            .comment("Ticks needed near a heat source to regain one flame (20 = 1 second).")
            .defineInRange("warmup_ticks_per_flame", 20, 1, 2400);

    public static final ModConfigSpec.IntValue COOL_TICKS = BUILDER
            .comment("Ticks without a heat source to lose one flame (800 = 40 seconds).")
            .defineInRange("cool_ticks_per_flame", 800, 20, 48000);

    public static final ModConfigSpec.BooleanValue BLIZZARD_ENABLED = BUILDER
            .comment("Spawn the endless snowstorm particles inside the realm.")
            .define("blizzard_enabled", true);

    public static final ModConfigSpec.BooleanValue THIRST_ENABLED = BUILDER
            .comment("Enable the thirst stat inside the candy dimension.")
            .define("thirst_enabled", true);

    public static final ModConfigSpec.IntValue THIRST_TICKS_PER_UNIT = BUILDER
            .comment("Ticks per half-flask of thirst lost (1200 = half a flask per minute).")
            .defineInRange("thirst_ticks_per_unit", 1200, 20, 96000);

    // ------------------------------------------------------------------ congealed land ---

    public static final ModConfigSpec.BooleanValue CONGEALED_PUNISHMENT_ENABLED = BUILDER
            .comment("Apply the congealed land penalties (damage, slowdown, faster warmth loss).")
            .define("congealed_punishment_enabled", true);

    public static final ModConfigSpec.DoubleValue CONGEALED_DAMAGE_PER_SECOND = BUILDER
            .comment("Damage per second inside the congealed land (0.5 = one heart every four seconds).")
            .defineInRange("congealed_damage_per_second", 0.5D, 0.0D, 20.0D);

    public static final ModConfigSpec.DoubleValue CONGEALED_SPEED_PENALTY = BUILDER
            .comment("Movement speed change inside the congealed land (-0.5 = 50% slower).")
            .defineInRange("congealed_speed_penalty", -0.5D, -0.95D, 0.0D);

    public static final ModConfigSpec.IntValue CONGEALED_DRAIN_MULTIPLIER = BUILDER
            .comment("Warmth and heat reserve drain this many times faster in the congealed land.")
            .defineInRange("congealed_drain_multiplier", 2, 1, 10);

    public static final ModConfigSpec.IntValue CONGEALED_BLIZZARD_PARTICLES = BUILDER
            .comment("Snowflake particles spawned around a player every 5 ticks in the congealed land.")
            .defineInRange("congealed_blizzard_particles", 34, 0, 200);

    public static final ModConfigSpec.BooleanValue CONGEALED_TORNADOES_ENABLED = BUILDER
            .comment("Let snow tornadoes form in the congealed land.")
            .define("congealed_tornadoes_enabled", true);

    public static final ModConfigSpec.DoubleValue CONGEALED_TORNADO_CHANCE = BUILDER
            .comment("Chance per player per second that a snow tornado forms (0.011 ~ one per 90 seconds).")
            .defineInRange("congealed_tornado_chance_per_second", 0.0111D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue CONGEALED_TORNADO_MAX = BUILDER
            .comment("Maximum number of snow tornadoes alive at once.")
            .defineInRange("congealed_tornado_max", 3, 0, 16);

    public static final ModConfigSpec.IntValue CONGEALED_TORNADO_LIFETIME = BUILDER
            .comment("Lifetime of one snow tornado in ticks.")
            .defineInRange("congealed_tornado_lifetime_ticks", 220, 20, 2400);

    // ---------------------------------------------------------------- snow fortress boss ---

    public static final ModConfigSpec.IntValue ORIGIN_APE_XP = BUILDER
            .comment("Experience the origin ape drops (1200 = a tenth of the ender dragon).")
            .defineInRange("origin_ape_xp", 1200, 0, 100000);

    public static final ModConfigSpec.BooleanValue ORIGIN_APE_BOSS_BAR = BUILDER
            .comment("Show the boss health bar while the origin ape is alive.")
            .define("origin_ape_boss_bar", true);

    // ------------------------------------------------------------------- frost blaze ---

    public static final ModConfigSpec.IntValue FROST_BLAZE_BOLTS = BUILDER
            .comment("Ice bolts fired per burst.")
            .defineInRange("frost_blaze_bolts_per_shot", 3, 1, 8);

    public static final ModConfigSpec.DoubleValue FROST_BLAZE_BOLT_DAMAGE = BUILDER
            .comment("Damage of one ice bolt.")
            .defineInRange("frost_blaze_bolt_damage", 6.0D, 0.0D, 40.0D);

    public static final ModConfigSpec.DoubleValue FROST_BLAZE_BOLT_KNOCKBACK = BUILDER
            .comment("Knockback strength of one ice bolt.")
            .defineInRange("frost_blaze_bolt_knockback", 0.3D, 0.0D, 3.0D);

    public static final ModConfigSpec.BooleanValue FROST_BLAZE_LAVA_KILLS = BUILDER
            .comment("The frost blaze dies instantly when it touches lava.")
            .define("frost_blaze_lava_kills", true);

    public static final ModConfigSpec.BooleanValue FROST_BLAZE_FREEZES_WATER = BUILDER
            .comment("Water under the frost blaze freezes to ice.")
            .define("frost_blaze_freezes_water", true);

    // ---------------------------------------------------------------------- thin ice ---

    public static final ModConfigSpec.IntValue THIN_ICE_BREAK_TICKS = BUILDER
            .comment("Ticks an entity has to stand on thin ice before it breaks (60 = 3 seconds).")
            .defineInRange("thin_ice_break_ticks", 60, 5, 1200);

    public static final ModConfigSpec.IntValue THIN_ICE_RESTORE_TICKS = BUILDER
            .comment("Ticks until broken thin ice freezes over again (6000 = 5 minutes).")
            .defineInRange("thin_ice_restore_ticks", 6000, 20, 240000);

    // ------------------------------------------------------- portal destination rules ---

    public static final ModConfigSpec.BooleanValue PORTAL_AVOID_SPECIAL_BIOMES = BUILDER
            .comment("Never arrive in the frosted land or the congealed land.")
            .define("portal_avoid_special_biomes", true);

    public static final ModConfigSpec.IntValue PORTAL_CLEARANCE = BUILDER
            .comment("How far (blocks) the arrival point must stay away from those biomes.")
            .defineInRange("portal_clearance_blocks", 64, 0, 256);

    // ------------------------------------------------------- ice ocean atmosphere -------

    public static final ModConfigSpec.BooleanValue ICE_OCEAN_DREAD = BUILDER
            .comment("While 极冰聚合物 is alive in an ice ocean, the patch closes in: dense cold fog and",
                    "a pulsing dark vignette. Disable to keep full visibility inside the arena.")
            .define("ice_ocean_dread", true);

    // ------------------------------------------------------- random events --------------

    public static final ModConfigSpec.BooleanValue RANDOM_EVENTS_ENABLED = BUILDER
            .comment("Enables the random events (暴风雪, 时间凝滞) that can happen anywhere in the realm.")
            .define("random_events_enabled", true);

    public static final ModConfigSpec.IntValue RANDOM_EVENT_INTERVAL_TICKS = BUILDER
            .comment("How often each event eligible dimension rolls for a random event (600 = 30 seconds).")
            .defineInRange("random_event_interval_ticks", 600, 20, 24000);

    public static final ModConfigSpec.DoubleValue RANDOM_EVENT_CHANCE = BUILDER
            .comment("Chance per roll that an event actually starts (0.15 = 15%, so roughly every 3 minutes).")
            .defineInRange("random_event_chance", 0.15D, 0.0D, 1.0D);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private FrostConfig() {
    }
}
