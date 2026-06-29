package net.mehvahdjukaar.vista.integration.vampire.platform;

import de.teamlapen.vampirism.entity.player.VampirismPlayerAttributes;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.minecraft.world.entity.player.Player;

public class VampireCompatImpl {

    public static boolean isVampire(Player player) {
        // Guard before touching any Vampirism class so this stays classload-safe when the mod is absent.
        if (!CompatHandler.VAMPIRISM) return false;
        // vampireLevel is > 0 once the player has actually turned (0 / -1 while still human).
        return VampirismPlayerAttributes.get(player).vampireLevel > 0;
    }
}
