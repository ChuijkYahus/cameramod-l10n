package net.mehvahdjukaar.vista.integration.vampirism;

import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;

/**
 * Detects whether a <i>player</i> is a vampire through the Vampirism mod (NeoForge only), so vampire
 * players can be hidden from mirrors and camera/TV feeds.
 *
 * <p>A player turned vampire is still {@code minecraft:player}, so the {@code cant_see_through_mirror}
 * / {@code cant_see_through_tv} entity-type tags can't match it — this compat layer fills that gap.
 * Vampirism has no notion of vampire mobs, so only players are handled here.
 */
public class VampirismCompat {

    /**
     * @return true if {@code player} is currently a vampire according to Vampirism.
     * Always false when Vampirism is not present.
     */
    @Contract
    @PlatformImpl
    public static boolean isVampire(Player player) {
        throw new AssertionError();
    }
}
