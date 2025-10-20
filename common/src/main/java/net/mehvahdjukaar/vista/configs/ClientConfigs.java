package net.mehvahdjukaar.vista.configs;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.moonlight.api.platform.configs.ModConfigHolder;

public class ClientConfigs {

    public static void init() {
    }

    public static final ModConfigHolder SPEC;

    static {
        ConfigBuilder builder = ConfigBuilder.create(VistaMod.MOD_ID, ConfigType.CLIENT);

        builder.push("general");

        builder.pop();

        SPEC = builder.build();
        SPEC.forceLoad();
    }
}
