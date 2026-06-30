package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import net.mehvahdjukaar.vista.common.chunk_tracking.ILevelRendererExt;
import net.mehvahdjukaar.vista.common.chunk_tracking.IViewAreaExt;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.mehvahdjukaar.vista.integration.supernatural.SupernaturalCompat;
import net.mehvahdjukaar.vista.integration.vampirism.VampirismCompat;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin implements ILevelRendererExt {

    @Shadow
    public SectionOcclusionGraph sectionOcclusionGraph;

    @Shadow @Nullable
    public ViewArea viewArea;

    /**
     * Rebuilds only the pinned (extra-zone) render section slots, leaving all
     * existing compiled chunk geometry intact. Replaces the heavy
     * {@code allChanged()} call for zone-data changes.
     */
    @Unique
    @Override
    public void vista$refreshPinnedSections() {
        if (viewArea instanceof IViewAreaExt va) {
            va.vista$rebuildPinnedSections();
        }
        if (sectionOcclusionGraph != null) {
            sectionOcclusionGraph.invalidate();
        }
        // Also invalidate all feed graphs so they redo their BFS and pick up
        // the newly created pinned sections on the next feed render.
        VistaLevelRenderer.invalidateManagedGraphs();
    }

    @ModifyReturnValue(method = "shouldShowEntityOutlines", at = @At(value = "RETURN"))
    public boolean vista$disableEntityOutlines(boolean original) {
        if (VistaLevelRenderer.isRenderingLiveFeed()) {
            return false;
        }
        return original;
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z"),
            require = 1)
    public boolean vista$isCameraDetached(boolean original) {
        if (VistaLevelRenderer.isRenderingLiveFeed()) {
            return true;
        }
        return original;
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getEntity()Lnet/minecraft/world/entity/Entity;",
            ordinal = 3), require = 1)
    public Entity vista$getActualPlayer(Entity original, @Local(ordinal = 0) Entity entity) {
        if (VistaLevelRenderer.isRenderingLiveFeed() && entity instanceof LocalPlayer) {
            return entity;
        }
        return original;
    }

    /**
     * Hide entities that shouldn't appear on reflective/recorded surfaces (e.g. vampires) while
     * rendering a mirror reflection or a camera/TV feed. Mirrors use the
     * {@code cant_see_through_mirror} tag, feeds use {@code cant_see_through_tv}; vampires from mods
     * (queried via {@link VampirismCompat} and {@link SupernaturalCompat}) are hidden from both. The
     * main view is never affected.
     */
    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void vista$hideMirrorInvisibleEntities(Entity entity, double camX, double camY, double camZ,
                                                   float partialTick, PoseStack poseStack,
                                                   MultiBufferSource bufferSource, CallbackInfo ci) {
        boolean mirror = VistaLevelRenderer.isRenderingMirrorReflection();
        boolean tv = VistaLevelRenderer.isRenderingCameraFeed();
        if (!mirror && !tv) return;

        boolean hidden = (mirror && entity.getType().is(VistaMod.CANT_SEE_THROUGH_MIRROR))
                || (tv && entity.getType().is(VistaMod.CANT_SEE_THROUGH_TV));

        // Vampire players are still minecraft:player, and some vampire mods flag mobs through an API
        // rather than a tag, so the entity-type tags above can't catch either — ask each vampire compat.
        // The mod-loaded flags gate the calls so the compat impls (which reference mod types) are never
        // classloaded when the mod is absent. Supernatural handles both its mobs and players; Vampirism
        // only knows about players.
        if (!hidden && entity instanceof LivingEntity living) {
            hidden = (CompatHandler.SUPERNATURAL && SupernaturalCompat.isVampire(living))
                    || (CompatHandler.VAMPIRISM && living instanceof Player player && VampirismCompat.isVampire(player));
        }

        if (hidden) ci.cancel();
    }

    @Inject(method = "setupRender", at = @At("HEAD"), cancellable = true)
    public void vista$alterSetupRender(Camera camera, Frustum frustum, boolean hasCapturedFrustum, boolean isSpectator, CallbackInfo ci) {
        if (VistaLevelRenderer.onSetupRenderer((LevelRenderer) (Object) this, camera, frustum, hasCapturedFrustum, isSpectator)) {
            ci.cancel();
        }
    }

    @Inject(method = "onChunkLoaded", at = @At("HEAD"))
    public void vista$onChunkLoaded(ChunkPos chunkPos, CallbackInfo ci) {
        VistaLevelRenderer.onChunkLoaded(chunkPos, this.sectionOcclusionGraph);
    }

    @Inject(method = "addRecentlyCompiledSection", at = @At("HEAD"))
    public void vista$onRecentlyCompiledSection(SectionRenderDispatcher.RenderSection renderSection, CallbackInfo ci) {
        VistaLevelRenderer.onRecentlyCompiledSection(renderSection, this.sectionOcclusionGraph);
    }

    /**
     * A block/light change calls {@code setSectionDirty} -> {@code ViewArea.setDirty}, which is
     * {@code floorMod}-indexed into the normal torus and can never reach the appended pinned
     * sections — so far zone chunks compile once and their mesh freezes (water/sculk/piston-base
     * never update). Route the dirty to the pinned section at the exact coordinates as well.
     */
    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
    private void vista$dirtyPinnedSection(int sectionX, int sectionY, int sectionZ, boolean reRenderOnMainThread, CallbackInfo ci) {
        if (viewArea instanceof IViewAreaExt va
                && VistaModClient.CLIENT_EXTRA_CHUNK_VIEW_DATA.containsChunk(sectionX, sectionZ)) {
            va.vista$setPinnedSectionDirty(sectionX, sectionY, sectionZ, reRenderOnMainThread);
        }
    }

    /**
     * Vanilla skips rendering any entity whose section "isn't compiled", resolved via
     * {@code ViewArea.getRenderSectionAt} (floorMod torus) — which can't see pinned
     * sections, so far zone-chunk entities (armor stands, items, mobs) never draw even
     * though they exist on the client. Satisfy the gate from the pinned section instead.
     */
    @ModifyReturnValue(method = "isSectionCompiled", at = @At("RETURN"))
    private boolean vista$pinnedSectionCompiled(boolean original, BlockPos pos) {
        if (original) return true;
        int secX = SectionPos.blockToSectionCoord(pos.getX());
        int secZ = SectionPos.blockToSectionCoord(pos.getZ());
        if (viewArea instanceof IViewAreaExt va
                && VistaModClient.CLIENT_EXTRA_CHUNK_VIEW_DATA.containsChunk(secX, secZ)) {
            return va.vista$isPinnedSectionCompiled(secX, SectionPos.blockToSectionCoord(pos.getY()), secZ);
        }
        return original;
    }

    // F3+A (and any other reload path that funnels through allChanged) releases
    // every section VertexBuffer. Reset all cached feed states so they don't try
    // to draw closed buffers.
    @Inject(method = "allChanged", at = @At("TAIL"))
    public void vista$onAllChanged(CallbackInfo ci) {
        VistaLevelRenderer.onLevelRendererAllChanged();
    }
}
