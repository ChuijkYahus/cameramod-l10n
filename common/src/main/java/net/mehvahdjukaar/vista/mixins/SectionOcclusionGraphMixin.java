package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.ClientChunkStuffHelper;
import net.mehvahdjukaar.vista.client.renderer.FeedSectionOcclusionGraph;
import net.mehvahdjukaar.vista.common.chunk_tracking.IPinnableRenderSection;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

@Mixin(SectionOcclusionGraph.class)
public class SectionOcclusionGraphMixin {

    @Shadow
    private ViewArea viewArea;

    @WrapOperation(
            method = "isInViewDistance",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkTrackingView;isInViewDistance(IIIII)Z"))
    private boolean vista$allowPinnedChunk(int centerX, int centerZ, int viewDistance, int x, int z,
                                           Operation<Boolean> original) {
        return original.call(centerX, centerZ, viewDistance, x, z)
                || VistaModClient.CLIENT_EXTRA_CHUNK_VIEW_DATA.containsChunk(x, z);
    }

    /**
     * Seed every pinned section as an additional BFS root — but only for feed graphs,
     * never for the player's own graph.
     *
     * <p>The full BFS runs asynchronously on a background thread, so we cannot use
     * {@code isRenderingLiveFeed()} here. Instead we compare {@code this} against the
     * The check uses {@code instanceof} {@link FeedSectionOcclusionGraph} which is safe
     * from any thread, requires no external registry, and is never ambiguous.
     *
     * <p>Excluding the player's graph prevents pinned sections (at the ViewFinder,
     * potentially hundreds of chunks away) from entering the player's {@code renderSections}
     * and leaking into the player's world view when the player looks toward the ViewFinder.
     *
     * <p>For feed graphs the camera IS at the ViewFinder, so seeding ensures pinned
     * sections enter the BFS even though they lie outside the normal torus. Frustum
     * culling in {@code addSectionsInFrustum} then correctly limits which ones actually
     * appear in {@code visibleSections}.
     */
    @SuppressWarnings("unchecked")
    @Inject(method = "initializeQueueForFullUpdate", at = @At("TAIL"))
    private void vista$seedPinnedSections(Camera camera, Queue nodeQueue, CallbackInfo ci) {
        if (!((Object) this instanceof FeedSectionOcclusionGraph)) return;
        if (this.viewArea == null) return;
        for (SectionRenderDispatcher.RenderSection section : this.viewArea.sections) {
            if (section instanceof IPinnableRenderSection ps && ps.vista$isPinned()) {
                var node = ClientChunkStuffHelper.createNode(section);
                if (node != null) nodeQueue.add(node);
            }
        }
    }

}
