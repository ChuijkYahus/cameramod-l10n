package net.mehvahdjukaar.vista.client.textures;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;


public class WebTexture extends AbstractTexture implements Dumpable {
    @Nullable
    private final String urlString;
    private boolean uploaded;
    private NativeImage cpuImage;
    private ResourceLocation resourceLocation;

    public WebTexture(String urlString, ResourceLocation resourceLocation) {
        this.resourceLocation = resourceLocation;
        this.urlString = urlString;
    }

    public ResourceLocation getResourceLocation() {
        return resourceLocation;
    }

    protected void onLoaded(NativeImage image) {
        this.cpuImage = image;
        Minecraft.getInstance().execute(() -> {
            this.uploaded = true;
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(() -> this.upload(image));
            } else {
                this.upload(image);
            }
        });
    }

    private void upload(NativeImage image) {
        TextureUtil.prepareImage(this.getId(), image.getWidth(), image.getHeight());
        image.upload(0, 0, 0, 0, 0,
                image.getWidth(), image.getHeight(), true, false);
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
        //TODO: dont keep this in memory here
    }
}


