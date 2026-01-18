package net.mehvahdjukaar.vista;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.mehvahdjukaar.moonlight.api.client.CoreShaderContainer;
import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.mehvahdjukaar.vista.client.*;
import net.mehvahdjukaar.vista.client.renderer.TvBlockEntityRenderer;
import net.mehvahdjukaar.vista.client.renderer.TvItemRenderer;
import net.mehvahdjukaar.vista.client.renderer.ViewFinderBlockEntityRenderer;
import net.mehvahdjukaar.vista.client.textures.CassetteTexturesManager;
import net.mehvahdjukaar.vista.client.textures.LiveFeedTexturesManager;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import static net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;

public class VistaModClient {

    public static final CoreShaderContainer POSTERIZE_SHADER = new CoreShaderContainer(GameRenderer::getPositionTexColorShader);
    public static final CoreShaderContainer CAMERA_VIEW_SHADER = new CoreShaderContainer(GameRenderer::getRendertypeEntitySolidShader);
    public static final CoreShaderContainer STATIC_SHADER = new CoreShaderContainer(GameRenderer::getPositionColorShader);

    public static final ModelLayerLocation VIEWFINDER_MODEL = loc("viewfinder");

    public static final Material VIEW_FINDER_MATERIAL = new Material(LOCATION_BLOCKS,
            VistaMod.res("block/viewfinder"));


    private static ModelLayerLocation loc(String name) {
        return new ModelLayerLocation(VistaMod.res(name), name);
    }

    public static void init() {
        ClientConfigs.init();
        ClientHelper.addBlockEntityRenderersRegistration(VistaModClient::registerBlockEntityRenderers);
        ClientHelper.addShaderRegistration(VistaModClient::registerShaders);
        ClientHelper.addModelLayerRegistration(VistaModClient::registerModelLayers);
        ClientHelper.addItemColorsRegistration(VistaModClient::registerItemColors);
        ClientHelper.addItemRenderersRegistration(VistaModClient::registerItemRenderers);

        ClientHelper.addClientReloadListener(()-> CassetteTexturesManager.INSTANCE, VistaMod.res("gif_manager"));
    }

    private static void registerItemRenderers(ClientHelper.ItemRendererEvent event) {
        event.register(VistaMod.TV_ITEM.get(), new TvItemRenderer());
    }

    private static void registerItemColors(ClientHelper.ItemColorEvent event) {
        event.register((itemStack, i) -> {
            if (i == 1) {
                var tape = itemStack.get(VistaMod.CASSETTE_TAPE_COMPONENT.get());
                if (tape == null) return -1;
                return tape.value().color();
            }
            return -1;
        }, VistaMod.CASSETTE.get());
    }

    private static void registerBlockEntityRenderers(ClientHelper.BlockEntityRendererEvent event) {
        event.register(VistaMod.TV_TILE.get(), TvBlockEntityRenderer::new);
        event.register(VistaMod.VIEWFINDER_TILE.get(), ViewFinderBlockEntityRenderer::new);
    }

    private static void registerShaders(ClientHelper.ShaderEvent event) {
        event.register(VistaMod.res("static_noise"), DefaultVertexFormat.NEW_ENTITY, STATIC_SHADER::assign);
        event.register(VistaMod.res("camera_view"), DefaultVertexFormat.NEW_ENTITY, CAMERA_VIEW_SHADER::assign);
        event.register(VistaMod.res("posterize"), DefaultVertexFormat.POSITION_TEX, POSTERIZE_SHADER::assign);
    }

    private static void registerModelLayers(ClientHelper.ModelLayerEvent event) {
        event.register(VIEWFINDER_MODEL, ViewFinderModel::createMesh);
    }

    public static Level getLevel() {
        return Minecraft.getInstance().level;
    }

    public static void onLevelClose() {
        LiveFeedTexturesManager.clear();
    }

    @EventCalled
    public static void onClientTick(Minecraft minecraft) {
        if (minecraft.isPaused() || minecraft.level == null) return;

        Player p = minecraft.player;
        if (p == null) return;

        ViewFinderController.onClientTick(minecraft);
    }

    @EventCalled
    public static void onRenderTickEnd(Minecraft minecraft) {
        LiveFeedTexturesManager.onRenderTickEnd();
    }


    private static boolean preventShiftTillNextKeyUp = false;

    public static void modifyInputUpdate(Input instance, LocalPlayer player) {
        if (ViewFinderController.isActive()) {
            ViewFinderController.onInputUpdate(instance);
            preventShiftTillNextKeyUp = true;
        } else if (preventShiftTillNextKeyUp) {
            if (!instance.shiftKeyDown) {
                preventShiftTillNextKeyUp = false;
            } else {
                instance.shiftKeyDown = false;
            }
        }
    }

}
