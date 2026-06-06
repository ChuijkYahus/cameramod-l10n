package net.mehvahdjukaar.vista.configs;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.moonlight.api.platform.configs.ModConfigHolder;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.textures.ScalingMode;
import net.mehvahdjukaar.vista.integration.CompatHandler;

import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class ClientConfigs {

    public static void init() {
    }

    public static final ModConfigHolder SPEC;

    public static final Supplier<Integer> RENDER_DISTANCE;
    public static final Supplier<Integer> MIRROR_RENDER_DISTANCE;
    public static final Supplier<Integer> MIRROR_RESOLUTION_SCALE;
    public static final Supplier<MirrorUpdateMode> MIRROR_UPDATE_MODE;
    public static final Supplier<Double> UPDATE_FPS;
    public static final Supplier<Double> MIN_UPDATE_FPS;
    public static final Supplier<Double> THROTTLING_UPDATE_MS;
    public static final Supplier<Double> UPDATE_DISTANCE;
    public static final Supplier<Integer> LIVE_FEED_RESOLUTION_SCALE;
    public static final Supplier<Boolean> RENDER_DEBUG;
    public static final Supplier<Boolean> SCALE_PIXELS;
    public static final Supplier<Boolean> TURN_OFF_EFFECTS;
    public static final Supplier<Float> PIXEL_DENSITY;
    public static final Supplier<Float> VIGNETTE;
    public static final Supplier<Boolean> SCREEN_EFFECTS;
    public static final Supplier<Boolean> ENABLE_FFMPEG;
    public static final Supplier<Integer> WEB_RESOLUTION_SCALE;
    public static final Supplier<ScalingMode> SCALING_MODE;
    public static final Supplier<Boolean> BILINEAR;
    public static final Supplier<EngineMode> VIDEO_ENGINE;
    public static final Supplier<List<String>> SAFE_URLS;
    public static Pattern safeRegex;


    static {
        ConfigBuilder builder = ConfigBuilder.create(VistaMod.MOD_ID, ConfigType.CLIENT);

        builder.push("mirror");
        MIRROR_RENDER_DISTANCE = builder
                .comment("Block entity render distance for mirrors. Mirrors beyond this distance will not render their reflection.")
                .define("render_distance", 64, 1, 2048);
        MIRROR_RESOLUTION_SCALE = builder
                .comment("Scale factor for mirror reflection resolution. Each mirror block is 16 virtual pixels wide; this multiplies that area. Higher values are sharper but more expensive.")
                .define("resolution_scale", 8, 1, 32);
        MIRROR_UPDATE_MODE = builder
                .comment("How mirror reflections are scheduled. TEXTURE_REFRESH: piggybacks on the live-feed texture refresh dispatch (one render per visible mirror, end-of-frame). RENDER_TICK_END: the BE renderer queues mirrors into a pending list which is flushed from the onRenderTickEnd hook (the old behavior). Switch if you suspect a timing-related rendering glitch.")
                .define("update_mode", MirrorUpdateMode.TEXTURE_REFRESH);
        builder.pop();

        builder.push("television");
        builder.push("visuals");
        RENDER_DISTANCE = builder
                .comment("Render distance that television will use when rendering the live feed. Decreasing this will improve the performance of TVs, possibly by a lot")
                .define("render_distance", 64, 1, 2048);

        SCREEN_EFFECTS = builder
                .comment("Turns off all the tv screen effects and draws it as a simple texture. Disabling can make the render slightly faster. All below options will be ignored if this is disabled")
                .define("screen_effects", true);
        PIXEL_DENSITY = builder
                .comment("Pixel density of televisions, in pixels per block side")
                .define("pixel_density", 1.37f, 0.1f, 10);
        SCALE_PIXELS = builder.comment("Make connected tvs have higher pixel density, such that the per block pixel density is constant")
                .define("constant_pixel_density", true);
        VIGNETTE = builder
                .comment("Amount of vignette effect applied to television live feed (0 = none, 1 = full)")
                .define("vignette", 1f, 0f, 1f);
        TURN_OFF_EFFECTS = builder
                .comment("Plays an animation when the television is turned off or on")
                .define("turn_off_animation", true);
        builder.pop();
        builder.push("live_feed");

        UPDATE_FPS = builder
                .gameRestart()
                .comment("How many times per second the television updates its live feed texture. Lowering this will improve performance but make the video less smooth, fractions work too")
                .define("update_fps", 10.0, 1, 60);
        MIN_UPDATE_FPS = builder
                .gameRestart()
                .comment("The minimum update fps for live feed. The mod will throttle update rate when fps are low so this serves at a lower limit")
                .define("min_update_fps", 4.0, 1, 60); //once every 5 ticks
        THROTTLING_UPDATE_MS = builder
                .gameRestart()
                .comment("The maximum number of milliseconds all the logic for updating live feeds can take before fps throttling begins. Lowering this will improve performance but make the video less smooth. 16.66ms equals to 5fps")
                .define("throttling_update_ms", 16.66, 0, 1000000);

        UPDATE_DISTANCE = builder
                .comment("Distance from a TV after which the feed will update in real time")
                .define("update_distance", 24, 1, 512d);

        LIVE_FEED_RESOLUTION_SCALE = builder
                .comment("Scale factor for live feed resolution. A tv screen is 12x12 pixels, this number multiplies that area")
                .define("resolution_scale", 8, 1, 32);

        RENDER_DEBUG = builder
                .comment("Enables rendering of debug information for televisions")
                .define("render_debug", false);

        CompatHandler.addConfigs(builder);

        builder.pop();

        builder.push("wave_gate");
        VIDEO_ENGINE = CompatHandler.WATERMEDIA ? builder.comment("Toggle between local FFmpeg driven video loading and WaterMedia (VLC) mod usage. Requires Watermedia mod. FFmpeg mode has improved visuals and functionality, and likely supports more media types. Watermedia on the other hand supports youtube links. The first mode uses both, prioritizing our local FFmpeg impl and falling back to watermedia on media player links.")
                                                  .define("media_engine", EngineMode.TRY_FFMPEG_FIRST_THEN_VLC) : () -> EngineMode.TRY_FFMPEG_FIRST_THEN_VLC;
        ENABLE_FFMPEG = builder.comment("Enable FFmpeg use. This is needed if you want to use the Wave Gate")
                .define("ffmpeg_enabled", true);
        WEB_RESOLUTION_SCALE = builder
                .comment("Scale factor for web images resolution")
                .define("resolution_scale", 8, 1, 32);
        SCALING_MODE = builder
                .comment("Scaling mode for web images")
                .define("scaling_mode", ScalingMode.COVER);
        BILINEAR = builder.comment("Enable bilinear sampling for rescaled images. Enable for a less pixelated look")
                .define("bilinear_sampling", false);
        SAFE_URLS = builder.comment("A list of regex which will filter out valid URLs. At least one of these must match for a URL video to work")
                .define("safe_urls", List.of());
        builder.pop();

        builder.pop();

        builder.onChange(() -> {
            List<String> elements = SAFE_URLS.get();
            if (elements.isEmpty()) {
                safeRegex = Pattern.compile(".*");
            } else {
                String combined = String.join("|", elements);
                safeRegex = Pattern.compile(combined);
            }
        });
        SPEC = builder.build();
        SPEC.forceLoad();
    }

    public static boolean rendersDebug() {
        return RENDER_DEBUG.get() || PlatHelper.isDev();
    }


    public static boolean canUseFFmpeg() {
        return ENABLE_FFMPEG.get();
    }

    public static boolean canUseWatermedia() {
        if (PlatHelper.isDev()) return true;
        return CompatHandler.WATERMEDIA && VIDEO_ENGINE.get() == EngineMode.USE_VLC;
    }

    public static void turnOffFFmpeg() {
        SPEC.manuallySetValue(ENABLE_FFMPEG, false);
    }

    public static boolean isSafeUrl(String input) {
        return safeRegex.matcher(input).find();
    }

    public enum EngineMode {
        TRY_FFMPEG_FIRST_THEN_VLC,
        USE_FFMPEG,
        USE_VLC
    }

    public enum MirrorUpdateMode {
        TEXTURE_REFRESH,
        RENDER_TICK_END
    }
}
