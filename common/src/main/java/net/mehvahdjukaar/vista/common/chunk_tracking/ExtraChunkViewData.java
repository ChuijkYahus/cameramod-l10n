package net.mehvahdjukaar.vista.common.chunk_tracking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.ChunkPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class ExtraChunkViewData {

    public record Zone(ChunkPos center, byte radius) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Zone> STREAM_CODEC = StreamCodec.of(
                (buf, zone) -> {
                    buf.writeLong(zone.center.toLong());
                    buf.writeByte(zone.radius);
                },
                buf -> new Zone(new ChunkPos(buf.readLong()), buf.readByte())
        );

        public Set<ChunkPos> chunks() {
            Set<ChunkPos> chunks = new HashSet<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius) {
                        chunks.add(new ChunkPos(center.x + dx, center.z + dz));
                    }
                }
            }
            return chunks;
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtraChunkViewData> STREAM_CODEC =
            Zone.STREAM_CODEC.apply(ByteBufCodecs.list()).map(ExtraChunkViewData::new, d -> d.zones);

    protected final List<Zone> zones = new CopyOnWriteArrayList<>();
    // flat set of every chunk across all zones, so containsChunk is O(1)
    private Set<Long> allChunkKeys = Set.of();
    private Set<ChunkPos> allChunks = Set.of();

    public ExtraChunkViewData() {
    }

    private ExtraChunkViewData(List<Zone> zones) {
        this.zones.addAll(zones);
        rebuildChunkSets();
    }

    public void addZone(ChunkPos center, int radius) {
        zones.add(new Zone(center, (byte) radius));
        rebuildChunkSets();
    }

    public void clearZones() {
        zones.clear();
        rebuildChunkSets();
    }

    private void rebuildChunkSets() {
        Set<Long> keys = new HashSet<>();
        Set<ChunkPos> chunks = new HashSet<>();
        for (Zone zone : zones) {
            for (ChunkPos pos : zone.chunks()) {
                if (keys.add(pos.toLong())) chunks.add(pos);
            }
        }
        allChunkKeys = keys;
        allChunks = Collections.unmodifiableSet(chunks);
    }

    public List<Zone> getZones() {
        return Collections.unmodifiableList(zones);
    }

    // cached, don't mutate
    public Set<ChunkPos> getAllChunks() {
        return allChunks;
    }

    public boolean containsChunk(int chunkX, int chunkZ) {
        if (allChunkKeys.isEmpty()) return false;
        return allChunkKeys.contains(ChunkPos.asLong(chunkX, chunkZ));
    }

}
