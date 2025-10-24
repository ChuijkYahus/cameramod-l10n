package net.mehvahdjukaar.vista.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.client.ItemStackRenderer;
import net.mehvahdjukaar.moonlight.api.client.util.VertexUtil;
import net.mehvahdjukaar.vista.common.TelevisionItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class TvItemRenderer extends ItemStackRenderer {

    @Override
    public void renderByItem(ItemStack itemStack, ItemDisplayContext itemDisplayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int light, int overlay) {
        Block block = ((TelevisionItem) itemStack.getItem()).getBlock();

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(), poseStack, buffer, light, overlay);

        if (itemDisplayContext == ItemDisplayContext.HEAD) {
            boolean drawingCamera = LiveFeedRendererManager.LIVE_FEED_BEING_RENDERED != null;
            //TODO: make this using armor layer so we have entity context
            VertexConsumer vc = TapeTextureManager.getSmileTapeVC(buffer, Minecraft.getInstance().player,  drawingCamera);

            //bugged when looking up. draw shader must be bugged
            int lightU = light & 0xFFFF;
            int lightV = (light >> 16) & 0xFFFF;
            float s = 6 / 16f;
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, -0.005f);
            VertexUtil.addQuad(vc, poseStack, -s, -s, s, s, lightU, lightV);
            poseStack.popPose();
        }
    }
}
