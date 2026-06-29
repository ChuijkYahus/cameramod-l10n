package net.mehvahdjukaar.vista.integration.vampire.platform;

import net.minecraft.world.entity.player.Player;

public class VampireCompatImpl {

    public static boolean isVampire(Player player) {
        // No vampire mod with player factions is available on Fabric for our loaders/version:
        // Vampirism is NeoForge-only and Origins: Vampire has no 1.21.1 build. Nothing to check yet.
        return false;
    }
}
