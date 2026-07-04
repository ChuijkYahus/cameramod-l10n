package net.mehvahdjukaar.vista.platform;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.chunk_tracking.ServerCameraChunkManager;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.mehvahdjukaar.vista.integration.create.CreateCompat;

public class VistaFabric implements ModInitializer {


    public void onInitialize() {
        VistaMod.init();
        // registered here (not in common CompatHandler) because the Create classes live in the platform module
        if (CompatHandler.CREATE) PlatHelper.addCommonSetup(CreateCompat::setup);
        if (PlatHelper.getPhysicalSide().isClient()) {
            VistaFabricClient.init();
        }

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            VistaMod.onPlayerLoggedIn(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerCameraChunkManager.onPlayerLeave(handler.player);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
           for(var p  :server.getPlayerList().getPlayers()){
               VistaMod.onServerPlayerTick(p);
           }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(ServerCameraChunkManager::clearAll);
    }

}
