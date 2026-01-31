package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.client.textures.LiveFeedTexturesManager;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class ViewFinderVideoSource implements IVideoSource {


    private final ViewFinderBlockEntity viewFinder;

    public ViewFinderVideoSource(ViewFinderBlockEntity viewFinder) {
        this.viewFinder = viewFinder;
    }


    @Override
    public @Nullable VertexConsumer getVideoFrameBuilder(TVBlockEntity targetScreen, float partialTick, MultiBufferSource buffer, boolean shouldUpdate, int screenSize, int pixelEffectRes, int switchOnAnim) {

        ResourceLocation postShader = viewFinder.getPostShader();

        ResourceLocation tex = LiveFeedTexturesManager.requestLiveFeedTexture(viewFinder.getLevel(),
                viewFinder.getUUID(), screenSize, shouldUpdate, postShader);

        if (tex != null) {
            if (ClientConfigs.rendersDebug()) {
                // renderDebug(tex, poseStack, buffer, partialTick, blockEntity);
            }
            float enderman = targetScreen.getLookingAtEndermanAnimation(partialTick);
            vc = TvScreenVertexConsumers.getFullSpriteVC(tex, buffer, enderman, pixelEffectRes, switchAnim);
        } else {
            vc = TvScreenVertexConsumers.getMissingTapeVC(buffer, pixelEffectRes, switchAnim);
        }
        return vc;
    }

}
