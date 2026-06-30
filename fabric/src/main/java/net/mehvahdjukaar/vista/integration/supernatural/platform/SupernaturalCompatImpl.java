package net.mehvahdjukaar.vista.integration.supernatural.platform;

import net.minecraft.world.entity.LivingEntity;

public class SupernaturalCompatImpl {

    public static boolean isVampire(LivingEntity entity) {
        // Supernatural is Forge/NeoForge-only, so there is nothing to check on Fabric.
        return false;
    }
}
