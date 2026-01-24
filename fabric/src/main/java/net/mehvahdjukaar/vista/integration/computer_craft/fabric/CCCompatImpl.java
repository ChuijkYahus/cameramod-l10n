package net.mehvahdjukaar.vista.integration.computer_craft.fabric;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.integration.computer_craft.CCCompat;
import net.mehvahdjukaar.vista.integration.computer_craft.ViewFinderPeripheral;

public class CCCompatImpl {

    public static void setup() {
        PeripheralLookup.get().registerForBlockEntity((tile, direction) -> {
            if (tile.ccHack == null) tile.ccHack = new ViewFinderPeripheral(tile);
            return (IPeripheral) tile.ccHack;
        }, VistaMod.VIEWFINDER_TILE.get());

        PeripheralLookup.get().registerForBlockEntity((tile, direction) ->
                tile.peripheral, CCCompat.CASSETTE_BURNER_TILE.get());
    }

    public static void init() {
    }

}
