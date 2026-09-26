package net.mehvahdjukaar.vista.integration.vampirism.platform;

import de.teamlapen.vampirism.entity.player.VampirismPlayerAttributes;
import net.minecraft.world.entity.player.Player;

public class VampirismCompatImpl {

    public static boolean isVampire(Player player) {
        return VampirismPlayerAttributes.get(player).vampireLevel > 0; //-1 or 0 when human
    }
}
