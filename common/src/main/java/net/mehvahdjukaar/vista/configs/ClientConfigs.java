package net.mehvahdjukaar.vista.configs;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.moonlight.api.platform.configs.ModConfigHolder;
import net.mehvahdjukaar.vista.VistaMod;

import java.util.function.Supplier;

public class ClientConfigs {

    public static void init() {
    }

    public static final ModConfigHolder SPEC;

    public static final Supplier<Integer> RENDER_DISTANCE;
    public static final Supplier<Double> UPDATE_FPS;
    public static final Supplier<Double> MIN_UPDATE_FPS;
    public static final Supplier<Double> THROTTLING_UPDATE_MS;
    public static final Supplier<Double> UPDATE_DISTANCE;
    public static final Supplier<Integer> RESOLUTION_SCALE;
    public static final Supplier<Boolean> RENDER_DEBUG;

    static {
        ConfigBuilder builder = ConfigBuilder.create(VistaMod.MOD_ID, ConfigType.CLIENT);

        builder.push("television");
        builder.push("live_feed");

        RENDER_DISTANCE = builder
                .comment("Render distance that television will use when rendering the live feed. Decreasing this will improve the performance of TVs, possibly by a lot")
                .define("render_distance", 64, 1, 256);

        UPDATE_FPS = builder
                .gameRestart()
                .comment("How many times per second the television updates its live feed texture. Lowering this will improve performance but make the video less smooth, fractions work too")
                .define("update_fps", 10.0, 0, 60);
        MIN_UPDATE_FPS = builder
                .gameRestart()
                .comment("The minimum update fps for live feed. The mod will throttle update rate when fps are low so this serves at a lower limit")
                .define("min_update_fps", 4.0, 0, 60); //once every 5 ticks
        THROTTLING_UPDATE_MS = builder
                .gameRestart()
                .comment("The maximum number of milliseconds all the logic for updating live feeds can take before throttling begins. Lowering this will improve performance but make the video less smooth. 16.66ms equals to 5fps")
                .define("throttling_update_ms", 16.66, 0, 1000000);

        UPDATE_DISTANCE = builder
                .comment("Distance from a TV after which the feed will update in real time")
                .define("update_distance", 20, 1, 512d);

        RESOLUTION_SCALE = builder
                .comment("Scale factor for live feed resolution")
                .define("resolution_scale", 8, 1, 32);

        RENDER_DEBUG = builder
                .comment("Enables rendering of debug information for televisions")
                .define("render_debug", false);

        builder.pop();
        builder.pop();

        SPEC = builder.build();
        SPEC.forceLoad();
    }

    public static boolean isDebugOn() {
        return RENDER_DEBUG.get() || PlatHelper.isDev();
    }
}
