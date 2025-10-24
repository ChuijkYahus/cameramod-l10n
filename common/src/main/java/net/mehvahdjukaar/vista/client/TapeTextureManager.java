package net.mehvahdjukaar.vista.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.CassetteTape;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class TapeTextureManager {

    public static final ResourceLocation ATLAS_LOCATION = VistaMod.res("textures/atlas/cassette_tape.png");
    public static final ResourceLocation ATLAS_INFO_LOCATION = VistaMod.res("cassette_tapes");

    private static final ResourceLocation BARS_LOCATION = VistaMod.res("color_bars");
    private static final ResourceLocation SMILE_LOCATION = VistaMod.res("smile");
    private static final ResourceLocation NEUTRAL_LOCATION = VistaMod.res("neutral");
    private static final ResourceLocation SAD_LOCATION = VistaMod.res("neutral");

    private static final Map<ResourceKey<CassetteTape>, Material> MATERIALS = new HashMap<>();
    private static final Map<ResourceKey<CassetteTape>, Material> MATERIALS_FLAT = new HashMap<>();
    private static final Material DEFAULT_MATERIAL = new Material(ATLAS_LOCATION, BARS_LOCATION);
    private static final Material DEFAULT_MATERIAL_FLAT = new Material(ATLAS_LOCATION, BARS_LOCATION);
    private static final Material SMILE_MATERIAL = new Material(ATLAS_LOCATION, SMILE_LOCATION);
    private static final Material SMILE_MATERIAL_FLAT = new Material(ATLAS_LOCATION, SMILE_LOCATION);
    private static final Material NEUTRAL_MATERIAL = new Material(ATLAS_LOCATION, NEUTRAL_LOCATION);
    private static final Material NEUTRAL_MATERIAL_FLAT = new Material(ATLAS_LOCATION, NEUTRAL_LOCATION);
    private static final Material SAD_MATERIAL = new Material(ATLAS_LOCATION, SAD_LOCATION);
    private static final Material SAD_MATERIAL_FLAT = new Material(ATLAS_LOCATION, SAD_LOCATION);

    public static Material getMaterial(Holder<CassetteTape> tapeKey) {
        return MATERIALS.computeIfAbsent(tapeKey.unwrapKey().get(), k ->
                new Material(ATLAS_LOCATION, tapeKey.value().assetId()));
    }

    public static Material getMaterialFlat(Holder<CassetteTape> tapeKey) {
        return MATERIALS_FLAT.computeIfAbsent(tapeKey.unwrapKey().get(), k ->
                new Material(ATLAS_LOCATION, tapeKey.value().assetId()));
    }


    public static VertexConsumer getTapeVC(Holder<CassetteTape> tapeKey, MultiBufferSource buffer, boolean flat) {
        Material mat = flat ? getMaterialFlat(tapeKey) : getMaterial(tapeKey);
        return mat.buffer(buffer, !flat ? t -> ModRenderTypes.CAMERA_DRAW_SPRITE.apply(t, mat) : RenderType::text);
    }

    public static VertexConsumer getDefaultTapeVC(MultiBufferSource buffer, boolean flat) {
        Material mat = flat ? DEFAULT_MATERIAL_FLAT : DEFAULT_MATERIAL;
        return mat.buffer(buffer, !flat ? t -> ModRenderTypes.CAMERA_DRAW_SPRITE.apply(t, mat) : RenderType::text);
    }

    public static VertexConsumer getSmileTapeVC(MultiBufferSource buffer, LivingEntity player, boolean flat) {
        float health = player.getHealth()/player.getMaxHealth();
        Material mat;
        if (health > 0.66f) {
            mat = flat ? SMILE_MATERIAL_FLAT : SMILE_MATERIAL;
        } else if (health > 0.33f) {
            mat = flat ? NEUTRAL_MATERIAL_FLAT : NEUTRAL_MATERIAL;
        } else {
            mat = flat ? SAD_MATERIAL_FLAT : SAD_MATERIAL;
        }
        return mat.buffer(buffer, !flat ? t -> ModRenderTypes.CAMERA_DRAW_SPRITE.apply(t, mat) : RenderType::text);
    }

    public static VertexConsumer getFullSpriteVC(ResourceLocation tex, MultiBufferSource buffer, boolean flat) {
        RenderType rt = !flat ? ModRenderTypes.CAMERA_DRAW.apply(tex) : RenderType.text(tex);
        return buffer.getBuffer(rt);
    }

    public static void onWorldReload() {
        MATERIALS.clear();
        MATERIALS_FLAT.clear();
    }


}
