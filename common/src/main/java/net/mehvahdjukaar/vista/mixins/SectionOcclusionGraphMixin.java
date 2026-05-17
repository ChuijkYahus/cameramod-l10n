package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.logging.LogUtils;
import net.mehvahdjukaar.vista.common.chunk_tracking.ExtraChunkViewData;
import net.mehvahdjukaar.vista.common.chunk_tracking.IPinnableRenderSection;
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

    @WrapOperation(
            method = "isInViewDistance",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkTrackingView;isInViewDistance(IIIII)Z"))
    private boolean vista$allowPinnedChunk(int centerX, int centerZ, int viewDistance, int x, int z,
                                           Operation<Boolean> original) {
        return original.call(centerX, centerZ, viewDistance, x, z)
                || ExtraChunkViewData.CLIENT_INSTANCE.containsChunk(x, z);
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
                Object node = vista$createNode(section);
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
