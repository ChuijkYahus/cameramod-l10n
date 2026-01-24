package net.mehvahdjukaar.vista.client.textures;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

public class SimpleAnimatedStripTexture extends AbstractTexture implements Dumpable {
    private static final SpriteResourceLoader DEFAULT_LOADER = SpriteResourceLoader.create(SpriteLoader.DEFAULT_METADATA_SECTIONS);

    private final ResourceLocation fileLocation;
    private final ResourceLocation location;
    private AnimationStripData stripData = AnimationStripData.EMPTY;

    public SimpleAnimatedStripTexture(ResourceLocation location) {
        this.fileLocation = location;
        //remove extension
        this.location = location.withPath(p ->
                p.substring(0, p.lastIndexOf('.')));
    }

    @NotNull
    public AnimationStripData getStripData() {
        return stripData;
    }

    public ResourceLocation location() {
        return location;
    }

    public ResourceLocation fileLocation() {
        return fileLocation;
    }

    @Override
    public void load(ResourceManager resourceManager) throws IOException {
        SpriteContents spriteContents = loadContent(resourceManager);
        if (spriteContents == null) return;

        this.stripData = AnimationStripData.create(spriteContents);

        NativeImage nativeImage = spriteContents.originalImage;
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> this.doLoad(nativeImage));
        } else {
            this.doLoad(nativeImage);
        }

    }

    private @Nullable SpriteContents loadContent(ResourceManager resourceManager) throws FileNotFoundException {
        SpriteContents spriteContents;
        Resource resource = resourceManager.getResourceOrThrow(this.fileLocation);
        if (this.fileLocation.getPath().endsWith(".gif")) {
            spriteContents = GifPathSpriteSource.readGif(resource, this.fileLocation);
        } else {
            spriteContents = DEFAULT_LOADER.loadSprite(fileLocation, resource);
        }
        return spriteContents;
    }

    private void doLoad(NativeImage image) {
        TextureUtil.prepareImage(this.getId(), 0, image.getWidth(), image.getHeight());
        image.upload(0, 0, 0, 0, 0, image.getWidth(), image.getHeight(),
                false, false, true, true);
    }


    @Override
    public void dumpContents(ResourceLocation resourceLocation, Path path) throws IOException {
        SpriteContents spriteContents = this.loadContent(Minecraft.getInstance().getResourceManager());
        if (spriteContents == null) return;
        String string = resourceLocation.toDebugFileName() + ".png";
        Path path2 = path.resolve(string);
        spriteContents.originalImage.writeToFile(path2);
    }
}
