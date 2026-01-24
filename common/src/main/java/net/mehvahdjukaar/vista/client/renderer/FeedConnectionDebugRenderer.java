package net.mehvahdjukaar.vista.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.vista.common.LiveFeedConnectionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.UUID;

public class FeedConnectionDebugRenderer implements DebugRenderer.SimpleDebugRenderer {

    public static final FeedConnectionDebugRenderer INSTANCE = new FeedConnectionDebugRenderer();

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, double camX, double camY, double camZ) {

        ClientLevel level = Minecraft.getInstance().level;
        for (var p : LiveFeedConnectionManager.getInstance(level).getAll()) {

            GlobalPos from = p.getValue();
            UUID feedId = p.getKey();
            if (from.dimension() == level.dimension()) {
                BlockPos pos = from.pos();
                this.highlightPosition(pos, poseStack, camX, camY, camZ, buffer, 0.02 + 0,
                        1.0F, 0.0F, 0.0F, level);

                int j = Mth.hsvToRgb(1, 0.9F, 0.9F);
                DebugRenderer.renderFloatingText(poseStack, buffer,
                        String.valueOf(feedId),
                        (double) pos.getX() + (double) 0.5F,
                        (double) pos.getY() + (double) 0.75F,
                        (double) pos.getZ() + (double) 0.5F, j);

            }
        }
    }

    private void highlightPosition(BlockPos pos, PoseStack poseStack, double camX, double camY, double camZ,
                                   MultiBufferSource buffer, double bias, float red, float green, float blue,
                                   ClientLevel level) {
        double d = (double) pos.getX() - camX - (double) 2.0F * bias;
        double e = (double) pos.getY() - camY - (double) 2.0F * bias;
        double f = (double) pos.getZ() - camZ - (double) 2.0F * bias;
        double g = d + (double) 1.0F + (double) 4.0F * bias;
        double h = e + (double) 1.0F + (double) 4.0F * bias;
        double i = f + (double) 1.0F + (double) 4.0F * bias;
        LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), d, e, f, g, h, i, red, green, blue, 0.4F);
        LevelRenderer.renderVoxelShape(poseStack, buffer.getBuffer(RenderType.lines()),
                level.getBlockState(pos).getCollisionShape(level, pos, CollisionContext.empty()).move((double) pos.getX(), (double) pos.getY(), (double) pos.getZ()), -camX, -camY, -camZ, red, green, blue, 1.0F, false);
    }


    @Override
    public void clear() {
    }
}
