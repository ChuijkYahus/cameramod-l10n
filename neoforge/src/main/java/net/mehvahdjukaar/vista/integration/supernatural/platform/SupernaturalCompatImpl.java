package net.mehvahdjukaar.vista.integration.supernatural.platform;

import net.minecraft.world.entity.LivingEntity;
import net.salju.supernatural.events.SupernaturalManager;

public class SupernaturalCompatImpl {

    // Callers must gate this behind CompatHandler.SUPERNATURAL: this class references Supernatural
    // types directly, so it must never be loaded when the mod is absent (no internal mod-loaded guard).
    public static boolean isVampire(LivingEntity entity) {
        // Two-for-one: covers tagged vampire mobs and NBT-flagged vampire players in a single call.
        return SupernaturalManager.isVampire(entity);
    }
}
