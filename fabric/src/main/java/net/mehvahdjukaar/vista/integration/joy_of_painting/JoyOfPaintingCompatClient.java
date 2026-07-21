package net.mehvahdjukaar.vista.integration.joy_of_painting;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.mehvahdjukaar.vista.client.ui.PictureTapeRenderers;
import net.mehvahdjukaar.vista.client.video_source.PictureTapeFrames;

public class JoyOfPaintingCompatClient {

    public static void init() {
        // draw painted canvases in the picture tape gallery
        PictureTapeRenderers.register(new CanvasTapeEntryRenderer());
        // and let them play on a TV
        PictureTapeFrames.register(new CanvasFrameTextureProvider());
        // free the canvas textures we built when leaving a world
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CanvasTapeTextures.clear());
    }
}
