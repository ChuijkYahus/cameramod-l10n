package net.mehvahdjukaar.vista.common.chunk_tracking;

/**
 * Injected onto {@link net.minecraft.client.renderer.ViewArea} to allow
 * rebuilding only the pinned (extra-zone) section slots without a full
 * {@code allChanged()} rebuild of the entire renderer.
 */
public interface IViewAreaExt {
    /** Replaces the pinned section slots for the current zone set. */
    void vista$rebuildPinnedSections();

    /**
     * Marks the appended pinned section at the exact section coordinates dirty so it
     * recompiles. The normal {@code ViewArea.setDirty} is {@code floorMod}-indexed and
     * can only ever address the torus grid, never the appended pinned slots, so block/
     * light updates in a far zone chunk must be routed here to refresh their mesh.
     */
    void vista$setPinnedSectionDirty(int secX, int secY, int secZ, boolean reRenderOnMainThread);

    /**
     * True if a pinned section exists at the exact section coordinates and has been
     * compiled. Used to satisfy {@code LevelRenderer.isSectionCompiled} — the entity
     * render gate — which otherwise resolves the section via the {@code floorMod} torus
     * and never finds pinned sections, so far-chunk entities are skipped.
     */
    boolean vista$isPinnedSectionCompiled(int secX, int secY, int secZ);
}
