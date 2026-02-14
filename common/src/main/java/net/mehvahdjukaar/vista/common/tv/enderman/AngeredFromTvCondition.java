package net.mehvahdjukaar.vista.common.tv.enderman;

import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public class AngeredFromTvCondition implements LootItemCondition {

    public static final MapCodec<AngeredFromTvCondition> CODEC = MapCodec.unit(AngeredFromTvCondition::new);


    @Override
    public LootItemConditionType getType() {
        return VistaMod.TV_ENDERMAN_CONDITION.get();
    }

    @Override
    public boolean test(LootContext lootContext) {
        Entity entity = lootContext.getParam(LootContextParams.THIS_ENTITY);
        if (entity instanceof EnderMan em) {
            Boolean wasAngry = VistaMod.ENDERMAN_CAP.getOrNull(em);
            return wasAngry != null && wasAngry;
        }
        return false;
    }
}
