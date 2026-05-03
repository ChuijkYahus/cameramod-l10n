package net.mehvahdjukaar.vista.common.view_finder;

import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.moonlight.api.util.math.EntityAngles;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class EntityReferenceFrame implements ReferenceFrame {

    private final Entity entity;

    public EntityReferenceFrame(Entity entity) {
        this.entity = entity;
    }

    @Override
    public Vec3 position(float partialTicks) {
        return entity.getPosition(partialTicks);
    }

    @Override
    public Quaternionf getRotation(float partialTicks) {
        return EntityAngles
                .of(entity.getViewXRot(partialTicks), entity.getViewYRot(partialTicks))
                .toQuaternion();
    }

    @Override
    public Vec3 velocity() {
        return entity.getDeltaMovement();
    }

    @Override
    public boolean isStillValid(Player player) {
        return !entity.isRemoved();
    }

    @Override
    public TileOrEntityTarget makeNetworkTarget() {
        return TileOrEntityTarget.of(entity);
    }

}
