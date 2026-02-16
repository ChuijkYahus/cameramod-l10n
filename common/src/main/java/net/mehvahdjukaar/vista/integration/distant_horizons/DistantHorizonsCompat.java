package net.mehvahdjukaar.vista.integration.distant_horizons;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.config.EDhApiHorizontalQuality;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;

import java.util.function.Supplier;

public class DistantHorizonsCompat {


    private static Supplier<DHMode> dhMode = () -> DHMode.OFF;

    public static Runnable decorateRenderWithoutLOD(Runnable task) {

        return () -> {
            var config = DhApi.Delayed.configs.graphics().renderingEnabled();
            DHMode mode = dhMode.get();
            if (mode == DHMode.OFF) {
                boolean valueBefore = config.getValue();
                config.setValue(false);
                task.run();
                config.setValue(valueBefore);
            } else {
                var newDHConfig = mode.horizontal();
                var config1 = DhApi.Delayed.configs.graphics().horizontalQuality();
                var valueBefore = config1.getValue();
                config1.setValue(newDHConfig);
                task.run();
                config1.setValue(valueBefore);
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

        //low lod on camera!
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
