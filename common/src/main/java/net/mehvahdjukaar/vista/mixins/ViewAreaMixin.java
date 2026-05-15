package net.mehvahdjukaar.vista.mixins;

import com.mojang.logging.LogUtils;
import net.mehvahdjukaar.vista.common.ExtraChunkViewData;
import net.mehvahdjukaar.vista.client.renderer.PinnedSections;
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
public class ViewAreaMixin {

    @Unique
    private static final Logger VISTA_LOGGER = LogUtils.getLogger();

    @Shadow protected Level level;
    @Shadow protected int sectionGridSizeY;
    @Shadow public SectionRenderDispatcher.RenderSection[] sections;

    /**
     * After the normal torus grid is built, append one extra Y-column of RenderSections
     * for every chunk position registered in ExtraChunkViewData. These slots sit beyond
     * the torus index range so repositionCamera never moves them.
     */
    @Inject(method = "createSections", at = @At("TAIL"))
    private void vista$appendPinnedSections(SectionRenderDispatcher dispatcher, CallbackInfo ci) {
        PinnedSections.clear();

        Set<ChunkPos> extraChunks = ExtraChunkViewData.getAllChunks();
        if (extraChunks.isEmpty()) return;

        int normalSize = this.sections.length;
        int extraSlots = extraChunks.size() * this.sectionGridSizeY;
        SectionRenderDispatcher.RenderSection[] extended = Arrays.copyOf(this.sections, normalSize + extraSlots);

        int slotOffset = 0;
        for (ChunkPos chunkPos : extraChunks) {
            int blockX = chunkPos.getMinBlockX();
            int blockZ = chunkPos.getMinBlockZ();
            for (int yIndex = 0; yIndex < this.sectionGridSizeY; yIndex++) {
                int newIndex = normalSize + slotOffset;
                int yOrigin = this.level.getMinBuildHeight() + yIndex * 16;
                SectionRenderDispatcher.RenderSection section = vista$createRenderSection(dispatcher, newIndex, blockX, yOrigin, blockZ);
                if (section == null) {
                    VISTA_LOGGER.error("Failed to create pinned RenderSection at ({}, {}, {})", blockX, yOrigin, blockZ);
                } else {
                    extended[newIndex] = section;
                    PinnedSections.register(newIndex);
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
