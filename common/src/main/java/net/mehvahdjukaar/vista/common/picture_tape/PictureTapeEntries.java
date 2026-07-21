package net.mehvahdjukaar.vista.common.picture_tape;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class PictureTapeEntries {

    private static final List<Predicate<ItemStack>> VALIDATORS = new ArrayList<>();

    static {
        // any filled map, including other mods' maps (they all carry the vanilla map id component)
        register(stack -> stack.has(DataComponents.MAP_ID));
        register(stack -> stack.is(Items.PAINTING));
    }

    public static void register(Predicate<ItemStack> validator) {
        VALIDATORS.add(validator);
    }

    public static boolean isValid(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (Predicate<ItemStack> validator : VALIDATORS) {
            if (validator.test(stack)) return true;
        }
        return false;
    }
}
