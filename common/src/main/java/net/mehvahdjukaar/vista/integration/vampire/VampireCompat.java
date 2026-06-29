package net.mehvahdjukaar.vista.integration.vampire;

import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;

/**
 * Detects whether a <i>player</i> is a vampire through supported vampire mods, so vampire players
 * can be hidden from mirrors and camera/TV feeds the same way vampire mobs are.
 *
 * <p>Vampire <i>mobs</i> are handled purely by the {@code cant_see_through_mirror} /
 * {@code cant_see_through_tv} entity-type tags. A player turned vampire is still
 * {@code minecraft:player}, so those tags can't match it — this compat layer fills that gap.
 *
 * <p>Currently backed by Vampirism (NeoForge only). Origins: Vampire has no 1.21.1 release for our
 * loaders yet; when one ships, its detection can be added to the platform impls without touching
 * callers.
 */
public class VampireCompat {

    /**
     * @return true if {@code player} is currently a vampire according to any installed vampire mod.
     * Always false when no supported vampire mod is present.
     */
    @Contract
    @PlatformImpl
    public static boolean isVampire(Player player) {
        throw new AssertionError();
    }
}
