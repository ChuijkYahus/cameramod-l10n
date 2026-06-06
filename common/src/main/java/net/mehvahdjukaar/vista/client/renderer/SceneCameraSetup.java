package net.mehvahdjukaar.vista.client.renderer;

import net.minecraft.client.Camera;

@FunctionalInterface
public interface SceneCameraSetup {
    void setup(Camera camera, float partialTicks);
}
