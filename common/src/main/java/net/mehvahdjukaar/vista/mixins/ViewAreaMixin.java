package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.client.chunk_tracking.IViewAreaExt;
import net.mehvahdjukaar.vista.client.chunk_tracking.PinnedSections;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    private PinnedSections vista$pinnedSections;

    @Inject(method = "createSections", at = @At("TAIL"))
    private void vista$appendPinnedSections(SectionRenderDispatcher dispatcher, CallbackInfo ci) {
        this.vista$pinnedSections = new PinnedSections(dispatcher, this.level, this.sectionGridSizeY, this.sections.length);
        this.sections = vista$pinnedSections.appendTo(this.sections);
    }

    @Override
    public void vista$rebuildPinnedSections() {
        if (vista$pinnedSections == null) return;
        this.sections = vista$pinnedSections.removeFrom(this.sections);
        this.sections = vista$pinnedSections.appendTo(this.sections);
    }

    @Override
    public void vista$setPinnedSectionDirty(int secX, int secY, int secZ, boolean reRenderOnMainThread) {
        var section = vista$pinnedSections == null ? null : vista$pinnedSections.get(secX, secY, secZ);
        if (section != null) section.setDirty(reRenderOnMainThread);
    }

    @Override
    public boolean vista$isPinnedSectionCompiled(int secX, int secY, int secZ) {
        var section = vista$pinnedSections == null ? null : vista$pinnedSections.get(secX, secY, secZ);
        return section != null && section.getCompiled() != SectionRenderDispatcher.CompiledSection.UNCOMPILED;
    }
}
