package net.mehvahdjukaar.vista.integration.computer_craft.neoforge;


import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderAccess;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.integration.computer_craft.CCCompat;
import net.mehvahdjukaar.vista.integration.computer_craft.CassetteBurnerBlockEntity;
import net.mehvahdjukaar.vista.integration.computer_craft.CassetteBurnerPeripheral;
import net.mehvahdjukaar.vista.integration.computer_craft.ViewFinderPeripheral;
import net.mehvahdjukaar.vista.neoforge.VistaForge;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


public class CCCompatImpl {

    protected static final BlockCapability<ViewFinderPeripheral, Direction> VIEW_FINDER_CAP =
            BlockCapability.createSided(VistaMod.res("view_finder"), ViewFinderPeripheral.class);
    protected static final BlockCapability<CassetteBurnerPeripheral, Direction> CASSETTE_BURNER_CAP =
            BlockCapability.createSided(VistaMod.res("cassette_burner"), CassetteBurnerPeripheral.class);


    public static void init() {
        VistaForge.modBus.get().addListener(CCCompatImpl::registerCap);
    }

    public static void setup() {
        ForgeComputerCraftAPI.registerGenericCapability(VIEW_FINDER_CAP);
    }

    public static void registerCap(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(VIEW_FINDER_CAP, VistaMod.VIEWFINDER_TILE.get(),
                (tile, object2) -> new ViewFinderPeripheral(tile));
        event.registerBlockEntity(CASSETTE_BURNER_CAP, CCCompat.CASSETTE_BURNER_TILE.get(),
                (tile, object2) -> tile.peripheral);
    }

}
