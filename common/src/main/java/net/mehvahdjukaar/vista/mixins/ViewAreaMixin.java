package net.mehvahdjukaar.vista.mixins;

import com.mojang.logging.LogUtils;
import net.mehvahdjukaar.vista.common.chunk_tracking.ExtraChunkViewData;
import net.mehvahdjukaar.vista.common.chunk_tracking.ILevelRendererExt;
import net.mehvahdjukaar.vista.common.chunk_tracking.IPinnableRenderSection;
import net.mehvahdjukaar.vista.common.chunk_tracking.IViewAreaExt;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;

@Mixin(ViewArea.class)
public class ViewAreaMixin implements IViewAreaExt {

    @Unique
    private static final Logger VISTA_LOGGER = LogUtils.getLogger();

    @Shadow protected Level level;
    @Shadow protected int sectionGridSizeY;
    @Shadow public SectionRenderDispatcher.RenderSection[] sections;

    /** Stored dispatcher so we can create new sections outside the constructor. */
    @Unique private SectionRenderDispatcher vista$dispatcher;

    /**
     * Index at which the pinned (extra-zone) sections begin in {@link #sections}.
     * Captured right before we append them so we always know where the normal
     * torus grid ends.
     */
    @Unique private int vista$normalSectionCount = -1;

    // ── Constructor hook ───────────────────────────────────────────────────────

    @Inject(method = "createSections", at = @At("HEAD"))
    private void vista$captureDispatcher(SectionRenderDispatcher dispatcher, CallbackInfo ci) {
        this.vista$dispatcher = dispatcher;
    }

    /**
     * After the normal torus grid is built, append one extra Y-column of RenderSections
     * for every chunk position registered in ExtraChunkViewData. These slots sit beyond
     * the torus index range so repositionCamera never moves them.
     */
    @Inject(method = "createSections", at = @At("TAIL"))
    private void vista$appendPinnedSections(SectionRenderDispatcher dispatcher, CallbackInfo ci) {
        this.vista$normalSectionCount = this.sections.length;
        vista$buildPinnedSections(dispatcher);
    }

    // ── IViewAreaExt ───────────────────────────────────────────────────────────

    /**
     * Replaces the pinned section slots in-place without destroying any existing
     * compiled section geometry. Called from {@link ILevelRendererExt} instead of
     * the heavy {@code allChanged()} rebuild when zone data changes.
     */
    @Override
    public void vista$rebuildPinnedSections() {
        if (vista$dispatcher == null || vista$normalSectionCount < 0) return;

        // Release GPU buffers of old pinned sections so they don't leak.
        for (int i = vista$normalSectionCount; i < this.sections.length; i++) {
            if (this.sections[i] != null) {
                this.sections[i].releaseBuffers();
            }
        }

        // Truncate to normal grid, then append fresh pinned sections.
        this.sections = Arrays.copyOf(this.sections, vista$normalSectionCount);
        vista$buildPinnedSections(vista$dispatcher);
    }

    // ── Shared builder ─────────────────────────────────────────────────────────

    @Unique
    private void vista$buildPinnedSections(SectionRenderDispatcher dispatcher) {
        Set<ChunkPos> extraChunks = ExtraChunkViewData.CLIENT_INSTANCE.getAllChunks();
        if (extraChunks.isEmpty()) return;

        int base = this.sections.length;
        int extraSlots = extraChunks.size() * this.sectionGridSizeY;
        SectionRenderDispatcher.RenderSection[] extended = Arrays.copyOf(this.sections, base + extraSlots);

        int slotOffset = 0;
        for (ChunkPos chunkPos : extraChunks) {
            int blockX = chunkPos.getMinBlockX();
            int blockZ = chunkPos.getMinBlockZ();
            for (int yIndex = 0; yIndex < this.sectionGridSizeY; yIndex++) {
                int newIndex = base + slotOffset;
                int yOrigin = this.level.getMinBuildHeight() + yIndex * 16;
                SectionRenderDispatcher.RenderSection section = vista$createRenderSection(dispatcher, newIndex, blockX, yOrigin, blockZ);
                if (section == null) {
                    VISTA_LOGGER.error("Failed to create pinned RenderSection at ({}, {}, {})", blockX, yOrigin, blockZ);
                } else {
                    extended[newIndex] = section;
                    if (section instanceof IPinnableRenderSection ps) {
                        ps.vista$setPinned(true);
                    }
                }
                slotOffset++;
            }
        }

        this.sections = extended;
    }

    @Unique
    private static SectionRenderDispatcher.RenderSection vista$createRenderSection(
            SectionRenderDispatcher dispatcher, int index, int x, int y, int z) {
        try {
            Class<?> clazz = Class.forName("net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection");
            Constructor<?> ctor = clazz.getDeclaredConstructor(
                    SectionRenderDispatcher.class, int.class, int.class, int.class, int.class);
            ctor.setAccessible(true);
            return (SectionRenderDispatcher.RenderSection) ctor.newInstance(dispatcher, index, x, y, z);
        } catch (ReflectiveOperationException e) {
            VISTA_LOGGER.error("Could not instantiate pinned RenderSection", e);
            return null;
        }
    }
}
