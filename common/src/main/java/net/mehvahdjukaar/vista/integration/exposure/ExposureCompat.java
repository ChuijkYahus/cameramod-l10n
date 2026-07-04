package net.mehvahdjukaar.vista.integration.exposure;

import io.github.mortuusars.exposure.world.item.AlbumItem;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.StackedPhotographsItem;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.vista.common.picture_tape.PictureTapeEntries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ExposureCompat {

    public static void init() {
        // let the picture tape store exposure pictures too
        PictureTapeEntries.register(ExposureCompat::isExposurePicture);

        if (PlatHelper.getPhysicalSide().isClient()) {
            ExposureCompatClient.init();
        }
    }

    public static boolean isExposurePicture(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof PhotographItem
                || item instanceof StackedPhotographsItem
                || item instanceof AlbumItem;
    }

}
