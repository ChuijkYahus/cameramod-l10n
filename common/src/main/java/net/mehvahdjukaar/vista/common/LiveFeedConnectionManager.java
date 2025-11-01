package net.mehvahdjukaar.vista.common;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.moonlight.api.misc.WorldSavedData;
import net.mehvahdjukaar.moonlight.api.misc.WorldSavedDataType;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.core.BlockPos;
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
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unchecked")
public class LiveFeedConnectionManager extends WorldSavedData {

    public static final Codec<LiveFeedConnectionManager> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, GlobalPos.CODEC)
            .xmap(map -> {
                LiveFeedConnectionManager storage = new LiveFeedConnectionManager();
                storage.linkedFeeds.putAll(map);
                return storage;
            }, storage -> storage.linkedFeeds);

    public static final StreamCodec<RegistryFriendlyByteBuf, LiveFeedConnectionManager> STREAM_CODEC =
            (StreamCodec) ByteBufCodecs.map(
                    i -> new HashMap<>(),
                    UUIDUtil.STREAM_CODEC,
                    GlobalPos.STREAM_CODEC
            ).map(map -> {
                LiveFeedConnectionManager storage = new LiveFeedConnectionManager();
                storage.linkedFeeds.putAll(map);
                return storage;
            }, storage -> storage.linkedFeeds);

    private LiveFeedConnectionManager() {
    }

    public static LiveFeedConnectionManager create(ServerLevel serverLevel) {
        return new LiveFeedConnectionManager();
    }

    private final HashMap<UUID, GlobalPos> linkedFeeds = new HashMap<>();

    public void linkFeed(UUID viewFinderUUID, GlobalPos projectorPos) {
        GlobalPos old = linkedFeeds.get(viewFinderUUID);
        if (projectorPos.equals(old)) return;
        linkedFeeds.put(viewFinderUUID, projectorPos);
        this.setDirty();
        this.sync();
    }

    public void unlinkFeed(UUID viewFinderUUID) {
        if (linkedFeeds.remove(viewFinderUUID) != null) {
            this.setDirty();
            this.sync();
        }
    }

    @Nullable
    public GlobalPos getLinkedFeedLocation(UUID viewFinderUUID) {
        return linkedFeeds.get(viewFinderUUID);
    }


    @Override
    public WorldSavedDataType<LiveFeedConnectionManager> getType() {
        return VistaMod.VIEWFINDER_CONNECTION;
    }

    public void validateAll(ServerLevel level) {
        if (!level.isClientSide) {
            //validate all
            AtomicBoolean changed = new AtomicBoolean(false);
            this.linkedFeeds.entrySet().removeIf(e -> {
                GlobalPos pos = e.getValue();
                if (pos.dimension() != level.dimension()) return false;
                if (level.getBlockEntity(pos.pos()) instanceof ViewFinderBlockEntity) {
                    return false;
                } else {
                    changed.set(true);
                    this.setDirty();
                    return true;
                }
            });
            if (changed.get()) {
                this.sync();
            }
        }
    }

    //static helpers

    public static LiveFeedConnectionManager getInstance(Level level) {
        return VistaMod.VIEWFINDER_CONNECTION.getData(level);
    }


    @Nullable
    public static ViewFinderBlockEntity findLinkedViewFinder(Level level, @Nullable UUID viewFinderUUID) {
        if (viewFinderUUID == null) return null;
        LiveFeedConnectionManager connection = getInstance(level);
        if (connection == null) return null;
        GlobalPos gp = connection.getLinkedFeedLocation(viewFinderUUID);
        if (gp != null && gp.dimension() == level.dimension()) {
            BlockPos pos = gp.pos();
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof ViewFinderBlockEntity be) {
                return be;
            }
        }
        return null;
    }
}
