package net.mehvahdjukaar.vista;

import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.moonlight.api.misc.IAttachmentType;
import net.mehvahdjukaar.moonlight.api.misc.RegSupplier;
import net.mehvahdjukaar.moonlight.api.misc.WorldSavedDataType;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.vista.client.TapeTextureManager;
import net.mehvahdjukaar.vista.common.*;
import net.mehvahdjukaar.vista.configs.CommonConfigs;
import net.mehvahdjukaar.vista.integration.exposure.ExposureCompat;
import net.mehvahdjukaar.vista.network.ModNetwork;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;


public class VistaMod {
    public static final String MOD_ID = "vista";
    public static final Logger LOGGER = LogManager.getLogger("Vista");

    public static final boolean EXPOSURE_ON = PlatHelper.isModLoaded("exposure");

    public static ResourceLocation res(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
    }

    public static final WorldSavedDataType<LiveFeedConnectionManager> VIEWFINDER_CONNECTION =
            RegHelper.registerWorldSavedData(res("viewfinder_connection"), LiveFeedConnectionManager::create,
                    LiveFeedConnectionManager.CODEC, LiveFeedConnectionManager.STREAM_CODEC);

    public static final ResourceKey<Registry<CassetteTape>> CASSETTE_TAPE_REGISTRY_KEY =
            ResourceKey.createRegistryKey(res("cassette_tape"));

    static {
        RegHelper.registerDataPackRegistry(CASSETTE_TAPE_REGISTRY_KEY, CassetteTape.DIRECT_CODEC, CassetteTape.DIRECT_CODEC);
    }

    public static final Supplier<Block> TV = RegHelper.registerBlock(res("television"),
            () -> new TVBlock(Block.Properties.of()
                    .sound(SoundType.WOOD)
                    .isRedstoneConductor((blockState, blockGetter, blockPos) -> false)
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(1.5f)));

    public static final Supplier<BlockItem> TV_ITEM = RegHelper.registerItem(res("television"),
            () -> new TelevisionItem(TV.get(), new Item.Properties()));

    public final static Supplier<BlockEntityType<TVBlockEntity>> TV_TILE = RegHelper.registerBlockEntityType(
            res("television"), TVBlockEntity::new, TV);

    public static final Supplier<Block> VIEWFINDER = RegHelper.registerBlockWithItem(res("viewfinder"),
            () -> new ViewFinderBlock(Block.Properties.of().strength(1.5f).noOcclusion()));

    public static final Supplier<BlockEntityType<ViewFinderBlockEntity>> VIEWFINDER_TILE = RegHelper.registerBlockEntityType(
            res("viewfinder"), ViewFinderBlockEntity::new, VIEWFINDER);

    public static final Supplier<CassetteItem> CASSETTE = RegHelper.registerItem(res("cassette"),
            () -> new CassetteItem(new Item.Properties()
                    .rarity(Rarity.RARE)
                    .stacksTo(1)));

    public static final Supplier<HollowCassetteItem> HOLLOW_CASSETTE = RegHelper.registerItem(res("hollow_cassette"),
            () -> new HollowCassetteItem(new Item.Properties()
                    .rarity(Rarity.RARE)
                    .stacksTo(1)));


    public static final Supplier<DataComponentType<UUID>> LINKED_FEED_COMPONENT = RegHelper.registerDataComponent(
            res("linked_feed"), () ->
                    DataComponentType.<UUID>builder()
                            .persistent(UUIDUtil.CODEC)
                            .networkSynchronized(UUIDUtil.STREAM_CODEC)
                            .build());

    public static final Supplier<DataComponentType<Holder<CassetteTape>>> CASSETTE_TAPE_COMPONENT = RegHelper.registerDataComponent(
            res("cassette_tape"), () ->
                    DataComponentType.<Holder<CassetteTape>>builder()
                            .persistent(CassetteTape.CODEC)
                            .networkSynchronized(CassetteTape.STREAM_CODEC)
                            .build());

