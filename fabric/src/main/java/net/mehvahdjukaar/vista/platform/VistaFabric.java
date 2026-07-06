package net.mehvahdjukaar.vista.platform;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.chunk_tracking.ServerCameraChunkManager;
public class VistaFabric implements ModInitializer {


    public void onInitialize() {
        VistaMod.init();
        // Create integration lives in :neoforge only; see integration.CompatHandler#CREATE
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
