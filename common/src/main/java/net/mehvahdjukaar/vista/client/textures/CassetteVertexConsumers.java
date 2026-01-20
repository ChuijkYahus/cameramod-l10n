package net.mehvahdjukaar.vista.client.textures;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.ModRenderTypes;
import net.mehvahdjukaar.vista.common.CassetteTape;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CassetteVertexConsumers {

    private static final ResourceLocation BARS_LOCATION = VistaMod.res("color_bars");
    private static final ResourceLocation SMILE_LOCATION = VistaMod.res("smile");
    private static final ResourceLocation NEUTRAL_LOCATION = VistaMod.res("neutral");
    private static final ResourceLocation SAD_LOCATION = VistaMod.res("sad");
    private static final Map<Smile, ResourceLocation> SMILES = Map.of(
            Smile.HAPPY, SMILE_LOCATION,
            Smile.NEUTRAL, NEUTRAL_LOCATION,
            Smile.SAD, SAD_LOCATION
    );

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

    public static VertexConsumer getTapeVC(Holder<CassetteTape> tapeKey, MultiBufferSource buffer, int scale,
                                           int tickCount, int switchAnim) {
        ResourceLocation tapeTexture = tapeKey.value().assetId();
        return createVC(buffer, scale, tapeTexture, tickCount, switchAnim);
    }

    public static VertexConsumer getDefaultTapeVC(MultiBufferSource buffer, int scale,int switchAnim) {
        return createVC(buffer, scale, BARS_LOCATION, 0, switchAnim);
    }

    private static @NotNull VertexConsumer createVC(MultiBufferSource buffer, int scale,
                                                    ResourceLocation id, int tickCount, int switchAnim) {
        boolean hasSfx =  hasSfx();
        SimpleAnimatedTexture animatedText = CassetteTexturesManager.INSTANCE.getAnimatedTexture(id);

        if (animatedText == null) {
            if (id == BARS_LOCATION) {
                return buffer.getBuffer(RenderType.entityCutout(MissingTextureAtlasSprite.getLocation()));
            } else return getDefaultTapeVC(buffer, scale, switchAnim);
        }
        RenderType rt = hasSfx ? ModRenderTypes.CAMERA_DRAW_SPRITE.apply(animatedText, scale, switchAnim) : RenderType.text(animatedText.location());
        VertexConsumer inner = buffer.getBuffer(rt);
        return new AnimatedTextureVertexConsumer(tickCount, animatedText.getStripData(), inner);
    }

    public static VertexConsumer getSmileTapeVC(MultiBufferSource buffer, LivingEntity player) {
        Smile smile = Smile.fromHealth(player);
        ResourceLocation id = SMILES.get(smile);
        return createVC(buffer, 1, id, player.tickCount, 0);
    }

    public static VertexConsumer getFullSpriteVC(ResourceLocation tex, MultiBufferSource buffer, float enderman, int scale) {
        boolean hasSfx = hasSfx();

        RenderType rt = hasSfx ? ModRenderTypes.getCameraDraw(tex, enderman, scale) : RenderType.text(tex);
        return buffer.getBuffer(rt);
    }

    private static boolean hasSfx() {
        return LiveFeedTexturesManager.LIVE_FEED_BEING_RENDERED == null;
    }


}
