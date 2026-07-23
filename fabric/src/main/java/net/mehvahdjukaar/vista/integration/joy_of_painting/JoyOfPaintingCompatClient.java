package net.mehvahdjukaar.vista.integration.joy_of_painting;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.mehvahdjukaar.vista.client.ui.PictureTapeRenderers;

public class JoyOfPaintingCompatClient {

    public static void init() {
        // draw painted canvases in the picture tape gallery, and let them play on a TV
        PictureTapeRenderers.register(new CanvasTapeEntryRenderer());
        // free the canvas textures we built when leaving a world
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CanvasTapeTextures.clear());
    }
}
