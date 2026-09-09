package net.mehvahdjukaar.vista.integration.sable;

import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class SableCompatClient {

    public static boolean isOnSubLevel(BlockEntity be) {
        return SableCompanion.INSTANCE.getContainingClient(be) != null;
    }

    public static Vec3 projectIntoSubLevel(BlockEntity be, Vec3 worldPos, float partialTicks) {
        ClientSubLevelAccess subLevel = SableCompanion.INSTANCE.getContainingClient(be);
        return subLevel == null ? worldPos : subLevel.renderPose(partialTicks).transformPositionInverse(worldPos);
    }

    public static Vec3 projectIntoSubLevel(BlockEntity be, Vec3 worldPos) {
        ClientSubLevelAccess subLevel = SableCompanion.INSTANCE.getContainingClient(be);
        return subLevel == null ? worldPos : subLevel.renderPose().transformPositionInverse(worldPos);
    }
}
