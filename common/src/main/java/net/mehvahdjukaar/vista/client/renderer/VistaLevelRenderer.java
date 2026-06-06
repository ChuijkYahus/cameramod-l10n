package net.mehvahdjukaar.vista.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.moonlight.api.misc.WeakHashSet;
import net.mehvahdjukaar.moonlight.api.util.math.EntityAngles;
import net.mehvahdjukaar.moonlight.core.client.DummyCamera;
import net.mehvahdjukaar.vista.VistaPlatStuff;
import net.mehvahdjukaar.vista.client.textures.LiveFeedTexture;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.mehvahdjukaar.vista.integration.iris.IrisCompat;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11C;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static net.minecraft.client.Minecraft.ON_OSX;

public class VistaLevelRenderer {

    private static final Set<LevelRendererCameraState> MANAGED_STATES = new WeakHashSet<>();
    private static final Object STATES_LOCK = new Object();
    private static final AtomicReference<SectionOcclusionGraph> MC_OWN_GRAPH = new AtomicReference<>(null);
    private static DummyCamera dummyCamera = new DummyCamera();

    private static Object currentRenderingToken = null;

    private static ResourceKey<Level> lastLevel = null;

    public static boolean isRenderingLiveFeed() {
        return currentRenderingToken != null;
    }

    public static boolean isViewFinderRenderingLiveFeed(ViewFinderBlockEntity vf) {
        return currentRenderingToken == vf;
    }

    public static void clear() {
        dummyCamera = null;
        MC_OWN_GRAPH.set(null);
        synchronized (STATES_LOCK) {
            MANAGED_STATES.clear();
        }
        currentRenderingToken = null;
    }

    /**
     * Invalidates all managed (feed) occlusion graphs so they redo their full BFS.
     * Call whenever zone data changes so newly-created pinned sections are picked up.
     */
    public static void invalidateManagedGraphs() {
        synchronized (STATES_LOCK) {
            for (LevelRendererCameraState state : MANAGED_STATES) {
                SectionOcclusionGraph graph = state.getOcclusionGraph();
                if (graph != null) graph.invalidate();
            }
        }
    }

    public static void registerManagedState(LevelRendererCameraState state) {
        synchronized (STATES_LOCK) {
            MANAGED_STATES.add(state);
        }
    }

    /**
     * Called at the tail of {@link LevelRenderer#allChanged()}. That method releases
     * every section's VertexBuffer (setting their mode to null and deleting their VAO)
     * and swaps in a fresh ViewArea. Every cached feed state still references the now
     * dead RenderSections, so we wipe them — the next feed render will rebuild a fresh
     * occlusion graph against the new ViewArea.
     */
    public static void onLevelRendererAllChanged() {
        synchronized (STATES_LOCK) {
            for (LevelRendererCameraState state : MANAGED_STATES) {
                state.resetForLevelRendererReload();
            }
        }
        MC_OWN_GRAPH.set(null);
    }

    public static DummyCamera getDummyCamera() {
        if (dummyCamera == null) {
            dummyCamera = new DummyCamera();
        }
        return dummyCamera;
    }

    public static void render(LiveFeedTexture text, ViewFinderBlockEntity tile) {
        render(text, tile, (camera, partialTicks) -> setupSceneCamera(tile, camera, partialTicks),
                tile.getFOV(), true, null);
    }

