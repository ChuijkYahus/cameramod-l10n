package net.mehvahdjukaar.vista.common;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.UUID;

public class ProjectorStorage extends SavedData {

    public static final String DATA_NAME = VistaMod.res("projector_data").toString();

    public static final Codec<ProjectorStorage> CODEC = Codec.unboundedMap(UUIDUtil.CODEC, GlobalPos.CODEC)
            .xmap(map -> {
                ProjectorStorage storage = new ProjectorStorage();
                storage.projectorMap.putAll(map);
                return storage;
            }, storage -> storage.projectorMap);

    public static final StreamCodec<ByteBuf, ProjectorStorage> STREAM_CODEC = ByteBufCodecs.map(
            i -> new HashMap<>(),
            UUIDUtil.STREAM_CODEC,
            GlobalPos.STREAM_CODEC
    ).map(map -> {
        ProjectorStorage storage = new ProjectorStorage();
        storage.projectorMap.putAll(map);
        return storage;
    }, storage -> storage.projectorMap);

    //data received from network is stored here
    private static ProjectorStorage clientSideInstance = null;



    private static void setClientSideInstance(ProjectorStorage instance) {
        clientSideInstance = instance;
    }



    private final HashMap<UUID, GlobalPos> projectorMap = new HashMap<>();

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return null;
    }
}
