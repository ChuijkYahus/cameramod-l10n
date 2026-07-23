package net.mehvahdjukaar.vista.integration.exposure;

import net.mehvahdjukaar.vista.client.ui.PictureTapeRenderers;

public class ExposureCompatClient {

    public static void init() {
        // draw exposure pictures in the picture tape gallery, and let them play on a TV
        PictureTapeRenderers.register(new ExposurePictureRenderer());
    }

}
