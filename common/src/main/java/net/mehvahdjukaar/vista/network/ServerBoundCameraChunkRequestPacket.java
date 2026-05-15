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

        ExtraChunkViewData data = VistaMod.EXTRA_VIEW_AREAS.getOrCreate(player);
        ChunkPos cameraChunk = new ChunkPos(cameraPos);

        VistaMod.LOGGER.info("[Vista/Chunks] Received {} request for camera at {} (chunk {}) radius={}",
                register ? "REGISTER" : "UNREGISTER", cameraPos, cameraChunk, radius);

        if (register) {
            data.addZone(cameraChunk, radius);
            sendCameraZoneChunks(player, data, cameraChunk);
        } else {
            data.removeZone(cameraChunk);
        }

        NetworkHelper.sendToClientPlayer(player, new ClientBoundSyncExtraChunksPacket(data));
    }

    private static void sendCameraZoneChunks(ServerPlayer player, ExtraChunkViewData data, ChunkPos center) {
        ServerLevel level = (ServerLevel) player.level();
        ChunkMapAccessor chunkMap = (ChunkMapAccessor) level.getChunkSource().chunkMap;

        ExtraChunkViewData.Zone zone = data.getZones().getLast();
        int sent = 0, missing = 0;
        for (ChunkPos pos : zone.chunks()) {
            boolean loaded = chunkMap.vista$getChunkToSend(pos.toLong()) != null;
            if (loaded) {
                chunkMap.vista$markChunkPendingToSend(player, pos);
                data.markZoneChunkQueued(pos);
                sent++;
            } else {
                missing++;
            }
        }
        VistaMod.LOGGER.info("[Vista/Chunks] Zone around {}: queued={} not-loaded={}", center, sent, missing);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return CODEC.type();
    }
}
