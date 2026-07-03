package net.mehvahdjukaar.vista.integration.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.vista.common.view_finder.ReferenceFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Reference frame for a view finder sitting inside a Create contraption (train, cart, gantry, bearing...).
 * All world-space queries are delegated to the contraption entity's transform so the view finder tracks
 * the structure as it moves and rotates.
 *
 * @param localPos position of the block within the contraption's local grid
 */
public record ContraptionReferenceFrame(AbstractContraptionEntity contraption,
                                        BlockPos localPos) implements ReferenceFrame {

    @Override
    public Vec3 position(float partialTicks) {
        return contraption.toGlobalVector(Vec3.atCenterOf(localPos), partialTicks);
    }

    @Override
    public Quaternionf getRotation(float partialTicks) {
        // derive the contraption's world rotation by transforming the three basis vectors.
        // convention-agnostic: works for any contraption type without matching Create's euler order.
        Vec3 x = contraption.applyRotation(new Vec3(1, 0, 0), partialTicks);
        Vec3 y = contraption.applyRotation(new Vec3(0, 1, 0), partialTicks);
        Vec3 z = contraption.applyRotation(new Vec3(0, 0, 1), partialTicks);
        Matrix3f rot = new Matrix3f(
                new Vector3f((float) x.x, (float) x.y, (float) x.z),
                new Vector3f((float) y.x, (float) y.y, (float) y.z),
                new Vector3f((float) z.x, (float) z.y, (float) z.z));
        return new Quaternionf().setFromNormalized(rot);
    }

    @Override
    public Vec3 velocity() {
        return contraption.getContactPointMotion(position(1));
    }

    @Override
    public TileOrEntityTarget makeNetworkTarget() {
        return TileOrEntityTarget.of(contraption);
    }

    @Override
    public boolean isStillValid(Player player) {
        return !contraption.isRemoved();
    }
}
