package net.mehvahdjukaar.vista.integration.computer_craft;

import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import org.jetbrains.annotations.Contract;

public class CCCompat {

    @Contract
    @PlatformImpl
    public static void init() {
        throw new AssertionError();
    }

    @Contract
    @PlatformImpl
    public static void setup() {
        throw new AssertionError();
    }


}
