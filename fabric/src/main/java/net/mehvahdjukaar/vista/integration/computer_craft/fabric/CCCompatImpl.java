package net.mehvahdjukaar.vista.integration.computer_craft.fabric;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderAccess;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.integration.computer_craft.CCCompat;
import net.mehvahdjukaar.vista.integration.computer_craft.CassetteBurnerBlockEntity;
import net.mehvahdjukaar.vista.integration.computer_craft.CassetteBurnerPeripheral;
import net.mehvahdjukaar.vista.integration.computer_craft.ViewFinderPeripheral;
import net.minecraft.client.renderer.LevelRenderer;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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
