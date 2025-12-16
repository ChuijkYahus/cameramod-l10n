package net.mehvahdjukaar.vista.integration.distant_horizons;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.config.EDhApiHorizontalQuality;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;

import java.util.function.Supplier;

public class DistantHorizonsCompat {

    private static Supplier<DHMode> dhMode = () -> DHMode.OFF;

    public static Runnable renderWithoutLOD(Runnable task) {
        return () -> {
            DHMode mode = dhMode.get();
            if (mode == DHMode.OFF) {
                IDhApiConfigValue<Boolean> config = DhApi.Delayed.configs.graphics().renderingEnabled();
                boolean valueBefore = config.getValue();
                config.setValue(false);
                task.run();
                config.setValue(valueBefore);
            } else {
                EDhApiHorizontalQuality newDHConfig = mode.horizontal();
                var config = DhApi.Delayed.configs.graphics().horizontalQuality();
                var valueBefore = config.getValue();
                config.setValue(newDHConfig);
                task.run();
                config.setValue(valueBefore);
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
