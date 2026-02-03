package net.mehvahdjukaar.vista.integration.supplementaries;

import net.mehvahdjukaar.supplementaries.client.MobHeadShadersManager;
import net.mehvahdjukaar.vista.client.ModRenderTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class SuppCompat {

    public static ResourceLocation getShaderForItem(Item item) {
        String shaderForItem = MobHeadShadersManager.INSTANCE.getShaderForItem(item);
        return shaderForItem == null ? null : ResourceLocation.parse(shaderForItem);
    }
}
