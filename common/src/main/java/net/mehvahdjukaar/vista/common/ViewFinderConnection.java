package net.mehvahdjukaar.vista.common;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.moonlight.api.misc.WorldSavedData;
import net.mehvahdjukaar.moonlight.api.misc.WorldSavedDataType;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class ViewFinderConnection extends WorldSavedData {

    public static final Codec<ViewFinderConnection> CODEC = Codec.unboundedMap(UUIDUtil.CODEC, GlobalPos.CODEC)
            .xmap(map -> {
                ViewFinderConnection storage = new ViewFinderConnection();
                storage.projectorMap.putAll(map);
                return storage;
            }, storage -> storage.projectorMap);

    public static final StreamCodec<RegistryFriendlyByteBuf, ViewFinderConnection> STREAM_CODEC =
            (StreamCodec) ByteBufCodecs.map(
                    i -> new HashMap<>(),
                    UUIDUtil.STREAM_CODEC,
                    GlobalPos.STREAM_CODEC
            ).map(map -> {
                ViewFinderConnection storage = new ViewFinderConnection();
                storage.projectorMap.putAll(map);
                return storage;
            }, storage -> storage.projectorMap);

    private ViewFinderConnection() {
    }

    public static ViewFinderConnection create(ServerLevel serverLevel) {
        return new ViewFinderConnection();
    }

    private final HashMap<UUID, GlobalPos> projectorMap = new HashMap<>();

    @Override
    public WorldSavedDataType<ViewFinderConnection> getType() {
        return VistaMod.VIEW_FINDER_LOCATOR;
    }
}
