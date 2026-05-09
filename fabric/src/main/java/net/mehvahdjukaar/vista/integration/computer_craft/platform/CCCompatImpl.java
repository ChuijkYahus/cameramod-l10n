package net.mehvahdjukaar.vista.integration.computer_craft.platform;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.integration.computer_craft.WaveGatePeripheral;
import net.mehvahdjukaar.vista.integration.computer_craft.ViewFinderPeripheral;

public class CCCompatImpl {

    public static void setup() {
        PeripheralLookup.get().registerForBlockEntity((tile, direction) -> {
            if (tile.ccPeripheral == null) tile.ccPeripheral = new ViewFinderPeripheral(tile);
            return (IPeripheral) tile.ccPeripheral;
        }, VistaMod.VIEWFINDER_TILE.get());

        PeripheralLookup.get().registerForBlockEntity((tile, direction) -> {
            if (tile.ccPeripheral == null) tile.ccPeripheral = new WaveGatePeripheral(tile);
            return (IPeripheral) tile.ccPeripheral;
        }, VistaMod.WAVE_GATE_TILE.get());

    }

    public static void init() {
    }

}
