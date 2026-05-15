package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.vista.client.renderer.PinnedSections;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Allows pinned RenderSections to compile even when their real chunk neighbors
 * are not loaded. Without this, RebuildTask cancels itself if hasAllNeighbors()
 * returns false, and the section never gets geometry.
 *
 * NOTE: Do NOT use @Shadow on the final `index` field. Mixin merges the initializer
 * (= 0) into RenderSection.<init>, zeroing every section's index and breaking
 * SectionToNodeMap. Access the field via a cast instead.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public class RenderSectionMixin {

    @ModifyReturnValue(method = "hasAllNeighbors", at = @At("RETURN"))
    private boolean vista$pinnedAlwaysHasNeighbors(boolean original) {
        int idx = ((SectionRenderDispatcher.RenderSection) (Object) this).index;
        return original || PinnedSections.isPinned(idx);
    }
}
