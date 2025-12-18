package net.mehvahdjukaar.vista.integration.computer_craft;

import dev.architectury.injectables.annotations.ExpectPlatform;
import org.jetbrains.annotations.Contract;

public class CCCompat {

    @Contract
    @ExpectPlatform
    public static void init() {
        throw new AssertionError();
    }
    @Contract
    @ExpectPlatform
    public static void setup() {
        throw new AssertionError();
    }
}
