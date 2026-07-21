package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds (and caches) a {@link DynamicTexture} of a filled map's colours for TV playback.
 */
public class MapFrameTextureProvider implements PictureTapeFrames.ImageProvider {

    private static final Map<Integer, ResourceLocation> CACHE = new HashMap<>();

    @Override
    public boolean matches(ItemStack stack) {
        return stack.has(DataComponents.MAP_ID);
    }

    @Override
    @Nullable
    public ResourceLocation getTexture(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        MapId mapId = stack.get(DataComponents.MAP_ID);
        if (mapId == null || mc.level == null) return null;
        ResourceLocation cached = CACHE.get(mapId.id());
        if (cached != null) return cached;
        MapItemSavedData data = MapItem.getSavedData(mapId, mc.level);
        if (data == null) return null;
        ResourceLocation created = build(mc, mapId.id(), data);
        CACHE.put(mapId.id(), created);
        return created;
    }

    private static ResourceLocation build(Minecraft mc, int id, MapItemSavedData data) {
        DynamicTexture texture = new DynamicTexture(128, 128, true);
        NativeImage image = texture.getPixels();
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                image.setPixelRGBA(x, y, MapColor.getColorFromPackedId(data.colors[x + y * 128]));
            }
        }
        texture.upload();
        return mc.getTextureManager().register("vista_tape_map/" + id, texture);
    }

    public static void clear() {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation loc : CACHE.values()) {
            mc.getTextureManager().release(loc);
        }
        CACHE.clear();
    }
}
