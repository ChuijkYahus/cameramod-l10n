package net.mehvahdjukaar.vista;

import net.mehvahdjukaar.moonlight.api.MoonlightRegistry;
import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.moonlight.api.misc.RegSupplier;
import net.mehvahdjukaar.moonlight.api.misc.WorldSavedDataType;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.vista.common.*;
import net.mehvahdjukaar.vista.common.cassette.CassetteItem;
import net.mehvahdjukaar.vista.common.cassette.CassetteTape;
import net.mehvahdjukaar.vista.common.cassette.CassetteTapeLootFunction;
import net.mehvahdjukaar.vista.common.cassette.HollowCassetteItem;
import net.mehvahdjukaar.vista.common.projector.SignalProjectorBlock;
import net.mehvahdjukaar.vista.common.projector.SignalProjectorBlockEntity;
import net.mehvahdjukaar.vista.common.tv.TVBlock;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.mehvahdjukaar.vista.common.tv.TVItem;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlock;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.configs.CommonConfigs;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.mehvahdjukaar.vista.network.ModNetwork;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
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
            () -> new TVItem(TV.get(), new Item.Properties()));

    public final static Supplier<BlockEntityType<TVBlockEntity>> TV_TILE = RegHelper.registerBlockEntityType(
            res("television"), TVBlockEntity::new, TV);

    public static final Supplier<Block> VIEWFINDER = RegHelper.registerBlockWithItem(res("viewfinder"),
            () -> new ViewFinderBlock(Block.Properties.of().strength(1.5f).noOcclusion()));

    public static final Supplier<BlockEntityType<ViewFinderBlockEntity>> VIEWFINDER_TILE = RegHelper.registerBlockEntityType(
            res("viewfinder"), ViewFinderBlockEntity::new, VIEWFINDER);

    public static final RegSupplier<SignalProjectorBlock> SIGNAL_PROJECTOR =
            RegHelper.registerBlockWithItem(VistaMod.res("signal_projector"),
                    () -> new SignalProjectorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE)));

    public static final RegSupplier<BlockEntityType<SignalProjectorBlockEntity>> SIGNAL_PROJECTOR_TILE =
            RegHelper.registerBlockEntityType(VistaMod.res("signal_projector"),
                    SignalProjectorBlockEntity::new,
                    SIGNAL_PROJECTOR);

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

    public static final TagKey<Item> VIEW_FINDER_FILTER = TagKey.create(
            Registries.ITEM, res("view_finder_filter"));


    public static void init() {

        //TODO:
        //fix recursive rendering by using a canvas buffer
        //check if view are sections are not duplicated
        //frustum shenanigans
        //amendments mixed pot not saving on servers
        //divining rod can use maybe has on the blockstate container of a chunk
        //no adjusted frustum causing to clip through blocks when placed next to it
        //view finder scroll bug
        //creative only cassette that targets a video
        //turn table rotation thing
        //show tv screen for far away cunks using a static screenshot with pause shaderand pause icon
        //liveleak icon
        //ball item
        //jittery scroll
        //turn on sound
        //player holding hands like when using explosure cameera and cannons
        //shader when you wear a tv. fnaf
        //new cassettes
        //view distance scales with zoom
        //check turn table
        //cannon maoeuvering sound
        //view finder maneuvering sound
        //verify the connection system
        //tv glitch shader
        //exposure compat
        //camera lensens

        CommonConfigs.init();

        ModNetwork.init();
        ModLootOverrides.init();
        CompatHandler.init();

        RegHelper.addItemsToTabsRegistration(VistaMod::addItemsToTabs);

        if (PlatHelper.getPhysicalSide().isClient()) {
            VistaModClient.init();
            PlatHelper.addReloadableCommonSetup((ra, dataReload) -> {
                // if (!dataReload) CassetteTexturesMaterials.onWorldReload();
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

        if (CompatHandler.COMPUTER_CRAFT) {
            event.add(CreativeModeTabs.FUNCTIONAL_BLOCKS, SIGNAL_PROJECTOR.get());
        } else {
            if (event.getTab().hasAnyItems()) {
                event.add(CreativeModeTabs.OP_BLOCKS, SIGNAL_PROJECTOR.get(), MoonlightRegistry.SPAWN_BOX_BLOCK.get());
            }
        }

        CompatHandler.addItemsToTabs(event);
    }

    @EventCalled
    public static void addEntityGoal(Entity entity, ServerLevel serverLevel) {
        if (entity instanceof EnderMan man) {
            EndermanFreezeWhenLookedAtThroughTVGoal task = new EndermanFreezeWhenLookedAtThroughTVGoal(man);
            man.goalSelector.addGoal(1, task);
        }
    }


}
