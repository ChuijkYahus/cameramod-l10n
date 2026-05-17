package net.mehvahdjukaar.vista.common.chunk_tracking;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.broadcast.BroadcastManager;
import net.mehvahdjukaar.vista.common.broadcast.IBroadcastLocation;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.mehvahdjukaar.vista.configs.CommonConfigs;
import net.mehvahdjukaar.vista.mixins.accessor.ChunkMapAccessor;
import net.mehvahdjukaar.vista.network.ClientBoundSyncExtraChunksPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.*;

/**
 * Server-authoritative camera chunk manager.
 *
 * <p>Every {@link #TICK_INTERVAL} game-ticks (staggered per player) this class:
 * <ol>
 *   <li>Scans every loaded chunk in the player's normal view distance for
 *       {@link TVBlockEntity} instances whose cassette has a linked ViewFinder UUID.</li>
 *   <li>Follows the {@link BroadcastManager} connection to find the ViewFinder's
 *       world position.</li>
 *   <li>Force-loads the circle of chunks around the ViewFinder via
 *       {@link ServerLevel#setChunkForced} (ref-counted across multiple players
 *       watching the same ViewFinder).</li>
 *   <li>Updates the player's {@link ExtraChunkViewData} attachment and syncs it to the
 *       client so the extra chunk zone system sends the ViewFinder chunks to them.</li>
 * </ol>
 *
 * <p>ViewFinders already inside the player's normal view distance are skipped —
 * the server sends those chunks naturally without any special treatment.
 *
 * <p>Cross-dimension ViewFinders are force-loaded in their own dimension but are
 * <em>not</em> added to the player's ExtraChunkViewData, since chunk-sending zones
 * only apply to the player's current dimension.
 */
public class ServerCameraChunkManager {

    public static final int CHUNK_RADIUS = 4;
    private static final int TICK_INTERVAL = 40;
    /**
     * How often (in ticks) we retry sending zone chunks that weren't loaded on the first attempt.
     */
    private static final int FLUSH_INTERVAL = 5;

    /**
     * ViewFinder GlobalPos → number of players currently watching it.
     * Used for ref-counting {@code setChunkForced} across multiple players.
     * Global server state — intentionally not per-player.
     */
    private static final Map<GlobalPos, Integer> linkedViewFindersTrackedByPlayers = new HashMap<>();

    // ── Per-player tick ───────────────────────────────────────────────────────

    /**
     * Called every server tick for each {@link ServerPlayer} (via {@code ServerPlayerMixin}).
     * Updates are staggered so not all players recalculate on the same tick.
     */
    public static void onServerPlayerTick(ServerPlayer player) {
        if(true)return;
        boolean sends = CommonConfigs.SEND_CHUNKS_VIEWED_BY_VIEW_FINDER.get();
        boolean loads = CommonConfigs.LOAD_CHUNKS_VIEWED_BY_VIEW_FINDER.get() || PlatHelper.isDev();
        if (!sends && !loads) return;
        long gameTime = player.serverLevel().getGameTime();

        // Periodic flush: retry sending zone chunks that weren't loaded on the first attempt.
        // Runs more frequently than the zone-detection scan so chunks appear promptly after
        // force-loading completes (which is async and may take several ticks).
        if ((gameTime + player.getId()) % FLUSH_INTERVAL == 0) {
            flushPendingZoneChunks(player);
        }

        if ((gameTime + player.getId()) % TICK_INTERVAL != 0) return;

        ServerExtraChunkViewData data = VistaMod.EXTRA_VIEW_AREAS.getOrCreate(player);
        Set<GlobalPos> desired = findViewFindersNeededForPlayer(player);
        Set<GlobalPos> current = data.getTrackedWantedZoneCenters();
        if (desired.equals(current)) return;

        Set<GlobalPos> added = new HashSet<>(desired);
        added.removeAll(current);
        Set<GlobalPos> removed = new HashSet<>(current);
        removed.removeAll(desired);

        if(loads) {
            for (GlobalPos vf : added) {
                int refs = linkedViewFindersTrackedByPlayers.getOrDefault(vf, 0);
                if (refs == 0) {
                    ServerLevel vfLevel = player.getServer().getLevel(vf.dimension());
                    if (vfLevel != null) setChunksForceLoaded(vfLevel, vf.pos(), true);
                }
                linkedViewFindersTrackedByPlayers.put(vf, refs + 1);
            }
            for (GlobalPos vf : removed) {
                int refs = linkedViewFindersTrackedByPlayers.getOrDefault(vf, 0) - 1;
                if (refs <= 0) {
                    linkedViewFindersTrackedByPlayers.remove(vf);
                    ServerLevel vfLevel = player.getServer().getLevel(vf.dimension());
                    if (vfLevel != null) setChunksForceLoaded(vfLevel, vf.pos(), false);
                } else {
                    linkedViewFindersTrackedByPlayers.put(vf, refs);
                }
            }
        }
        // Persist the new tracked set inside the per-player attachment.
        data.setTrackedWantedZoneCenters(desired);

        // Rebuild zones (same-dimension ViewFinders only; cross-dim only force-loads).
        data.clearZones();
        for (GlobalPos vf : desired) {
            if (vf.dimension().equals(player.level().dimension())) {
                data.addZone(new ChunkPos(vf.pos()), CHUNK_RADIUS);
            }
        }

        NetworkHelper.sendToClientPlayer(player, new ClientBoundSyncExtraChunksPacket(data));
        VistaMod.LOGGER.debug("[Vista] {} zones updated: {} viewfinders, {} same-dim zones",
                player.getName().getString(), desired.size(), data.getZones().size());
    }

