package net.mehvahdjukaar.vista.integration.distant_horizons;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.config.EDhApiHorizontalQuality;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;

import java.util.function.Supplier;

public class DistantHorizonsCompat {

    private static Supplier<DHMode> dhMode = () -> DHMode.OFF;

    public static Runnable decorateRenderWithoutLOD(Runnable task) {
        return () -> {
            // Per DH dev advice these configs are set-and-forget rather than reverted every frame:
            //  - horizontalQuality(): changing it forces DH to rebuild the entire LOD dataset (several
            //    seconds), so we only write it when the desired value actually differs.
            //  - useCameraPositionForQualityDropOff(): DH uses the camera position for LOD quality
            //    drop-off; with multiple cameras (TVs) that produces stutters and inaccurate LODs, so we
            //    fall back to the legacy player-position behavior. It only matters during rendering, so
            //    leaving it set causes no issues.
            IDhApiConfigValue<Boolean> cameraPositionConfig = DhApi.Delayed.configs.graphics().useCameraPositionForQualityDropOff();
            if (Boolean.TRUE.equals(cameraPositionConfig.getValue())) {
                cameraPositionConfig.setValue(false);
            }

            DHMode mode = dhMode.get();
            if (mode != DHMode.OFF) {
                var qualityConfig = DhApi.Delayed.configs.graphics().horizontalQuality();
                EDhApiHorizontalQuality wanted = mode.horizontal();
                if (qualityConfig.getValue() != wanted) {
                    qualityConfig.setValue(wanted);
                }
            }

            task.run();
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
