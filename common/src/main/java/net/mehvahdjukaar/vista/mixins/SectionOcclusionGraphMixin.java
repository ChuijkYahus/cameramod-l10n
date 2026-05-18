package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.ClientChunkStuffHelper;
import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import net.mehvahdjukaar.vista.common.chunk_tracking.IPinnableRenderSection;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
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
     * Seed every pinned section as an additional BFS root after the normal player-section
     * seed. Only active during ViewFinder feed renders: the feed camera is positioned at
     * the ViewFinder, so its BFS starts from the normal section torus (centred on the
     * player) and might not naturally reach the pinned sections without explicit seeds.
     * During the player's own render this is skipped — pinned chunks are irrelevant there.
     */
    @SuppressWarnings("unchecked")
    @Inject(method = "initializeQueueForFullUpdate", at = @At("TAIL"))
    private void vista$seedPinnedSections(Camera camera, Queue nodeQueue, CallbackInfo ci) {
        if (!VistaLevelRenderer.isRenderingLiveFeed()) return;
        if (this.viewArea == null) return;
        for (SectionRenderDispatcher.RenderSection section : this.viewArea.sections) {
            if (section instanceof IPinnableRenderSection ps && ps.vista$isPinned()) {
                var node = ClientChunkStuffHelper.createNode(section);
                if (node != null) nodeQueue.add(node);
            }
        }
    }

    /**
     * Appends pinned sections to the visible list during ViewFinder feed renders only.
     *
     * <p>During the player's own render the feed camera is inactive; pinned sections are
     * at the ViewFinder's location (potentially hundreds of chunks away) and must not be
     * drawn to the player's view.
     *
     * <p>During feed renders the feed camera IS at the ViewFinder, so the normal BFS +
     * {@link #vista$allowPinnedChunk} handles reachability. We still add them
     * unconditionally here because the ViewArea torus is centred on the player, meaning
     * pinned sections may sit outside the torus index range and never enter the BFS even
     * with the view-distance patch.
     */
    @Inject(method = "addSectionsInFrustum", at = @At("TAIL"))
    private void vista$addPinnedSectionsToVisible(Frustum frustum,
                                                  List<SectionRenderDispatcher.RenderSection> sections, CallbackInfo ci) {
        if (!VistaLevelRenderer.isRenderingLiveFeed()) return;
        if (this.viewArea == null) return;
        for (SectionRenderDispatcher.RenderSection section : this.viewArea.sections) {
            if (section instanceof IPinnableRenderSection ps && ps.vista$isPinned()) {
                sections.add(section);
            }
        }
    }

}
