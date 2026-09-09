package net.mehvahdjukaar.vista.mixins.compat;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.chunk_tracking.ExtraChunkViewData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem", remap = false)
public class CompatSableTrackingMixin {

    @Inject(method = "shouldLoad", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void vista$trackSubLevelsInCameraZones(Player player, Vector3dc shipPosition,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer serverPlayer) {
            ExtraChunkViewData data = VistaMod.EXTRA_VIEW_AREAS.getOrCreate(serverPlayer);
            if (data != null && data.containsChunk(
                    Mth.floor(shipPosition.x()) >> 4,
                    Mth.floor(shipPosition.z()) >> 4)) {
                cir.setReturnValue(true);
            }
        }
    }
}
