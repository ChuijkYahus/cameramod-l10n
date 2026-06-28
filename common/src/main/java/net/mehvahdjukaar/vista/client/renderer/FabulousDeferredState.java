package net.mehvahdjukaar.vista.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;

/**
 * Snapshot of {@link LevelRenderer}'s Fabulous-graphics deferred-rendering state (the transparency
 * post-chain and the per-pass render targets), used to force the plain FANCY path during our nested
 * off-screen level renders (mirror reflections / TV feeds).
 *
 * <p>Fabulous can't compose its separate translucency/weather/etc. targets into our off-screen
 * canvas. Worse, even on the inline (non-fabulous) code path {@code renderLevel} still calls
 * {@code if (someTarget != null) someTarget.clear()} for several of these targets (translucent,
 * weather, item-entity), and {@link RenderTarget#clear} ends by binding framebuffer 0 — stealing
 * the write binding away from our canvas, so everything drawn afterwards (entities, block entities
 * incl. nested mirrors/TVs, particles, translucent blocks) would render into the window instead of
 * the texture. When Fabulous is genuinely off all these fields are null and those clears are
 * skipped; {@link #captureAndDisable} reproduces that state exactly, and {@link #restore} puts it
 * back. Pair with the {@code Minecraft.useShaderTransparency() -> false} override that gates the
 * render-type target binding (see MinecraftMixin).
 *
 * <p>Returned snapshot is held in a local across the render so re-entrant nested renders each
 * restore their own values. All fields are access-widened (see vista.accesswidener).
 */
public record FabulousDeferredState(
        PostChain transparencyChain,
        RenderTarget translucentTarget,
        RenderTarget itemEntityTarget,
        RenderTarget weatherTarget,
        RenderTarget cloudsTarget,
        RenderTarget entityTarget) {

    public static FabulousDeferredState captureAndDisable(LevelRenderer lr) {
        FabulousDeferredState state = new FabulousDeferredState(
                lr.transparencyChain, lr.translucentTarget, lr.itemEntityTarget,
                lr.weatherTarget, lr.cloudsTarget, lr.entityTarget);
        lr.transparencyChain = null;
        lr.translucentTarget = null;
        lr.itemEntityTarget = null;
        lr.weatherTarget = null;
        lr.cloudsTarget = null;
        lr.entityTarget = null;
        return state;
    }

    public void restore(LevelRenderer lr) {
        lr.transparencyChain = transparencyChain;
        lr.translucentTarget = translucentTarget;
        lr.itemEntityTarget = itemEntityTarget;
        lr.weatherTarget = weatherTarget;
        lr.cloudsTarget = cloudsTarget;
        lr.entityTarget = entityTarget;
    }
}
