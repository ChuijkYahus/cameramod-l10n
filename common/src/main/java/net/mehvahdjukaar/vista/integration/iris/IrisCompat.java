package net.mehvahdjukaar.vista.integration.iris;

import net.irisshaders.iris.shadows.ShadowRenderer;

public class IrisCompat {

    public static Runnable decorateRendererWithoutShadows(Runnable renderTask) {
        return ()-> {
            boolean old = ShadowRenderer.ACTIVE;
            ShadowRenderer.ACTIVE = false;
            renderTask.run();
            ShadowRenderer.ACTIVE = old;
        };
    }
}
