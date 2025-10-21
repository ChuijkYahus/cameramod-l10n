package net.mehvahdjukaar.vista.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.vista.common.CassetteTape;
import net.mehvahdjukaar.vista.common.TVBlock;
import net.mehvahdjukaar.vista.common.TVBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class TvBlockEntityRenderer implements BlockEntityRenderer<TVBlockEntity> {

    public TvBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static final int SCREEN_RESOLUTION_SCALE = 8;

    @Override
    public void render(TVBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int light, int overlay) {

        if (!blockEntity.getBlockState().getValue(TVBlock.POWERED)) return;

        VertexConsumer vertexConsumer;

        int screenPixelSize = blockEntity.getScreenPixelSize();
        UUID liveFeedId = blockEntity.getLinkedFeedUUID();
        Holder<CassetteTape> tape = blockEntity.getTape();

        if (liveFeedId != null) {
            ResourceLocation tex = LiveFeedRendererManager.requestLiveFeedTexture(blockEntity.getLevel(),
                    liveFeedId, screenPixelSize * SCREEN_RESOLUTION_SCALE);

            vertexConsumer = buffer.getBuffer(ModRenderTypes.CAMERA_DRAW.apply(tex));

        } else if (tape != null) {
            Material mat = TapeTextureManager.getMaterial(tape);

            vertexConsumer = mat.buffer(buffer, RenderType::text);

        } else {

            vertexConsumer = buffer.getBuffer(ModRenderTypes.CAMERA_DRAW_STATIC);
        }

        Direction dir = blockEntity.getBlockState().getValue(TVBlock.FACING);
        float yaw = dir.toYRot();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        poseStack.translate(-0.5, -0.5, -0.5);

        light = LightTexture.FULL_BRIGHT;
        overlay = OverlayTexture.NO_OVERLAY;

        float s = screenPixelSize / 32f;
        PoseStack.Pose pose = poseStack.last();

        poseStack.translate(0.5, 0.5, -0.001);

        vertexConsumer.addVertex(pose, -s, -s, 0).setColor(-1)
                .setUv(1f, 1f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);
        vertexConsumer.addVertex(pose, -s, s, 0).setColor(-1)
                .setUv(1f, 0f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);

        vertexConsumer.addVertex(pose, s, s, 0).setColor(-1)
                .setUv(0f, 0f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);
        vertexConsumer.addVertex(pose, s, -s, 0).setColor(-1)
                .setUv(0f, 1f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);


    }


}
