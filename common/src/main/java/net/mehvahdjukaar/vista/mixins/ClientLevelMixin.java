package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.VistaModClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps camera-zone chunks ticking even after the client's circular-buffer chunk
 * storage evicts them.
 *
 * <p>The client {@code ClientChunkCache.Storage} is a {@code floorMod}-indexed torus.
 * A far zone chunk and a normal nearby chunk can hash to the same slot; when the
 * nearby chunk streams in it overwrites the slot and {@code Storage.replace} calls
 * {@link ClientLevel#unload(LevelChunk)} on the evicted zone chunk. That
 * {@code unload} runs {@code clearAllBlockEntities()} (removing block-entity tickers,
 * e.g. a moving piston), {@code entityStorage.stopTicking()} (freezing entities) and
 * disables the chunk's light. Vista still returns the chunk from its pinned map so it
 * keeps <em>rendering</em>, but it stops <em>ticking</em> — the "rendered but frozen"
 * symptom on far feeds.
 *
 * <p>{@code Storage.replace} only fires for a slot whose current chunk differs from the
 * incoming one, so a zone chunk reaching {@code unload} is always a collision eviction,
 * never a same-position refresh — cancelling here is safe. When the zone is genuinely
 * removed the chunk is no longer in {@link VistaModClient#CLIENT_EXTRA_CHUNK_VIEW_DATA},
 * so normal unload proceeds.
 */
@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Inject(method = "unload", at = @At("HEAD"), cancellable = true)
    private void vista$keepZoneChunkTicking(LevelChunk chunk, CallbackInfo ci) {
        ChunkPos pos = chunk.getPos();
        if (VistaModClient.CLIENT_EXTRA_CHUNK_VIEW_DATA.containsChunk(pos.x, pos.z)) {
            ci.cancel();
        }
    }
}