    /**
     * @param fov              ignored when {@code customProjection} is non-null.
     * @param customProjection if non-null, used as-is instead of building a symmetric perspective
     *                         from {@code fov}. Mirrors use this to bake an off-axis frustum
     *                         shaped to the mirror's frame, so the near plane *is* the mirror.
     */
    public static void render(LiveFeedTexture text, Object renderingToken,
                              SceneCameraSetup cameraSetup, float fov,
                              boolean applyPostChain,
                              @Nullable Matrix4f customProjection) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null) return;
        //debounce dimension changing for some reason idk yet
        if (mc.level.dimension() != lastLevel) {
            lastLevel = mc.level.dimension();
            return;
        }

        RenderTarget mainTarget = mc.getMainRenderTarget();
        RenderTarget canvas = text.getRenderTarget();
        mc.mainRenderTarget = canvas;

        // Tell Iris-side compat that the main render target may have changed identity
        // (TV resize swaps in a fresh LiveFeedTexture → fresh RenderTarget). Iris's
        // own change detection relies on a version counter that doesn't increment on
        // a brand-new RenderTarget, so without this nudge the iris pipeline keeps
        // its gbuffers attached to the old (smaller / freed) canvas.
        if (CompatHandler.IRIS) {
            IrisCompat.onFeedCanvasBound(canvas);
        }

        Camera camera = getDummyCamera();
        Camera mainCamera = mc.gameRenderer.mainCamera;
        mc.gameRenderer.mainCamera = camera;

        // save old state
        float oldRenderDistance = mc.gameRenderer.renderDistance;
        PostChain oldPostEffect = mc.gameRenderer.postEffect;
        boolean wasEffectActive = mc.gameRenderer.effectActive;

        // switch to feed camera state
        LevelRendererCameraState oldCameraState = LevelRendererCameraState.capture(mc.levelRenderer);
        LevelRendererCameraState feedCameraState = text.getRendererState();

        RenderSystemState oldRenderState = RenderSystemState.capture();

        try {
            currentRenderingToken = renderingToken;

            float partialTicks = mc.getTimer().getGameTimeDeltaTicks();
            cameraSetup.setup(camera, partialTicks);

            canvas.bindWrite(true);
            RenderSystem.viewport(0, 0, canvas.width, canvas.height);

            if (applyPostChain) {
                text.applyPostChain();
            } else {
                mc.gameRenderer.postEffect = null;
                mc.gameRenderer.effectActive = false;
            }

            mc.gameRenderer.renderDistance = Math.min(oldRenderDistance, calculateRenderDistance(fov));
            RenderSystem.clear(16640, ON_OSX);
            FogRenderer.setupNoFog();
            RenderSystem.enableCull();

            feedCameraState.apply(mc.levelRenderer);

            MC_OWN_GRAPH.set(oldCameraState.getOcclusionGraph());

            // already wrapped outside; don't double-wrap this or it fucks everything over omg.
            renderLevel(mc, canvas, camera, fov, customProjection);

            // save updated feed camera state
            feedCameraState.copyFrom(mc.levelRenderer);

            if (mc.gameRenderer.postEffect != null && mc.gameRenderer.effectActive) {
                RenderSystem.disableBlend();
                RenderSystem.disableDepthTest();
                RenderSystem.resetTextureMatrix();
                DeltaTracker deltaTracker = mc.getTimer();
                mc.gameRenderer.postEffect.process(deltaTracker.getGameTimeDeltaTicks());
            }
        } finally {
            MC_OWN_GRAPH.set(null);

            // restore old camera state
            oldCameraState.apply(mc.levelRenderer);

            // restore old render state
            oldRenderState.apply();
            // clear depth only; clearing color here causes visible world/water popping
            RenderSystem.clear(GL11C.GL_DEPTH_BUFFER_BIT, ON_OSX);

            // swap back
            currentRenderingToken = null;

            mc.mainRenderTarget = mainTarget;
            mc.gameRenderer.mainCamera = mainCamera;

            // restore post process
            mc.gameRenderer.postEffect = oldPostEffect;
            mc.gameRenderer.effectActive = wasEffectActive;
            mc.gameRenderer.renderDistance = oldRenderDistance;
        }
    }

    private static Integer calculateRenderDistance(float fov) {
        //TODO: improve
        return ClientConfigs.RENDER_DISTANCE.get();
    }


    //same as game renderer render level but simplified
    private static void renderLevel(Minecraft mc, RenderTarget target, Camera camera, float fov,
                                    @Nullable Matrix4f customProjection) {
        DeltaTracker deltaTracker = mc.getTimer();
        GameRenderer gr = mc.gameRenderer;
        LevelRenderer lr = mc.levelRenderer;
        Matrix4f oldProjectionMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());

        Matrix4f projMatrix = customProjection != null
                ? new Matrix4f(customProjection)
                : createProjectionMatrixForCamera(gr, target, fov);

        PoseStack poseStack = new PoseStack();

        Quaternionf cameraRotation = camera.rotation().conjugate(new Quaternionf());
        Matrix4f cameraMatrix = (new Matrix4f()).rotation(cameraRotation);
        Vec3 cameraPos = camera.getPosition();

        gr.resetProjectionMatrix(projMatrix);
        lr.prepareCullFrustum(cameraPos, cameraMatrix, projMatrix);

        lr.renderLevel(deltaTracker, false, camera, gr,
                gr.lightTexture(), cameraMatrix, projMatrix);

        Matrix4f modelViewMatrix = RenderSystem.getModelViewMatrix();

        VistaPlatStuff.dispatchRenderStageAfterLevel(mc, poseStack, camera, modelViewMatrix, projMatrix);
        gr.resetProjectionMatrix(oldProjectionMatrix);
    }

    @SuppressWarnings("ConstantConditions")
    private static void setupSceneCamera(ViewFinderBlockEntity tile, Camera camera, float partialTicks) {
        Level level = tile.getLevel();
        Quaternionf viewFinderRot = tile.getWorldOrientation(partialTicks);
        //TODO: add Z for when looking up
        EntityAngles entityAngles = EntityAngles.fromQuaternion(viewFinderRot);
        float yaw = entityAngles.yaw();
        float pitch = entityAngles.pitch();

        camera.initialized = true;
        camera.level = level;
        if (camera.entity == null) {
            camera.entity = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
        }
        Entity dummyCameraEntity = camera.getEntity();
        Vec3 pos = tile.getBlockPos().getCenter();
        dummyCameraEntity.setPos(pos);
        dummyCameraEntity.setXRot(pitch);
        dummyCameraEntity.setYRot(yaw + 180);

        camera.setPosition(pos);
        camera.setRotation(yaw, pitch);
    }

    //Same as GameRenderer getProjectionMatrix but with custom fov and aspect ratio based on target size, and no zoom support (for now)
    private static Matrix4f createProjectionMatrixForCamera(GameRenderer gr, RenderTarget target, float fov) {
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
                (float) target.width / (float) target.height,
                ViewFinderBlockEntity.NEAR_PLANE, depthFar);
    }


    //mixin called stuff

    public static boolean onSetupRenderer(LevelRenderer lr, Camera camera, Frustum frustum, boolean hasCapturedFrustum, boolean isSpectator) {
        if (!isRenderingLiveFeed()) {
            return false;
        }

        if (CompatHandler.SODIUM) return false;

        Vec3 cameraPosition = camera.getPosition();
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;

        // Check if the effective render distance has changed; if so, mark all chunks as needing update
        //TODO: change
        if (minecraft.options.getEffectiveRenderDistance() != lr.lastViewDistance) {
            viewAreaStuffChanged(lr); //this initializes stuff and is crucial but could be hooked up better
        }

        clientLevel.getProfiler().push("camera");

        SectionOcclusionGraph graph = lr.sectionOcclusionGraph;


        // Get player's exact coordinates
        Entity cameraEntity = camera.entity; //this.minecraft.player
        double playerX = cameraEntity.getX();
        double playerY = cameraEntity.getY();
        double playerZ = cameraEntity.getZ();

        // Convert world coordinates to section (chunk) coordinates
        int cameraSectionX = SectionPos.posToSectionCoord(playerX);
        int cameraSectionY = SectionPos.posToSectionCoord(playerY);
        int cameraSectionZ = SectionPos.posToSectionCoord(playerZ);

        // If the camera has moved to a new section, update the renderer's tracking and reposition the view area
        if (lr.lastCameraSectionX != cameraSectionX ||
                lr.lastCameraSectionY != cameraSectionY ||
                lr.lastCameraSectionZ != cameraSectionZ) {

            lr.lastCameraSectionX = cameraSectionX;
            lr.lastCameraSectionY = cameraSectionY;
            lr.lastCameraSectionZ = cameraSectionZ;

            //lr.viewArea.repositionCamera(cameraX, cameraZ);
        }

        lr.sectionRenderDispatcher.setCamera(cameraPosition);

        clientLevel.getProfiler().popPush("cull");
        minecraft.getProfiler().popPush("culling");

        // Camera's block position (rounded to nearest block)
        BlockPos cameraBlockPos = camera.getBlockPosition();

        // Compute camera position in 8-block "units" for occlusion checks.
        // Use the actual render camera (ViewFinder position), not the player.
        double cameraUnitX = Math.floor(cameraPosition.x / 8.0);
        double cameraUnitY = Math.floor(cameraPosition.y / 8.0);
        double cameraUnitZ = Math.floor(cameraPosition.z / 8.0);

        if (cameraUnitX != lr.prevCamX ||
                cameraUnitY != lr.prevCamY ||
                cameraUnitZ != lr.prevCamZ) {
            // ViewFinder moved to a new 8-block cell — invalidate the feed graph.
            graph.invalidate();
        }

        // Store current 8-block unit for future comparisons
        lr.prevCamX = cameraUnitX;
        lr.prevCamY = cameraUnitY;
        lr.prevCamZ = cameraUnitZ;

        minecraft.getProfiler().popPush("update");

        // If the frustum has not already been captured
        if (!hasCapturedFrustum) {
            boolean smartCulling = minecraft.smartCull;

            // Disable smart culling for spectators inside solid blocks
            if (isSpectator && clientLevel.getBlockState(cameraBlockPos).isSolidRender(clientLevel, cameraBlockPos)) {
                //    smartCulling = false;
            }

            // Adjust entity view scale based on render distance and scaling option
            double entityViewScale = Mth.clamp( //TODO: change these
                    (double) minecraft.options.getEffectiveRenderDistance() / 8.0, 1.0, 2.5
            ) * minecraft.options.entityDistanceScaling().get();
            Entity.setViewScale(entityViewScale);

            minecraft.getProfiler().push("section_occlusion_graph");

            // Update occlusion graph to determine which sections are visible
            //needs full update should be performed when new chunks came into view (our camera moved too much compared to the vista cam)
            graph.update(smartCulling, camera, frustum, lr.visibleSections);

            minecraft.getProfiler().pop();


            // Divide camera rotation by 2 to track significant rotation changes
            double cameraRotXHalf = Math.floor(camera.getXRot() / 2.0);
            double cameraRotYHalf = Math.floor(camera.getYRot() / 2.0);

            // Apply frustum update if the graph changed or camera rotated significantly
            if (graph.consumeFrustumUpdate() ||
                    cameraRotXHalf != lr.prevCamRotX ||
                    cameraRotYHalf != lr.prevCamRotY) {

                lr.applyFrustum(LevelRenderer.offsetFrustum(frustum));
                lr.prevCamRotX = cameraRotXHalf;
                lr.prevCamRotY = cameraRotYHalf;
            }
        }

        minecraft.getProfiler().pop();

        return true;
    }

    private static void viewAreaStuffChanged(LevelRenderer lr) {
        Level level = Minecraft.getInstance().level;
        Minecraft mc = Minecraft.getInstance();

        lr.lastViewDistance = mc.options.getEffectiveRenderDistance();
        if (lr.viewArea != null) {
            //    lr.viewArea.releaseAllBuffers();
        }

        //    lr.sectionRenderDispatcher.blockUntilClear();

        //lr.viewArea = new ViewArea(lr.sectionRenderDispatcher, level, mc.options.getEffectiveRenderDistance(), lr);
        lr.sectionOcclusionGraph.waitAndReset(lr.viewArea);
        lr.visibleSections.clear();
        Entity entity = mc.getCameraEntity();
        if (entity != null) {
            lr.viewArea.repositionCamera(entity.getX(), entity.getZ());
        }

    }

    //very ugly because these can be called on another thread

    public static void onChunkLoaded(ChunkPos chunkPos, SectionOcclusionGraph sectionOcclusionGraph) {
        if (CompatHandler.SODIUM) return;
        LevelRendererCameraState[] snapshot;
        synchronized (STATES_LOCK) {
            snapshot = MANAGED_STATES.toArray(new LevelRendererCameraState[0]);
        }
        for (LevelRendererCameraState state : snapshot) {
            SectionOcclusionGraph graph = state.getOcclusionGraph();
            if (graph != null && graph != sectionOcclusionGraph) {
                graph.onChunkLoaded(chunkPos);
            }
        }
        SectionOcclusionGraph old = MC_OWN_GRAPH.get();
        if (old != null && old != sectionOcclusionGraph) {
            old.onChunkLoaded(chunkPos);
        }
    }

    public static void onRecentlyCompiledSection(SectionRenderDispatcher.RenderSection renderSection, SectionOcclusionGraph sectionOcclusionGraph) {
        if (CompatHandler.SODIUM) return;
        LevelRendererCameraState[] snapshot;
        synchronized (STATES_LOCK) {
            snapshot = MANAGED_STATES.toArray(new LevelRendererCameraState[0]);
        }
        for (LevelRendererCameraState state : snapshot) {
            SectionOcclusionGraph graph = state.getOcclusionGraph();
            if (graph != null && graph != sectionOcclusionGraph) {
                graph.onSectionCompiled(renderSection);
            }
        }
        SectionOcclusionGraph old = MC_OWN_GRAPH.get();
        if (old != null && old != sectionOcclusionGraph) {
            old.onSectionCompiled(renderSection);
        }
    }
}
