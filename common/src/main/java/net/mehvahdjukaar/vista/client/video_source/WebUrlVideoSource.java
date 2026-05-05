package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.client.web.FrameLookup;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
import net.mehvahdjukaar.vista.client.textures.WebTexture;
import net.mehvahdjukaar.vista.client.textures.WebTexturesManager;
import net.mehvahdjukaar.vista.common.tv.IntAnimationState;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;

public class WebUrlVideoSource implements IVideoSource {
    private final String url;
    private final WebTexture texture;

    public WebUrlVideoSource(String url) {
        this.url = url;
        this.texture = WebTexturesManager.requestWebTexture(url);
    }

    public String getUrl() {
        return url;
    }

    @Override
    public @NotNull VertexConsumer getVideoFrameBuilder(float partialTick, MultiBufferSource buffer,
                                                        boolean shouldUpdate, int screenSize, int pixelEffectRes,
                                                        int videoAnimationTick, boolean paused,
                                                        IntAnimationState switchAnim, IntAnimationState staticAnim) {
        if (texture.isFailed()) {
            return TvScreenVertexConsumers.getNoiseVC(buffer, pixelEffectRes, switchAnim);
        }

        double seconds = (videoAnimationTick + partialTick) / 20.0;
        FrameLookup lookup = texture.uploadFrameAtTime(seconds);
        if (lookup.state() == FrameLookup.State.FAILED || lookup.state() == FrameLookup.State.CLOSED) {
            return TvScreenVertexConsumers.getNoiseVC(buffer, pixelEffectRes, switchAnim);
        }
        if (!lookup.hasFrame()) {
            return TvScreenVertexConsumers.getBarsVC(buffer, pixelEffectRes, paused, switchAnim);
        }
        return TvScreenVertexConsumers.getWebVC(buffer, texture, pixelEffectRes, paused, switchAnim, staticAnim);
    }
}
