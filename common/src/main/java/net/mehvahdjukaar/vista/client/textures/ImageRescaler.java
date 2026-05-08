package net.mehvahdjukaar.vista.client.textures;

import com.mojang.blaze3d.platform.NativeImage;
import net.mehvahdjukaar.moonlight.api.resources.textures.TextureCollager;
import net.mehvahdjukaar.moonlight.api.resources.textures.TextureImage;
import net.mehvahdjukaar.moonlight.api.util.math.Rect2D;

import java.awt.*;

public final class ImageRescaler {

    public enum FitMode {
        STRETCH,
        FIT_WIDTH,
        FIT_HEIGHT,
        CONTAIN,
        COVER
    }

    public static NativeImage resize(NativeImage source,
                                     int targetW, int targetH,
                                     FitMode fitMode,
                                     boolean bilinear) {
        int srcW = source.getWidth();
        int srcH = source.getHeight();
        if (srcW == targetW && srcH == targetH) return source;

        // Wrap source and create an empty destination image.
        // TextureImage is assumed to have a constructor or factory
        // that takes a NativeImage and a debug path String.
        TextureImage srcTex = TextureImage.of(source);
        TextureImage destTex = TextureImage.createNew(targetW, targetH);

        // Calculate source and destination rectangles in "origin" space.
        Rect2D srcRect;
        Rect2D dstRect;

        switch (fitMode) {
            case STRETCH:
                srcRect = new Rect2D(0, 0, srcW, srcH);
                dstRect = new Rect2D(0, 0, targetW, targetH);
                break;

            case FIT_WIDTH:
                int outH = Math.max(1, (int) (targetW * (double) srcH / srcW));
                srcRect = new Rect2D(0, 0, srcW, srcH);
                dstRect = new Rect2D(0, 0, targetW, outH);
                break;

            case FIT_HEIGHT:
                int outW = Math.max(1, (int) (targetH * (double) srcW / srcH));
                srcRect = new Rect2D(0, 0, srcW, srcH);
                dstRect = new Rect2D(0, 0, outW, targetH);
                break;

            case CONTAIN: {
                double scale = Math.min((double) targetW / srcW,
                        (double) targetH / srcH);
                int scaledW = (int) (srcW * scale);
                int scaledH = (int) (srcH * scale);
                int offX = (targetW - scaledW) / 2;
                int offY = (targetH - scaledH) / 2;
                srcRect = new Rect2D(0, 0, srcW, srcH);
                dstRect = new Rect2D(offX, offY, scaledW, scaledH);
                break;
            }

            case COVER: {
                double scale = Math.max((double) targetW / srcW,
                        (double) targetH / srcH);
                int cropW = (int) Math.ceil(targetW / scale);
                int cropH = (int) Math.ceil(targetH / scale);
                int cropX = (srcW - cropW) / 2;
                int cropY = (srcH - cropH) / 2;

                srcRect = new Rect2D(cropX, cropY, cropW, cropH);
                dstRect = new Rect2D(0, 0, targetW, targetH);
                break;
            }

            default:
                throw new IllegalArgumentException("Unknown fit mode: " + fitMode);
        }

        // Build the collager operation.
        TextureCollager.Builder builder = TextureCollager.builder(srcW, srcH, targetW, targetH)
                .copyFrom(srcRect.x(), srcRect.y(), srcRect.width(), srcRect.height())
                .to(dstRect.x(), dstRect.y(), dstRect.width(), dstRect.height());

        if (bilinear) {
            builder.bilinearScaling();
        }

        TextureCollager collager = builder.build();
          collager.apply(srcTex, destTex);

        return destTex.getImage();
    }

}