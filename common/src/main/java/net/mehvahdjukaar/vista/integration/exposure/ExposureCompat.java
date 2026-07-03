package net.mehvahdjukaar.vista.integration.exposure;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;

public class ExposureCompat {

    public static void init() {
        if (PlatHelper.getPhysicalSide().isClient()) {
            ExposureCompatClient.init();
        }
    }
/*
    public static final Supplier<DataComponentType<PictureTapeContent>> PICTURE_TAPE_CONTENT =
            RegHelper.registerDataComponent(VistaMod.res("item_list"),
                    () -> DataComponentType.<PictureTapeContent>builder()
                            .persistent(PictureTapeContent.CODEC)
                            .networkSynchronized(PictureTapeContent.STREAM_CODEC)
                            .build());

    public static final Supplier<Item> PICTURE_TAPE = RegHelper.registerItem(
            VistaMod.res("picture_tape"),
            () -> new PictureTapeItem(new Item.Properties()
                    .component(PICTURE_TAPE_CONTENT.get(), new PictureTapeContent(List.of())))
    );

    public static final Supplier<MenuType<PictureTapeMenu>> PICTURE_TAPE_MENU =
            RegHelper.registerMenuType(VistaMod.res("picture_tape"), PictureTapeMenu::fromBuffer);

    //TODO: add picture tape item that can be insterted inplace of photops. basically a clone of the album but less functionality. also it holds maps


    private static boolean isPictureItem(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof PictureTapeItem ||
                item instanceof PhotographItem || item instanceof StackedPhotographsItem || item instanceof AlbumItem;
    }
*/

}
