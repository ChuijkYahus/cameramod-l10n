package net.mehvahdjukaar.vista.client.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * Renders a filled map as its actual map image.
 */
public class MapTapeEntryRenderer implements TapeEntryRenderer {

    @Override
    public boolean matches(ItemStack stack) {
        return stack.is(Items.FILLED_MAP);
    }

    @Override
    public void render(GuiGraphics graphics, ItemStack stack, int x, int y, int size) {
        Minecraft mc = Minecraft.getInstance();
        MapId mapId = stack.get(DataComponents.MAP_ID);
        MapItemSavedData data = mc.level == null || mapId == null
                ? null : MapItem.getSavedData(mapId, mc.level);
        if (mapId == null || data == null) {
            // map data not received yet: neutral placeholder with the item icon
            graphics.fill(x, y, x + size, y + size, 0xFF888888);
            graphics.renderItem(stack, x + size / 2 - 8, y + size / 2 - 8);
            return;
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 1.0);
        float scale = size / 128f;
        pose.scale(scale, scale, 1f);
        MultiBufferSource.BufferSource buffer = graphics.bufferSource();
        mc.gameRenderer.getMapRenderer().render(pose, buffer, mapId, data, false, LightTexture.FULL_BRIGHT);
        graphics.flush();
        pose.popPose();
    }
}
