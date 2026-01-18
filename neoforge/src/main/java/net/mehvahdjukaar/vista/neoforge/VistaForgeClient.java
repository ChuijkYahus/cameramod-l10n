package net.mehvahdjukaar.vista.neoforge;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.GifPathSpriteSource;
import net.mehvahdjukaar.vista.client.TapeTextureHelper;
import net.mehvahdjukaar.vista.client.ViewFinderController;
import net.mehvahdjukaar.vista.client.ViewFinderHud;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@EventBusSubscriber(Dist.CLIENT)
public class VistaForgeClient {

    @SubscribeEvent
    public static void onClientEndTick(ClientTickEvent.Post event) {
        VistaModClient.onClientTick(Minecraft.getInstance());
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
    public static void registerAtlases(RegisterMaterialAtlasesEvent event) {
        event.register(TapeTextureHelper.ATLAS_LOCATION, TapeTextureHelper.ATLAS_INFO_LOCATION);
    }

    @SubscribeEvent
    public static void registerSpriteSources(RegisterSpriteSourceTypesEvent event) {
        event.register(VistaMod.res("directory_gifs"), GifPathSpriteSource.TYPE);
    }

    @SubscribeEvent
    public static void onServerShuttingDown(ServerStoppingEvent event) {
        VistaModClient.onLevelClose();
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
