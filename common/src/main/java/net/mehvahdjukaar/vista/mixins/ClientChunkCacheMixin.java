package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.IClientChunkCacheExt;
import net.mehvahdjukaar.vista.common.chunk_tracking.ExtraChunkViewData;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Prevents the client's circular-buffer chunk cache from evicting chunks that belong
 * to a camera zone registered in {@link ExtraChunkViewData#CLIENT_INSTANCE}.
 *
 * The circular buffer is indexed by {@code floorMod(x/z, viewRange)}, so when the
 * player moves far from a zone origin, another chunk overwrites that slot and evicts
 * the zone chunk. We keep our own {@code Map<Long, LevelChunk>} reference so
 * {@code getChunk} always returns real data for the pinned RenderSection to compile.
 */
@Mixin(ClientChunkCache.class)
public class ClientChunkCacheMixin implements IClientChunkCacheExt {

    @Unique
    private final Map<Long, LevelChunk> vista$pinnedChunks = new HashMap<>();

    @Override
    public Map<Long, LevelChunk> vista$getPinnedChunks() {
        return Collections.unmodifiableMap(vista$pinnedChunks);
    }

    /** Short-circuit getChunk for any pinned zone chunk before the circular-buffer lookup. */
    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;",
            at = @At("HEAD"), cancellable = true)
    private void vista$getPinnedChunk(int x, int z, ChunkStatus status, boolean require,
            CallbackInfoReturnable<LevelChunk> cir) {
        if (VistaModClient.CLIENT_EXTRA_CHUNK_VIEW_DATA.containsChunk(x, z)) {
            LevelChunk chunk = this.vista$pinnedChunks.get(ChunkPos.asLong(x, z));
            if (chunk != null) {
                cir.setReturnValue(chunk);
            }
        }
    }

    /** When the server sends a zone chunk, save a permanent reference. */
    @Inject(method = "replaceWithPacketData", at = @At("RETURN"))
    private void vista$capturePinnedChunk(int x, int z, FriendlyByteBuf buffer, CompoundTag tag,
            Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer,
            CallbackInfoReturnable<LevelChunk> cir) {
        if (VistaModClient.CLIENT_EXTRA_CHUNK_VIEW_DATA.containsChunk(x, z) && cir.getReturnValue() != null) {
            this.vista$pinnedChunks.put(ChunkPos.asLong(x, z), cir.getReturnValue());
            net.mehvahdjukaar.vista.VistaMod.LOGGER.info(
                    "[Vista/Chunks] Client received zone chunk ({}, {})", x, z);
        }
    }

    /** Prevent a server-initiated drop packet from clearing a pinned zone chunk. */
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void vista$preventDropPinnedChunk(ChunkPos chunkPos, CallbackInfo ci) {
        if (VistaModClient.CLIENT_EXTRA_CHUNK_VIEW_DATA.containsChunk(chunkPos.x, chunkPos.z)) {
            ci.cancel();
        }
    }
}
