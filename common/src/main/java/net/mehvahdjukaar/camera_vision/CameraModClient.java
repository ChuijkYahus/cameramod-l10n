package net.mehvahdjukaar.camera_vision;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.mehvahdjukaar.camera_vision.client.TvBlockEntityRenderer;
import net.mehvahdjukaar.moonlight.api.client.CoreShaderContainer;
import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.minecraft.client.renderer.GameRenderer;

public class CameraModClient {

    public static final CoreShaderContainer POST_SHADER = new CoreShaderContainer(GameRenderer::getPositionTexColorShader);
    public static final CoreShaderContainer CAMERA_VIEW_SHADER = new CoreShaderContainer(GameRenderer::getRendertypeEntitySolidShader);
    public static final CoreShaderContainer NOISE_SHADER = new CoreShaderContainer(GameRenderer::getRendertypeEntitySolidShader);


    public static void init() {
        ClientHelper.addBlockEntityRenderersRegistration(CameraModClient::registerBlockEntityRenderers);
        ClientHelper.addShaderRegistration(CameraModClient::registerShaders);
    }

    private static void registerBlockEntityRenderers(ClientHelper.BlockEntityRendererEvent event) {
        event.register(CameraVision.TV_TILE.get(), TvBlockEntityRenderer::new);
    }

    @EventCalled
    private static void registerShaders(ClientHelper.ShaderEvent event) {
        event.register(CameraVision.res("static_noise"), DefaultVertexFormat.NEW_ENTITY, NOISE_SHADER::assign);
        event.register(CameraVision.res("camera_view"), DefaultVertexFormat.NEW_ENTITY, CAMERA_VIEW_SHADER::assign);
    }

}
