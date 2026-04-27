package net.mehvahdjukaar.vista.platform;

import net.fabricmc.api.ModInitializer;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

public class VistaFabric implements ModInitializer {


    public void onInitialize() {
        VistaMod.init();
        if (PlatHelper.getPhysicalSide().isClient()) {
            VistaFabricClient.init();
        }
    }

}
