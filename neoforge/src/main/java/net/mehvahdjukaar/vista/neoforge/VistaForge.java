package net.mehvahdjukaar.vista.neoforge;

import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.CassetteItem;
import net.mehvahdjukaar.vista.common.ViewFinderConnection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import static net.mehvahdjukaar.vista.VistaMod.MOD_ID;

/**
 * Author: MehVahdJukaar
 */
@Mod(MOD_ID)
public class VistaForge {

    public VistaForge(IEventBus bus) {
        RegHelper.startRegisteringFor(bus);
        VistaMod.init();
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onAnvilRepair(AnvilUpdateEvent event) {
        int aa = 1;
        ItemStack stack = event.getLeft();

        if (stack.is(VistaMod.CASSETTE.get()) && event.getRight().isEmpty()) {
            ItemStack newOutput = stack.copy();
            CassetteItem.assignCustomCassette(newOutput, event.getPlayer().level(), event.getName());
            if(!ItemStack.isSameItemSameComponents(newOutput, stack)) event.setOutput(newOutput);
        }
    }


    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        LevelAccessor level = event.getLevel();
        if (level instanceof ServerLevel sl) {
            ViewFinderConnection conn = ViewFinderConnection.get(sl);
            if (conn != null) {
                conn.validateAll(sl);
            }
        }
    }


}