    // ── Zone chunk flushing ───────────────────────────────────────────────────

    /**
     * Directly calls {@code markChunkPendingToSend} for every zone chunk that is now
     * loaded but not yet queued. This is the server-side counterpart to
     * {@code ChunkMapMixin.vista$flushPendingZoneChunks} and runs independently of
     * player movement so chunks are sent promptly after force-loading completes.
     */
    private static void flushPendingZoneChunks(ServerPlayer player) {
        ServerExtraChunkViewData data = VistaMod.EXTRA_VIEW_AREAS.getOrCreate(player);
        if (data.getZones().isEmpty()) return;

        ChunkMapAccessor chunkMap = (ChunkMapAccessor) player.serverLevel().getChunkSource().chunkMap;
        int flushed = 0;
        for (ChunkPos pos : data.getAllChunks()) {
            if (!player.getChunkTrackingView().isInViewDistance(pos.x, pos.z) && !data.isZoneChunkQueued(pos)) {
                if (chunkMap.vista$getChunkToSend(pos.toLong()) != null) {
                    chunkMap.vista$markChunkPendingToSend(player, pos);
                    data.markZoneChunkQueued(pos);
                    flushed++;
                }
            }
        }
        if (flushed > 0) {
            VistaMod.LOGGER.debug("[Vista] Periodic flush sent {} zone chunks to {}", flushed, player.getName().getString());
        }
    }

    // ── ViewFinder discovery ──────────────────────────────────────────────────

    private static Set<GlobalPos> findViewFindersNeededForPlayer(ServerPlayer player) {
        Set<GlobalPos> result = new HashSet<>();
        ServerLevel level = player.serverLevel();
        BroadcastManager bm = BroadcastManager.getInstance(level);
        int pcx = player.chunkPosition().x;
        int pcz = player.chunkPosition().z;
        int trackingDist = CommonConfigs.distanceFromTvForServerToLoadViewFinders(level);

        for (int dx = -trackingDist; dx <= trackingDist; dx++) {
            for (int dz = -trackingDist; dz <= trackingDist; dz++) {
                int chunkX = pcx + dx;
                int chunkZ = pcz + dz;
                if (!player.getChunkTrackingView().isInViewDistance(chunkX, chunkZ)) continue;
                ChunkAccess access = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (!(access instanceof LevelChunk chunk)) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof TVBlockEntity tv)) continue;
                    UUID feedId = tv.getViewingFeedId();
                    if (feedId == null) continue;
                    IBroadcastLocation broadCastLoc = bm.getFeedLocationById(feedId);
                    if (broadCastLoc == null) continue;

                    GlobalPos chunkPosToSend = broadCastLoc.getChunkSendPosition();
                    if (chunkPosToSend == null) continue;

                    //TODO: send chunks even if outside of current dim.
                    if (chunkPosToSend.dimension().equals(level.dimension())) {
                        // Skip ViewFinders already within the player's normal view (server sends them naturally)
                        ChunkPos vfChunk = new ChunkPos(chunkPosToSend.pos());
                        if (player.getChunkTrackingView().isInViewDistance(vfChunk.x, vfChunk.z)) {
                            continue;
                        }
                    }
                    result.add(chunkPosToSend);
                }
            }
        }
        return result;
    }

    // ── Force-loading ─────────────────────────────────────────────────────────

    private static void setChunksForceLoaded(ServerLevel level, BlockPos viewFinderPos, boolean force) {
        int cx = viewFinderPos.getX() >> 4;
        int cz = viewFinderPos.getZ() >> 4;
        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                if (dx * dx + dz * dz <= CHUNK_RADIUS * CHUNK_RADIUS) {
                    level.setChunkForced(cx + dx, cz + dz, force);
                }
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Call when a player disconnects to release their force-loading references.
     */
    public static void onPlayerLeave(ServerPlayer player) {
        Set<GlobalPos> watching = VistaMod.EXTRA_VIEW_AREAS.getOrCreate(player).getTrackedWantedZoneCenters();
        for (GlobalPos vf : watching) {
            int refs = linkedViewFindersTrackedByPlayers.getOrDefault(vf, 0) - 1;
            if (refs <= 0) {
                linkedViewFindersTrackedByPlayers.remove(vf);
                ServerLevel vfLevel = player.getServer().getLevel(vf.dimension());
                if (vfLevel != null) setChunksForceLoaded(vfLevel, vf.pos(), false);
            } else {
                linkedViewFindersTrackedByPlayers.put(vf, refs);
            }
        }
    }

    /**
     * Clear all state on server stop.
     */
    public static void clearAll() {
        linkedViewFindersTrackedByPlayers.clear();
    }
}
