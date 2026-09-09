package net.mehvahdjukaar.vista.integration.supplementaries;

import net.mehvahdjukaar.supplementaries.client.BlackboardTextureManager;
import net.mehvahdjukaar.supplementaries.common.items.components.BlackboardData;
import net.mehvahdjukaar.vista.client.ui.picture_tape.TapeEntryRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class BlackboardTapeEntryRenderer implements TapeEntryRenderer {

    @Override
    public boolean matches(ItemStack stack) {
        return SuppCompat.isDrawnBlackboard(stack);
    }

    @Override
    @Nullable
    public ResourceLocation getTexture(ItemStack stack) {
        BlackboardData data = SuppCompat.getBlackboardData(stack);
        if (data == null) return null;
        return BlackboardTextureManager.getInstance(data).getTextureLocation();
    }
}
