package net.mehvahdjukaar.vista.integration.supernatural.platform;

import net.minecraft.world.entity.LivingEntity;
import net.salju.supernatural.events.SupernaturalManager;

public class SupernaturalCompatImpl {

    public static boolean isVampire(LivingEntity entity) {
        return SupernaturalManager.isVampire(entity);
    }
}
