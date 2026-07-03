package net.mehvahdjukaar.vista.integration.supernatural;

import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Contract;


public class SupernaturalCompat {

    @Contract
    @PlatformImpl
    public static boolean isVampire(LivingEntity entity) {
        throw new AssertionError();
    }
}
