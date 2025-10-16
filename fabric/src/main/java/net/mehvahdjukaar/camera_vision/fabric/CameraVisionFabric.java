package net.mehvahdjukaar.camera_vision.fabric;

import net.fabricmc.api.ModInitializer;
import net.mehvahdjukaar.camera_vision.CameraVision;

public class CameraVisionFabric implements ModInitializer {


    public void onInitialize() {
        CameraVision.init();
    }

}
