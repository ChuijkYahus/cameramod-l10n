package net.mehvahdjukaar.vista.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.vista.common.BroadcastManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class FeedConnectionDebugRenderer implements DebugRenderer.SimpleDebugRenderer {

    public static final FeedConnectionDebugRenderer INSTANCE = new FeedConnectionDebugRenderer();

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, double camX, double camY, double camZ) {

        ClientLevel level = Minecraft.getInstance().level;

        renderAll(poseStack, buffer, camX, camY, camZ, level, level, 0.9f, 0.9f, 0.1f, 0);

        MinecraftServer server = PlatHelper.getCurrentServer();
        if (server != null) {
            Level serverLevel = server.getLevel(level.dimension());

            renderAll(poseStack, buffer, camX, camY, camZ, serverLevel, level, 0.1f, 0.9f, 0.4f, 0.1f);
        }
    }

    private void renderAll(PoseStack poseStack, MultiBufferSource buffer, double camX, double camY, double camZ,
                           Level l, ClientLevel level, float red, float green, float blue, float offset) {
        BroadcastManager manager = BroadcastManager.getInstance(l);

        for (var p : manager.getAll()) {

            GlobalPos from = p.getValue();
            UUID feedId = p.getKey();
            if (from.dimension() == level.dimension()) {
                BlockPos pos = from.pos();
                this.highlightPosition(pos, poseStack, camX, camY, camZ, buffer, 0.02 + 0 + offset,
                        red, green, blue, level);

                int j = FastColor.ABGR32.color(255,
                        (int) (red * 255),
                        (int) (green * 255),
                        (int) (blue * 255));
                DebugRenderer.renderFloatingText(poseStack, buffer,
                        String.valueOf(feedId.getLeastSignificantBits()),
                        (double) pos.getX() + (double) 0.5F,
                        (double) pos.getY() + (double) 1.25F + (offset * 4),
                        (double) pos.getZ() + (double) 0.5F, j);

                UUID feedProv = manager.getIdOfFeedAt( from);
                if (feedProv != null) {
                    j = feedProv.equals(feedId) ? j : 0xffff0000;
                    DebugRenderer.renderFloatingText(poseStack, buffer,
                            String.valueOf(feedId.getLeastSignificantBits()),
                            (double) pos.getX() + (double) 0.5F,
                            (double) pos.getY() + (double) 1.25F + (offset * 4 + 0.2),
                            (double) pos.getZ() + (double) 0.5F, j);
                }

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
    }


    @Override
    public void clear() {
    }
}
