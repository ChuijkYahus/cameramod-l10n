package net.mehvahdjukaar.vista.neoforge;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import static net.mehvahdjukaar.vista.VistaMod.MOD_ID;

/**
 * Author: MehVahdJukaar
 */
@Mod(MOD_ID)
public class VistaForge {

    public VistaForge(IEventBus bus) {
        RegHelper.startRegisteringFor(bus);

        VistaMod.init();
    //    NeoForge.EVENT_BUS.register(this);
    }




}
