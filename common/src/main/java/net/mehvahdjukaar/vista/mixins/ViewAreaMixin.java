package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.common.chunk_tracking.IPinnableRenderSection;
import net.mehvahdjukaar.vista.common.chunk_tracking.IViewAreaExt;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Mixin(ViewArea.class)
public class ViewAreaMixin implements IViewAreaExt {

    @Shadow
    @Final
    protected Level level;
    @Shadow
    protected int sectionGridSizeY;
    @Shadow
    public SectionRenderDispatcher.RenderSection[] sections;

    @Unique
    private SectionRenderDispatcher vista$dispatcher;
    @Unique
    private int vista$normalSectionCount = -1;
    @Unique
    private final Map<Long, SectionRenderDispatcher.RenderSection> vista$pinnedSectionsByPos = new HashMap<>();

    @Inject(method = "createSections", at = @At("HEAD"))
    private void vista$captureDispatcher(SectionRenderDispatcher dispatcher, CallbackInfo ci) {
        this.vista$dispatcher = dispatcher;
    }

    @Inject(method = "createSections", at = @At("TAIL"))
    private void vista$appendPinnedSections(SectionRenderDispatcher dispatcher, CallbackInfo ci) {
        this.vista$normalSectionCount = this.sections.length;
        vista$appendPinnedSections();
    }

    @Override
    public void vista$rebuildPinnedSections() {
        if (vista$dispatcher == null || vista$normalSectionCount < 0) return;

        for (int i = vista$normalSectionCount; i < this.sections.length; i++) {
            if (this.sections[i] != null) this.sections[i].releaseBuffers();
        }
        this.sections = Arrays.copyOf(this.sections, vista$normalSectionCount);
        vista$appendPinnedSections();
    }

    @Unique
    private void vista$appendPinnedSections() {
        this.vista$pinnedSectionsByPos.clear();
        Set<ChunkPos> zoneChunks = VistaModClient.CLIENT_EXTRA_CHUNK_VIEW_DATA.getAllChunks();
        if (zoneChunks.isEmpty()) return;

        int normalCount = this.sections.length;
        SectionRenderDispatcher.RenderSection[] sectionsWithPinned = Arrays.copyOf(this.sections, normalCount + zoneChunks.size() * this.sectionGridSizeY);

        int index = normalCount;
        for (ChunkPos chunkPos : zoneChunks) {
            for (int yIndex = 0; yIndex < this.sectionGridSizeY; yIndex++) {
                int blockY = this.level.getMinBuildHeight() + yIndex * 16;
                var section = vista$dispatcher.new RenderSection(index, chunkPos.getMinBlockX(), blockY, chunkPos.getMinBlockZ());
                ((IPinnableRenderSection) section).vista$setPinned(true);
                sectionsWithPinned[index] = section;
                this.vista$pinnedSectionsByPos.put(SectionPos.asLong(chunkPos.x, SectionPos.blockToSectionCoord(blockY), chunkPos.z), section);
                index++;
            }
        }
        this.sections = sectionsWithPinned;
    }

    @Override
    public void vista$setPinnedSectionDirty(int secX, int secY, int secZ, boolean reRenderOnMainThread) {
        var section = this.vista$pinnedSectionsByPos.get(SectionPos.asLong(secX, secY, secZ));
        if (section != null) section.setDirty(reRenderOnMainThread);
    }

    @Override
    public boolean vista$isPinnedSectionCompiled(int secX, int secY, int secZ) {
        var section = this.vista$pinnedSectionsByPos.get(SectionPos.asLong(secX, secY, secZ));
        return section != null && section.getCompiled() != SectionRenderDispatcher.CompiledSection.UNCOMPILED;
    }
}
