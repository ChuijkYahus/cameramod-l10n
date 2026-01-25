package net.mehvahdjukaar.vista.client.textures;

import io.github.mortuusars.exposure.util.cycles.task.Result;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.github.mortuusars.exposure.client.capture.task.UrlCaptureTask.ERROR_CANNOT_READ;
import static io.github.mortuusars.exposure.client.capture.task.UrlCaptureTask.ERROR_TIMED_OUT;

public class UrlTexture extends HttpTexture {

    public UrlTexture(@Nullable File file, String urlString, ResourceLocation location, @Nullable Runnable onDownloaded) {
        super(file, urlString, location, false, onDownloaded);
    }

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
        }).completeOnTimeout(Result.error(ERROR_TIMED_OUT), (long) ((Integer) timeout * 50), TimeUnit.MILLISECONDS);
    }
}
