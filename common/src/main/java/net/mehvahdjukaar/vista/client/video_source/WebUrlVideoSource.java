package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;

public class WebUrlVideoSource implements IVideoSource {

    @Override
    public VertexConsumer getVideoFrameBuilder(float partialTick, MultiBufferSource buffer, boolean shouldUpdate, int screenSize, int pixelEffectRes, int videoAnimationTick, int switchAnim, float staticAnim) {
        return null;
    }
}
