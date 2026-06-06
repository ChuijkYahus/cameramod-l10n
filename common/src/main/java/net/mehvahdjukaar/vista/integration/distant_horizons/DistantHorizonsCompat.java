package net.mehvahdjukaar.vista.integration.distant_horizons;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.config.EDhApiHorizontalQuality;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue;
import com.seibel.distanthorizons.api.interfaces.config.client.IDhApiGraphicsConfig;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;

import java.lang.reflect.Method;
import java.util.function.Supplier;

public class DistantHorizonsCompat {

    private static Supplier<DHMode> dhMode = () -> DHMode.OFF;

    // Only present on DH nightly builds; resolve reflectively so we stay compatible with released DH.
    private static final Method USE_CAMERA_POSITION_METHOD;

    static {
        Method m = null;
        try {
            m = IDhApiGraphicsConfig.class.getMethod("useCameraPositionForQualityDropOff");
        } catch (NoSuchMethodException ignored) {
        }
        USE_CAMERA_POSITION_METHOD = m;
    }

    @SuppressWarnings("unchecked")
    private static IDhApiConfigValue<Boolean> getCameraPositionConfig() {
        if (USE_CAMERA_POSITION_METHOD == null) return null;
        try {
            return (IDhApiConfigValue<Boolean>) USE_CAMERA_POSITION_METHOD.invoke(DhApi.Delayed.configs.graphics());
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public static Runnable decorateRenderWithoutLOD(Runnable task) {
        return () -> {
            IDhApiConfigValue<Boolean> cameraPositionConfig = getCameraPositionConfig();
            Boolean cameraPositionBefore = cameraPositionConfig != null ? cameraPositionConfig.getValue() : null;

            DHMode mode = dhMode.get();

            try {
                if (cameraPositionConfig != null) cameraPositionConfig.setValue(false);

                if (mode != DHMode.OFF) {
                    var qualityConfig = DhApi.Delayed.configs.graphics().horizontalQuality();
                    var qualityBefore = qualityConfig.getValue();
                    try {
                        qualityConfig.setValue(mode.horizontal());
                        task.run();
                    } finally {
                        qualityConfig.setValue(qualityBefore);
                    }
                } else {
                    task.run();
                }
            } finally {
                if (cameraPositionConfig != null) cameraPositionConfig.setValue(cameraPositionBefore);
            }
        };
    }

    public static void addConfigs(ConfigBuilder builder) {
        dhMode = builder
                .comment("Distant Horizons compatibility lod render quality")
                .define("distant_horizons_LOD", DHMode.OFF);
    }

    private enum DHMode {
        OFF,
        LOW,
        MED,
        HIGH;

        public EDhApiHorizontalQuality horizontal() {
            return switch (this) {
                case OFF -> EDhApiHorizontalQuality.LOWEST;
                case LOW -> EDhApiHorizontalQuality.LOWEST;
                case MED -> EDhApiHorizontalQuality.LOW;
                case HIGH -> EDhApiHorizontalQuality.MEDIUM;
            };
        }
    }
}
