package net.mehvahdjukaar.vista.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.util.LOD;
import net.mehvahdjukaar.moonlight.api.client.util.VertexUtil;
import net.mehvahdjukaar.moonlight.api.misc.RollingBuffer;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.CassetteTape;
import net.mehvahdjukaar.vista.common.TVBlock;
import net.mehvahdjukaar.vista.common.TVBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.mehvahdjukaar.vista.integration.exposure.ExposureCompatClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class TvBlockEntityRenderer implements BlockEntityRenderer<TVBlockEntity> {

    public TvBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public void render(TVBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int light, int overlay) {

        if (!blockEntity.getBlockState().getValue(TVBlock.POWERED)) return;

        Direction dir = blockEntity.getBlockState().getValue(TVBlock.FACING);

        LOD lod = LOD.at(blockEntity);
        //TODO: change with tv size

        if (lod.isPlaneCulled(dir, 0.5f, 0f)) {
            return;
        }

        float yaw = dir.toYRot();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        poseStack.translate(-0.5, -0.5, -0.5);

        boolean drawingCamera = LiveFeedRendererManager.LIVE_FEED_BEING_RENDERED != null;

        VertexConsumer vc = null;

        int screenPixelSize = blockEntity.getScreenPixelSize();
        UUID liveFeedId = blockEntity.getLinkedFeedUUID();
        Holder<CassetteTape> tape = blockEntity.getTape();

        ItemStack stack = blockEntity.getDisplayedItem();

        if (liveFeedId != null) {

            boolean shouldUpdate = lod.within(ClientConfigs.UPDATE_DISTANCE.get());
            ResourceLocation tex = LiveFeedRendererManager.requestLiveFeedTexture(blockEntity.getLevel(),
                    liveFeedId, screenPixelSize, shouldUpdate);
            if (tex != null) {
                maybeRenderDebug(tex, poseStack, buffer, partialTick, blockEntity);
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


        light = LightTexture.FULL_BRIGHT;

        float s = screenPixelSize / 32f;

        poseStack.translate(0.5, 0.5, -0.001);

        int lightU = light & 0xFFFF;
        int lightV = (light >> 16) & 0xFFFF;
        VertexUtil.addQuad(vc, poseStack, -s, -s, s, s, lightU, lightV);
    }

    private void maybeRenderDebug(ResourceLocation tex, PoseStack poseStack, MultiBufferSource buffer, float partialTick,
                                  TVBlockEntity tile) {
        if (!ClientConfigs.isDebugOn()) return;
        poseStack.pushPose();

        try {
            Font font = Minecraft.getInstance().font;

            poseStack.translate(1, 1.5, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-180));
            poseStack.scale(1 / 16f, -1 / 16f, 1 / 16f);
            poseStack.scale(0.5f, 0.5f, 0.5f);

            RollingBuffer<Long> lastUpdateTimes = LiveFeedRendererManager.UPDATE_TIMES.get(tex);

            double averageUpdateinterval = calculateAverageUpdateTime(lastUpdateTimes);

            int y = 0;
            font.drawInBatch(String.format("up rate %.2f", averageUpdateinterval), 0, 7, -1,
                    false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL,
                    OverlayTexture.NO_OVERLAY,
                    LightTexture.FULL_BRIGHT);

            double updateMs = LiveFeedRendererManager.SCHEDULER.get().getAverageUpdateTimeMs();

            y-=9;
            font.drawInBatch(String.format("up ms %.2f", updateMs), 0, y, -1, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL,
                    OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);

            if( tile.canSeeEnderman) {
                y-=9;

                font.drawInBatch("p" , 0, y, -1, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL,
                        OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);

            }
        } catch (Exception ignored) {
            int aa = 1;
        }
        poseStack.popPose();
    }

    static double calculateAverageUpdateTime(RollingBuffer<Long> lastUpdateTimes) {
        int size = lastUpdateTimes.size();
        if (size < 2) return 0; // Need at least 2 timestamps to compute intervals

        long totalDiff = 0;
        for (int i = 1; i < size; i++) {
            totalDiff += lastUpdateTimes.get(i) - lastUpdateTimes.get(i - 1);
        }

        return totalDiff / (size - 1d); // average in nanoseconds
    }


}
