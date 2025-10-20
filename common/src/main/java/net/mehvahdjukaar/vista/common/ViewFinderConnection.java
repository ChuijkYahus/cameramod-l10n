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
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class ViewFinderConnection extends WorldSavedData {

    public static final Codec<ViewFinderConnection> CODEC = Codec.unboundedMap(UUIDUtil.CODEC, GlobalPos.CODEC)
            .xmap(map -> {
                ViewFinderConnection storage = new ViewFinderConnection();
                storage.linkedFeeds.putAll(map);
                return storage;
            }, storage -> storage.linkedFeeds);

    public static final StreamCodec<RegistryFriendlyByteBuf, ViewFinderConnection> STREAM_CODEC =
            (StreamCodec) ByteBufCodecs.map(
                    i -> new HashMap<>(),
                    UUIDUtil.STREAM_CODEC,
                    GlobalPos.STREAM_CODEC
            ).map(map -> {
                ViewFinderConnection storage = new ViewFinderConnection();
                storage.linkedFeeds.putAll(map);
                return storage;
            }, storage -> storage.linkedFeeds);

    private ViewFinderConnection() {
    }

    public static ViewFinderConnection create(ServerLevel serverLevel) {
        return new ViewFinderConnection();
    }

    private final HashMap<UUID, GlobalPos> linkedFeeds = new HashMap<>();

    public void linkFeed(UUID viewFinderUUID, GlobalPos projectorPos) {
        GlobalPos old = linkedFeeds.get(viewFinderUUID);
        if (projectorPos.equals(old)) return;
        linkedFeeds.put(viewFinderUUID, projectorPos);
        this.setDirty();
    }

    public void unlinkFeed(UUID viewFinderUUID) {
        if (linkedFeeds.remove(viewFinderUUID) != null) {
            this.setDirty();
        }
    }

    @Nullable
    public GlobalPos getLinkedFeed(UUID viewFinderUUID) {
        return linkedFeeds.get(viewFinderUUID);
    }

    @Override
    public WorldSavedDataType<ViewFinderConnection> getType() {
        return VistaMod.VIEW_FINDER_LOCATOR;
    }

    public static ViewFinderConnection get(Level level) {
        return VistaMod.VIEW_FINDER_LOCATOR.getData(level);
    }
}
