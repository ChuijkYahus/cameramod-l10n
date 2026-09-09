package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public class ChunkMapTrackedEntityMixin {

    @Shadow
    @Final
    Entity entity;

    @ModifyVariable(method = "updatePlayer", at = @At("STORE"), ordinal = 0)
    private boolean vista$trackEntitiesInZones(boolean inRange, ServerPlayer player) {
        if (inRange) return true;
        ChunkPos entityChunk = entity.chunkPosition();
        return VistaMod.EXTRA_VIEW_AREAS.getOrCreate(player).containsChunk(entityChunk.x, entityChunk.z)
                && entity.broadcastToPlayer(player);
    }
}
