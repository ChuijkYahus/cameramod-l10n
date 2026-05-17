package net.mehvahdjukaar.vista.common.chunk_tracking;

import net.mehvahdjukaar.vista.mixins.RenderSectionMixin;

/**
 * Duck-typed interface injected into {@code SectionRenderDispatcher.RenderSection}
 * by {@link RenderSectionMixin}. Stores whether this section was created as a
 * "pinned" extra-zone slot in {@link net.minecraft.client.renderer.ViewArea}.
 *
 * Keeping the flag on the section itself eliminates the global {@code PinnedSections}
 * set; ownership is fully scoped to the ViewArea that created the section.
 */
public interface IPinnableRenderSection {
    boolean vista$isPinned();
    void vista$setPinned(boolean pinned);
}
