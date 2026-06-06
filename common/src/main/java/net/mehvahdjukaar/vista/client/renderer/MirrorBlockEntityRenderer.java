package net.mehvahdjukaar.vista.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.util.LOD;
import net.mehvahdjukaar.moonlight.api.misc.ForgeOverride;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.client.MirrorReflection;
import net.mehvahdjukaar.vista.client.textures.LiveFeedTexture;
import net.mehvahdjukaar.vista.client.textures.LiveFeedTexturesManager;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlock;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MirrorBlockEntityRenderer implements BlockEntityRenderer<MirrorBlockEntity> {

    static final Vec2i SCREEN_SIZE = new Vec2i(16, 16);
    // Push the visual quad well clear of the block-model front face so geometry doesn't overdraw it.
    private static final float SURFACE_OFFSET = 0.01f;

    public MirrorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @ForgeOverride
    public net.minecraft.world.phys.AABB getRenderBoundingBox(MirrorBlockEntity tile) {
        return new net.minecraft.world.phys.AABB(tile.getBlockPos());
    }

    @Override
    public void render(MirrorBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {
        Direction dir = blockEntity.getBlockState().getValue(MirrorBlock.FACING);

        LOD lod = LOD.at(blockEntity);
        if (lod.isPlaneCulled(dir, 0.5f, 1.5f, 0f)) return;

        // Recursion guard: a mirror in the reflection of another mirror just shows as the
        // underlying block — we don't re-render mirrors inside mirrors.
        if (!VistaLevelRenderer.isRenderingLiveFeed()) {
            Vec3 normal = Vec3.atLowerCornerOf(dir.getNormal());
            Vec3 planePoint = Vec3.atCenterOf(blockEntity.getBlockPos()).add(normal.scale(0.5));

            Minecraft mc = Minecraft.getInstance();
            Camera mainCamera = mc.gameRenderer.mainCamera;
            MirrorReflection reflection = MirrorReflection.compute(
                    planePoint, normal, mainCamera.getPosition());
            if (!reflection.viewerInFront()) return;
        }
        LiveFeedTexture text = LiveFeedTexturesManager.getMirrorTexture(blockEntity.getId(), SCREEN_SIZE);
        if (text == null) return;

        // Queue the reflection render for the post-frame hook — we cannot trigger a nested
        // level render from inside a BE renderer without corrupting the outer batch.
        MirrorRenderManager.requestUpdate(blockEntity, SCREEN_SIZE);
        drawMirrorFace(blockEntity, dir, poseStack, buffer, text);
    }

    /**
     * Sets the dummy camera at the reflected eye position, oriented to look perpendicularly
     * into the mirror plane (yaw drives the orientation; pitch is always 0 because horizontal
     * mirrors don't tilt the eye axis). The off-axis projection in {@link MirrorRenderManager}
     * does the rest — it bends the frustum to fit the mirror's frame from that vantage point.
     */
    static void setupMirrorCamera(Camera camera, Level level, Vec3 reflectedEye, float yaw) {
        camera.initialized = true;
        camera.level = level;
        if (camera.entity == null) {
            camera.entity = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
        }
        Entity dummy = camera.getEntity();
        dummy.setPos(reflectedEye);
        dummy.setXRot(0f);
        dummy.setYRot(yaw);
        camera.setPosition(reflectedEye);
        camera.setRotation(yaw, 0f);
    }

    private static void drawMirrorFace(MirrorBlockEntity blockEntity, Direction dir, PoseStack poseStack,
                                       MultiBufferSource buffer, LiveFeedTexture text) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - dir.toYRot()));
        poseStack.translate(0, 0, -0.5 - SURFACE_OFFSET);

        Level level = blockEntity.getLevel();
        int skyBrightness = level.getBrightness(net.minecraft.world.level.LightLayer.SKY,
                blockEntity.getBlockPos().relative(dir));
        int light = LightTexture.pack(15, skyBrightness);

        VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(text.getTextureLocation()));

        PoseStack.Pose last = poseStack.last();
        Vector3f normal = last.normal().transform(new Vector3f(0, 0, -1));
        int lu = light & 0xFFFF;
        int lv = (light >> 16) & 0xFFFF;
        // V is flipped: framebuffer textures are Y-up in GL coords, so to display
        // right-side up we map the top of the quad to V=1 and the bottom to V=0.
        vert(vc, poseStack, -0.5f, 0.5f, 0f, 1f, lu, lv, normal);
        vert(vc, poseStack, 0.5f, 0.5f, 1f, 1f, lu, lv, normal);
        vert(vc, poseStack, 0.5f, -0.5f, 1f, 0f, lu, lv, normal);
        vert(vc, poseStack, -0.5f, -0.5f, 0f, 0f, lu, lv, normal);

        poseStack.popPose();
    }

    private static void vert(VertexConsumer builder, PoseStack poseStack,
                             float x, float y, float u, float v, int lu, int lv, Vector3f normal) {
        builder.addVertex(poseStack.last().pose(), x, y, 0);
        builder.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        builder.setUv(u, v);
        builder.setOverlay(OverlayTexture.NO_OVERLAY);
        builder.setUv2(lu, lv);
        builder.setNormal(normal.x, normal.y, normal.z);
    }
}
