package net.mehvahdjukaar.vista.neoforge;

import dan200.computercraft.shared.computer.core.ServerContext;
import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.common.LiveFeedConnectionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
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


    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        LevelAccessor level = event.getLevel();
        if (level instanceof ServerLevel sl) {
            LiveFeedConnectionManager conn = LiveFeedConnectionManager.getInstance(sl);
            if (conn != null) {
                conn.validateAll(sl);
            }
        }
    }


}
