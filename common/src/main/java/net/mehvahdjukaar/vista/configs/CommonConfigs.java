package net.mehvahdjukaar.vista.configs;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.moonlight.api.platform.configs.ModConfigHolder;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.minecraft.server.level.ServerLevel;

import java.util.function.Supplier;

public class CommonConfigs {


    public static final ModConfigHolder SPEC;

    public static final Supplier<Integer> TV_MAX_CONNECTED_TV_SIZE;
    public static final Supplier<Boolean> TV_SQUARE_ASPECT_RATIO;
    public static final Supplier<Integer> MAX_CONNECTED_MIRROR_SIZE;
    public static final Supplier<Boolean> MIRROR_SQUARE_ASPECT_RATIO;
    public static final Supplier<MirrorPlacement> MIRROR_PLACEMENT;
    public static final Supplier<Boolean> MIRROR_ENABLED;
    public static final Supplier<Boolean> CREEPER_DROP;
    public static final Supplier<Boolean> CHEST_DROP;
    public static final Supplier<Mode> WAVE_GATE_MODE;
    public static final Supplier<Integer> SEND_CHUNKS_VIEWED_BY_VIEW_FINDER;
    public static final Supplier<Boolean> LOAD_CHUNKS_VIEWED_BY_VIEW_FINDER;
    public static final Supplier<Boolean> TV_CONSUME_ENERGY;
    public static final Supplier<Integer> TV_ENERGY_CONSUMPTION_RATE;

    static {
        ConfigBuilder builder = ConfigBuilder.create(VistaMod.MOD_ID, ConfigType.COMMON_SYNCED);

        builder.push("general");
        SEND_CHUNKS_VIEWED_BY_VIEW_FINDER = builder.comment("Radius (in chunks) of the extra chunk zone sent to clients for each far-away view finder linked to a nearby TV. Set to 0 to disable client chunk sending.")
                .define("send_chunks_viewed_by_view_finders", 4, 0, 16);
        LOAD_CHUNKS_VIEWED_BY_VIEW_FINDER = builder.comment("Server loads chunks that are near a far away view finder linked to a tv that's close to at least 1 player. Will increase server strain")
                .define("chunkload_chunks_viewed_by_view_finders", false);
        TV_MAX_CONNECTED_TV_SIZE = builder
                .comment("Maximum size of connected TVs (in blocks). Set to 1 to disable multi-block TVs.")
                .define("max_connected_tv_size", 8, 1, 24);
        TV_SQUARE_ASPECT_RATIO = builder
                .comment("Makes connected tvs just have a square aspect ratio. If you set to false cassettes will be stretched and will look worse as a result")
                .define("tv_square_aspect_ratio", true);
        MAX_CONNECTED_MIRROR_SIZE = builder
                .comment("Maximum size of connected mirrors (in blocks). Set to 1 to disable multi-block mirrors.")
                .define("max_connected_mirror_size", 8, 1, 24);
        MIRROR_SQUARE_ASPECT_RATIO = builder
                .comment("Forces connected mirrors to have a square aspect ratio.")
                .define("mirror_square_aspect_ratio", true);
        MIRROR_PLACEMENT = builder
                .comment("Which mirror placements are allowed. NEAR: surface always flush with the front face. FAR: surface always recessed into the block. BOTH: near or far is chosen from where you click along the block's depth axis (near half = near, far half = recessed).")
                .define("mirror_placement", MirrorPlacement.BOTH);
        MIRROR_ENABLED = builder
                .comment("Whether mirrors (and the crystalline item used to craft them) are enabled. Disabling hides them from creative tabs, disables the mirror recipe, and stops crystalline from dropping from elder guardians.")
                .affectsDynamicPacks()
                .worldReload()
                .define("mirror_enabled", true);
        CREEPER_DROP = builder
                .comment("Whether creepers should drop tapes when killed by the pillagers.")
                .define("creeper_drop", true);
        CHEST_DROP = builder
                .comment("Whether loot chests could contain cassette tapes.")
                .define("chest_drop", true);

        WAVE_GATE_MODE = builder.comment("Where the wave gate is obtainable.")
                .affectsDynamicPacks()
                .worldReload()
                .define("wave_gate_enabled", Mode.CREATIVE_ONLY);

        TV_CONSUME_ENERGY =
                PlatHelper.getPlatform().isFabric() ? ()-> false :
                builder.comment("Whether TVs should consume Forge energy (NeoForge only).")
                .define("tv_consume_energy", false);
        TV_ENERGY_CONSUMPTION_RATE =
                PlatHelper.getPlatform().isFabric() ? ()-> 0 :
                builder.comment("Energy consumption rate per tick when TV is powered and has a cassette.")
                .define("tv_energy_consumption_rate", 20, 1, 10000);

        builder.pop();

        SPEC = builder.build();
        SPEC.forceLoad();
    }

    public static void init() {

    }

    public static boolean isMirrorEnabled() {
        return MIRROR_ENABLED.get();
    }

    public static boolean isWaveGateOn() {
        return WAVE_GATE_MODE.get() != Mode.OFF;
    }

    public static boolean isWaveGateCraftable() {
        Mode mode = WAVE_GATE_MODE.get();
        if (CompatHandler.COMPUTER_CRAFT && mode != Mode.OFF) return true;
        return mode == Mode.CRAFTABLE;
    }

    //TODO:
    public static int distanceFromTvForServerToLoadViewFinders(ServerLevel level) {
        return level.getServer().getPlayerList().getViewDistance();
    }

    public enum Mode {
        CRAFTABLE,
        CREATIVE_ONLY,
        OFF
    }

    public enum MirrorPlacement {
        NEAR,
        FAR,
        BOTH
    }

}
