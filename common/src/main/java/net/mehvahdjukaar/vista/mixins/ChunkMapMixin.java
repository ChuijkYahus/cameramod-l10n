package net.mehvahdjukaar.vista.mixins;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {

    @Inject(method = "dropChunk", at = @At("HEAD"), cancellable = true)
    private static void vista$preventDropChunk00(ServerPlayer player, ChunkPos chunkPos, CallbackInfo ci) {
        if (chunkPos.x == 0 && chunkPos.z == 0) {
            ci.cancel();
        }
    }
}
