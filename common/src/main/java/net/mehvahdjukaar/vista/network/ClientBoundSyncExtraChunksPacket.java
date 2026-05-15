package net.mehvahdjukaar.vista.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.ExtraChunkViewData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → Client: replaces the client's {@link ExtraChunkViewData#CLIENT_INSTANCE}
 * with the player's current server-side zone set and triggers a ViewArea rebuild.
 */
public record ClientBoundSyncExtraChunksPacket(ExtraChunkViewData data) implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, ClientBoundSyncExtraChunksPacket> CODEC =
            Message.makeType(VistaMod.res("s2c_sync_extra_chunks"), ClientBoundSyncExtraChunksPacket::new);

    public ClientBoundSyncExtraChunksPacket(RegistryFriendlyByteBuf buf) {
        this(ExtraChunkViewData.STREAM_CODEC.decode(buf));
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        ExtraChunkViewData.STREAM_CODEC.encode(buf, this.data);
    }

    @Override
    public void handle(Context context) {
        ExtraChunkViewData client = ExtraChunkViewData.CLIENT_INSTANCE;
        client.clearZones();
        for (ExtraChunkViewData.Zone zone : this.data.getZones()) {
            client.addZone(zone.center(), zone.radius());
        }
        net.mehvahdjukaar.vista.VistaMod.LOGGER.info(
                "[Vista/Chunks] Client received zone sync: {} zones, {} total chunks",
                client.getZones().size(), client.getAllChunks().size());
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return CODEC.type();
    }
}
