package net.mehvahdjukaar.vista.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.ICameraChunkTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public record ServerBoundCameraChunkRequestPacket(
        BlockPos cameraPos, boolean register) implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, ServerBoundCameraChunkRequestPacket> CODEC = Message.makeType(
            VistaMod.res("c2s_camera_chunk_request"), ServerBoundCameraChunkRequestPacket::new);

    public ServerBoundCameraChunkRequestPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readBoolean());
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(this.cameraPos);
        buf.writeBoolean(this.register);
    }

    @Override
    public void handle(Context context) {
        if (context.getPlayer() instanceof ServerPlayer player && player instanceof ICameraChunkTracker tracker) {
            if (register) {
                tracker.vista$addCameraPosition(cameraPos);
            } else {
                tracker.vista$removeCameraPosition(cameraPos);
            }

            ServerLevel level = (ServerLevel) player.level();
            level.getChunkSource().chunkMap.move(player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return CODEC.type();
    }
}
