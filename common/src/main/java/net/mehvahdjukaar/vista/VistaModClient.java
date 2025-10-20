package net.mehvahdjukaar.vista;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.mehvahdjukaar.moonlight.api.client.CoreShaderContainer;
import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.mehvahdjukaar.vista.client.TvBlockEntityRenderer;
import net.mehvahdjukaar.vista.client.ViewFinderBlockEntityRenderer;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.item.ItemStack;

import static net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;

public class VistaModClient {

    public static final CoreShaderContainer POSTERIZE_SHADER = new CoreShaderContainer(GameRenderer::getPositionTexColorShader);
    public static final CoreShaderContainer CAMERA_VIEW_SHADER = new CoreShaderContainer(GameRenderer::getRendertypeEntitySolidShader);
    public static final CoreShaderContainer STATIC_SHADER = new CoreShaderContainer(GameRenderer::getRendertypeEntitySolidShader);

    public static final ModelLayerLocation VIEWFINDER_MODEL = loc("viewfinder");

    public static final Material VIEW_FINDER_MATERIAL = new Material(LOCATION_BLOCKS,
            VistaMod.res("block/viewfinder"));


    private static ModelLayerLocation loc(String name) {
        return new ModelLayerLocation(VistaMod.res(name), name);
    }

    public static void init() {
        ClientHelper.addBlockEntityRenderersRegistration(VistaModClient::registerBlockEntityRenderers);
        ClientHelper.addShaderRegistration(VistaModClient::registerShaders);
        ClientHelper.addModelLayerRegistration(VistaModClient::registerModelLayers);
        ClientHelper.addItemColorsRegistration(VistaModClient::registerItemColors);
    }

    @EventCalled
    private static void registerItemColors(ClientHelper.ItemColorEvent event) {
        event.register(new ItemColor() {
            @Override
            public int getColor(ItemStack itemStack, int i) {
                if(i == 1){
                    itemStack.get(VistaMod.CASSETTE_TAPE_COMPONENT.get()).ifPresent(tape -> {
                        int color = tape.getColor().getColorValue();
                        //make sure it's not white
                        if (color == 0xFFFFFF) color = 0xAAAAAA;
                        return color;
                    });
                }
                return 0;
            }
        }, VistaMod.CASSETTE.get());
    }

    @EventCalled
    private static void registerBlockEntityRenderers(ClientHelper.BlockEntityRendererEvent event) {
        event.register(VistaMod.TV_TILE.get(), TvBlockEntityRenderer::new);
        event.register(VistaMod.VIEWFINDER_TILE.get(), ViewFinderBlockEntityRenderer::new);
    }

    @EventCalled
    private static void registerShaders(ClientHelper.ShaderEvent event) {
        event.register(VistaMod.res("static_noise"), DefaultVertexFormat.NEW_ENTITY, STATIC_SHADER::assign);
        event.register(VistaMod.res("camera_view"), DefaultVertexFormat.NEW_ENTITY, CAMERA_VIEW_SHADER::assign);
        event.register(VistaMod.res("posterize"), DefaultVertexFormat.POSITION_TEX, POSTERIZE_SHADER::assign);
    }


    @EventCalled
    private static void registerModelLayers(ClientHelper.ModelLayerEvent event) {
        event.register(VIEWFINDER_MODEL, ViewFinderBlockEntityRenderer::createMesh);
    }
}
