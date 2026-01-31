package net.mehvahdjukaar.vista.neoforge;

import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.BroadcastManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.lang.ref.WeakReference;

import static net.mehvahdjukaar.vista.VistaMod.MOD_ID;

/**
 * Author: MehVahdJukaar
 */
@Mod(MOD_ID)
public class VistaForge {

    public static WeakReference<IEventBus> modBus;

    public VistaForge(IEventBus bus) {

        modBus = new WeakReference<>(bus);
        RegHelper.startRegisteringFor(bus);
        VistaMod.init();
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        var level = event.getLevel();
        if (level instanceof ServerLevel serverLevel) {
            VistaMod.addEntityGoal(event.getEntity(), serverLevel);
        }
    }


}
