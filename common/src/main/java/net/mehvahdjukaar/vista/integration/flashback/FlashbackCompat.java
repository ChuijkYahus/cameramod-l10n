package net.mehvahdjukaar.vista.integration.flashback;


import net.mehvahdjukaar.candlelight.api.PlatformImpl;

public class FlashbackCompat {

    @PlatformImpl
    public static Runnable decorateRenderRestoringMatrices(Runnable task) {
        throw new AssertionError();
    }


}
