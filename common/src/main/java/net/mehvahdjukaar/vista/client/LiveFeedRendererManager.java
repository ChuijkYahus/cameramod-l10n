package net.mehvahdjukaar.vista.client;

import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.RenderedTexturesManager;
import net.mehvahdjukaar.moonlight.api.misc.RollingBuffer;
import net.mehvahdjukaar.moonlight.core.client.DummyCamera;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaPlatStuff;
import net.mehvahdjukaar.vista.common.LiveFeedConnectionManager;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.mehvahdjukaar.vista.integration.distant_horizons.DistantHorizonsCompat;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static net.minecraft.client.Minecraft.ON_OSX;

public class LiveFeedRendererManager {

    private static final Int2ObjectArrayMap<RenderTarget> CANVASES = new Int2ObjectArrayMap<>();
    private static final BiMap<UUID, ResourceLocation> LIVE_FEED_LOCATIONS = HashBiMap.create();
    private static final DummyCamera DUMMY_CAMERA = new DummyCamera();

    @VisibleForDebug
    public static final Supplier<AdaptiveUpdateScheduler<ResourceLocation>> SCHEDULER =
            Suppliers.memoize(() ->
                    AdaptiveUpdateScheduler.builder()
                            .baseFps(ClientConfigs.UPDATE_FPS.get())
                            .minFps(ClientConfigs.MIN_UPDATE_FPS.get())
                            .targetBudgetMs(ClientConfigs.THROTTLING_UPDATE_MS.get()) //10% of a frame which at 60fps = 16.6ms is ~1.66ms which should lower fps from 60 to 54. in other words at most a 6fps drop
                            .evictAfterTicks(20 * 5) //5 seconds

                            .guardTargetFps(60) //if we go under 6o fps, be more aggressive
                            .build()
            );


    private static long feedCounter = 0;

    @Nullable
    public static RenderTarget LIVE_FEED_BEING_RENDERED = null;


