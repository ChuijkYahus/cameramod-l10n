package net.mehvahdjukaar.camera_vision.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.camera_vision.common.TVBlock;
import net.mehvahdjukaar.camera_vision.common.TVBlockEntity;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.FrameBufferBackedDynamicTexture;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.RenderedTexturesManager;
import net.mehvahdjukaar.moonlight.core.client.DummyCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import static net.minecraft.client.Minecraft.ON_OSX;

public class TvBlockEntityRenderer implements BlockEntityRenderer<TVBlockEntity> {

    public TvBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static final int SCREEN_PIXEL_LENGTH = 12;
    private static final float SCREEN_HALF_LENGTH = SCREEN_PIXEL_LENGTH / 32f;
    private static final int SCREEN_RESOLUTION_SCALE = 4;
    private static final int SCREEN_TEXTURE_SIZE = SCREEN_PIXEL_LENGTH * SCREEN_RESOLUTION_SCALE;
    private static final float RENDER_DISTANCE = 32f;


    private static final RenderTarget CANVAS = new TextureTarget(SCREEN_TEXTURE_SIZE, SCREEN_TEXTURE_SIZE, true, Minecraft.ON_OSX);

    private static final DummyCamera DUMMY_CAMERA = new DummyCamera();

    @Override
    public void render(TVBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int light, int packedOverlay) {

        var tex = RenderedTexturesManager.requestTexture(blockEntity.getLinkedCamera(),
                SCREEN_TEXTURE_SIZE,
                this::refreshTexture, true);

        if (!tex.isInitialized()) return;

        Direction dir = blockEntity.getBlockState().getValue(TVBlock.FACING);
        float yaw = dir.toYRot();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180-yaw));
        poseStack.translate(-0.5, -0.5, -0.5);

        int overlay = OverlayTexture.NO_OVERLAY;
        float s = SCREEN_HALF_LENGTH;
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entitySolid(tex.getTextureLocation()));
        PoseStack.Pose pose = poseStack.last();

        poseStack.translate(0.5, 0.5,  -0.001);

        vertexConsumer.addVertex(pose, -s, -s, 0).setColor(1f, 1f, 1f, 1f).setUv(1f, 0f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);
        vertexConsumer.addVertex(pose, -s, s, 0).setColor(1f, 1f, 1f, 1f).setUv(1f, 1f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);

        vertexConsumer.addVertex(pose, s, s, 0).setColor(1f, 1f, 1f, 1f).setUv(0f, 1f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);
        vertexConsumer.addVertex(pose, s, -s, 0).setColor(1f, 1f, 1f, 1f).setUv(0f, 0f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);



        DUMMY_CAMERA.setPosition(blockEntity.getBlockPos().relative(dir.getOpposite()));
        DUMMY_CAMERA.setRotation(180-yaw, 0);
    }


    private void refreshTexture(FrameBufferBackedDynamicTexture text) {
        Minecraft mc = Minecraft.getInstance();

        if (!mc.isGameLoadFinished() || mc.level == null) return;

        long gameTime = mc.level.getGameTime();
        if (gameTime % 5 != 0) return; //update every 5 ticks

        RenderTarget renderTarget = text.getFrameBuffer();
        RenderTarget mainTarget = mc.getMainRenderTarget();

        CANVAS.bindWrite(true);
        CameraRendererManager.CAMERA_CANVAS = CANVAS;



        RenderSystem.clear(16640, ON_OSX);
        FogRenderer.setupNoFog();
        RenderSystem.enableCull();

        float oldRenderDistance = mc.gameRenderer.renderDistance;
        mc.gameRenderer.renderDistance = RENDER_DISTANCE;
        renderLevel(mc, CANVAS, DUMMY_CAMERA);
        mc.gameRenderer.renderDistance = oldRenderDistance;

        swapBuffers(CANVAS, renderTarget);

        CameraRendererManager.CAMERA_CANVAS = null;
        mainTarget.bindWrite(true);
    }

    //same as game renderer render level but simplified
    public static void renderLevel(Minecraft mc, RenderTarget target, Camera camera) {
        DeltaTracker deltaTracker = mc.getTimer();
        GameRenderer gr = mc.gameRenderer;
        LevelRenderer lr = mc.levelRenderer;


        Matrix4f projMatrix = createProjectionMatrix(gr, target);
        PoseStack poseStack = new PoseStack();
        projMatrix.mul(poseStack.last().pose());
        gr.resetProjectionMatrix(projMatrix);

        Quaternionf cameraRotation = camera.rotation().conjugate(new Quaternionf());
        Matrix4f cameraMatrix = (new Matrix4f()).rotation(cameraRotation);
        //this below is what actually renders everything
        lr.prepareCullFrustum(camera.getPosition(), cameraMatrix, projMatrix);
        lr.renderLevel(deltaTracker, false, camera, gr,
                gr.lightTexture(), cameraMatrix, projMatrix);

        mc.getProfiler().popPush("neoforge_render_last");
        //ClientHooks.dispatchRenderStage(Stage.AFTER_LEVEL, mc.levelRenderer, (PoseStack)null, matrix4f1, matrix4f, mc.levelRenderer.getTicks(), camera, mc.levelRenderer.getFrustum());

        mc.getProfiler().pop();
    }

    public static Matrix4f createProjectionMatrix(GameRenderer gr, RenderTarget target) {
        Matrix4f matrix4f = new Matrix4f();
        float zoom = 1;
        double fovNumber = 70;

        if (zoom != 1.0F) {
            float zoomX = 0;
            float zoomY = 0;
            matrix4f.translate(zoomX, -zoomY, 0.0F);
            matrix4f.scale(zoom, zoom, 1.0F);
        }
        float depthFar = gr.getDepthFar();

        return matrix4f.perspective((float) (fovNumber * Mth.DEG_TO_RAD),
                (float) target.width / (float) target.height, 0.05F, depthFar);
    }


    private static void swapBuffers(RenderTarget src, RenderTarget dst) {
        // Must be on the render thread
        RenderSystem.assertOnRenderThreadOrInit();

        if (src == null || dst == null) {
            throw new IllegalArgumentException("Source and destination RenderTargets cannot be null");
        }
        if (src.frameBufferId <= 0 || dst.frameBufferId <= 0) {
            throw new IllegalStateException("Both RenderTargets must have a valid framebuffer");
        }
        if (src.getColorTextureId() <= 0 || dst.getColorTextureId() <= 0) {
            throw new IllegalStateException("Both RenderTargets must have a valid color texture");
        }
        // You enforced equal sizes; fine (blit can scale if needed, but keep as-is)
        if (src.width != dst.width || src.height != dst.height) {
            throw new IllegalStateException("RenderTarget sizes must match for copyColor()");
        }

        // Scissor can clip blits; disable temporarily
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (scissor) GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Bind src as READ, dst as DRAW
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, src.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dst.frameBufferId);

        // Blit color buffer (use LINEAR or NEAREST; sizes match so either is fine)
        GL30.glBlitFramebuffer(
                0, 0, src.width, src.height,      // src rect
                0, 0, dst.width, dst.height,      // dst rect
                GL11.GL_COLOR_BUFFER_BIT,         // copy color
                GL11.GL_NEAREST                   // filter (NEAREST is safe; LINEAR also fine for color)
        );

        // Unbind (bind 0 to GL_FRAMEBUFFER resets both READ/DRAW)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }
}
