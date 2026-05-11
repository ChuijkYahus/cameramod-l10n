package net.mehvahdjukaar.vista.mixins.accessor;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {
    
    @Invoker("applyChunkTrackingView")
    void vista$applyChunkTrackingView(ServerPlayer player, ChunkTrackingView chunkTrackingView);
    
    @Invoker("getPlayerViewDistance")
    int vista$getPlayerViewDistance(ServerPlayer player);
}
