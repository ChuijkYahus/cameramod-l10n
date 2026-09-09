package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.client.chunk_tracking.ClientPinnedChunksManager;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientChunkCache.class)
public class ClientChunkCacheMixin {

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;",
            at = @At("HEAD"), cancellable = true)
    private void vista$getPinnedChunk(int x, int z, ChunkStatus status, boolean require,
            CallbackInfoReturnable<LevelChunk> cir) {
        LevelChunk chunk = ClientPinnedChunksManager.get(x, z);
        if (chunk != null) {
            cir.setReturnValue(chunk);
        }
    }

    @Inject(method = "replaceBiomes", at = @At("HEAD"), cancellable = true)
    private void vista$replacePinnedBiomes(int x, int z, FriendlyByteBuf buffer, CallbackInfo ci) {
        LevelChunk chunk = ClientPinnedChunksManager.get(x, z);
        if (chunk != null) {
            chunk.replaceBiomes(buffer);
            ci.cancel();
        }
    }
}
