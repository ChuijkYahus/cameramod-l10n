package net.mehvahdjukaar.vista;

import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Contract;

//no client types in here, servers link this class. client stuff goes in VistaClientPlatStuff
public class VistaPlatStuff {

    @Contract
    @PlatformImpl
    public static void tickEnergy(TVBlockEntity tv) {
        throw new AssertionError();
    }

    @Contract
    @PlatformImpl
    public static void invalidateBlockCapabilities(Level level, BlockPos pos) {
        throw new AssertionError();
    }
}
