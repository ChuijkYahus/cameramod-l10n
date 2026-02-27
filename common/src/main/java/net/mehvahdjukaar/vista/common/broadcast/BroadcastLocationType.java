package net.mehvahdjukaar.vista.common.broadcast;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record BroadcastLocationType(MapCodec<? extends IBroadcastLocation> codec,
                                    StreamCodec<ByteBuf, ? extends IBroadcastLocation> streamCodec) {
}
