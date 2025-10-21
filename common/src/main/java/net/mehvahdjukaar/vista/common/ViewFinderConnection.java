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
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unchecked")
public class ViewFinderConnection extends WorldSavedData {

    public static final Codec<ViewFinderConnection> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, GlobalPos.CODEC)
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

    @Nullable
    public ViewFinderBlockEntity getLinkedViewFinder(Level level, UUID viewFinderUUID) {
        GlobalPos pos = linkedFeeds.get(viewFinderUUID);

        if (pos != null && pos.dimension() == level.dimension()) {
            if (level.getBlockEntity(pos.pos()) instanceof ViewFinderBlockEntity be) {
                return be;
            }
        }
        return null;
    }

    @Override
    public WorldSavedDataType<ViewFinderConnection> getType() {
        return VistaMod.VIEWFINDER_CONNECTION;
    }

    public static ViewFinderConnection get(Level level) {
        ViewFinderConnection connection = VistaMod.VIEWFINDER_CONNECTION.getData(level);

        return connection;
    }

    public void validateAll(ServerLevel level) {
        if(!level.isClientSide){
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
}
