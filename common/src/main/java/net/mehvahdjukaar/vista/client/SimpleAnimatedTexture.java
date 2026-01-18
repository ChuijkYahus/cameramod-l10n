package net.mehvahdjukaar.vista.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class SimpleAnimatedTexture extends AbstractTexture {
    protected final ResourceLocation location;

    protected AnimationStripData stripData = AnimationStripData.EMPTY;

    public SimpleAnimatedTexture(ResourceLocation location) {
        this.location = location;
    }

    @Override
    public void load(ResourceManager resourceManager) throws IOException {
        SpriteContents spriteContents;
        if (this.location.getPath().endsWith(".gif")) {
            spriteContents = GifPathSpriteSource.readGif(resourceManager.getResourceOrThrow(this.location), this.location);
        } else {
            spriteContents = PngPathSpriteSource.readPng(resourceManager.getResourceOrThrow(this.location), this.location);
        }
        if (spriteContents == null) return;

        this.stripData = new AnimationStripData(
                spriteContents.width(),
                spriteContents.height(),
                spriteContents.getFrameCount()
        );

        NativeImage nativeImage = spriteContents.originalImage;
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> this.doLoad(nativeImage));
        } else {
            this.doLoad(nativeImage);
        }

    }

    private void doLoad(NativeImage image) {
        TextureUtil.prepareImage(this.getId(), 0, image.getWidth(), image.getHeight());
        image.upload(0, 0, 0, 0, 0, image.getWidth(), image.getHeight(),
                false, false, true, true);
    }


    public record AnimationStripData(int width, int height, int frameCount){
        public static final AnimationStripData EMPTY =  new AnimationStripData(16,16,1);
    }
}
