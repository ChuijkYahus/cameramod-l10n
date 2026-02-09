package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.common.tv.IntAnimationState;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;

public class WebUrlVideoSource implements IVideoSource {


    @Override
    public @NotNull VertexConsumer getVideoFrameBuilder(float partialTick, MultiBufferSource buffer, boolean shouldUpdate, int screenSize, int pixelEffectRes, int videoAnimationTick, boolean paused, IntAnimationState switchAnim, IntAnimationState staticAnim) {
        return null;
    }
}
