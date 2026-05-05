package net.mehvahdjukaar.vista.network;

import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.Optional;
import java.util.UUID;

public record SyncViewFinderPacket(
        Quaternionf localRot, int zoomLevel, boolean locked, boolean stopControlling,
        TileOrEntityTarget target, @Nullable UUID userEntityId) implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, SyncViewFinderPacket> CODEC = Message.makeType(
            VistaMod.res("sync_view_finder"), SyncViewFinderPacket::new);

    public SyncViewFinderPacket(FriendlyByteBuf buf) {
        this(ByteBufCodecs.QUATERNIONF.decode(buf), buf.readVarInt(),
                buf.readBoolean(), buf.readBoolean(), TileOrEntityTarget.read(buf),
                buf.readOptional(buffer -> buffer.readUUID()).orElse(null));
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        ByteBufCodecs.QUATERNIONF.encode(buf, localRot);
        buf.writeVarInt(this.zoomLevel);
        buf.writeBoolean(this.locked);
        buf.writeBoolean(this.stopControlling);
        this.target.write(buf);
        buf.writeOptional(Optional.ofNullable(this.userEntityId), (buffer, value) ->
                buffer.writeUUID(value));
    }

    @Override
    public void handle(Context context) {

        Level level = context.getPlayer().level();


        BlockEntity be = this.target.findTileOrContainedTile(level);
        if (!(be instanceof ViewFinderBlockEntity viewFinder)) {
            VistaMod.LOGGER.warn("Cannon not found: {}", this.target);
            return;
        }
        //trusted
        if (level.isClientSide) {
            viewFinder.setTrustedInternalAttributes(this.localRot, this.zoomLevel, this.locked);
            if (stopControlling) {
                viewFinder.setCurrentUser(null);
            }
        } else if (level instanceof ServerLevel sl) {
            Entity entity = null;

            if (this.userEntityId != null) {
                entity = sl.getEntity(userEntityId);

                if (entity == null) {
                    VistaMod.LOGGER.error("Failed to find entity with id {} for cannon controlling", userEntityId);
                    return;
                }
            }

            if (entity == null || viewFinder.canBeUsedBy(BlockPos.containing(viewFinder.getGlobalPosition(1)), entity)) {
                viewFinder.setTrustedInternalAttributes(this.localRot, this.zoomLevel, this.locked);
                viewFinder.setChanged();
                if (stopControlling) {
                    viewFinder.setCurrentUser(null);
                }
                viewFinder.syncToClients();
            } else {
                VistaMod.LOGGER.warn("Entity {} tried to control cannon {} without permission", entity, this.target);
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return CODEC.type();
    }
}