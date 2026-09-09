package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.client.chunk_tracking.ClientPinnedChunksManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Inject(method = "unload", at = @At("HEAD"), cancellable = true)
    private void vista$keepPinnedChunkAlive(LevelChunk chunk, CallbackInfo ci) {
        if (ClientPinnedChunksManager.isPinned(chunk)) {
            ci.cancel();
        }
    }
}
