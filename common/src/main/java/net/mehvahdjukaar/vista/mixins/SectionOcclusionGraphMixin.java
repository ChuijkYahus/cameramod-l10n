package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.SectionOcclusionGraphHelper;
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
     * seed. Pinned sections live beyond the torus range and are never reached by normal
     * propagation.
     */
    @SuppressWarnings("unchecked")
    @Inject(method = "initializeQueueForFullUpdate", at = @At("TAIL"))
    private void vista$seedPinnedSections(Camera camera, Queue nodeQueue, CallbackInfo ci) {
        if (this.viewArea == null) return;
        for (SectionRenderDispatcher.RenderSection section : this.viewArea.sections) {
            if (section instanceof IPinnableRenderSection ps && ps.vista$isPinned()) {
                var node = SectionOcclusionGraphHelper.createNode(section);
                if (node != null) nodeQueue.add(node);
            }
        }
    }

    /**
     * Unconditionally append every pinned section to the visible list after frustum
     * culling, bypassing the Frustum.isVisible check (which would always cull them).
     */
    @Inject(method = "addSectionsInFrustum", at = @At("TAIL"))
    private void vista$addPinnedSectionsToVisible(Frustum frustum,
                                                  List<SectionRenderDispatcher.RenderSection> sections, CallbackInfo ci) {
        if (this.viewArea == null) return;
        for (SectionRenderDispatcher.RenderSection section : this.viewArea.sections) {
            if (section instanceof IPinnableRenderSection ps && ps.vista$isPinned()) {
                sections.add(section);
            }
        }
    }

}
