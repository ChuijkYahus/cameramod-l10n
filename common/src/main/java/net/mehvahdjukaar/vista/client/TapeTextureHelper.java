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
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public class TapeTextureHelper {

    public static final ResourceLocation ATLAS_LOCATION = VistaMod.res("textures/atlas/cassette_tape.png");
    public static final ResourceLocation ATLAS_INFO_LOCATION = VistaMod.res("cassette_tapes");

    private static final ResourceLocation BARS_LOCATION = VistaMod.res("color_bars");
    private static final ResourceLocation SMILE_LOCATION = VistaMod.res("smile");
    private static final ResourceLocation NEUTRAL_LOCATION = VistaMod.res("neutral");
    private static final ResourceLocation SAD_LOCATION = VistaMod.res("sad");
    private static final Map<Smile, ResourceLocation> SMILES = Map.of(
            Smile.HAPPY, SMILE_LOCATION,
            Smile.NEUTRAL, NEUTRAL_LOCATION,
            Smile.SAD, SAD_LOCATION
    );

    private static final Map<CassetteMaterialKey, Material> CASSETTE_MATERIALS = new HashMap<>();
    private static final Map<DefaultMaterialKey, Material> BARS_MATERIAL = new HashMap<>();
    private static final Map<SmileMaterialKey, Material> SMILE_MATERIAL = new HashMap<>();

    private record CassetteMaterialKey(ResourceKey<CassetteTape> tapeKey, int scale, boolean hasSfx) {
    }

    private record DefaultMaterialKey(int scale, boolean hasSfx) {
    }

    private record SmileMaterialKey(Smile smile, boolean hasSfx) {
    }

    private enum Smile {
        HAPPY, NEUTRAL, SAD;

        public static Smile fromHealth(LivingEntity entity) {
            float health = entity.getHealth() / entity.getMaxHealth();
            if (health > 0.66f) {
                return HAPPY;
            } else if (health > 0.33f) {
                return NEUTRAL;
            } else {
                return SAD;
            }
        }
    }

    public static VertexConsumer getTapeVC(Holder<CassetteTape> tapeKey, MultiBufferSource buffer, int scale) {
        boolean hasSfx = hasSfx();
        var materialKey = new CassetteMaterialKey(tapeKey.unwrapKey().get(), scale, hasSfx);
        Material mat = CASSETTE_MATERIALS.computeIfAbsent(materialKey, k ->
                new Material(ATLAS_LOCATION, tapeKey.value().assetId()));
        return mat.buffer(buffer, hasSfx ? t -> ModRenderTypes.CAMERA_DRAW_SPRITE.apply(t, mat, scale) : RenderType::text);
    }

    public static VertexConsumer getDefaultTapeVC(MultiBufferSource buffer, int scale) {
        boolean hasSfx = hasSfx();
        var materialKey = new DefaultMaterialKey(scale, hasSfx);
        Material mat = BARS_MATERIAL.computeIfAbsent(materialKey, k ->
                new Material(ATLAS_LOCATION, BARS_LOCATION));
        return mat.buffer(buffer, hasSfx ? t -> ModRenderTypes.CAMERA_DRAW_SPRITE.apply(t, mat, scale) : RenderType::text);
    }

    public static VertexConsumer getSmileTapeVC(MultiBufferSource buffer, LivingEntity player) {
        boolean hasSfx = hasSfx();
        Smile smile = Smile.fromHealth(player);
        var materialKey = new SmileMaterialKey(smile, hasSfx);
        Material mat = SMILE_MATERIAL.computeIfAbsent(materialKey, k ->
                new Material(ATLAS_LOCATION, SMILES.get(smile)));
        return mat.buffer(buffer, hasSfx ? t -> ModRenderTypes.CAMERA_DRAW_SPRITE.apply(t, mat, 1) : RenderType::text);
    }

    public static VertexConsumer getFullSpriteVC(ResourceLocation tex, MultiBufferSource buffer, float enderman, int scale) {
        boolean hasSfx = hasSfx();

        RenderType rt = hasSfx ? ModRenderTypes.getCameraDraw(tex, enderman, scale) : RenderType.text(tex);
        return buffer.getBuffer(rt);
    }

    public static void onWorldReload() {
        CASSETTE_MATERIALS.clear();
        BARS_MATERIAL.clear();
        SMILE_MATERIAL.clear();
    }

    private static boolean hasSfx() {
        return LiveFeedRendererManager.LIVE_FEED_BEING_RENDERED == null;
    }


}
