package net.mehvahdjukaar.vista.integration.computer_craft.fabric;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.integration.computer_craft.CCCompat;
import net.mehvahdjukaar.vista.integration.computer_craft.SignalProjectorPeripheral;
import net.mehvahdjukaar.vista.integration.computer_craft.ViewFinderPeripheral;

public class CCCompatImpl {

    public static void setup() {
        PeripheralLookup.get().registerForBlockEntity((tile, direction) -> {
            if (tile.ccPeripheral == null) tile.ccPeripheral = new ViewFinderPeripheral(tile);
            return (IPeripheral) tile.ccPeripheral;
        }, VistaMod.VIEWFINDER_TILE.get());

        PeripheralLookup.get().registerForBlockEntity((tile, direction) -> {
            if (tile.ccPeripheral == null) tile.ccPeripheral = new SignalProjectorPeripheral(tile);
            return (IPeripheral) tile.ccPeripheral;
        }, VistaMod.SIGNAL_PROJECTOR_TILE.get());

    }

    public static void init() {
    }

}
