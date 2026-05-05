package net.mehvahdjukaar.vista.client.textures;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.web.MediaFrame;
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
    private final String urlString;
    private final WebTexturesManager.WebMediaSession session;
    private final ResourceLocation textureLocation;

    @Nullable
    private NativeImage cpuImage;
    @Nullable
    private MediaFrame uploadedFrame;
    private int width = -1;
    private int height = -1;

    WebTexture(String urlString, ResourceLocation textureLocation, WebTexturesManager.WebMediaSession session) {
        this.urlString = urlString;
        this.textureLocation = textureLocation;
        this.session = session;
    }

    public void register() {
        Minecraft.getInstance().getTextureManager().register(this.textureLocation, this);
    }

    public void unregister() {
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        AbstractTexture t = tm.getTexture(this.textureLocation);
        if (t == this) {
            tm.release(this.textureLocation);
        }
    }

    public ResourceLocation getResourceLocation() {
        return textureLocation;
    }

    public String getUrlString() {
        return urlString;
    }

    public boolean isReady() {
        return session.isReady();
    }

    public boolean isFailed() {
        return session.isFailed();
    }

    public boolean uploadFrameAtTime(double seconds) {
        MediaFrame frame = session.getFrameAtTime(seconds);
        if (frame == null || frame == uploadedFrame || frame.image() == null) {
            return frame != null;
        }
        uploadedFrame = frame;
        uploadOnRenderThread(frame.image());
        return true;
    }

    private void uploadOnRenderThread(NativeImage image) {
        Runnable upload = () -> {
            this.cpuImage = image;
            if (this.cpuImage != null) {
                if (this.width != this.cpuImage.getWidth() || this.height != this.cpuImage.getHeight()) {
                    this.width = this.cpuImage.getWidth();
                    this.height = this.cpuImage.getHeight();
                    TextureUtil.prepareImage(this.getId(), this.width, this.height);
                }
                this.upload();
            }
        };
        if (RenderSystem.isOnRenderThread()) {
            upload.run();
        } else {
            RenderSystem.recordRenderCall(upload::run);
        }
    }

    @Override
    public void load(ResourceManager resourceManager) {
    }

    public void upload() {
        if (this.cpuImage != null) {
            this.bind();
            this.cpuImage.upload(0, 0, 0, false);
        } else {
            VistaMod.LOGGER.warn("Trying to upload disposed texture {}", this.getId());
        }
    }

    @Override
    public void close() {
        this.cpuImage = null;
        this.uploadedFrame = null;
        this.releaseId();
    }

    @Override
    public void dumpContents(ResourceLocation resourceLocation, Path p_path) throws IOException {
        if (this.cpuImage != null) {
            String s = resourceLocation.toDebugFileName() + ".png";
            Path path = p_path.resolve(s);
            this.cpuImage.writeToFile(path);
        }
    }
}
