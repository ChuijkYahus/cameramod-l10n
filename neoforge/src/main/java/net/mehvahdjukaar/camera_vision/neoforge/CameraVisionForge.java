package net.mehvahdjukaar.camera_vision.neoforge;

import net.mehvahdjukaar.camera_vision.CameraVision;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

import static net.mehvahdjukaar.camera_vision.CameraVision.MOD_ID;

/**
 * Author: MehVahdJukaar
 */
@Mod(MOD_ID)
public class CameraVisionForge {

    public CameraVisionForge(IEventBus bus) {
        RegHelper.startRegisteringFor(bus);

        CameraVision.init();
    //    NeoForge.EVENT_BUS.register(this);
    }



}
