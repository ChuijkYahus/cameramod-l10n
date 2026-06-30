package net.mehvahdjukaar.vista.integration.supernatural;

import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Contract;

/**
 * Detects whether a <i>living entity</i> is a vampire through the Supernatural mod (Forge/NeoForge
 * only), so its vampires can be hidden from mirrors and camera/TV feeds.
 *
 * <p>Supernatural's check is a two-for-one: a single query covers both vampire <i>mobs</i> (tagged)
 * and vampire <i>players</i> (flagged through persistent NBT), so it accepts any {@code LivingEntity}
 * and there's no need to maintain the {@code cant_see_through_mirror} / {@code cant_see_through_tv}
 * tags for its mobs.
 */
public class SupernaturalCompat {

    /**
     * @return true if {@code entity} is currently a vampire according to Supernatural.
     * Always false when Supernatural is not present.
     */
    @Contract
    @PlatformImpl
    public static boolean isVampire(LivingEntity entity) {
        throw new AssertionError();
    }
}
