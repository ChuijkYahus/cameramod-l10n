package net.mehvahdjukaar.vista.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.util.LOD;
import net.mehvahdjukaar.moonlight.api.misc.ForgeOverride;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.client.MirrorReflection;
import net.mehvahdjukaar.vista.client.textures.MirrorReflectionTexture;
import net.mehvahdjukaar.vista.client.textures.LiveFeedTexturesManager;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlock;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MirrorBlockEntityRenderer implements BlockEntityRenderer<MirrorBlockEntity> {

    // Push the visual quad well clear of the block-model front face so geometry doesn't overdraw it.
    private static final float SURFACE_OFFSET = 0.01f;

    public MirrorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return ClientConfigs.MIRROR_RENDER_DISTANCE.get();
    }

    @ForgeOverride
    public AABB getRenderBoundingBox(MirrorBlockEntity tile) {
        AABB aabb = new AABB(tile.getBlockPos());
        Direction dir = tile.getBlockState().getValue(MirrorBlock.FACING);
        Vec2i connection = tile.getConnectedCount();
        float width = connection.x();
        float height = connection.y();
        if (dir == Direction.EAST) {
            return aabb.expandTowards(0, height - 1, -width + 1);
        } else if (dir == Direction.WEST) {
            return aabb.expandTowards(0, height - 1, width - 1);
        } else if (dir == Direction.NORTH) {
            return aabb.expandTowards(-width + 1, height - 1, 0);
        } else if (dir == Direction.SOUTH) {
            return aabb.expandTowards(width - 1, height - 1, 0);
        }
        return aabb;
    }

    @Override
    public void render(MirrorBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {
        Direction dir = blockEntity.getBlockState().getValue(MirrorBlock.FACING);

        LOD lod = LOD.at(blockEntity);
        if (lod.isPlaneCulled(dir, 0.5f, 1.5f, 0f)) return;

        // Recursion guard: a mirror inside another mirror's reflection draws only its block
        // model (frame). The cached texture is the previous frame's reflection from THIS
        // mirror's POV — meaningless content for a different reflection.
        if (VistaLevelRenderer.isRenderingLiveFeed()) return;

        Vec3 normal = Vec3.atLowerCornerOf(dir.getNormal());
        Vec3 planePoint = Vec3.atCenterOf(blockEntity.getBlockPos()).add(normal.scale(0.5));

        Minecraft mc = Minecraft.getInstance();
        Camera mainCamera = mc.gameRenderer.mainCamera;
        MirrorReflection reflection = MirrorReflection.compute(
                planePoint, normal, mainCamera.getPosition());
        if (!reflection.viewerInFront()) return;

        Vec2i screenSize = blockEntity.getScreenPixelSize();
        // Capture the eye (post head-bob compensation) NOW, while we still have the camera that
        // was used to render the BE. The actual reflection render fires later in the frame; if we
        // resampled the camera at refresh time it could drift from the source view.
        Vec3 eye = MirrorTextureRenderer.captureEye(mc, mainCamera);

        MirrorReflectionTexture text;
        if (ClientConfigs.MIRROR_UPDATE_MODE.get() == ClientConfigs.MirrorUpdateMode.TEXTURE_REFRESH) {
            // Stashes the BE+eye on the texture and marks it for refresh — the actual reflection
            // render runs from the end-of-frame texture refresh callback, after the outer BE
            // batch is flushed.
            text = LiveFeedTexturesManager.getMirrorTexture(blockEntity, screenSize, eye);
        } else {
            // Queue into MirrorTextureRenderer's PENDING list; processPending flushes it from the
            // onRenderTickEnd hook.
            text = LiveFeedTexturesManager.getMirrorTexture(blockEntity.getId(), screenSize);
            if (text != null) MirrorTextureRenderer.requestUpdate(blockEntity, screenSize, eye);
        }
        if (text == null) return;

        drawMirrorFace(blockEntity, dir, poseStack, buffer, text);
    }

    /**
     * Sets the dummy camera at the reflected eye position, oriented to look perpendicularly
     * into the mirror plane (yaw drives the orientation; pitch is always 0 because horizontal
     * mirrors don't tilt the eye axis). The off-axis projection in {@link MirrorTextureRenderer}
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
                                       MultiBufferSource buffer, MirrorReflectionTexture text) {
        Vec2i connection = blockEntity.getConnectedCount();
        float w = connection.x();
        float h = connection.y();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - dir.toYRot()));
        poseStack.translate(0, 0, -0.5 - SURFACE_OFFSET);

        Level level = blockEntity.getLevel();
        int skyBrightness = level.getBrightness(net.minecraft.world.level.LightLayer.SKY,
                blockEntity.getBlockPos().relative(dir));
        int light = LightTexture.pack(15, skyBrightness);

        VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(text.getTextureLocation()));

        Vector3f normal = new Vector3f(0, 1, 0);
        int lu = light & 0xFFFF;
        int lv = (light >> 16) & 0xFFFF;

        // Master is at bottom-left; quad extends right by (w-1) and up by (h-1) blocks.
        // V is flipped: framebuffer textures are Y-up in GL, so top maps to V=1, bottom to V=0.
        float right = w - 0.5f;
        float top   = h - 0.5f;
        vert(vc, poseStack, -0.5f, top,   0f, 1f, lu, lv, normal);
        vert(vc, poseStack,  right, top,   1f, 1f, lu, lv, normal);
        vert(vc, poseStack,  right, -0.5f, 1f, 0f, lu, lv, normal);
        vert(vc, poseStack, -0.5f, -0.5f,  0f, 0f, lu, lv, normal);

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
