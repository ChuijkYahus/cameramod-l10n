package net.mehvahdjukaar.vista.integration.supplementaries;

import com.google.common.base.Suppliers;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.supplementaries.client.MobHeadShadersManager;
import net.mehvahdjukaar.supplementaries.common.items.BlackboardItem;
import net.mehvahdjukaar.supplementaries.common.items.components.BlackboardData;
import net.mehvahdjukaar.supplementaries.common.utils.MiscUtils;
import net.mehvahdjukaar.supplementaries.reg.ModComponents;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.picture_tape.PictureTapeEntries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class SuppCompat {

    private static final Supplier<Item> QUARK_GLASS_PANE =
            Suppliers.memoize(() -> (BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("quark","dirty_glass_pane")))
                    .orElse(null));
    private static final ResourceLocation RETRO_SHADER =  VistaMod.res("shaders/post/ntsc_codec.json");

    public static void init() {
        // let the picture tape store drawn blackboards too
        PictureTapeEntries.register(SuppCompat::isDrawnBlackboard);

        if (PlatHelper.getPhysicalSide().isClient()) {
            SuppCompatClient.init();
        }
    }

    // a blackboard someone actually drew on. A blank one has nothing to show
    public static boolean isDrawnBlackboard(ItemStack stack) {
        return getBlackboardData(stack) != null;
    }

    @Nullable
    public static BlackboardData getBlackboardData(ItemStack stack) {
        if (!(stack.getItem() instanceof BlackboardItem)) return null;
        BlackboardData data = stack.get(ModComponents.BLACKBOARD.get());
        return data == null || data.isEmpty() ? null : data;
    }

    public static ResourceLocation getShaderForItem(Item item) {
        if (item == VistaMod.TV_ITEM.get()) return null;
        if (item == QUARK_GLASS_PANE.get()) return RETRO_SHADER;
        return MobHeadShadersManager.INSTANCE.getShaderPathForItem(item);
    }

    public static boolean isFunny() {
        return MiscUtils.getFestivity().isAprilsFool();
    }
}
