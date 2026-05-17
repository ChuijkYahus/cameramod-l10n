package net.mehvahdjukaar.vista.configs;

import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.moonlight.api.platform.configs.ModConfigHolder;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.minecraft.server.level.ServerLevel;

import java.util.function.Supplier;

public class CommonConfigs {


    public static final ModConfigHolder SPEC;

    public static final Supplier<Integer> MAX_CONNECTED_TV_SIZE;
    public static final Supplier<Boolean> CREEPER_DROP;
    public static final Supplier<Boolean> CHEST_DROP;
    public static final Supplier<Mode> WAVE_GATE_MODE;
    public static final Supplier<Boolean> SEND_CHUNKS_VIEWED_BY_VIEW_FINDER;
    public static final Supplier<Boolean> LOAD_CHUNKS_VIEWED_BY_VIEW_FINDER;

    static {
        ConfigBuilder builder = ConfigBuilder.create(VistaMod.MOD_ID, ConfigType.COMMON_SYNCED);

        builder.push("general");
        SEND_CHUNKS_VIEWED_BY_VIEW_FINDER = builder.comment("Allows the server to send chunks that a player could be able to see from one of the tvs nearby that is linked to a view finder far away.")
                .define("send_chunks_viewed_by_view_finders", true);
        LOAD_CHUNKS_VIEWED_BY_VIEW_FINDER = builder.comment("Server loads chunks that are near a far away view finder linked to a tv that's close to at least 1 player. Will increase server strain")
                .define("chunkload_chunks_viewed_by_view_finders", false);
        MAX_CONNECTED_TV_SIZE = builder
                .comment("Maximum size of connected TVs (in blocks). Set to 1 to disable multi-block TVs.")
                .define("max_connected_tv_size", 8, 1, 32);
        CREEPER_DROP = builder
                .comment("Whether creepers should drop tapes when killed by the pillagers.")
                .define("creeper_drop", true);
        CHEST_DROP = builder
                .comment("Whether loot chests could contain cassette tapes.")
                .define("chest_drop", true);

        WAVE_GATE_MODE = builder.comment("Where the wave gate is obtainable.")
                .define("wave_gate_enabled", Mode.CREATIVE_ONLY);
        builder.pop();

        SPEC = builder.build();
        SPEC.forceLoad();
    }

    public static void init() {

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

}
