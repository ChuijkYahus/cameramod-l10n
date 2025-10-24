package net.mehvahdjukaar.vista.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.util.VertexUtil;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.CassetteTape;
import net.mehvahdjukaar.vista.common.TVBlock;
import net.mehvahdjukaar.vista.common.TVBlockEntity;
import net.mehvahdjukaar.vista.integration.ExposureCompat;
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

    private static final int SCREEN_RESOLUTION_SCALE = 8;

    @Override
    public void render(TVBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int light, int overlay) {

        if (!blockEntity.getBlockState().getValue(TVBlock.POWERED)) return;

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
            ResourceLocation texture = ExposureCompat.getPictureTextureForRenderer(stack, blockEntity.getAnimationTick());
            if (texture != null) {
                vc = TapeTextureManager.getFullSpriteVC(texture, buffer, drawingCamera);
            }
        }
        if (vc == null) {
            vc = buffer.getBuffer(ModRenderTypes.NOISE);
        }

        Direction dir = blockEntity.getBlockState().getValue(TVBlock.FACING);
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


}
