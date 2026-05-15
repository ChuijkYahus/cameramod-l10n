package net.mehvahdjukaar.vista.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Per-instance registry of extra chunk zones that should be pinned into the ViewArea
 * and kept visible regardless of player position or frustum.
 * Each zone is a circle (Euclidean) of chunks centred on a fixed world chunk position.
 *
 * One singleton lives on the client ({@link #CLIENT_INSTANCE}); one instance per
 * {@link net.minecraft.server.level.ServerPlayer} is stored in
 * {@link net.mehvahdjukaar.vista.VistaMod#TRACKED_CAMERAS_ATTACH}.
 */
public class ExtraChunkViewData {

    // -------------------------------------------------------------------------
    // Zone record
    // -------------------------------------------------------------------------

    public record Zone(ChunkPos center, byte radius) {

        public static final Codec<Zone> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.LONG.xmap(ChunkPos::new, ChunkPos::toLong).fieldOf("center").forGetter(Zone::center),
                Codec.BYTE.fieldOf("radius").forGetter(Zone::radius)
        ).apply(inst, Zone::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Zone> STREAM_CODEC = StreamCodec.of(
                (buf, zone) -> { buf.writeLong(zone.center.toLong()); buf.writeByte(zone.radius); },
                buf -> new Zone(new ChunkPos(buf.readLong()), buf.readByte())
        );

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

    // -------------------------------------------------------------------------
    // Codecs for the whole instance (list of zones)
    // -------------------------------------------------------------------------

    public static final Codec<ExtraChunkViewData> CODEC = Zone.CODEC.listOf().xmap(
            zones -> {
                ExtraChunkViewData d = new ExtraChunkViewData();
                d.zones.addAll(zones);
                return d;
            },
            d -> List.copyOf(d.zones)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtraChunkViewData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ExtraChunkViewData decode(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            ExtraChunkViewData data = new ExtraChunkViewData();
            for (int i = 0; i < size; i++) {
                data.zones.add(Zone.STREAM_CODEC.decode(buf));
            }
            return data;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ExtraChunkViewData data) {
            buf.writeVarInt(data.zones.size());
            for (Zone z : data.zones) {
                Zone.STREAM_CODEC.encode(buf, z);
            }
        }
    };

    // -------------------------------------------------------------------------
    // Instance state
    // -------------------------------------------------------------------------

    private final List<Zone> zones = new CopyOnWriteArrayList<>();

    public ExtraChunkViewData() {
    }

    public void addZone(ChunkPos center, int radius) {
        zones.add(new Zone(center, (byte) radius));
    }

    /** Removes all zones whose centre matches the given chunk position. */
    public void removeZone(ChunkPos center) {
        zones.removeIf(z -> z.center().equals(center));
    }

    public void clearZones() {
        zones.clear();
    }

    public List<Zone> getZones() {
        return Collections.unmodifiableList(zones);
    }

    /** Returns true if the given chunk position falls inside any registered zone. */
    public boolean containsChunk(int chunkX, int chunkZ) {
        for (Zone zone : zones) {
            if (zone.contains(chunkX, chunkZ)) return true;
        }
        return false;
    }

    /** Returns all unique chunk positions across all zones. */
    public Set<ChunkPos> getAllChunks() {
        Set<ChunkPos> chunks = new HashSet<>();
        for (Zone zone : zones) {
            chunks.addAll(zone.chunks());
        }
        return chunks;
    }

    // -------------------------------------------------------------------------
    // Client singleton
    // -------------------------------------------------------------------------

    /** Client-side singleton – updated by {@code ClientBoundSyncExtraChunksPacket}. */
    public static final ExtraChunkViewData CLIENT_INSTANCE = new ExtraChunkViewData();
}
