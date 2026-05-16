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

    static {
        ConfigBuilder builder = ConfigBuilder.create(VistaMod.MOD_ID, ConfigType.COMMON_SYNCED);

        builder.push("general");
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
