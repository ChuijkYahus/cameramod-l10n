package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.vista.common.IPinnableRenderSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Injects a {@code vista$pinned} flag into every RenderSection.
 * {@link ViewAreaMixin} sets the flag when it appends the extra-zone slots.
 * The flag is then used here and in {@link SectionOcclusionGraphMixin} to identify
 * pinned sections without any global state.
 *
 * NOTE: Do NOT @Shadow the final `index` field — Mixin merges the initializer
 * (= 0) into every RenderSection constructor, zeroing all indices and breaking
 * SectionToNodeMap.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public class RenderSectionMixin implements IPinnableRenderSection {

    @Unique
    private boolean vista$pinned = false;

    @Override
    public boolean vista$isPinned() {
        return vista$pinned;
    }

    @Override
    public void vista$setPinned(boolean pinned) {
        this.vista$pinned = pinned;
    }

    @ModifyReturnValue(method = "hasAllNeighbors", at = @At("RETURN"))
    private boolean vista$pinnedAlwaysHasNeighbors(boolean original) {
        return original || vista$pinned;
    }
}
