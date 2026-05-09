package net.mehvahdjukaar.vista.integration.computer_craft.platform;


import dan200.computercraft.api.ForgeComputerCraftAPI;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.integration.computer_craft.WaveGatePeripheral;
import net.mehvahdjukaar.vista.integration.computer_craft.ViewFinderPeripheral;
import net.mehvahdjukaar.vista.platform.VistaForge;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;


public class CCCompatImpl {

    protected static final BlockCapability<ViewFinderPeripheral, Direction> VIEW_FINDER_CAP =
            BlockCapability.createSided(VistaMod.res("view_finder"), ViewFinderPeripheral.class);
    protected static final BlockCapability<WaveGatePeripheral, Direction> CASSETTE_BURNER_CAP =
            BlockCapability.createSided(VistaMod.res("cassette_burner"), WaveGatePeripheral.class);


    public static void init() {
        VistaForge.modBus.get().addListener(CCCompatImpl::registerCap);
    }

    public static void setup() {
        ForgeComputerCraftAPI.registerGenericCapability(VIEW_FINDER_CAP);
    }

    public static void registerCap(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(VIEW_FINDER_CAP, VistaMod.VIEWFINDER_TILE.get(),
                (tile, object2) -> new ViewFinderPeripheral(tile));
        event.registerBlockEntity(CASSETTE_BURNER_CAP, VistaMod.WAVE_GATE_TILE.get(),
                (tile, object2) -> new WaveGatePeripheral(tile));
    }

}
