package net.mehvahdjukaar.vista.integration.exposure;

import net.mehvahdjukaar.vista.client.ui.picture_tape.PictureTapeRenderers;

public class ExposureCompatClient {

    public static void init() {
        PictureTapeRenderers.register(new ExposurePictureRenderer());
    }

}
