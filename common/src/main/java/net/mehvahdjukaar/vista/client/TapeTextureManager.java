package net.mehvahdjukaar.vista.client;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.CassetteTape;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class TapeTextureManager {

    public static final ResourceLocation ATLAS_LOCATION = VistaMod.res("textures/atlas/cassette_tape.png");
    public static final ResourceLocation ATLAS_INFO_LOCATION = VistaMod.res("cassette_tapes");


    private static final Map<ResourceKey<CassetteTape>, Material> MATERIALS = new HashMap<>();

    public static Material getMaterial(Holder<CassetteTape> tapeKey) {
        return MATERIALS.computeIfAbsent(tapeKey.unwrapKey().get(), k ->
                new Material(ATLAS_LOCATION, tapeKey.value().assetId()));
    }


    public static void onWorldReload() {
        MATERIALS.clear();
    }
}
