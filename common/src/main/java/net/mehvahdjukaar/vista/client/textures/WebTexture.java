package net.mehvahdjukaar.vista.client.textures;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.web.MediaFrame;
import net.mehvahdjukaar.vista.client.web.MediaSession;
import net.mehvahdjukaar.vista.client.web.MediaState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

public class WebTexture extends AbstractTexture implements Dumpable {
    private final ResourceLocation textureLocation;
    private final int screenSize;
    private final MediaSession session;


    @Nullable
    private NativeImage pixels;
    @Nullable
    private MediaFrame lastOriginalFrame;

    public WebTexture(ResourceLocation textureLocation, MediaSession session, int screenSize) {
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
        boolean wasUploaded = pixels != null;
        MediaFrame frame = lookup.frame();
        if (frame != null && frame != this.lastOriginalFrame) {
            uploadOnRenderThread(frame.scaledImage(screenSize, screenSize));
            this.lastOriginalFrame = frame;
        }
        if (!wasUploaded) return MediaState.LOADING;
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
        };
        if (RenderSystem.isOnRenderThread()) {
            upload.run();
        } else {
            RenderSystem.recordRenderCall(upload::run);
        }
    }


    public void upload() {
        if (this.pixels != null) {
            this.bind();
            this.pixels.upload(0, 0, 0, false);
        } else {
            VistaMod.LOGGER.warn("Trying to upload disposed texture {}", this.getId());
        }
    }

    @Override
    public void load(ResourceManager resourceManager) throws IOException {

    }

    @Override
    public void close() {
        if (this.pixels != null) {
            this.pixels.close();
            this.releaseId();
            this.pixels = null;
        }
    }

    @Override
    public void dumpContents(ResourceLocation resourceLocation, Path p_path) throws IOException {
        if (this.pixels != null) {
            String s = resourceLocation.toDebugFileName() + ".png";
            Path path = p_path.resolve(s);
            this.pixels.writeToFile(path);
        }
    }
}
