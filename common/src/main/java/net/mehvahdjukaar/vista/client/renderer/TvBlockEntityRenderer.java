package net.mehvahdjukaar.vista.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.util.LOD;
import net.mehvahdjukaar.moonlight.api.client.util.RenderUtil;
import net.mehvahdjukaar.moonlight.api.client.util.RotHlpr;
import net.mehvahdjukaar.moonlight.api.client.util.VertexUtil;
import net.mehvahdjukaar.moonlight.api.misc.ForgeOverride;
import net.mehvahdjukaar.moonlight.api.misc.RollingBuffer;
import net.mehvahdjukaar.vista.client.ModRenderTypes;
import net.mehvahdjukaar.vista.client.textures.LiveFeedTexturesManager;
import net.mehvahdjukaar.vista.common.tv.TVBlock;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;

public class TvBlockEntityRenderer implements BlockEntityRenderer<TVBlockEntity> {

    public TvBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public boolean shouldRenderOffScreen(TVBlockEntity blockEntity) {
        return false;// PlatHelper.getPlatform().isFabric();
    }

    @ForgeOverride
    public AABB getRenderBoundingBox(BlockEntity tile) {
        AABB aabb = new AABB(tile.getBlockPos());
        Direction dir = tile.getBlockState().getValue(TVBlock.FACING);
        float width = ((TVBlockEntity) tile).getConnectedWidth();
        float height = ((TVBlockEntity) tile).getConnectedHeight();
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

        if (!blockEntity.isScreenOn()) return;

        Direction dir = blockEntity.getBlockState().getValue(TVBlock.FACING);

        LOD lod = LOD.at(blockEntity);
        int screenSize = blockEntity.getScreenPixelWidth();

        Vec2 screenCenter = blockEntity.getScreenBlockCenter();

        if (!lod.isMedium()) return;
        if (lod.isPlaneCulled(dir, 0.5f, screenSize / 16f * 1.5f, 0f)) return;

        float yaw = dir.toYRot();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        poseStack.translate(-screenCenter.x, screenCenter.y, -0.501);


//        renderDebugBox(blockEntity, poseStack, buffer);

        float s = screenSize / 32f;
        int pixelEffectRes = ClientConfigs.SCALE_PIXELS.get() ? screenSize : TVBlockEntity.MIN_SCREEN_PIXEL_SIZE;
        boolean shouldUpdate = lod.within(ClientConfigs.UPDATE_DISTANCE.get());
        int switchAnim = blockEntity.getSwitchAnimationTicks();
        int videoAnim = blockEntity.getAnimationTick();
        float staticAnim = blockEntity.getLookingAtEndermanAnimation(partialTick);

        VertexConsumer vc = blockEntity.getVideoSource()
                .getVideoFrameBuilder(partialTick, buffer,
                        shouldUpdate, screenSize, pixelEffectRes,
                        videoAnim, switchAnim, staticAnim);

        //technically not correct as tv could be multiple block. just matters for transition so it's probably ok
        light = LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos()
                .relative(dir));

        int lightU = light & 0xFFFF;
        int lightV = (light >> 16) & 0xFFFF;
        VertexUtil.addQuad(vc, poseStack, -s, -s, s, s, lightU, lightV);
    }


    private static void renderDebugBox(TVBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();

        poseStack.mulPose(RotHlpr.Y180);
        poseStack.scale(7, 7, 7);
        poseStack.translate(-0.5, -0.5, -0.5);

        RenderUtil.renderBlock((long) 0, poseStack, buffer, Blocks.COBWEB.defaultBlockState(),
                blockEntity.getLevel(), blockEntity.getBlockPos(), Minecraft.getInstance().getBlockRenderer());


        poseStack.popPose();
    }


    // ========== DEBUG RENDERING ========== //


    private void renderDebug(ResourceLocation tex, PoseStack poseStack, MultiBufferSource buffer, float partialTick,
                             TVBlockEntity tile) {
        RollingBuffer<Long> lastUpdateTimes = LiveFeedTexturesManager.UPDATE_TIMES.get(tex);
        if (lastUpdateTimes == null) {
            return;
        }

        poseStack.pushPose();

        Font font = Minecraft.getInstance().font;

        poseStack.translate(1, 1.5, -0.1);
        poseStack.mulPose(Axis.YP.rotationDegrees(-180));
        poseStack.scale(1 / 16f, -1 / 16f, 1 / 16f);
        poseStack.scale(0.5f, 0.5f, 0.5f);


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
