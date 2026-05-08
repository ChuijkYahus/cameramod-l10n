package net.mehvahdjukaar.vista.client.textures;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.vista.client.web.MediaFrame;
import net.mehvahdjukaar.vista.client.web.MediaSession;
import net.mehvahdjukaar.vista.client.web.MediaState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class WebTexture extends DynamicTexture {
    private final ResourceLocation textureLocation;
    private final MediaSession session;
    private final int screenSize;
    @Nullable
    private MediaFrame lastOriginalFrame;
    private boolean wasFirstUploaded = false;

    public WebTexture(ResourceLocation textureLocation, MediaSession session, int screenSize) {
        super(screenSize, screenSize, false);
        this.textureLocation = textureLocation;
        this.session = session;
        this.screenSize = screenSize;
    }

    public ResourceLocation getResourceLocation() {
        return textureLocation;
    }

    public void register() {
        Minecraft.getInstance().getTextureManager().register(this.textureLocation, this);
    }

    public void unregister() {
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        AbstractTexture texture = tm.getTexture(this.textureLocation);
        if (texture == this) {
            tm.release(this.textureLocation);
        }
    }

    public MediaState uploadFrameAtTime(double seconds) {
        MediaState.Pair lookup = session.lookupFrame(seconds);
        MediaFrame frame = lookup.frame();
        if (frame != null && frame != this.lastOriginalFrame) {
            NativeImage scaledImage = frame.scaledImage(screenSize, screenSize);
            uploadOnRenderThread(scaledImage);
            this.lastOriginalFrame = frame;
        }
        if (!wasFirstUploaded) return MediaState.LOADING;
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

    @Override
    public void close() {
        //pixels are not closed here but by media frame
        this.releaseId();
    }
}
