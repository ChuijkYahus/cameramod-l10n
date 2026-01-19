package net.mehvahdjukaar.vista.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.util.LOD;
import net.mehvahdjukaar.moonlight.api.client.util.VertexUtil;
import net.mehvahdjukaar.moonlight.api.misc.ForgeOverride;
import net.mehvahdjukaar.moonlight.api.misc.RollingBuffer;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.set.BlocksColorAPI;
import net.mehvahdjukaar.vista.client.textures.LiveFeedTexturesManager;
import net.mehvahdjukaar.vista.client.ModRenderTypes;
import net.mehvahdjukaar.vista.client.textures.CassetteVertexConsumers;
import net.mehvahdjukaar.vista.common.CassetteTape;
import net.mehvahdjukaar.vista.common.tv.TVBlock;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.mehvahdjukaar.vista.integration.exposure.ExposureCompatClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;

import java.util.UUID;

public class TvBlockEntityRenderer implements BlockEntityRenderer<TVBlockEntity> {

    public TvBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public boolean shouldRenderOffScreen(TVBlockEntity blockEntity) {
        return PlatHelper.getPlatform().isFabric();
    }

    @ForgeOverride
    public AABB getRenderBoundingBox(BlockEntity tile) {
        AABB aabb = new AABB(tile.getBlockPos());
        Direction dir = tile.getBlockState().getValue(TVBlock.FACING);
        float width = ((TVBlockEntity) tile).getScreenPixelWidth() / 16f;
        float height = ((TVBlockEntity) tile).getConnectedHeight() / 16f;
        return switch (dir) {
            case NORTH -> aabb.expandTowards(-(width - 1), height - 1, 0);
            case SOUTH -> aabb.expandTowards(0, height - 1, width - 1);
            case WEST -> aabb.expandTowards(0, height - 1, -(width - 1));
            case EAST -> aabb.expandTowards(width - 1, height - 1, 0);
            default -> aabb;
        };
    }


    @Override
    public void render(TVBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int light, int overlay) {

        if (!blockEntity.isPowered()) return;

        Direction dir = blockEntity.getBlockState().getValue(TVBlock.FACING);

        LOD lod = LOD.at(blockEntity);
        int screenSize = blockEntity.getScreenPixelWidth();

        Vec2 screenCenter = blockEntity.getScreenBlockCenter();

        if (!lod.isMedium()) return;
        if (lod.isPlaneCulled(dir, 0.5f, screenSize * 1.5f, 0f)) return;

        float yaw = dir.toYRot();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        poseStack.translate(-screenCenter.x ,screenCenter.y , -0.501);

        float s = screenSize / 32f;
        int pixelEffectRes = ClientConfigs.SCALE_PIXELS.get() ? screenSize : TVBlockEntity.MIN_SCREEN_PIXEL_SIZE;

        VertexConsumer vc = null;

        UUID liveFeedId = blockEntity.getLinkedFeedUUID();
        Holder<CassetteTape> tape = blockEntity.getTape();

        ItemStack stack = blockEntity.getDisplayedItem();

        if (liveFeedId != null) {

            ResourceLocation postShader = getPostShader(blockEntity);

            boolean shouldUpdate = lod.within(ClientConfigs.UPDATE_DISTANCE.get());
            ResourceLocation tex = LiveFeedTexturesManager.requestLiveFeedTexture(blockEntity.getLevel(),
                    liveFeedId, screenSize, shouldUpdate, postShader);
            if (tex != null) {
                if (ClientConfigs.rendersDebug()) {
                    renderDebug(tex, poseStack, buffer, partialTick, blockEntity);
                }
                float enderman = blockEntity.getLookingAtEndermanAnimation(partialTick);
                vc = CassetteVertexConsumers.getFullSpriteVC(tex, buffer, enderman, pixelEffectRes);
            } else {
                vc = CassetteVertexConsumers.getDefaultTapeVC(buffer, pixelEffectRes);
            }

        } else if (tape != null) {
            vc = CassetteVertexConsumers.getTapeVC(tape, buffer, pixelEffectRes, blockEntity.getAnimationTick());
        } else if (CompatHandler.EXPOSURE) {
            ResourceLocation texture = ExposureCompatClient.getPictureTextureForRenderer(stack, blockEntity.getAnimationTick());
            if (texture != null) {
                vc = CassetteVertexConsumers.getFullSpriteVC(texture, buffer, 0, pixelEffectRes);
            }
        }
        if (vc == null) {
            vc = buffer.getBuffer(ModRenderTypes.NOISE);
        }


        light = LightTexture.FULL_BRIGHT;


        int lightU = light & 0xFFFF;
        int lightV = (light >> 16) & 0xFFFF;
        VertexUtil.addQuad(vc, poseStack, -s, -s, s, s, lightU, lightV);
    }

    private ResourceLocation getPostShader(TVBlockEntity blockEntity) {
        ItemStack filterItem = blockEntity.getDisplayedItem();
        if (filterItem.isEmpty())return null;
        Item item = filterItem.getItem();
        DyeColor color = BlocksColorAPI.getColor(item);
        if (color != null) {
          //  return ModRenderTypes.getColorFilter(color);
        }
        if(CompatHandler.SUPPLEMENTARIES){
            //TODO:
//            return SuppCompat.getShaderForItem(filterItem);
        }
        return null;
    }

    // ========== DEBUG RENDERING ========== //


    private void renderDebug(ResourceLocation tex, PoseStack poseStack, MultiBufferSource buffer, float partialTick,
                             TVBlockEntity tile) {

        poseStack.pushPose();

        Font font = Minecraft.getInstance().font;

        poseStack.translate(1, 1.5, -0.1);
        poseStack.mulPose(Axis.YP.rotationDegrees(-180));
        poseStack.scale(1 / 16f, -1 / 16f, 1 / 16f);
        poseStack.scale(0.5f, 0.5f, 0.5f);

        RollingBuffer<Long> lastUpdateTimes = LiveFeedTexturesManager.UPDATE_TIMES.get(tex);

        double averageUpdateinterval = calculateAverageUpdateTime(lastUpdateTimes);

        int y = 0;
        font.drawInBatch(String.format("up rate %.2f", averageUpdateinterval), 0, 7, -1,
                false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL,
                OverlayTexture.NO_OVERLAY,
                LightTexture.FULL_BRIGHT);

        double updateMs = LiveFeedTexturesManager.SCHEDULER.get().getAverageUpdateTimeMs();

        y -= 9;
        font.drawInBatch(String.format("up ms %.2f", updateMs), 0, y, -1, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL,
                OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);

        float endermanAnim = tile.getLookingAtEndermanAnimation(partialTick);
        if (endermanAnim != 0) {
            y -= 9;

            font.drawInBatch("p " + endermanAnim, 0, y, -1, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL,
                    OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);

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
