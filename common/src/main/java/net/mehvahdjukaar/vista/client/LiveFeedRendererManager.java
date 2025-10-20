package net.mehvahdjukaar.vista.client;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.FrameBufferBackedDynamicTexture;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.RenderedTexturesManager;
import net.mehvahdjukaar.moonlight.core.client.DummyCamera;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.common.ViewFinderConnection;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.UUID;

import static net.minecraft.client.Minecraft.ON_OSX;

public class LiveFeedRendererManager {

    private static final float RENDER_DISTANCE = 32f;
    private static final DummyCamera DUMMY_CAMERA = new DummyCamera();
    private static final Int2ObjectArrayMap<RenderTarget> CANVASES = new Int2ObjectArrayMap<>();
    private static final BiMap<UUID, ResourceLocation> LIVE_FEED_LOCATIONS = HashBiMap.create();
    private static final ResourceLocation INVALID_FEED_LOCATION = VistaMod.res("textures/block/invalid_live_feed.png");

    private static long feedCounter = 0;

    @Nullable
    public static RenderTarget LIVE_FEED_BEING_RENDERED = null;


    public static ResourceLocation requestLiveFeedTexture(Level level, UUID location, int size) {
        ViewFinderConnection connection = ViewFinderConnection.get(level);
        if (connection != null) {
            ViewFinderBlockEntity tile = connection.getLinkedViewFinder(level, location);
            if (tile != null) {
                ResourceLocation feedId = getOrCreateFeedId(location);
                var texture = RenderedTexturesManager.requestTexture(feedId, size,
                        LiveFeedRendererManager::refreshTexture,
                        true);
                if (texture.isInitialized()) {
                    return texture.getTextureLocation();
                }
            }
        }
        return INVALID_FEED_LOCATION;
    }

    private static ResourceLocation getOrCreateFeedId(UUID uuid) {
        var loc = LIVE_FEED_LOCATIONS.get(uuid);
        if (loc == null) {
            loc = VistaMod.res("live_feed_" + feedCounter++);
            LIVE_FEED_LOCATIONS.put(uuid, loc);
        }
        return loc;
    }

    public static RenderTarget getOrCreateCanvas(int size) {
        RenderTarget canvas = CANVASES.get(size);
        if (canvas == null) {
            canvas = new TextureTarget(size, size, true, ON_OSX);
            CANVASES.put(size, canvas);
        }
        return canvas;
    }


    private static void refreshTexture(FrameBufferBackedDynamicTexture text) {
        Minecraft mc = Minecraft.getInstance();

        ClientLevel level = mc.level;
        if (!mc.isGameLoadFinished() || level == null) return;

        long gameTime = level.getGameTime();
        if (gameTime % 5 != 0) return; //update every 5 ticks TODO: round robin between feeds
        ViewFinderConnection connection = ViewFinderConnection.get(level);
        if (connection == null) return;

        UUID uuid = LIVE_FEED_LOCATIONS.inverse().get(text.getTextureLocation());
        ViewFinderBlockEntity tile = connection.getLinkedViewFinder(level, uuid);
        if (tile == null) return; //TODO: do something here

        float partialTicks = mc.getTimer().getGameTimeDeltaTicks();

        setupSceneCamera(tile, partialTicks);


        BlockPos blockPos = new BlockPos(0, -62, 0);
        float yaw = 0;
        DUMMY_CAMERA.setPosition(blockPos);
        DUMMY_CAMERA.setRotation(180 - yaw, 0);

        RenderTarget renderTarget = text.getFrameBuffer();
        RenderTarget mainTarget = mc.getMainRenderTarget();

        int size = text.getWidth();
        RenderTarget canvas = getOrCreateCanvas(size);

        canvas.bindWrite(true);
        LIVE_FEED_BEING_RENDERED = canvas;


        RenderSystem.clear(16640, ON_OSX);
        FogRenderer.setupNoFog();
        RenderSystem.enableCull();

        float oldRenderDistance = mc.gameRenderer.renderDistance;
        mc.gameRenderer.renderDistance = RENDER_DISTANCE;
        renderLevel(mc, canvas, DUMMY_CAMERA);
        mc.gameRenderer.renderDistance = oldRenderDistance;

        copyWithShader(canvas, renderTarget, ModRenderTypes.POSTERIZE.apply(canvas));

        LiveFeedRendererManager.LIVE_FEED_BEING_RENDERED = null;
        mainTarget.bindWrite(true);
    }

    private static void setupSceneCamera(ViewFinderBlockEntity tile, float partialTicks) {
        float pitch = tile.getPitch(partialTicks);
        float yaw = tile.getYaw(partialTicks);
        BlockPos pos = tile.getBlockPos();
        DUMMY_CAMERA.setPosition(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        DUMMY_CAMERA.setRotation(yaw, pitch);

    }

    //same as game renderer render level but simplified
    private static void renderLevel(Minecraft mc, RenderTarget target, Camera camera) {
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

    private static Matrix4f createProjectionMatrix(GameRenderer gr, RenderTarget target) {
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

    public static void copyWithShader(RenderTarget src, RenderTarget dst, RenderType rt) {
        RenderSystem.assertOnRenderThreadOrInit();

        if (src == null || dst == null)
            throw new IllegalArgumentException("Source and destination RenderTargets cannot be null");
        if (src.frameBufferId <= 0 || dst.frameBufferId <= 0)
            throw new IllegalStateException("Both RenderTargets must have valid framebuffers");
        if (src.getColorTextureId() <= 0 || dst.getColorTextureId() <= 0)
            throw new IllegalStateException("Both RenderTargets must have valid color textures");
        if (src.width != dst.width || src.height != dst.height)
            throw new IllegalStateException("RenderTarget sizes must match for shader copy");


        if (false) {
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
        } else {
            // Bind destination framebuffer
            dst.clear(true);

            dst.bindWrite(true);

            RenderSystem.getModelViewMatrix().set(new Matrix4f().identity());
            RenderSystem.getProjectionMatrix().set(new Matrix4f().identity());

            var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

            VertexConsumer vc = bufferSource.getBuffer(rt);

            vc.addVertex(-1, -1, 0).setUv(0f, 0f);
            vc.addVertex(1, -1, 0).setUv(1f, 0f);
            vc.addVertex(1, 1, 0).setUv(1f, 1f);
            vc.addVertex(-1, 1, 0).setUv(0f, 1f);
            bufferSource.endBatch(rt);

        }

    }

}
