package net.mehvahdjukaar.vista.client.textures;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.VistaRenderTypes;
import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import net.mehvahdjukaar.vista.common.cassette.CassetteTape;
import net.mehvahdjukaar.vista.common.tv.IntAnimationState;
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

    @Nullable
    public static VertexConsumer getTapeVC(MultiBufferSource buffer, Holder<CassetteTape> tapeKey, int scale,
                                           int tickCount, boolean paused, IntAnimationState switchAnim) {
        ResourceLocation tapeTexture = tapeKey.value().assetId();
        return createAnimatedStripVC(buffer, tapeTexture, scale, tickCount, paused, switchAnim);
    }

    @Nullable
    public static VertexConsumer getBarsVC(MultiBufferSource buffer, int scale, IntAnimationState switchAnim) {
        return createAnimatedStripVC(buffer, BARS_LOCATION, scale, 0, false, switchAnim);
    }

    @Nullable
    public static VertexConsumer getSmileTapeVC(MultiBufferSource buffer, LivingEntity player) {
        Smile smile = Smile.fromHealth(player);
        ResourceLocation id = SMILES.get(smile);
        int scale = 1;//always on a 1x1 tv
        return createAnimatedStripVC(buffer, id, scale, player.tickCount, false, IntAnimationState.NO_ANIM);
    }

    @Nullable
    private static VertexConsumer createAnimatedStripVC(MultiBufferSource buffer,
                                                        ResourceLocation id,
                                                        int scale, int tickCount,
                                                        boolean paused,
                                                        IntAnimationState switchAnim) {
        boolean hasSfx = hasSfx();
        if (!hasSfx && switchAnim.isDecreasing()) return null;

        AnimatedStripTexture animatedStrip = CassetteTexturesManager.INSTANCE.getAnimatedTexture(id);
        if (animatedStrip == null) return null;

        ResourceLocation overlay = paused ? VistaModClient.PAUSE_OVERLAY : null;

        ResourceLocation textureId = animatedStrip.getTextureLocation();
        AnimationStripData stripData = animatedStrip.getStripData();
        RenderType rt = hasSfx ?
                VistaRenderTypes.crtRenderType(textureId, scale,
                        stripData.frameRelativeW(),
                        stripData.frameRelativeH(),
                        switchAnim, IntAnimationState.NO_ANIM,
                        overlay) :
                RenderType.text(textureId);
        VertexConsumer inner = buffer.getBuffer(rt);
        return new AnimatedStripVertexConsumer(tickCount, stripData, inner);
    }

    @Nullable
    public static VertexConsumer getLiveFeedVC(MultiBufferSource buffer,
                                               LiveFeedTexture tex,
                                               int scale, boolean paused,
                                               IntAnimationState switchAnim,
                                               IntAnimationState enderman) {
        boolean hasSfx = hasSfx();
        if (!hasSfx && switchAnim.isDecreasing()) return null;
        ResourceLocation overlay = paused ? VistaModClient.PAUSE_OVERLAY : null;

        ResourceLocation textureId = tex.getTextureLocation();
        RenderType rt = hasSfx ?
                VistaRenderTypes.crtRenderType(textureId, scale,
                        1, 1, switchAnim,
                        enderman, overlay) :
                RenderType.text(textureId);
        return buffer.getBuffer(rt);
    }


    private static boolean hasSfx() {
        return !VistaLevelRenderer.isRenderingLiveFeed() &&
                Minecraft.getInstance().options.graphicsMode().get() != GraphicsStatus.FAST;
    }


}
