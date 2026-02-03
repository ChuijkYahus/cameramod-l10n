package net.mehvahdjukaar.vista.common;

import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.moonlight.api.misc.WorldSavedData;
import net.mehvahdjukaar.moonlight.api.misc.WorldSavedDataType;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.common.cassette.IBroadcastProvider;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class BroadcastManager extends WorldSavedData {

    public static final Codec<BroadcastManager> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, GlobalPos.CODEC)
            .xmap(map -> {
                BroadcastManager storage = new BroadcastManager();
                map.forEach((uuid, globalPos) -> {
                    storage.addFeed(uuid, globalPos, false);
                });
                return storage;
            }, storage -> storage.linkedFeeds);

    public static final StreamCodec<RegistryFriendlyByteBuf, BroadcastManager> STREAM_CODEC =
            (StreamCodec) ByteBufCodecs.map(
                    i -> new HashMap<>(),
                    UUIDUtil.STREAM_CODEC,
                    GlobalPos.STREAM_CODEC
            ).map(map -> {
                BroadcastManager storage = new BroadcastManager();
                map.forEach((uuid, globalPos) -> {
                    storage.addFeed(uuid, globalPos, true); //client always follows server rather than what it has. If we do our homework on server side this will always work
                });
                return storage;
            }, storage -> new HashMap<>(storage.linkedFeeds));

    private BroadcastManager() {
    }

    public static BroadcastManager create(ServerLevel serverLevel) {
        return new BroadcastManager();
    }

    private final HashBiMap<UUID, GlobalPos> linkedFeeds = HashBiMap.create();

    private boolean addFeed(UUID viewFinderUUID, GlobalPos projectorPos, boolean trusted) {
        Set<GlobalPos> keys = linkedFeeds.inverse().keySet();
        boolean contains = keys.contains(projectorPos);
        if (contains || trusted) {
            linkedFeeds.forcePut(viewFinderUUID, projectorPos);
            return true;
        }
        //error
        return false;
    }

    public void linkFeed(UUID viewFinderUUID, GlobalPos projectorPos) {
        GlobalPos old = linkedFeeds.get(viewFinderUUID);
        if (projectorPos.equals(old)) return;
        else if (old != null) {
            //if the old one is valid we have a problem.
            //we invalidate the old one if the same id is placed somewhere else
            linkedFeeds.remove(viewFinderUUID);
        }

        if (addFeed(viewFinderUUID, projectorPos, true)) {
            this.setDirty();
            this.sync();
        }
    }

    public void unlinkFeed(GlobalPos projectorPos) {
        UUID id = linkedFeeds.inverse().get(projectorPos);
        if (id != null) {
            linkedFeeds.remove(id);
            this.setDirty();
            this.sync();
        }
    }

    public void unlinkFeed(UUID viewFinderUUID) {
        if (linkedFeeds.remove(viewFinderUUID) != null) {
            this.setDirty();
            this.sync();
        }
    }

    @Nullable
    public GlobalPos getBroadcastOriginById(UUID viewFinderUUID) {
        return linkedFeeds.get(viewFinderUUID);
    }

    @Nullable
    public IBroadcastProvider getBroadcast(UUID feedId, boolean clientSide) {
        GlobalPos pos = getBroadcastOriginById(feedId);
        if (pos != null) {
          Level otherLevel = clientSide ? VistaModClient.getLocalLevelByDimension(pos.dimension()) :
                  PlatHelper.getCurrentServer().getLevel(pos.dimension());
            if (otherLevel != null && otherLevel.isLoaded(pos.pos())) {
                if (otherLevel.getBlockEntity(pos.pos()) instanceof IBroadcastProvider provider) {
                    return provider;
                } else {
                    //clean up
                    if (!clientSide) {
                        this.unlinkFeed(feedId);
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public UUID getIdOfFeedAt(GlobalPos from) {
        return linkedFeeds.inverse().get(from);
    }

    @Override
    public WorldSavedDataType<BroadcastManager> getType() {
        return VistaMod.VIEWFINDER_CONNECTION;
    }

    //static helpers

    public static BroadcastManager getInstance(Level level) {
        return VistaMod.VIEWFINDER_CONNECTION.getData(level);
    }

    @Nullable
    public static IBroadcastProvider findLinkedFeedProvider(Level level, @Nullable UUID viewFinderUUID) {
        if (viewFinderUUID == null) return null;
        BroadcastManager connection = getInstance(level);
        if (connection == null) return null;
        GlobalPos gp = connection.getBroadcastOriginById(viewFinderUUID);
        if (gp != null && gp.dimension() == level.dimension()) {
            BlockPos pos = gp.pos();
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof IBroadcastProvider be) {
                return be;
            }
        }
        return null;
    }

    @Nullable
    public static ViewFinderBlockEntity findLinkedViewFinder(Level level, @Nullable UUID viewFinderUUID) {
        if (viewFinderUUID == null) return null;
        BroadcastManager connection = getInstance(level);
        if (connection == null) return null;
        GlobalPos gp = connection.getBroadcastOriginById(viewFinderUUID);
        if (gp != null && gp.dimension() == level.dimension()) {
            BlockPos pos = gp.pos();
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof ViewFinderBlockEntity be) {
                return be;
            }
        }
        return null;
    }

    public Iterable<Map.Entry<UUID, GlobalPos>> getAll() {
        return linkedFeeds.entrySet();
    }


}
