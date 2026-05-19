package net.mehvahdjukaar.vista.platform;

import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.ViewFinderController;
import net.mehvahdjukaar.vista.client.renderer.FeedConnectionDebugRenderer;
import net.mehvahdjukaar.vista.client.renderer.VistaChunksDebugRenderer;
import net.mehvahdjukaar.vista.client.textures.GifPathSpriteSource;
import net.mehvahdjukaar.vista.client.ui.ViewFinderHud;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(Dist.CLIENT)
public class VistaForgeClient {

    @SubscribeEvent
    public static void onLevelLoaded(LevelEvent.Load event) {
        if (event.getLevel() instanceof ClientLevel cl) {
            VistaModClient.onLevelLoaded(cl);
        }
    }

    @SubscribeEvent
    public static void onLevelUnloaded(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel cl && cl == Minecraft.getInstance().level) {
            VistaModClient.onLevelUnloaded(cl);
        }
    }

    private static boolean firstScreenShown;

    @SubscribeEvent
    public static void onScreenDrawPost(ScreenEvent.Init.Post event) {
        if (!firstScreenShown && event.getScreen() instanceof TitleScreen) {
            VistaModClient.onFirstScreen(event.getScreen());
            firstScreenShown = true;
        }
    }

    private static boolean firstRenderTick = true;
    @SubscribeEvent
    public static void onClientEndTick(ClientTickEvent.Post event) {
        VistaModClient.onClientTick(Minecraft.getInstance());
        if(firstRenderTick && Minecraft.getInstance().level != null) {
            firstRenderTick = false;
            PointLightData lightData = new PointLightData();
            lightData.setPosition(30, -60, -63);
            lightData.setRadius(10);
            VeilRenderSystem.renderer().getLightRenderer().addLight(lightData);
        }
        if(Minecraft.getInstance().level == null){
            firstRenderTick = true;
        }
    }

    @SubscribeEvent
    public static void onRenderTick(RenderFrameEvent.Post event) {
        VistaModClient.onRenderTickEnd(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onAddGuiLayers(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CAMERA_OVERLAYS, VistaMod.res("viewfinder"),
                ViewFinderHud.INSTANCE);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiLayerEvent.Pre event) {
        if (ViewFinderController.isActive()) {
            ResourceLocation overlay = event.getName();
            if (overlay == (VanillaGuiLayers.EXPERIENCE_BAR) ||
                    overlay == (VanillaGuiLayers.EXPERIENCE_LEVEL) ||
                    overlay == (VanillaGuiLayers.HOTBAR)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void computeFOV(ComputeFovModifierEvent event) {
        float original = event.getFovModifier();
        float modified = event.getNewFovModifier();
        float newFOV = ViewFinderController.modifyFOV(original, modified, event.getPlayer());
        if (newFOV != modified) {
            event.setNewFovModifier(newFOV);
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(InputEvent.MouseScrollingEvent event) {
        if (ViewFinderController.onMouseScrolled(event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void registerSpriteSources(RegisterSpriteSourceTypesEvent event) {
        event.register(VistaMod.res("directory_gifs"), GifPathSpriteSource.TYPE);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VistaModClient.onClientDisconnect();
    }

    @SubscribeEvent
    public static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack() && ViewFinderController.onPlayerAttack()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        } else if (event.isUseItem() && ViewFinderController.onPlayerUse()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }
}
