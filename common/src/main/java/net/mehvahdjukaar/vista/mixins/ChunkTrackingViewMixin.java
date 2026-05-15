package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.common.ExtraChunkViewData;
import net.mehvahdjukaar.vista.common.IChunkViewWithZones;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Injects an {@link ExtraChunkViewData} field into every {@link ChunkTrackingView.Positioned}
 * instance. {@link ChunkMapMixin} sets this field on the new view just before
 * {@code applyChunkTrackingView} calls {@code difference}.
 *
 * IMPORTANT — we do NOT patch {@code contains(x,z,includeOuter)}.
 * If we did, both the old and new stored views would report zone chunks as
 * "already tracked". When the player later walks into the zone's normal view range
 * the bounding-box fast path sees old.contains==true AND new.contains==true → no diff
 * → the chunk is never (re-)sent; the server wrongly believes the client already has it.
 *
 * By patching only {@code forEach}:
 *  - Zone chunks are emitted during the fallback path (initial join / teleport, where
 *    old==EMPTY or views don't intersect) → sent to the client on first opportunity.
 *  - Once a zone chunk enters the player's normal view distance, the bounding-box
 *    fast path sees old.contains==false, new.contains==true → toAdd → sent normally,
 *    just like any ordinary chunk coming into range.
 */
@Mixin(ChunkTrackingView.Positioned.class)
public class ChunkTrackingViewMixin implements IChunkViewWithZones {

    @Unique
    private ExtraChunkViewData vista$zones;

    @Override
    public ExtraChunkViewData vista$getExtraZones() {
        return vista$zones;
    }

    @Override
    public void vista$setExtraZones(ExtraChunkViewData data) {
        this.vista$zones = data;
    }

    /**
     * Appends zone chunks that lie outside the player's normal view distance after
     * the regular bounding-box iteration. Exercised by {@code difference}'s fallback
     * path only (initial join / teleport); normal movement uses the fast path which
     * never visits coordinates outside its bounding box.
     */
    @Inject(method = "forEach", at = @At("RETURN"))
    private void vista$addExtraZoneChunks(Consumer<ChunkPos> action, CallbackInfo ci) {
        if (vista$zones == null || vista$zones.getZones().isEmpty()) return;
        ChunkTrackingView self = (ChunkTrackingView) (Object) this;
        for (ChunkPos pos : vista$zones.getAllChunks()) {
            if (!self.isInViewDistance(pos.x, pos.z)) {
                action.accept(pos);
            }
        }
    }
}
