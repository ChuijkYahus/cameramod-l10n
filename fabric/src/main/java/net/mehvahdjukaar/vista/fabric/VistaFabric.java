package net.mehvahdjukaar.vista.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;

public class VistaFabric implements ModInitializer {


    public void onInitialize() {
        VistaMod.init();

        if (PlatHelper.getPhysicalSide().isClient()) {
            VistaFabricClient.init();
        }
    }

}