    @Nullable
    public static ResourceLocation requestLiveFeedTexture(Level level, UUID location, int screenSize,
                                                          boolean requiresUpdate, ResourceLocation postShader) {
        ViewFinderBlockEntity tile = LiveFeedConnectionManager.findLinkedViewFinder(level, location);
        if (tile != null) {
            ResourceLocation feedId = getOrCreateFeedId(location);
            TVLiveFeedTexture texture = RenderedTexturesManager.requestTexture(feedId,
                    () -> new TVLiveFeedTexture(feedId,
                            screenSize * ClientConfigs.RESOLUTION_SCALE.get(),
                            LiveFeedRendererManager::refreshTexture, location));

            ResourceLocation currentShader = texture.getPostShader();
            if (currentShader != postShader) {
                texture.setPostShader(postShader);
                requiresUpdate = true;
            }
            if (!requiresUpdate){
                texture.unMarkForUpdate();
            }
            if (texture.isInitialized()) {
                return texture.getTextureLocation();
            } else {
                SCHEDULER.get().forceUpdateNextTick(feedId);
            }
        }
        return null;
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

    @SuppressWarnings("ConstantConditions")
    public static void clear() {
        CANVASES.clear();
        LIVE_FEED_LOCATIONS.clear();
        DUMMY_CAMERA.entity = null;
    }

    public static void onRenderTickEnd() {
        SCHEDULER.get().onEndOfFrame();
    }

    @VisibleForDebug
    public static final Map<ResourceLocation, RollingBuffer<Long>> UPDATE_TIMES = new HashMap<>();


    private static void refreshTexture(TVLiveFeedTexture text) {
        Minecraft mc = Minecraft.getInstance();

        ClientLevel level = mc.level;
        if (!mc.isGameLoadFinished() || level == null) return;
        if (mc.isPaused()) return;

        ResourceLocation textureId = text.getTextureLocation();

        Runnable runTask = () -> {

            setLastUpdatedTime(textureId, level);


            UUID uuid = text.getAssociatedUUID();
            ViewFinderBlockEntity tile = LiveFeedConnectionManager.findLinkedViewFinder(level, uuid);
            if (tile == null) return; //TODO: do something here

            float partialTicks = mc.getTimer().getGameTimeDeltaTicks();

            setupSceneCamera(tile, partialTicks);

            RenderTarget renderTarget = text.getFrameBuffer();
            RenderTarget mainTarget = mc.getMainRenderTarget();

            int size = text.getWidth();
            RenderTarget canvas = getOrCreateCanvas(size);

            canvas.bindWrite(true);
            LIVE_FEED_BEING_RENDERED = canvas;

            //same as field of view modifier
            float fov = ViewFinderBlockEntity.BASE_FOV * tile.getFOVModifier();


            RenderSystem.clear(16640, ON_OSX);
            FogRenderer.setupNoFog();
            RenderSystem.enableCull();

            float oldRenderDistance = mc.gameRenderer.renderDistance;
            mc.gameRenderer.renderDistance = Math.min(oldRenderDistance, ClientConfigs.RENDER_DISTANCE.get());
            renderLevel(mc, canvas, DUMMY_CAMERA, fov);
            mc.gameRenderer.renderDistance = oldRenderDistance;

            applyPostShader(canvas, renderTarget, ModRenderTypes.POSTERIZE.apply(canvas));

            LiveFeedRendererManager.LIVE_FEED_BEING_RENDERED = null;
            mainTarget.bindWrite(true);

            //important otherwise we get flicker
            RenderSystem.clear(16640, ON_OSX);
        };

        if (CompatHandler.DISTANT_HORIZONS) {
            runTask = DistantHorizonsCompat.renderWithoutLOD(runTask);
        }

        SCHEDULER.get().runIfShouldUpdate(textureId, runTask);

    }

    private static void setLastUpdatedTime(ResourceLocation textureId, ClientLevel level) {
        if (ClientConfigs.rendersDebug()) {
            UPDATE_TIMES.computeIfAbsent(textureId, k -> new RollingBuffer<>(20))
                    .push(level.getGameTime());
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static void setupSceneCamera(ViewFinderBlockEntity tile, float partialTicks) {
        Level level = tile.getLevel();
        float pitch = tile.getPitch(partialTicks);
        float yaw = tile.getYaw(partialTicks);

        if (DUMMY_CAMERA.entity == null) {
            DUMMY_CAMERA.entity = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
        }
        Entity dummyCameraEntity = DUMMY_CAMERA.getEntity();
        Vec3 pos = tile.getBlockPos().getCenter();
        dummyCameraEntity.setPos(pos);
        dummyCameraEntity.setXRot(pitch);
        dummyCameraEntity.setYRot(yaw + 180);

        DUMMY_CAMERA.setPosition(pos);
        DUMMY_CAMERA.setRotation(yaw, pitch);

    }


    //same as game renderer render level but simplified
    private static void renderLevel(Minecraft mc, RenderTarget target, Camera camera, float fov) {
        DeltaTracker deltaTracker = mc.getTimer();
        GameRenderer gr = mc.gameRenderer;
        LevelRenderer lr = mc.levelRenderer;

        Matrix4f projMatrix = createProjectionMatrix(gr, target, fov);
        PoseStack poseStack = new PoseStack();
        projMatrix.mul(poseStack.last().pose());
        gr.resetProjectionMatrix(projMatrix);

        Quaternionf cameraRotation = camera.rotation().conjugate(new Quaternionf());
        Matrix4f cameraMatrix = (new Matrix4f()).rotation(cameraRotation);
        //this below is what actually renders everything
        lr.prepareCullFrustum(camera.getPosition(), cameraMatrix, projMatrix);
        lr.renderLevel(deltaTracker, false, camera, gr,
                gr.lightTexture(), cameraMatrix, projMatrix);

        Matrix4f modelViewMatrix = RenderSystem.getModelViewMatrix();

        VistaPlatStuff.dispatchRenderStageAfterLevel(mc, poseStack, camera, modelViewMatrix, projMatrix);
    }

    private static Matrix4f createProjectionMatrix(GameRenderer gr, RenderTarget target, float fov) {
        Matrix4f matrix4f = new Matrix4f();
        float zoom = 1;

        if (zoom != 1.0F) {
            float zoomX = 0;
            float zoomY = 0;
            matrix4f.translate(zoomX, -zoomY, 0.0F);
            matrix4f.scale(zoom, zoom, 1.0F);
        }
        float depthFar = gr.getDepthFar();

        return matrix4f.perspective(fov * Mth.DEG_TO_RAD,
                (float) target.width / (float) target.height, ViewFinderBlockEntity.NEAR_PLANE, depthFar);
    }

    public static void applyPostShader(RenderTarget src, RenderTarget dst, RenderType rt) {
        RenderSystem.assertOnRenderThreadOrInit();

        if (src == null || dst == null)
            throw new IllegalArgumentException("Source and destination RenderTargets cannot be null");
        if (src.frameBufferId <= 0 || dst.frameBufferId <= 0)
            throw new IllegalStateException("Both RenderTargets must have valid framebuffers");
        if (src.getColorTextureId() <= 0 || dst.getColorTextureId() <= 0)
            throw new IllegalStateException("Both RenderTargets must have valid color textures");
        if (src.width != dst.width || src.height != dst.height)
            throw new IllegalStateException("RenderTarget sizes must match for shader copy");


        // Bind destination framebuffer
        dst.clear(true);

        dst.bindWrite(true);

        RenderSystem.getModelViewMatrix().set(new Matrix4f().identity());
        RenderSystem.getProjectionMatrix().set(new Matrix4f().identity());

        var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        VertexConsumer vc = bufferSource.getBuffer(rt);

        vc.addVertex(-1, -1, 0).setUv(0f, 1f);
        vc.addVertex(1, -1, 0).setUv(1f, 1f);
        vc.addVertex(1, 1, 0).setUv(1f, 0f);
        vc.addVertex(-1, 1, 0).setUv(0f, 0f);
        bufferSource.endBatch(rt);
    }

}
