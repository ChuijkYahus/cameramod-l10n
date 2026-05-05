package net.mehvahdjukaar.vista.network;

import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.ViewFinderController;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

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
        Level level = context.getPlayer().level();
        BlockEntity be = this.target().findTileOrContainedTile(level);
        if (be instanceof ViewFinderBlockEntity vf) {
            ViewFinderController.startControlling(vf);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return CODEC.type();
    }


}