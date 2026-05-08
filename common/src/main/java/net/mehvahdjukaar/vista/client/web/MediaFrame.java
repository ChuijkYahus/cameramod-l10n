package net.mehvahdjukaar.vista.client.web;

import com.mojang.blaze3d.platform.NativeImage;
import net.mehvahdjukaar.moonlight.api.resources.textures.TextureImage;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.client.textures.ImageRescaler;
import net.mehvahdjukaar.vista.client.textures.TextureCollager;
import net.mehvahdjukaar.vista.configs.ClientConfigs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MediaFrame implements AutoCloseable {
    private final NativeImage image;
    private final double pts;
    private final Map<Vec2i, NativeImage> scaledImages = new ConcurrentHashMap<>();

    public MediaFrame(NativeImage image, double pts) {
        this.image = image;
        this.pts = pts;
    }

    public NativeImage originalImage() {
        return image;
    }

    public double pts() {
        return pts;
    }

    public NativeImage scaledImage(int width, int height) {
        return scaledImages.computeIfAbsent(new Vec2i(width, height), d -> {
            return ImageRescaler.resize(image, width, height,
                    ClientConfigs.SCALING_MODE.get(), ClientConfigs.BILINEAR.get());
        });
    }


    @Override
    public void close() throws Exception {
        image.close();
        for (var i : scaledImages.values()) {
            i.close();
        }
    }
}
