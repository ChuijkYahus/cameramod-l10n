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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class WebTexture extends AbstractTexture implements Dumpable {
    private final String urlString;
    private final WebTexturesManager.WebVideoSession session;
    private final ResourceLocation textureLocation;

    @Nullable
    private NativeImage cpuImage;
    @Nullable
    private MediaFrame uploadedFrame;

    WebTexture(String urlString, ResourceLocation textureLocation, WebTexturesManager.WebVideoSession session) {
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
        NativeImage image = toNativeImage(frame.image());
        uploadOnRenderThread(image);
        return true;
    }

    private void uploadOnRenderThread(NativeImage image) {
        Runnable upload = () -> {
            NativeImage oldImage = this.cpuImage;
            this.cpuImage = image;
            if (cpuImage != null){
            if(oldImage == null){
                TextureUtil.prepareImage(this.getId(), this.cpuImage.getWidth(), this.cpuImage.getHeight());
            }
             this.upload();
            }
            if (oldImage != null && oldImage != image) {
                oldImage.close();
            }
        };
        if (RenderSystem.isOnRenderThread()) {
            upload.run();
        } else {
            RenderSystem.recordRenderCall(upload::run);
        }
    }

    private static NativeImage toNativeImage(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        NativeImage out = new NativeImage(NativeImage.Format.RGBA, width, height, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = src.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                out.setPixelRGBA(x, y, abgr);
            }
        }
        return out;
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
        if (this.cpuImage != null) {
            this.cpuImage.close();
            this.releaseId();
            this.cpuImage = null;
        }
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
