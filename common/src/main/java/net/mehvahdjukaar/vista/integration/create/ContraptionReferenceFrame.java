package net.mehvahdjukaar.vista.integration.create;

import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.vista.common.view_finder.ReferenceFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public record ContraptionReferenceFrame(Entity contraption, BlockPos localPos) implements ReferenceFrame {

    @Override
    public Vec3 position(float partialTicks) {
        return CreateCompat.contraptionPosToGlobalPos(contraption, Vec3.atCenterOf(localPos), partialTicks);
    }

    @Override
    public Quaternionf getRotation(float partialTicks) {
        return CreateCompat.getContraptionRotation(contraption, partialTicks);
    }

    @Override
    public Vec3 velocity() {
        return CreateCompat.getContactPointMotion(contraption, position(1));
    }

    @Override
    public TileOrEntityTarget makeNetworkTarget() {
        return TileOrEntityTarget.of(contraption);
    }

    @Override
    public boolean isStillValid(Player player) {
        return !contraption.isRemoved();
    }

    @Override
    public void syncToServer(Quaternionf localRot, int zoom, boolean locked, boolean removeOwner, Player player) {
        NetworkHelper.sendToServer(new SyncContraptionViewFinderPacket(
                contraption.getUUID(), localPos, localRot, zoom, locked, removeOwner));
    }

    @Override
    public void syncToCLients(ServerLevel level, Quaternionf localRot, int zoom, boolean locked) {
        // contraptions have no server-side block entity, so nothing calls this; the server relay happens inside
        // SyncContraptionViewFinderPacket when it receives the client's aim change.
    }
}
