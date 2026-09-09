package net.mehvahdjukaar.vista.client.chunk_tracking;

public interface IViewAreaExt {
    void vista$rebuildPinnedSections();

    void vista$setPinnedSectionDirty(int secX, int secY, int secZ, boolean reRenderOnMainThread);

    boolean vista$isPinnedSectionCompiled(int secX, int secY, int secZ);
}
