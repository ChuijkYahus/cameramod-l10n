package net.mehvahdjukaar.vista.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.vista.common.TVBlock;
import net.mehvahdjukaar.vista.common.TVBlockEntity;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.FrameBufferBackedDynamicTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;

import java.util.UUID;

public class TvBlockEntityRenderer implements BlockEntityRenderer<TVBlockEntity> {

    public TvBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static final int SCREEN_RESOLUTION_SCALE = 8;

    @Override
    public void render(TVBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int light, int overlay) {

        if (!blockEntity.getBlockState().getValue(TVBlock.LIT)) return;

        UUID liveFeedId = blockEntity.getLinkedFeedUUID();
        int screenPixelSize = blockEntity.getScreenPixelSize();
        if (liveFeedId == null) return;

        FrameBufferBackedDynamicTexture tex = LiveFeedRendererManager.requestLiveFeedTexture(
                liveFeedId, screenPixelSize * SCREEN_RESOLUTION_SCALE);

        if (!tex.isInitialized()) return;

        Direction dir = blockEntity.getBlockState().getValue(TVBlock.FACING);
        float yaw = dir.toYRot();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        poseStack.translate(-0.5, -0.5, -0.5);

        light = LightTexture.FULL_BRIGHT;
        overlay = OverlayTexture.NO_OVERLAY;

        float s = screenPixelSize / 32f;
        VertexConsumer vertexConsumer = buffer.getBuffer(ModRenderTypes.CAMERA_DRAW.apply(tex));
        PoseStack.Pose pose = poseStack.last();

        poseStack.translate(0.5, 0.5, -0.001);

        vertexConsumer.addVertex(pose, -s, -s, 0).setColor(1f, 1f, 1f, 1f).setUv(1f, 0f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);
        vertexConsumer.addVertex(pose, -s, s, 0).setColor(1f, 1f, 1f, 1f).setUv(1f, 1f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);

        vertexConsumer.addVertex(pose, s, s, 0).setColor(1f, 1f, 1f, 1f).setUv(0f, 1f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);
        vertexConsumer.addVertex(pose, s, -s, 0).setColor(1f, 1f, 1f, 1f).setUv(0f, 0f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);


    }


}
