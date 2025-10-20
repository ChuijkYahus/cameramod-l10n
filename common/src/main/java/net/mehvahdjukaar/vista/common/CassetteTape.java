package net.mehvahdjukaar.vista.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.moonlight.api.util.math.ColorUtils;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceLocation;

public record CassetteTape(ResourceLocation assetId, int color) {

    public static final Codec<CassetteTape> DIRECT_CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            ResourceLocation.CODEC.fieldOf("asset_id").forGetter(CassetteTape::assetId),
            ColorUtils.CODEC.fieldOf("color").forGetter(CassetteTape::color)
    ).apply(instance, CassetteTape::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CassetteTape> DIRECT_STREAM_CODEC =
            StreamCodec.composite(ResourceLocation.STREAM_CODEC, CassetteTape::assetId,
                    ByteBufCodecs.VAR_INT, CassetteTape::color, CassetteTape::new);


    public static final Codec<Holder<CassetteTape>> CODEC = RegistryFileCodec.create(VistaMod.CASSETTE_TAPE_REGISTRY_KEY, DIRECT_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<CassetteTape>> STREAM_CODEC = ByteBufCodecs.holder(
            VistaMod.CASSETTE_TAPE_REGISTRY_KEY, DIRECT_STREAM_CODEC);
}
