package net.mehvahdjukaar.vista.integration.vampirism.platform;

import de.teamlapen.vampirism.entity.player.VampirismPlayerAttributes;
import net.minecraft.world.entity.player.Player;

public class VampirismCompatImpl {

    // Callers must gate this behind CompatHandler.VAMPIRISM: this class references Vampirism types
    // directly, so it must never be loaded when the mod is absent (no internal mod-loaded guard).
    public static boolean isVampire(Player player) {
        // vampireLevel is > 0 once the player has actually turned (0 / -1 while still human).
        return VampirismPlayerAttributes.get(player).vampireLevel > 0;
    }
}
