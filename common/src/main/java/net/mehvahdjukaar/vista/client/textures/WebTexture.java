package net.mehvahdjukaar.vista.client.textures;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.vista.client.web.FrameLookup;
import net.mehvahdjukaar.vista.client.web.MediaFrame;
import net.mehvahdjukaar.vista.client.web.MediaSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class WebTexture extends DynamicTexture implements Dumpable {

    private final ResourceLocation textureLocation;
    private final MediaSession session;
    @Nullable
    private MediaFrame uploadedFrame;
    private int width = -1;
    private int height = -1;

    public static CompletableFuture<WebTexture> createFuture(String urlString, ResourceLocation textureLocation, MediaSession session) {

    }

    public WebTexture(ResourceLocation textureLocation, NativeImage firstFrame, MediaSession session) {
        super(firstFrame);
        this.session = session;
        this.textureLocation = textureLocation;
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

    public boolean isReady() {
        return session.isReady();
    }

    public boolean isFailed() {
        return session.isFailed();
    }

    public FrameLookup uploadFrameAtTime(double seconds) {
        FrameLookup lookup = session.lookupFrame(seconds);
        MediaFrame frame = lookup.frame();
        if (frame == null || frame == uploadedFrame || frame.image() == null) {
            return lookup;
        }
        uploadedFrame = frame;
        uploadOnRenderThread(frame.image());
        return lookup;
    }

    private void uploadOnRenderThread(NativeImage image) {
        Runnable upload = () -> {
            this.setPixels(image);
            if (this.width != image.getWidth() || this.height != image.getHeight()) {
                this.width = image.getWidth();
                this.height = image.getHeight();
                TextureUtil.prepareImage(this.getId(), this.width, this.height);
            }
            this.upload();
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

}
