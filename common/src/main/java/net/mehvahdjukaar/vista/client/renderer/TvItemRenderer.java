package net.mehvahdjukaar.vista.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.client.ItemStackRenderer;
import net.mehvahdjukaar.moonlight.api.client.util.VertexUtil;
import net.mehvahdjukaar.vista.client.VistaRenderTypes;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
import net.mehvahdjukaar.vista.common.tv.TVItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import static net.mehvahdjukaar.vista.client.renderer.TvBlockEntityRenderer.addQuad;

public class TvItemRenderer extends ItemStackRenderer {

    @Override
    public void renderByItem(ItemStack itemStack, ItemDisplayContext itemDisplayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        renderTvHead(itemStack, itemDisplayContext, poseStack, buffer, light, overlay,
                Minecraft.getInstance().player);
        poseStack.popPose();
    }

    public static void renderTvHead(ItemStack itemStack, ItemDisplayContext itemDisplayContext, PoseStack poseStack,
                                    MultiBufferSource buffer, int light, int overlay,
                                    LivingEntity le) {
        poseStack.pushPose();
        Block block = ((TVItem) itemStack.getItem()).getBlock();
        poseStack.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(), poseStack, buffer, light, overlay);

        if (itemDisplayContext == ItemDisplayContext.HEAD) {
            VertexConsumer vc = TvScreenVertexConsumers.getSmileTapeVC(buffer, le);
            if (vc == null) vc = buffer.getBuffer(VistaRenderTypes.NOISE);

            //bugged when looking up. draw shader must be bugged
            float s = 6 / 16f;
            poseStack.translate(0.5, 0.5, -0.005f);

            addQuad(vc, poseStack, -s, -s, s, s, light);
        }
        poseStack.popPose();
    }
}
