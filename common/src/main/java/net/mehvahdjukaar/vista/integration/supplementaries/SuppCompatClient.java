package net.mehvahdjukaar.vista.integration.supplementaries;

import net.mehvahdjukaar.vista.client.ui.picture_tape.PictureTapeRenderers;

public class SuppCompatClient {

    public static void init() {
        PictureTapeRenderers.register(new BlackboardTapeEntryRenderer());
    }
}
