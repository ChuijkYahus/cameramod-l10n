package net.mehvahdjukaar.vista.common.chunk_tracking;

/**
 * Injected onto {@link net.minecraft.client.renderer.ViewArea} to allow
 * rebuilding only the pinned (extra-zone) section slots without a full
 * {@code allChanged()} rebuild of the entire renderer.
 */
public interface IViewAreaExt {
    /** Replaces the pinned section slots for the current zone set. */
    void vista$rebuildPinnedSections();
}
