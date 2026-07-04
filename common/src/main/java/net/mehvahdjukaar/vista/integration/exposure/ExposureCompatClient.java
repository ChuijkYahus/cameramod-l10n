package net.mehvahdjukaar.vista.integration.exposure;

import net.mehvahdjukaar.vista.client.ui.PictureTapeRenderers;
import net.mehvahdjukaar.vista.client.video_source.PictureTapeFrames;

public class ExposureCompatClient {

    public static void init() {
        // draw exposure pictures in the picture tape gallery
        PictureTapeRenderers.register(new ExposurePictureRenderer());
        // and let them play on a TV
        PictureTapeFrames.register(new ExposureFrameTextureProvider());
    }

}
