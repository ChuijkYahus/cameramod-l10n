package net.mehvahdjukaar.vista;

import net.mehvahdjukaar.vista.common.*;
import net.mehvahdjukaar.vista.configs.CommonConfigs;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;


public class VistaMod {
    public static final String MOD_ID = "vista";
    public static final Logger LOGGER = LogManager.getLogger("Vista");

    public static ResourceLocation res(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
    }

    public static final Supplier<Block> TV = RegHelper.registerBlockWithItem(res("television"),
            () -> new TVBlock(Block.Properties.of().strength(1.5f).noOcclusion()));

    public final static Supplier<BlockEntityType<TVBlockEntity>> TV_TILE = RegHelper.registerBlockEntityType(
            res("television"), TVBlockEntity::new, TV);


    public static final Supplier<Block> VIEW_FINDER = RegHelper.registerBlockWithItem(res("view_finder"),
            () -> new ViewFinderBlock(Block.Properties.of().strength(1.5f).noOcclusion()));

    public  static final Supplier<BlockEntityType<ViewFinderBlockEntity>> VIEW_FINDER_TILE = RegHelper.registerBlockEntityType(
            res("view_finder"), ViewFinderBlockEntity::new, VIEW_FINDER);

    public static final Supplier<EchoCassetteItem> ECHO_CASSETTE_ITEM = RegHelper.registerItem(res("echo_cassette"),
            () -> new EchoCassetteItem(new Item.Properties()
                    .rarity(Rarity.RARE)
                    .stacksTo(1)));

    public static final Supplier<DataComponentType<UUID>> LINKED_FEED_COMPONENT = RegHelper.registerDataComponent(
            res("linked_feed"), ()->
                    DataComponentType.<UUID>builder()
                            .persistent(UUIDUtil.CODEC)
                            .networkSynchronized(UUIDUtil.STREAM_CODEC)
                            .build());

    public static void init() {
        CommonConfigs.init();

        NetworkHelper.addNetworkRegistration(VistaMod::registerMessages, 2);

        if (PlatHelper.getPhysicalSide().isClient()) {
            VistaModClient.init();
        }
    }

    private static void registerMessages(NetworkHelper.RegisterMessagesEvent event) {

    }

}
