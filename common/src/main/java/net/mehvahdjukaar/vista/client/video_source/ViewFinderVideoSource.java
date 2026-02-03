package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.set.BlocksColorAPI;
import net.mehvahdjukaar.vista.client.ModRenderTypes;
import net.mehvahdjukaar.vista.client.textures.LiveFeedTexturesManager;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
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

public class ViewFinderVideoSource implements IVideoSource {

    private final ViewFinderBlockEntity viewFinder;
    private ResourceLocation postShader = null;

    public ViewFinderVideoSource(ViewFinderBlockEntity viewFinder) {
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
            this.postShader = ModRenderTypes.getColoredShader(color);
        } else if (CompatHandler.SUPPLEMENTARIES) {
            this.postShader = SuppCompat.getShaderForItem(filterItem.getItem());
        }
    }


    @Override
    public @NotNull VertexConsumer getVideoFrameBuilder(
            float partialTick, MultiBufferSource buffer, boolean shouldUpdate,
            int screenSize, int pixelEffectRes,
            int videoAnimationTick, int switchAnim, float staticAnim) {

        ResourceLocation tex = LiveFeedTexturesManager.requestLiveFeedTexture(viewFinder.getLevel(),
                viewFinder.getUUID(), screenSize, shouldUpdate, postShader);

        VertexConsumer vc = null;
        if (tex != null) {
            if (ClientConfigs.rendersDebug()) {
                // renderDebug(tex, poseStack, buffer, partialTick, blockEntity);
            }
            vc = TvScreenVertexConsumers.getFullSpriteVC(tex, buffer, staticAnim,
                    pixelEffectRes, switchAnim);
        }
        if (vc == null) {
            vc = buffer.getBuffer(ModRenderTypes.NOISE);
        }
        return vc;
    }

}
