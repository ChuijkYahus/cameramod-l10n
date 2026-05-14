package net.mehvahdjukaar.vista.mixins;

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

import java.util.function.Consumer;

@Mixin(ClientChunkCache.class)
public class ClientChunkCacheMixin {

    /**
     * Permanent reference to chunk (0,0). The ClientChunkCache uses a circular buffer
     * keyed by floorMod(x/z, viewRange). When the player is far from origin, another
     * chunk X with floorMod(X.x, viewRange)==0 & floorMod(X.z, viewRange)==0 overwrites
     * slot 0, evicting chunk (0,0). We keep our own reference so getChunk(0,0) always
     * returns real data for the pinned section to compile against.
     */
    @Unique
    private LevelChunk vista$pinnedChunk00;

    /**
     * Short-circuit getChunk before the circular-buffer lookup for chunk (0,0).
     * This ensures block data is always available for the pinned RenderSection to
     * compile against, regardless of what the circular buffer slot currently holds.
     */
    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;",
            at = @At("HEAD"), cancellable = true)
    private void vista$getPinnedChunk(int x, int z, ChunkStatus status, boolean require,
            CallbackInfoReturnable<LevelChunk> cir) {
        if (x == 0 && z == 0 && this.vista$pinnedChunk00 != null) {
            cir.setReturnValue(this.vista$pinnedChunk00);
        }
    }

    /**
     * When the server sends chunk (0,0) save a permanent reference so that even
     * after the circular buffer evicts it we can still serve it from getChunk.
     */
    @Inject(method = "replaceWithPacketData", at = @At("RETURN"))
    private void vista$capturePinnedChunk(int x, int z, FriendlyByteBuf buffer, CompoundTag tag,
            Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer,
            CallbackInfoReturnable<LevelChunk> cir) {
        if (x == 0 && z == 0 && cir.getReturnValue() != null) {
            this.vista$pinnedChunk00 = cir.getReturnValue();
        }
    }

    /** Prevent a server-initiated drop packet from clearing our pinned reference. */
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void vista$preventDropChunk00(ChunkPos chunkPos, CallbackInfo ci) {
        if (chunkPos.x == 0 && chunkPos.z == 0) {
            ci.cancel();
        }
    }
}
