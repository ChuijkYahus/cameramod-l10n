package net.mehvahdjukaar.vista.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record ServerBoundWantsZoneChunksPacket(boolean wantsZoneChunks) implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, ServerBoundWantsZoneChunksPacket> CODEC =
            Message.makeType(VistaMod.res("c2s_wants_zone_chunks"), ServerBoundWantsZoneChunksPacket::new);

    public ServerBoundWantsZoneChunksPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(this.wantsZoneChunks);
    }

    @Override
    public void handle(Context context) {
        if (context.getPlayer() instanceof ServerPlayer sender) {
            VistaMod.EXTRA_VIEW_AREAS.getOrCreate(sender).setClientWantsZoneChunks(this.wantsZoneChunks);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return CODEC.type();
    }
}
