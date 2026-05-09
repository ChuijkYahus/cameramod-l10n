package net.mehvahdjukaar.vista.client.web;

import com.mojang.blaze3d.platform.NativeImage;

public record MediaFrame(NativeImage image, double pts) implements AutoCloseable {

    @Override
    public void close() throws Exception {
        image.close();
    }
}
