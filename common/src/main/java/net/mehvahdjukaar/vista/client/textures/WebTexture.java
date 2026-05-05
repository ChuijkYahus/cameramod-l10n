package net.mehvahdjukaar.vista.client.textures;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.vista.client.web.MediaFrame;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class WebTexture extends AbstractTexture implements Dumpable {
    private final String urlString;
    private final WebTexturesManager.WebVideoSession session;
    private final ResourceLocation resourceLocation;

    @Nullable
    private NativeImage cpuImage;
    @Nullable
    private MediaFrame uploadedFrame;
    private int width = -1;
    private int height = -1;

    WebTexture(String urlString, ResourceLocation resourceLocation, WebTexturesManager.WebVideoSession session) {
        this.urlString = urlString;
        this.resourceLocation = resourceLocation;
        this.session = session;
    }

    public ResourceLocation getResourceLocation() {
        return resourceLocation;
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
            this.upload(image);
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

    private void upload(NativeImage image) {
        if (this.width != image.getWidth() || this.height != image.getHeight()) {
            this.width = image.getWidth();
            this.height = image.getHeight();
            TextureUtil.prepareImage(this.getId(), width, height);
        }
        image.upload(0, 0, 0, 0, 0, width, height, true, false);
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
    public void load(ResourceManager resourceManager) throws IOException {
    }

    @Override
    public void dumpContents(ResourceLocation resourceLocation, Path path) throws IOException {
        if (cpuImage == null) return;
        String string = resourceLocation.toDebugFileName() + ".png";
        Path path2 = path.resolve(string);
        cpuImage.writeToFile(path2);
    }

    @Override
    public void close() {
        super.close();
        if (cpuImage != null) {
            cpuImage.close();
            cpuImage = null;
        }
    }
}
