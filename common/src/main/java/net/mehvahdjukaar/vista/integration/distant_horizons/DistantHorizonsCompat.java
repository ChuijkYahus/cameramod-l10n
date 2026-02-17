package net.mehvahdjukaar.vista.integration.distant_horizons;

import com.seibel.distanthorizons.api.enums.config.EDhApiHorizontalQuality;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;

import java.util.function.Supplier;

public class DistantHorizonsCompat {

    static {
        int aa = 1;
    }


    private static Supplier<DHMode> dhMode = () -> DHMode.OFF;

    public static Runnable decorateRenderWithoutLOD(Runnable task) {
        return new RunnableWithoutLOD(task);
    }

    private record RunnableWithoutLOD(Runnable task) implements Runnable {
        @Override
        public void run() {
            // change start: no-per-frame-dh-toggle
            // Changing DH configs per frame forces DH render cache clears.
            // That causes visible world flicker/reloads while Vista feeds update.
            // Keep feed rendering side-effect free for DH and just run the task.
            task.run();
            // change end: no-per-frame-dh-toggle
        }
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
