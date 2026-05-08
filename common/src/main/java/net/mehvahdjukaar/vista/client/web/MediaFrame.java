package net.mehvahdjukaar.vista.client.web;

import com.mojang.blaze3d.platform.NativeImage;

public final class MediaFrame implements AutoCloseable {
    private final NativeImage image;
    private final double pts;

    public MediaFrame(NativeImage image, double pts) {
        this.image = image;
        this.pts = pts;
    }

    public NativeImage image() {
        return image;
    }

    public double pts() {
        return pts;
    }


    @Override
    public void close() throws Exception {
        image.close();
    }
}
