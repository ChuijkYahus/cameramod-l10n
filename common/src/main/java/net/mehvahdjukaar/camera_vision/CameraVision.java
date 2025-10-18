package net.mehvahdjukaar.camera_vision;

import net.mehvahdjukaar.camera_vision.common.CameraBlock;
import net.mehvahdjukaar.camera_vision.common.CameraBlockEntity;
import net.mehvahdjukaar.camera_vision.common.TVBlock;
import net.mehvahdjukaar.camera_vision.common.TVBlockEntity;
import net.mehvahdjukaar.camera_vision.configs.CommonConfigs;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;


public class CameraVision {
    public static final String MOD_ID = "camera_vision";
    public static final Logger LOGGER = LogManager.getLogger("Camera Vision");

    public static ResourceLocation res(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
    }

    public static final Supplier<Block> TV_BLOCK = RegHelper.registerBlockWithItem(res("tv"),
            () -> new TVBlock(Block.Properties.of().strength(1.5f).noOcclusion()));

    public final static Supplier<BlockEntityType<TVBlockEntity>> TV_TILE = RegHelper.registerBlockEntityType(
            res("tv"), TVBlockEntity::new, TV_BLOCK);


    public static final Supplier<Block> CAMERA_BLOCK = RegHelper.registerBlockWithItem(res("camera"),
            () -> new CameraBlock(Block.Properties.of().strength(1.5f).noOcclusion()));

    public final static Supplier<BlockEntityType<CameraBlockEntity>> CAMERA_TILE = RegHelper.registerBlockEntityType(
            res("camera"), CameraBlockEntity::new, CAMERA_BLOCK);


    public static void init() {
        CommonConfigs.init();
        ModNetwork.init();

        NetworkHelper.addNetworkRegistration(CameraVision::registerMessages, 2);

        if (PlatHelper.getPhysicalSide().isClient()) {
            CameraModClient.init();
        }
    }

    private static void registerMessages(NetworkHelper.RegisterMessagesEvent event) {

    }

}
