package net.mehvahdjukaar.vista.integration.computer_craft;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class CCCompat {

    @ExpectPlatform
    public static void init() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setup() {
        throw new AssertionError();
    }
}
