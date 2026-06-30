package net.mehvahdjukaar.vista.integration.vampirism.platform;

import net.minecraft.world.entity.player.Player;

public class VampirismCompatImpl {

    public static boolean isVampire(Player player) {
        // Vampirism is NeoForge-only, so there is nothing to check on Fabric.
        return false;
    }
}
