package net.mehvahdjukaar.vista.integration.iris;

import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;

public class IrisCompat {
    @PlatformImpl
    public static Runnable decorateRendererWithoutShaderPacks(Runnable runTask) {
        throw new AssertionError();
    }
    @PlatformImpl
    public static void addConfigs(ConfigBuilder builder) {
        throw new AssertionError();
    }
}