    public static final Supplier<SoundEvent> CASSETTE_INSERT_SOUND = RegHelper.registerSound(res("block.television.insert"));
    public static final Supplier<SoundEvent> CASSETTE_EJECT_SOUND = RegHelper.registerSound(res("block.television.eject"));
    public static final RegSupplier<SoundEvent> TV_STATIC_SOUND = RegHelper.registerSound(res("block.television.static"));
    public static final int STATIC_SOUND_DURATION = 4 * 20; //4 seconds

    public static final Supplier<LootItemFunctionType<CassetteTapeLootFunction>> CASSETTE_TAPE_LOOT_FUNCTION =
            RegHelper.registerLootFunction(res("random_tape"), CassetteTapeLootFunction.CODEC);

    public static final TagKey<CassetteTape> SUPPORTER_TAPES = TagKey.create(
            CASSETTE_TAPE_REGISTRY_KEY, res("supporter_tapes"));

    public static void init() {

        //TODO:
        //connection 2x2
        //no adjusted frustum causing to clip through blocks when placed next to it
        //emderman view through camera
        //view finder scroll bug
        //creative only cassette that targets a video
        //turn table rotation thing
        //update time fps change with round robin
        //click sounds and new insert sound
        //turn on shader
        //show tv screen for far away cunks using a static screenshot with pause shaderand pause icon
        //liveleak icon
        //jittery scroll
        //turn on sound
        //player holding hand slike when using explosure cameera
        //shader when you wear a tv. fnaf
        //gifs play independently. No more atlas. sad
        //new cassettes
        //view distance scales with zoom
        //fix flickering
        //zoom broken on fabric?
        //check turn table
        //cannon maoeuvering sound
        //view finder maneuvering sound
        //verify the connection system
        //tv glitch shader
        //exposure compat

        //for gifs maybe we could send the strips to the atlas, then in the shader do the scrolling
        CommonConfigs.init();

        ModNetwork.init();
        ModLootOverrides.init();
        if (EXPOSURE_ON) ExposureCompat.init();

        RegHelper.addItemsToTabsRegistration(VistaMod::addItemsToTabs);

        if (PlatHelper.getPhysicalSide().isClient()) {
            VistaModClient.init();
            PlatHelper.addReloadableCommonSetup((ra, dataReload) -> {
                if (!dataReload) TapeTextureManager.onWorldReload();
            });
        }
    }

    private static void addItemsToTabs(RegHelper.ItemToTabEvent event) {
        event.add(CreativeModeTabs.REDSTONE_BLOCKS, TV.get());
        event.add(CreativeModeTabs.REDSTONE_BLOCKS, VIEWFINDER.get());
        CreativeModeTab.ItemDisplayParameters parameters = event.getParameters();
        for (var v : parameters.holders().lookupOrThrow(CASSETTE_TAPE_REGISTRY_KEY).listElements().toList()) {
            if (v.is(SUPPORTER_TAPES)) continue;
            ItemStack stack = CASSETTE.get().getDefaultInstance();
            stack.set(CASSETTE_TAPE_COMPONENT.get(), v);
            event.add(CreativeModeTabs.TOOLS_AND_UTILITIES, stack);
        }
        event.add(CreativeModeTabs.TOOLS_AND_UTILITIES, HOLLOW_CASSETTE.get());

        if (EXPOSURE_ON) {
            //   event.add(CreativeModeTabs.TOOLS_AND_UTILITIES, ExposureCompat.PICTURE_TAPE.get());
        }
    }

    @EventCalled
    public static void addEntityGoal(Entity entity, ServerLevel serverLevel) {
        if (entity instanceof EnderMan man) {
            EndermanFreezeWhenLookedAtThroughTVGoal task = new EndermanFreezeWhenLookedAtThroughTVGoal(man);
            man.goalSelector.addGoal(1, task);
        }
    }


}
