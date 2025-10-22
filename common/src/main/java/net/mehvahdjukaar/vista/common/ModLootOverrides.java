package net.mehvahdjukaar.vista.common;

import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public class ModLootOverrides {

    private static final Set<ResourceLocation> VALID = new HashSet<>();

    private static final ResourceLocation LOOT = VistaMod.res("inject/treasure_tapes");

    public static void init() {
        RegHelper.addLootTableInjects(ModLootOverrides::addLootMod);
        VALID.add(ResourceLocation.parse("minecraft:archaeology/trail_ruins_rare"));
        VALID.add(ResourceLocation.parse("minecraft:chests/bastion_hoglin_stable"));
        VALID.add(ResourceLocation.parse("minecraft:chests/igloo_chest"));
        VALID.add(ResourceLocation.parse("minecraft:chests/jungle_temple"));
        VALID.add(ResourceLocation.parse("minecraft:chests/woodland_mansion"));
        VALID.add(ResourceLocation.parse("minecraft:chests/stronghold_library"));
        VALID.add(ResourceLocation.parse("minecraft:chests/shipwreck_supply"));
        VALID.add(ResourceLocation.parse("minecraft:chests/simple_dungeon"));
        VALID.add(ResourceLocation.parse("minecraft:chests/nether_bridge"));
        VALID.add(ResourceLocation.parse("minecraft:chests/trial_chambers/reward_ominous_rare"));
    }

    private static void addLootMod(RegHelper.LootInjectEvent event) {
        ResourceLocation key = event.getTable();
        if (VALID.contains(key)) {
            event.addTableReference(LOOT);
        }
    }


}
