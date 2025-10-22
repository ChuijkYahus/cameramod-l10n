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

    public static final ResourceLocation BARS_LOCATION = VistaMod.res("color_bars");

    private static final Map<ResourceKey<CassetteTape>, Material> MATERIALS = new HashMap<>();
    private static final Map<ResourceKey<CassetteTape>, Material> MATERIALS_FLAT = new HashMap<>();
    public static final Material DEFAULT_MATERIAL = new Material(ATLAS_LOCATION, BARS_LOCATION);
    public static final Material DEFAULT_MATERIAL_FLAT = new Material(ATLAS_LOCATION, BARS_LOCATION);

    public static Material getMaterial(Holder<CassetteTape> tapeKey) {
        return MATERIALS.computeIfAbsent(tapeKey.unwrapKey().get(), k ->
                new Material(ATLAS_LOCATION, tapeKey.value().assetId()));
    }

    public static Material getMaterialFlat(Holder<CassetteTape> tapeKey) {
        return MATERIALS_FLAT.computeIfAbsent(tapeKey.unwrapKey().get(), k ->
                new Material(ATLAS_LOCATION, tapeKey.value().assetId()));
    }



    public static void onWorldReload() {
        MATERIALS.clear();
        MATERIALS_FLAT.clear();
    }
}
