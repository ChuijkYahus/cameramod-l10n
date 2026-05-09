package net.mehvahdjukaar.vista.client.textures;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.vista.client.web.FFmpegMediaSession;
import net.mehvahdjukaar.vista.client.web.MediaFrame;
import net.mehvahdjukaar.vista.client.web.MediaStatus;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class FFmpegWebTexture extends DynamicTexture implements IWebTexture {
    private final FFmpegMediaSession session;
    private final ResourceLocation textureLocation;
    @Nullable
    private MediaFrame lastOriginalFrame;
    private boolean wasFirstUploaded = false;

    public FFmpegWebTexture(ResourceLocation textureLocation, FFmpegMediaSession session, int width, int height) {
        super(width, height, false);
        this.session = session;
        this.textureLocation = textureLocation;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return textureLocation;
    }

    @Override
    public void close() {
        //pixels are not closed here but by media frame
        this.releaseId();
    }

    @Override
    public MediaStatus uploadFrameAtTime(int ticks, float deltaTime, boolean paused) {
        double seconds = (ticks + deltaTime) / 20.0;

        var lookup = session.lookupFrame(seconds);
        MediaFrame frame = lookup.frame();
        if (frame != null && frame != this.lastOriginalFrame) {
            uploadOnRenderThread(frame.image());
            this.lastOriginalFrame = frame;
        }
        if (!wasFirstUploaded && lookup.state().isGood()) {
            return MediaStatus.LOADING;
        }
        return lookup.state();
    }

    private void uploadOnRenderThread(NativeImage newPixels) {
        Runnable upload = () -> {
            NativeImage oldPixels = this.pixels;
            this.pixels = newPixels;
            if (oldPixels == null) {
                TextureUtil.prepareImage(this.getId(), newPixels.getWidth(), newPixels.getHeight());
            }
            this.upload();
            wasFirstUploaded = true;
        };
        if (RenderSystem.isOnRenderThread()) {
            upload.run();
        } else {
            RenderSystem.recordRenderCall(upload::run);
        }
    }

}
