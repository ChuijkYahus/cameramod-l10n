package net.mehvahdjukaar.vista.integration.supplementaries;

import net.mehvahdjukaar.supplementaries.client.MobHeadShadersManager;
import net.mehvahdjukaar.vista.client.ModRenderTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class SuppCompat {

    public static String getShaderForItem(Item item) {
        ModRenderTypes.POSTERIZE
        return MobHeadShadersManager.INSTANCE.getShaderForItem(item);
    }
}
