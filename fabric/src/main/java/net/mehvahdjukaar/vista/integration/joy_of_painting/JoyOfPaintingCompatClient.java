package net.mehvahdjukaar.vista.integration.joy_of_painting;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.mehvahdjukaar.vista.client.ui.picture_tape.PictureTapeRenderers;

public class JoyOfPaintingCompatClient {

    public static void init() {
        PictureTapeRenderers.register(new CanvasTapeEntryRenderer());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CanvasTapeTextures.clear());
    }
}
