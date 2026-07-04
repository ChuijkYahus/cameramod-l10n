package net.mehvahdjukaar.vista.common.picture_tape;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * Helper for pushing a filled map's pixel data to a specific player. Maps sitting inside a tape (in
 * the gallery or playing on a TV) aren't tracked/ticked by vanilla, so the client would otherwise
 * have no image to draw. We send the full colour patch with no decorations, so no player markers
 * get attached.
 */
public class PictureTapeMaps {

    public static void sendMapData(ServerPlayer player, ItemStack mapStack) {
        if (!mapStack.is(Items.FILLED_MAP)) return;
        MapId mapId = mapStack.get(DataComponents.MAP_ID);
        if (mapId == null) return;
        MapItemSavedData data = MapItem.getSavedData(mapId, player.level());
        if (data == null) return;
        MapItemSavedData.MapPatch patch = new MapItemSavedData.MapPatch(0, 0, 128, 128, data.colors);
        player.connection.send(new ClientboundMapItemDataPacket(mapId, data.scale, data.locked, null, patch));
    }
}
