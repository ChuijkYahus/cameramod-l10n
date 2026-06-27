package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.chunk_tracking.ExtraChunkViewData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Subscribes the player to entity tracking for entities sitting in a camera-zone
 * chunk. Vanilla {@code TrackedEntity.updatePlayer} gates on
 * {@code e <= f && broadcastToPlayer && isChunkTracked}: the {@code isChunkTracked}
 * term is fixed by {@link ChunkMapMixin}, but the {@code e <= f} euclidean
 * distance check still fails because zone chunks are far outside view distance, so
 * entities there are never paired and never spawn on the client. We force the
 * computed {@code bl} flag true for zone-chunk entities that still want to be
 * broadcast.
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public class ChunkMapTrackedEntityMixin {

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private Entity entity;

    @ModifyVariable(method = "updatePlayer", at = @At("STORE"), ordinal = 0)
    private boolean vista$trackZoneEntities(boolean bl, ServerPlayer player) {
        if (bl) return true;
        ExtraChunkViewData data = VistaMod.EXTRA_VIEW_AREAS.getOrCreate(player);
        if (data != null
                && data.containsChunk(this.entity.chunkPosition().x, this.entity.chunkPosition().z)
                && this.entity.broadcastToPlayer(player)) {
            return true;
        }
        return bl;
    }
}
