package net.mehvahdjukaar.vista.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.ExtraChunkViewData;
import net.mehvahdjukaar.vista.mixins.accessor.ChunkMapAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public record ServerBoundCameraChunkRequestPacket(
        BlockPos cameraPos, boolean register, byte radius) implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, ServerBoundCameraChunkRequestPacket> CODEC = Message.makeType(
            VistaMod.res("c2s_camera_chunk_request"), ServerBoundCameraChunkRequestPacket::new);

    public ServerBoundCameraChunkRequestPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readBoolean(), buf.readByte());
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(this.cameraPos);
        buf.writeBoolean(this.register);
        buf.writeByte(this.radius);
    }

    @Override
    public void handle(Context context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        ExtraChunkViewData data = VistaMod.TRACKED_CAMERAS_ATTACH.getOrCreate(player);
        ChunkPos cameraChunk = new ChunkPos(cameraPos);

        if (register) {
            data.addZone(cameraChunk, radius);
            sendCameraZoneChunks(player, data, cameraChunk);
        } else {
            data.removeZone(cameraChunk);
        }

        // Sync updated zone list back to client so it can rebuild its ViewArea
        NetworkHelper.sendToClientPlayer(player, new ClientBoundSyncExtraChunksPacket(data));
    }

    /** Force-queue all chunks in the newly added zone to be sent to the player. */
    private static void sendCameraZoneChunks(ServerPlayer player, ExtraChunkViewData data, ChunkPos center) {
        ServerLevel level = (ServerLevel) player.level();
        ChunkMapAccessor chunkMap = (ChunkMapAccessor) level.getChunkSource().chunkMap;

        // Iterate only the zone we just added (last element)
        ExtraChunkViewData.Zone zone = data.getZones().getLast();
        for (ChunkPos pos : zone.chunks()) {
            chunkMap.vista$markChunkPendingToSend(player, pos);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return CODEC.type();
    }
}
