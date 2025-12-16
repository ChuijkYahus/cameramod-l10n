package net.mehvahdjukaar.vista.integration.computer_craft.neoforge;


import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.ViewFinderAccess;
import net.mehvahdjukaar.vista.common.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.neoforge.VistaForge;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


public class CCCompatImpl {

    protected static BlockCapability<ViewFinderPeripheral, Direction> VIEW_FINDER_CAP =
            BlockCapability.createSided(VistaMod.res("view_finder"), ViewFinderPeripheral.class);


    public static void init() {
        VistaForge.modBus.get().addListener(CCCompatImpl::registerCap);
    }

    public static void setup() {
        ForgeComputerCraftAPI.registerGenericCapability(VIEW_FINDER_CAP);
    }

    public static void registerCap(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(VIEW_FINDER_CAP, VistaMod.VIEWFINDER_TILE.get(),
                (tile, object2) -> new ViewFinderPeripheral(tile));
    }


    public static final class ViewFinderPeripheral implements IPeripheral {
        private final ViewFinderBlockEntity tile;
        private final ViewFinderAccess acc;

        public ViewFinderPeripheral(ViewFinderBlockEntity tile) {
            this.tile = tile;
            this.acc = ViewFinderAccess.find(tile.getLevel(), TileOrEntityTarget.of(tile));
        }

        @LuaFunction
        public void setYaw(double value) {
            tile.setYaw(acc, (float) value);
            acc.updateClients();
        }

        @LuaFunction
        public float getYaw() {
            return tile.getYaw();
        }

        @LuaFunction
        public void setPitch(double value) {
            tile.setPitch(acc, (float) value);
            acc.updateClients();
        }

        @LuaFunction
        public float getPitch() {
            return tile.getPitch();
        }

        @LuaFunction
        public void setZoom(int zoom) {
            byte power = (byte) Math.min(Math.max(zoom, 1), ViewFinderBlockEntity.MAX_ZOOM);
            tile.setZoomLevel(power);
            acc.updateClients();
        }

        @LuaFunction
        public int getZoom() {
            return tile.getZoomLevel();
        }

        @Override
        public String getType() {
            return "view_finder";
        }

        @Override
        public boolean equals(@Nullable IPeripheral obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (ViewFinderPeripheral) obj;
            return Objects.equals(this.tile, that.tile);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tile);
        }

        @Override
        public String toString() {
            return "ViewFinderPeripheral[" +
                    "tile=" + tile + ']';
        }

    }
}
