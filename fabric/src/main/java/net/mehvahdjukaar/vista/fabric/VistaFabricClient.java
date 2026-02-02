package net.mehvahdjukaar.vista.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.ViewFinderController;
import net.mehvahdjukaar.vista.client.textures.GifPathSpriteSource;
import net.mehvahdjukaar.vista.client.ui.ViewFinderHud;
import net.mehvahdjukaar.vista.mixins.fabric.SpriteSourcesAccessor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public class VistaFabricClient {

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(VistaModClient::onClientTick);
        HudRenderCallback.EVENT.register(VistaFabricClient::onRenderHud);
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            try {
                if (PlatHelper.getPhysicalSide().isClient()) {
                    //got to be careful with classloading
                    VistaModClient.onLevelClose();
                }
            } catch (Exception e) {
                VistaMod.LOGGER.error("Error unloading Vista client level data", e);
            }
        });

        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register( (minecraft, clientLevel) -> {
            VistaModClient.onLevelLoaded(clientLevel);
        });

        ClientPreAttackCallback.EVENT.register((minecraft, localPlayer, i) -> {
            if (ViewFinderController.onPlayerAttack()) {
                return true;
            }
            return false;
        });


        SpriteSourcesAccessor.getTYPES().put(VistaMod.res("directory_gifs"), GifPathSpriteSource.TYPE);


    }

    private static void onRenderHud(GuiGraphics graphics, DeltaTracker partialTicks) {
        ViewFinderHud.INSTANCE.render(graphics, partialTicks);
    }


}
