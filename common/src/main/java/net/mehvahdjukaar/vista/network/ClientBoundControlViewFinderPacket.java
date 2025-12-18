package net.mehvahdjukaar.vista.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.ViewFinderController;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientBoundControlViewFinderPacket(TileOrEntityTarget target) implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, ClientBoundControlViewFinderPacket> CODEC = Message.makeType(
            VistaMod.res("s2c_control_viewfinder"), ClientBoundControlViewFinderPacket::new);


    public ClientBoundControlViewFinderPacket(RegistryFriendlyByteBuf buf) {
        this(TileOrEntityTarget.read(buf));
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        target.write(buf);
    }

    @Override
    public void handle(Context context) {
        // client world
        handleCannonControlPacket(this);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return CODEC.type();
    }

    @Environment(value = EnvType.CLIENT)
    public static void handleCannonControlPacket(ClientBoundControlViewFinderPacket packet) {
        var level = Minecraft.getInstance().level;
        ViewFinderAccess access = ViewFinderAccess.find(level, packet.target());
        if (access != null) {
            ViewFinderController.startControlling(access);
        }
    }
}