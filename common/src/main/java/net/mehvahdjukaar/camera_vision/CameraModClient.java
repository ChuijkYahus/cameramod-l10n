package net.mehvahdjukaar.camera_vision;

import net.mehvahdjukaar.camera_vision.client.TvBlockEntityRenderer;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;

public class CameraModClient {

    public static void init() {
        ClientHelper.addBlockEntityRenderersRegistration(CameraModClient::registerBlockEntityRenderers);
    }

    private static void registerBlockEntityRenderers(ClientHelper.BlockEntityRendererEvent event) {
        event.register(CameraVision.TV_TILE.get(), TvBlockEntityRenderer::new);
    }
}
