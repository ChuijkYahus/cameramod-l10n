package net.mehvahdjukaar.vista.configs;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.moonlight.api.platform.configs.ModConfigHolder;

import java.util.function.Supplier;

public class ClientConfigs {

    public static void init() {
    }

    public static final ModConfigHolder SPEC;

    public static final Supplier<Integer> RENDER_DISTANCE;

    static {
        ConfigBuilder builder = ConfigBuilder.create(VistaMod.MOD_ID, ConfigType.CLIENT);

        builder.push("television");

        RENDER_DISTANCE = builder
                .comment("Render distance that television will use when rendering the live feed. Decreasing this will improve the performance of TVs, possibly by a lot")
                .define("live_feed_render_distance", 64, 1, 256);

        builder.pop();

        SPEC = builder.build();
        SPEC.forceLoad();
    }
}
