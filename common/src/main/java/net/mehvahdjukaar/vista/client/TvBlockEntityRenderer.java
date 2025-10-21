package net.mehvahdjukaar.vista.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.vista.common.CassetteTape;
import net.mehvahdjukaar.vista.common.TVBlock;
import net.mehvahdjukaar.vista.common.TVBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
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

        VertexConsumer vc;

        int screenPixelSize = blockEntity.getScreenPixelSize();
        UUID liveFeedId = blockEntity.getLinkedFeedUUID();
        Holder<CassetteTape> tape = blockEntity.getTape();

        if (liveFeedId != null) {

            ResourceLocation tex = LiveFeedRendererManager.requestLiveFeedTexture(blockEntity.getLevel(),
                    liveFeedId, screenPixelSize * SCREEN_RESOLUTION_SCALE);
            if (tex != null) {
                vc = buffer.getBuffer(ModRenderTypes.CAMERA_DRAW.apply(tex));
            } else {
                vc = TapeTextureManager.DEFAULT_MATERIAL.buffer(buffer, ModRenderTypes.CAMERA_DRAW);
            }


        } else if (tape != null) {
            Material mat = TapeTextureManager.getMaterial(tape);

            vc = mat.buffer(buffer, t -> ModRenderTypes.CAMERA_DRAW_SPRITE.apply(t, mat));
        } else {
            vc = buffer.getBuffer(ModRenderTypes.CAMERA_DRAW_STATIC);
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

        //leave to avoid buffer bug
        //we could have also used whats passed in the shader and dont use a material
        vc.addVertex(pose, -s, -s, 0);
        vc.setColor(-1);
        vc.setUv(1f, 1f);
        vc.setOverlay(overlay);
        vc.setLight(light);
        vc.setNormal(pose, 0f, 0f, -1f);
        vc.addVertex(pose, -s, s, 0);
        vc.setColor(-1);
        vc.setUv(1f, 0f);
        vc.setOverlay(overlay);
        vc.setLight(light);
        vc.setNormal(pose, 0f, 0f, -1f);
        vc.addVertex(pose, s, s, 0);
        vc.setColor(-1);
        vc.setUv(0f, 0f);
        vc.setOverlay(overlay);
        vc.setLight(light);
        vc.setNormal(pose, 0f, 0f, -1f);
        vc.addVertex(pose, s, -s, 0);
        vc.setColor(-1);
        vc.setUv(0f, 1f);
        vc.setOverlay(overlay);
        vc.setLight(light);
        vc.setNormal(pose, 0f, 0f, -1f);
    }


}
