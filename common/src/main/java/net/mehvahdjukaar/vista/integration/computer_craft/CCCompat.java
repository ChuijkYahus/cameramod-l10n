package net.mehvahdjukaar.vista.integration.computer_craft;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.moonlight.api.misc.RegSupplier;
import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderAccess;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CCCompat {

    public static final RegSupplier<CassetteBurnerBlock> CASSETTE_BURNER =
            RegHelper.registerBlock(VistaMod.res("cassette_burner"),
                    () -> new CassetteBurnerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE)));

    public static final RegSupplier<BlockEntityType<CassetteBurnerBlockEntity>> CASSETTE_BURNER_TILE =
            RegHelper.registerBlockEntityType(VistaMod.res("cassette_burner"),
                    CassetteBurnerBlockEntity::new,
                    CASSETTE_BURNER);


    @Contract
    @ExpectPlatform
    public static void init() {
        throw new AssertionError();
    }

    @Contract
    @ExpectPlatform
    public static void setup() {
        throw new AssertionError();
    }


}
