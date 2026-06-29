package net.mehvahdjukaar.vista.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;

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
