package net.mehvahdjukaar.vista.common.broadcast;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.cassette.IBroadcastSource;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

public interface IBroadcastLocation {

    Codec<IBroadcastLocation> CODEC = Codec.lazyInitialized(
            () -> VistaMod.BROADCAST_LOCATION_REGISTRY.byNameCodec()
                    .dispatch(IBroadcastLocation::type, BroadcastLocationType::codec));


    StreamCodec<RegistryFriendlyByteBuf, IBroadcastLocation> STREAM_CODEC =
            ByteBufCodecs.registry(VistaMod.BROADCAST_LOCATION_REGISTRY.key())
                    .dispatch(IBroadcastLocation::type, BroadcastLocationType::streamCodec);

    TriResult<IBroadcastSource> get(boolean isClient);

    BroadcastLocationType type();

    MutableComponent getTooltipComponent(Level level);
}
