package net.mehvahdjukaar.camera_vision.configs;

import net.mehvahdjukaar.camera_vision.CameraVision;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.moonlight.api.platform.configs.ModConfigHolder;

public class CommonConfigs {


    public static final ModConfigHolder SPEC;

    static {
        ConfigBuilder builder = ConfigBuilder.create(CameraVision.MOD_ID, ConfigType.COMMON_SYNCED);

        builder.push("lectern");

        builder.pop();

        SPEC = builder.build();
        SPEC.forceLoad();
    }

    public static void init() {

    }

}
