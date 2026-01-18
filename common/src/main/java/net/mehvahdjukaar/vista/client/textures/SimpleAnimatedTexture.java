package net.mehvahdjukaar.vista.client.textures;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.vista.client.GifPathSpriteSource;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static net.minecraft.client.renderer.texture.SpriteLoader.DEFAULT_METADATA_SECTIONS;

public class SimpleAnimatedTexture extends AbstractTexture {
    private static final SpriteResourceLoader DEFAULT_LOADER = SpriteResourceLoader.create(DEFAULT_METADATA_SECTIONS);

    private final ResourceLocation location;
    private AnimationStripData stripData = AnimationStripData.EMPTY;

    public SimpleAnimatedTexture(ResourceLocation location) {
        this.location = location;
    }

    @NotNull
    public AnimationStripData getStripData() {
        return stripData;
    }

    public ResourceLocation getLocation() {
        return location;
    }

    @Override
    public void load(ResourceManager resourceManager) throws IOException {
        SpriteContents spriteContents;
        Resource resource = resourceManager.getResourceOrThrow(this.location);
        if (this.location.getPath().endsWith(".gif")) {
            spriteContents = GifPathSpriteSource.readGif(resource, this.location);
        } else {
            spriteContents = DEFAULT_LOADER.loadSprite(location, resource);
        }
        if (spriteContents == null) return;

        this.stripData = AnimationStripData.create(spriteContents);

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


}
