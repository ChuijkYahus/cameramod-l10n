package net.mehvahdjukaar.vista.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.util.VertexUtil;
import net.mehvahdjukaar.moonlight.api.misc.RollingBuffer;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.CassetteTape;
import net.mehvahdjukaar.vista.common.TVBlock;
import net.mehvahdjukaar.vista.common.TVBlockEntity;
import net.mehvahdjukaar.vista.integration.exposure.ExposureCompatClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class TvBlockEntityRenderer implements BlockEntityRenderer<TVBlockEntity> {

    public TvBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static final int SCREEN_RESOLUTION_SCALE = 8;

    @Override
    public void render(TVBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int light, int overlay) {

        if (!blockEntity.getBlockState().getValue(TVBlock.POWERED)) return;

        Direction dir = blockEntity.getBlockState().getValue(TVBlock.FACING);

        LOD lod = LOD.at(blockEntity);

        if (lod.isPlaneCulled(dir, 0.5f, 0f)) {
            return;
        }

        boolean drawingCamera = LiveFeedRendererManager.LIVE_FEED_BEING_RENDERED != null;

        VertexConsumer vc = null;

        int screenPixelSize = blockEntity.getScreenPixelSize();
        UUID liveFeedId = blockEntity.getLinkedFeedUUID();
        Holder<CassetteTape> tape = blockEntity.getTape();

        ItemStack stack = blockEntity.getDisplayedItem();

        if (liveFeedId != null) {

            ResourceLocation tex = LiveFeedRendererManager.requestLiveFeedTexture(blockEntity.getLevel(),
                    liveFeedId, screenPixelSize * SCREEN_RESOLUTION_SCALE);
            if (tex != null) {
                renderDebugStats(tex, poseStack, buffer, partialTick);
                vc = TapeTextureManager.getFullSpriteVC(tex, buffer, drawingCamera);


            } else {
                vc = TapeTextureManager.getDefaultTapeVC(buffer, drawingCamera);
            }

        } else if (tape != null) {
            if (drawingCamera) {
                Material mat = TapeTextureManager.getMaterialFlat(tape);
                vc = mat.buffer(buffer, RenderType::text);
            } else {
                Material mat = TapeTextureManager.getMaterial(tape);

                vc = mat.buffer(buffer, t -> ModRenderTypes.CAMERA_DRAW_SPRITE.apply(t, mat));
            }
        } else if (VistaMod.EXPOSURE_ON) {
            ResourceLocation texture = ExposureCompatClient.getPictureTextureForRenderer(stack, blockEntity.getAnimationTick());
            if (texture != null) {
                vc = TapeTextureManager.getFullSpriteVC(texture, buffer, drawingCamera);
            }
        }
        if (vc == null) {
            vc = buffer.getBuffer(ModRenderTypes.NOISE);
        }

        float yaw = dir.toYRot();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        poseStack.translate(-0.5, -0.5, -0.5);

        light = LightTexture.FULL_BRIGHT;

        float s = screenPixelSize / 32f;

        poseStack.translate(0.5, 0.5, -0.001);

        //leave to avoid buffer bug
        //we could have also used whats passed in the shader and dont use a material

        int lightU = light & 0xFFFF;
        int lightV = (light >> 16) & 0xFFFF;
        VertexUtil.addQuad(vc, poseStack, -s, -s, s, s, lightU, lightV);
    }

    private void renderDebugStats(ResourceLocation tex, PoseStack poseStack, MultiBufferSource buffer, float partialTick) {
        var font = Minecraft.getInstance().font;

        poseStack.pushPose();
        poseStack.translate(0,1.5,0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90));
        poseStack.scale(1/16f, -1/16f, 1/16f);
        poseStack.scale(0.5f, 0.5f, 0.5f);

        RollingBuffer<Long> lastUpdateTimes = LiveFeedRendererManager.UPDATE_TIMES.get(tex);

        float averageUpdateinterval = calculateAverageUpdateTime(lastUpdateTimes);

        try {
            font.drawInBatch("int " + averageUpdateinterval, 0, 0, -1,
                    false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL,
                    OverlayTexture.NO_OVERLAY,
                    LightTexture.FULL_BRIGHT);

              long lastUpdateTime = lastUpdateTimes.get(lastUpdateTimes.size() - 1);

              font.drawInBatch("last " + lastUpdateTime, 0, -9, -1, false, poseStack.last().pose(), buffer,Font.DisplayMode.NORMAL,
                      OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);

              var frameMs = LiveFeedRendererManager.SCHEDULER.getEmaFrameMs();
              var updateMs = LiveFeedRendererManager.SCHEDULER.getEmaUpdateMs();

            font.drawInBatch("ums " + frameMs, 0, -18, -1, false, poseStack.last().pose(), buffer,Font.DisplayMode.NORMAL,
                    OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);

            font.drawInBatch(String.format("updateMs %.2f" , updateMs), 0, -27, -1, false, poseStack.last().pose(), buffer,Font.DisplayMode.NORMAL,
                    OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);

        }catch (Exception e){}
        poseStack.popPose();
    }

    long calculateAverageUpdateTime(RollingBuffer<Long> lastUpdateTimes) {
        int size = lastUpdateTimes.size();
        if (size < 2) return 0; // Need at least 2 timestamps to compute intervals

        long totalDiff = 0;
        for (int i = 1; i < size; i++) {
            totalDiff += lastUpdateTimes.get(i) - lastUpdateTimes.get(i - 1);
        }

        long avgNano = totalDiff / (size - 1);
        return avgNano; // average in nanoseconds
    }


}
