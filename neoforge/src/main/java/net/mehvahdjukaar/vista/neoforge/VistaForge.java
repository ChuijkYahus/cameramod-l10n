package net.mehvahdjukaar.vista.neoforge;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

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
