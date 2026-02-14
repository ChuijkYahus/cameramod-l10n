package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.set.BlocksColorAPI;
import net.mehvahdjukaar.vista.client.VistaRenderTypes;
import net.mehvahdjukaar.vista.client.textures.LiveFeedTexture;
import net.mehvahdjukaar.vista.client.textures.LiveFeedTexturesManager;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
import net.mehvahdjukaar.vista.common.tv.IntAnimationState;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.mehvahdjukaar.vista.integration.supplementaries.SuppCompat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class LiveFeedVideoSource implements IVideoSource {

    private final ViewFinderBlockEntity viewFinder;
    private ResourceLocation postShader = null;

    public LiveFeedVideoSource(ViewFinderBlockEntity viewFinder) {
        this.viewFinder = viewFinder;
    }


    public void onItemChanged() {
        ItemStack filterItem = viewFinder.getDisplayedItem();
        this.postShader = null;
        if (filterItem.isEmpty()) {
            return;
        }
        Item item = filterItem.getItem();
        DyeColor color = BlocksColorAPI.getColor(item);
        if (color != null) {
            this.postShader = VistaRenderTypes.getColoredShader(color);
        } else if (CompatHandler.SUPPLEMENTARIES) {
            this.postShader = SuppCompat.getShaderForItem(filterItem.getItem());
        }
    }


    @Override
    public @NotNull VertexConsumer getVideoFrameBuilder(
            float partialTick, MultiBufferSource buffer, boolean shouldUpdate,
            int screenSize, int pixelEffectRes,
            int videoAnimationTick, boolean paused,
            IntAnimationState switchAnim, IntAnimationState staticAnim) {

        LiveFeedTexture tex = LiveFeedTexturesManager.requestLiveFeedTexture(
                viewFinder.getUUID(), screenSize, shouldUpdate, postShader);

        VertexConsumer vc = null;
        if (tex != null) {
            vc = TvScreenVertexConsumers.getLiveFeedVC(buffer, tex, pixelEffectRes, paused, switchAnim, staticAnim);
        }
        if (vc == null) {
            vc = TvScreenVertexConsumers.getNoiseVC(buffer, pixelEffectRes, switchAnim);
        }
        return vc;
    }

}
