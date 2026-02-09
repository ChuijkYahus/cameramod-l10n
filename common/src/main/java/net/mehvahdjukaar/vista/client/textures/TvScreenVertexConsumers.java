package net.mehvahdjukaar.vista.client.textures;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.VistaRenderTypes;
import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import net.mehvahdjukaar.vista.common.cassette.CassetteTape;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class TvScreenVertexConsumers {

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
        return createAnimatedStripVC(buffer, scale, tapeTexture, tickCount, switchAnim);
    }


    public static VertexConsumer getBarsVC(MultiBufferSource buffer, int scale, int switchAnim) {
        return createAnimatedStripVC(buffer, scale, BARS_LOCATION, 0, switchAnim, null);
    }

    private static VertexConsumer createAnimatedStripVC(MultiBufferSource buffer, int scale,
                                                        ResourceLocation id, int tickCount,
                                                        int switchAnim, @Nullable ResourceLocation overlay) {
        boolean hasSfx = hasSfx();
        if (!hasSfx && switchAnim < 0) {
            return buffer.getBuffer(VistaRenderTypes.NOISE);
        }

        AnimatedStripTexture animatedText = CassetteTexturesManager.INSTANCE.getAnimatedTexture(id);

        if (animatedText == null) {
            return buffer.getBuffer(VistaRenderTypes.NOISE);
        }
        RenderType rt = hasSfx ? VistaRenderTypes.crtRenderType(animatedText, scale, switchAnim, overlay) : RenderType.text(animatedText.textureId());
        VertexConsumer inner = buffer.getBuffer(rt);
        return new AnimatedStripVertexConsumer(tickCount, animatedText.getStripData(), inner);
    }

    public static VertexConsumer getSmileTapeVC(MultiBufferSource buffer, LivingEntity player) {
        Smile smile = Smile.fromHealth(player);
        ResourceLocation id = SMILES.get(smile);
        return createAnimatedStripVC(buffer, 1, id, player.tickCount, 0);
    }

    @Nullable
    public static VertexConsumer getFullSpriteVC(ResourceLocation tex, MultiBufferSource buffer,
                                                 float enderman, int scale, int switchAnim) {
        boolean hasSfx = hasSfx();
        if (!hasSfx && switchAnim < 0) return null;

        RenderType rt = hasSfx ? VistaRenderTypes.crtRenderType(tex,scale, switchAnim, enderman) : RenderType.text(tex);
        return buffer.getBuffer(rt);
    }

    private static boolean hasSfx() {
        return !VistaLevelRenderer.isRenderingLiveFeed() &&
                Minecraft.getInstance().options.graphicsMode().get() != GraphicsStatus.FAST;
    }


}
