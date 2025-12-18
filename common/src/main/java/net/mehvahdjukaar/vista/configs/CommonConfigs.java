package net.mehvahdjukaar.vista.configs;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.moonlight.api.platform.configs.ModConfigHolder;
import net.mehvahdjukaar.vista.VistaMod;

import java.util.function.Supplier;

public class CommonConfigs {


    public static final ModConfigHolder SPEC;

    public static final Supplier<Integer> MAX_CONNECTED_TV_SIZE;

    static {
        ConfigBuilder builder = ConfigBuilder.create(VistaMod.MOD_ID, ConfigType.COMMON_SYNCED);

        builder.push("general");
        MAX_CONNECTED_TV_SIZE = builder
                .comment("Maximum size of connected TVs (in blocks). Set to 1 to disable multi-block TVs.")
                .define("max_connected_tv_size", 8, 1, 32);

        builder.pop();

        SPEC = builder.build();
        SPEC.forceLoad();
    }

    public static void init() {

    }

}
