package net.mehvahdjukaar.vista;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.mehvahdjukaar.vista.client.TvBlockEntityRenderer;
import net.mehvahdjukaar.moonlight.api.client.CoreShaderContainer;
import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.minecraft.client.renderer.GameRenderer;

public class VistaModClient {

    public static final CoreShaderContainer POSTERIZE_SHADER = new CoreShaderContainer(GameRenderer::getPositionTexColorShader);
    public static final CoreShaderContainer CAMERA_VIEW_SHADER = new CoreShaderContainer(GameRenderer::getRendertypeEntitySolidShader);
    public static final CoreShaderContainer STATIC_SHADER = new CoreShaderContainer(GameRenderer::getRendertypeEntitySolidShader);


    public static void init() {
        ClientHelper.addBlockEntityRenderersRegistration(VistaModClient::registerBlockEntityRenderers);
        ClientHelper.addShaderRegistration(VistaModClient::registerShaders);
    }

    private static void registerBlockEntityRenderers(ClientHelper.BlockEntityRendererEvent event) {
        event.register(VistaMod.TV_TILE.get(), TvBlockEntityRenderer::new);
    }

    @EventCalled
    private static void registerShaders(ClientHelper.ShaderEvent event) {
        event.register(VistaMod.res("static_noise"), DefaultVertexFormat.NEW_ENTITY, STATIC_SHADER::assign);
        event.register(VistaMod.res("camera_view"), DefaultVertexFormat.NEW_ENTITY, CAMERA_VIEW_SHADER::assign);
        event.register(VistaMod.res("posterize"), DefaultVertexFormat.POSITION_TEX, POSTERIZE_SHADER::assign);
    }

}
