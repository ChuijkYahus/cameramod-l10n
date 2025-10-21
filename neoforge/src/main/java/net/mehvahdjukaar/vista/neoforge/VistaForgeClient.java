package net.mehvahdjukaar.vista.neoforge;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.ViewFinderController;
import net.mehvahdjukaar.vista.client.ViewFinderHud;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(Dist.CLIENT)
public class VistaForgeClient {

    @SubscribeEvent
    public static void onClientEndTick(ClientTickEvent.Post event) {
        VistaModClient.onClientTick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onAddGuiLayers(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CAMERA_OVERLAYS, VistaMod.res("viewfinder"),
                ViewFinderHud.INSTANCE);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiLayerEvent.Pre event) {
        if (ViewFinderController.isActive()) {
            var overlay = event.getName();
            if (overlay == (VanillaGuiLayers.EXPERIENCE_BAR) || overlay == (VanillaGuiLayers.HOTBAR)) {
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

}
