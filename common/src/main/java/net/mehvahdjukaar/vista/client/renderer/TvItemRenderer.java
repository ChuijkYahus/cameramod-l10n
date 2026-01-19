package net.mehvahdjukaar.vista.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.client.ItemStackRenderer;
import net.mehvahdjukaar.moonlight.api.client.util.VertexUtil;
import net.mehvahdjukaar.vista.client.textures.CassetteVertexConsumers;
import net.mehvahdjukaar.vista.common.TelevisionItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class TvItemRenderer extends ItemStackRenderer {

    @Override
    public void renderByItem(ItemStack itemStack, ItemDisplayContext itemDisplayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int light, int overlay) {
        renderTvHead(itemStack, itemDisplayContext, poseStack, buffer, light, overlay,
                Minecraft.getInstance().player);
    }

    public static void renderTvHead(ItemStack itemStack, ItemDisplayContext itemDisplayContext, PoseStack poseStack,
                                     MultiBufferSource buffer, int light, int overlay,
                                     LivingEntity le) {
        Block block = ((TelevisionItem) itemStack.getItem()).getBlock();

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(), poseStack, buffer, light, overlay);

        if (itemDisplayContext == ItemDisplayContext.HEAD) {
            VertexConsumer vc = CassetteVertexConsumers.getSmileTapeVC(buffer, le);

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
