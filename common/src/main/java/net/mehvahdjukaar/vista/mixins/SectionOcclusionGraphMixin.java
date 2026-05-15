package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.logging.LogUtils;
import net.mehvahdjukaar.vista.common.ExtraChunkViewData;
import net.mehvahdjukaar.vista.client.renderer.PinnedSections;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.Direction;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Queue;

@Mixin(SectionOcclusionGraph.class)
public class SectionOcclusionGraphMixin {

    @Unique
    private static final Logger VISTA_LOGGER = LogUtils.getLogger();

    @Shadow
    private ViewArea viewArea;

    /**
     * Wrap the ChunkTrackingView.isInViewDistance call INSIDE isInViewDistance so we can
     * additionally pass sections at chunk (0,0). The wrapped call is the only thing that
     * decides view-distance gating; adding an OR here doesn't touch anything else.
     */
    @WrapOperation(
            method = "isInViewDistance",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkTrackingView;isInViewDistance(IIIII)Z"))
    private boolean vista$allowPinnedChunk(int centerX, int centerZ, int viewDistance, int x, int z,
                                           Operation<Boolean> original) {
        return original.call(centerX, centerZ, viewDistance, x, z) || ExtraChunkViewData.containsChunk(x, z);
    }

    /**
     * After the normal full-update seed (player section or ring), also seed every pinned
     * section as an additional isolated BFS root. The pinned sections live beyond the normal
     * torus range and will never be reached by normal propagation.
     */
    @SuppressWarnings("unchecked")
    @Inject(method = "initializeQueueForFullUpdate", at = @At("TAIL"))
    private void vista$seedPinnedSections(Camera camera, Queue nodeQueue, CallbackInfo ci) {
        if (this.viewArea == null) return;

        SectionRenderDispatcher.RenderSection[] sections = this.viewArea.sections;
        for (int pinnedIndex : PinnedSections.getAll()) {
            if (pinnedIndex < 0 || pinnedIndex >= sections.length) continue;
            SectionRenderDispatcher.RenderSection section = sections[pinnedIndex];
            if (section == null) continue;

            Object node = vista$createNode(section);
            if (node != null) {
                nodeQueue.add(node);
            }
        }
    }

    /**
     * After the normal frustum-culled sections are collected, unconditionally append
     * every pinned section to the visible list. This avoids any dependency on
     * Frustum.isVisible (which would cull them since they are far from the camera).
     * Pinned sections that are dirty will then be scheduled for compilation by
     * LevelRenderer.compileSections in the same frame.
     */
    @Inject(method = "addSectionsInFrustum", at = @At("TAIL"))
    private void vista$addPinnedSectionsToVisible(Frustum frustum,
            List<SectionRenderDispatcher.RenderSection> sections, CallbackInfo ci) {
        if (this.viewArea == null) return;
        SectionRenderDispatcher.RenderSection[] allSections = this.viewArea.sections;
        for (int idx : PinnedSections.getAll()) {
            if (idx < 0 || idx >= allSections.length) continue;
            SectionRenderDispatcher.RenderSection section = allSections[idx];
            if (section != null) {
                sections.add(section);
            }
        }
    }

    @Unique
    private static Object vista$createNode(SectionRenderDispatcher.RenderSection section) {
        try {
            Class<?> nodeClass = Class.forName("net.minecraft.client.renderer.SectionOcclusionGraph$Node");
            Constructor<?> ctor = nodeClass.getDeclaredConstructor(
                    SectionRenderDispatcher.RenderSection.class, Direction.class, int.class);
            ctor.setAccessible(true);
            return ctor.newInstance(section, null, 0);
        } catch (ReflectiveOperationException e) {
            VISTA_LOGGER.error("Could not create SectionOcclusionGraph.Node for pinned section", e);
            return null;
        }
    }
}
