package net.mehvahdjukaar.vista.client.textures;

import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.RenderedTexturesManager;
import net.mehvahdjukaar.moonlight.api.misc.RollingBuffer;
import net.mehvahdjukaar.moonlight.core.client.DummyCamera;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaPlatStuff;
import net.mehvahdjukaar.vista.client.AdaptiveUpdateScheduler;
import net.mehvahdjukaar.vista.client.LevelRendererCameraState;
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
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import static net.minecraft.client.Minecraft.ON_OSX;

public class LiveFeedTexturesManager {

    private static final ResourceLocation POSTERIZE_FRAGMENT_SHADER = VistaMod.res("posterize");
    private static final BiMap<UUID, ResourceLocation> LIVE_FEED_LOCATIONS = HashBiMap.create();
    private static final DummyCamera DUMMY_CAMERA = new DummyCamera();
    @VisibleForDebug
    public static final Map<ResourceLocation, RollingBuffer<Long>> UPDATE_TIMES = new HashMap<>();

    @VisibleForDebug
    public static final Supplier<AdaptiveUpdateScheduler<ResourceLocation>> SCHEDULER =
            Suppliers.memoize(() ->
                    AdaptiveUpdateScheduler.builder()
                            .baseFps(ClientConfigs.UPDATE_FPS.get())
                            .minFps(ClientConfigs.MIN_UPDATE_FPS.get())
                            .targetBudgetMs(ClientConfigs.THROTTLING_UPDATE_MS.get()) //10% of a frame which at 60fps = 16.6ms is ~1.66ms which should lower fps from 60 to 54. in other words at most a 6fps drop
                            .evictAfterTicks(20 * 5) //5 seconds

                            .guardTargetFps(60) //if we go under 60 fps, be more aggressive
                            .build()
            );

    private static long feedCounter = 0;
    @Nullable
    private static RenderTarget lifeFeedBeingRendered = null;
    @Nullable
    private static LevelRendererCameraState liveFeedCameraState = null;

    public static RenderTarget getLifeFeedBeingRendered() {
        return lifeFeedBeingRendered;
    }

    public static LevelRendererCameraState getLiveFeedCameraState() {
        return liveFeedCameraState;
    }


    @Nullable
    public static ResourceLocation requestLiveFeedTexture(Level level, UUID location, int screenSize,
                                                          boolean requiresUpdate, @Nullable ResourceLocation postShader) {
        ViewFinderBlockEntity tile = LiveFeedConnectionManager.findLinkedViewFinder(level, location);
        if (tile != null) {
        postShader = ResourceLocation.parse("shaders/post/spider.json");
            ResourceLocation feedId = getOrCreateFeedId(location);
            TVLiveFeedTexture texture = RenderedTexturesManager.requestTexture(feedId,
                    () -> new TVLiveFeedTexture(feedId,
                            screenSize * ClientConfigs.RESOLUTION_SCALE.get(),
                            LiveFeedTexturesManager::refreshTexture, location, POSTERIZE_FRAGMENT_SHADER));

            ResourceLocation currentShader = texture.getPostShader();
            if (!Objects.equals(currentShader, postShader)) {
                texture.setPostChain(postShader);
                requiresUpdate = true;
            }
            if (!requiresUpdate) {
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
        ResourceLocation loc = LIVE_FEED_LOCATIONS.get(uuid);
        if (loc == null) {
            loc = VistaMod.res("live_feed_" + feedCounter++);
            LIVE_FEED_LOCATIONS.put(uuid, loc);
        }
        return loc;
    }

    @SuppressWarnings("ConstantConditions")
    public static void clear() {
        LIVE_FEED_LOCATIONS.clear();
        DUMMY_CAMERA.entity = null;
    }

    public static void onRenderTickEnd() {
        SCHEDULER.get().onEndOfFrame();
    }

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

            renderTarget.bindWrite(true);
            lifeFeedBeingRendered = renderTarget;


            //cache old
            float oldRenderDistance = mc.gameRenderer.renderDistance;
            PostChain oldPostEffect = mc.gameRenderer.postEffect;
            boolean wasEffectActive = mc.gameRenderer.effectActive;


            text.applyPostChain();
            mc.gameRenderer.renderDistance = Math.min(oldRenderDistance, ClientConfigs.RENDER_DISTANCE.get());



            //same as field of view modifier
            float fov = ViewFinderBlockEntity.BASE_FOV * tile.getFOVModifier();


            RenderSystem.clear(16640, ON_OSX);
            FogRenderer.setupNoFog();
            RenderSystem.enableCull();


            //set new shader

            renderTarget.bindWrite(false);
            renderLevel(mc, renderTarget, DUMMY_CAMERA, fov);

            if (mc.gameRenderer.postEffect != null && mc.gameRenderer.effectActive) {
                RenderSystem.disableBlend();
                RenderSystem.disableDepthTest();
                RenderSystem.resetTextureMatrix();
                DeltaTracker deltaTracker = mc.getTimer();
                mc.gameRenderer.postEffect.process(deltaTracker.getGameTimeDeltaTicks());
            }//35876, 36289

            mc.getMainRenderTarget().bindWrite(true);



            LiveFeedTexturesManager.lifeFeedBeingRendered = null;
            mainTarget.bindWrite(true);

            //important otherwise we get flicker
            RenderSystem.clear(16640, ON_OSX);
            //restore old post process
            mc.gameRenderer.postEffect = oldPostEffect;
            mc.gameRenderer.effectActive = wasEffectActive;
            mc.gameRenderer.renderDistance = oldRenderDistance;

        };

        if (CompatHandler.DISTANT_HORIZONS) {
            runTask = DistantHorizonsCompat.decorateRenderWithoutLOD(runTask);
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
        lr.renderLevel( deltaTracker, false, camera, gr,
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

}
