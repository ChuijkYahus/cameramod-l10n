package net.mehvahdjukaar.vista.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.ViewFinderHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public class VistaFabricClient {

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(VistaModClient::onClientTick);
        HudRenderCallback.EVENT.register(VistaFabricClient::onRenderHud);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            VistaModClient.onLevelClose();
        });

    }

    private static void onRenderHud(GuiGraphics graphics, DeltaTracker partialTicks) {
        ViewFinderHud.INSTANCE.render(graphics, partialTicks);
    }
}
