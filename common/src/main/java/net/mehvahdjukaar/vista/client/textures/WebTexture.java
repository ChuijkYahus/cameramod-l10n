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


//copy of HTTPTexture with minor additions
public class WebTexture extends AbstractTexture implements Dumpable {
    @Nullable
    private final File cachelLocation;
    private final String urlString;
    @Nullable
    private CompletableFuture<?> future;
    private boolean uploaded;
    private NativeImage cpuImage;
    private ResourceLocation resourceLocation;

    public WebTexture(@Nullable File cacheLocation, String urlString, ResourceLocation resourceLocation) {
        this.resourceLocation = resourceLocation;
        this.cachelLocation = cacheLocation;
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

    @Nullable
    private static NativeImage readImageFromStream(InputStream stream) {
        NativeImage nativeImage = null;

        try {
            nativeImage = NativeImage.read(stream);
        } catch (Exception exception) {
            VistaMod.LOGGER.warn("Error while loading the skin texture", exception);


        }

        return nativeImage;
    }

    @Override
    public void load(ResourceManager resourceManager) throws IOException {
        if (this.future == null) {
            NativeImage nativeImage;
            if (this.cachelLocation != null && this.cachelLocation.isFile()) {
                VistaMod.LOGGER.debug("Loading http texture from local cache ({})", this.cachelLocation);
                FileInputStream fileInputStream = new FileInputStream(this.cachelLocation);
                nativeImage = readImageFromStream(fileInputStream);
            } else {
                nativeImage = null;
            }

            if (nativeImage != null) {
                this.onLoaded(nativeImage);
            } else {
                this.future = CompletableFuture.runAsync(() -> {
                    HttpURLConnection httpURLConnection = null;
                    VistaMod.LOGGER.debug("Downloading http texture from {} to {}", this.urlString, this.cachelLocation);

                    try {
                        httpURLConnection = (HttpURLConnection) (new URL(this.urlString)).openConnection(Minecraft.getInstance().getProxy());
                        httpURLConnection.setDoInput(true);
                        httpURLConnection.setDoOutput(false);
                        httpURLConnection.connect();
                        if (httpURLConnection.getResponseCode() / 100 == 2) {
                            InputStream inputStream;
                            if (this.cachelLocation != null) {
                                FileUtils.copyInputStreamToFile(httpURLConnection.getInputStream(), this.cachelLocation);
                                inputStream = new FileInputStream(this.cachelLocation);
                            } else {
                                inputStream = httpURLConnection.getInputStream();
                            }

                            var buf = ImageIO.read(new URL(this.urlString));

                            NativeImage ni = readImageFromStream(inputStream);
                            if (ni != null) {
                                this.onLoaded(ni);
                            }
                        }
                    } catch (Exception exception) {
                        VistaMod.LOGGER.error("Couldn't download http texture", exception);
                    } finally {
                        if (httpURLConnection != null) {
                            httpURLConnection.disconnect();
                        }

                    }

                }, Util.backgroundExecutor());
            }
        }
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

/*
    public CompletableFuture<Result<BufferedImage>> execute(URL url) {
        int timeout = 10000;
        return CompletableFuture.<Result<BufferedImage>>supplyAsync(() -> {
            VistaMod.LOGGER.info("Attempting to load image from URL: '{}'", url.toString());
            // Util.nonCriticalIoPool()
            try {
                BufferedImage image = ImageIO.read(url);
                if (image == null) {
                    VistaMod.LOGGER.error("Cannot load image from URL '{}'", url);
                    return Result.error(ERROR_CANNOT_READ);
                } else {
                    return Result.success(image);
                }
            } catch (Exception e) {
                VistaMod.LOGGER.error("Cannot load image from URL: ", e);
                return Result.error(ERROR_CANNOT_READ);
            }
        }).completeOnTimeout(Result.error(ERROR_TIMED_OUT), (timeout * 50), TimeUnit.MILLISECONDS);
    }
 */

