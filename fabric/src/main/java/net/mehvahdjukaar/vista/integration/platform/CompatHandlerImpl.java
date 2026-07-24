package net.mehvahdjukaar.vista.integration.platform;

import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.mehvahdjukaar.vista.integration.joy_of_painting.JoyOfPaintingCompat;

public class CompatHandlerImpl {
    public static void initPlat() {
        if (CompatHandler.JOY_OF_PAINTING) JoyOfPaintingCompat.init();
    }

    public static void setupPlat() {
    }
}
