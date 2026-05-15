package net.mehvahdjukaar.vista.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.ChunkPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of extra chunk zones that should be pinned into the ViewArea and kept
 * visible regardless of player position or frustum. Each zone is a circle (Euclidean)
 * of chunks centred on a fixed world chunk position.
 * <p>
 * Populate this before ViewArea construction (i.e. before LevelRenderer.allChanged()).
 */
public class ExtraChunkViewData {
    public static final Codec<ExtraChunkViewData> CODEC
    public static final StreamCodec<? super RegistryFriendlyByteBuf, TrackedCameras> STREAM_CODEC = ;

    public record Zone(ChunkPos center, byte radius) {

        public static final Codec<Zone> CODEC = RecordCodecBuilder.create(zoneInstance ->
                zoneInstance.group(
                        Codec.LONG.xmap(ChunkPos::new, ChunkPos::toLong).fieldOf("center").forGetter(Zone::center),
                        Codec.BYTE.fieldOf("radius").forGetter(Zone::radius)
                ).apply(zoneInstance, Zone::new));

        public static final StreamCodec<Zone, RegistryFriendlyByteBuf> STREAM_CODEC =

        public boolean contains(int chunkX, int chunkZ) {
            int dx = chunkX - center.x;
            int dz = chunkZ - center.z;
            return dx * dx + dz * dz <= radius * radius;
        }

        public Set<ChunkPos> chunks() {
            Set<ChunkPos> result = new HashSet<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius) {
                        result.add(new ChunkPos(center.x + dx, center.z + dz));
                    }
                }
            }
            return result;
        }
    }

    public ExtraChunkViewData(){
        addZone(new ChunkPos(1,4 ),5); //to test
    }


    private final List<Zone> zones = new CopyOnWriteArrayList<>();

    public void addZone(ChunkPos center, int radius) {
        zones.add(new Zone(center, radius));
    }

    public void clearZones() {
        zones.clear();
    }

    public List<Zone> getZones() {
        return Collections.unmodifiableList(zones);
    }

    /**
     * Returns true if the given chunk position falls inside any registered zone.
     */
    public  boolean containsChunk(int chunkX, int chunkZ) {
        for (Zone zone : zones) {
            if (zone.contains(chunkX, chunkZ)) return true;
        }
        return false;
    }

    /**
     * Returns all unique chunk positions across all zones.
     */
    public  Set<ChunkPos> getAllChunks() {
        Set<ChunkPos> chunks = new HashSet<>();
        for (Zone zone : zones) {
            chunks.addAll(zone.chunks());
        }
        return chunks;
    }


    public static final ExtraChunkViewData CLIENT_INSTANCE = new ExtraChunkViewData();
}
