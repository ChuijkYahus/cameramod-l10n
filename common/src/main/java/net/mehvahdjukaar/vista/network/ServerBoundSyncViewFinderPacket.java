package net.mehvahdjukaar.vista.network;

import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record ServerBoundSyncViewFinderPacket(
        float yaw, float pitch, int zoomLevel, boolean locked, boolean stopControlling,
        TileOrEntityTarget target) implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, ServerBoundSyncViewFinderPacket> CODEC = Message.makeType(
            VistaMod.res("c2s_sync_viewfinder"), ServerBoundSyncViewFinderPacket::new);

    public ServerBoundSyncViewFinderPacket(FriendlyByteBuf buf) {
        this(buf.readFloat(), buf.readFloat(), buf.readVarInt(),
                buf.readBoolean(), buf.readBoolean(), TileOrEntityTarget.read(buf));
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeFloat(this.yaw);
        buf.writeFloat(this.pitch);
        buf.writeVarInt(this.zoomLevel);
        buf.writeBoolean(this.locked);
        buf.writeBoolean(this.stopControlling);
        this.target.write(buf);
    }

    @Override
    public void handle(Context context) {

        // server world
        if (context.getPlayer() instanceof ServerPlayer player) {

            ViewFinderAccess access = ViewFinderAccess.find(player.level(), this.target);
            if (access != null) {
                var cannon = access.getInternalTile();
                if (cannon.isEditingPlayer(BlockPos.containing(access.getCannonGlobalPosition(1)), player)) {
                    cannon.setAttributes(this.yaw, this.pitch, this.zoomLevel, this.locked, player, access);
                    cannon.setChanged();
                    if (stopControlling) {
                        cannon.setPlayerWhoMayEdit(null);
                    }
                    access.updateClients();
                } else {
                    VistaMod.LOGGER.warn("Player tried to control cannon {} without permission: {}", player.getName().getString(), this.target);
                }
            } else {
                VistaMod.LOGGER.warn("Cannon not found for player {}: {}", player.getName().getString(), this.target);
            }
        }
        // could happen if cannon is broken
        //Supplementaries.error(); //should not happen
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return CODEC.type();
    }
}