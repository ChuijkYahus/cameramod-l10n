package net.mehvahdjukaar.vista.platform;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.vista.ServerCameraChunkManager;
import net.mehvahdjukaar.vista.VistaMod;

public class VistaFabric implements ModInitializer {


    public void onInitialize() {
        VistaMod.init();
        if (PlatHelper.getPhysicalSide().isClient()) {
            VistaFabricClient.init();
        }

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            VistaMod.onPlayerLoggedIn(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerCameraChunkManager.onPlayerLeave(handler.player);
        });
    }

}
