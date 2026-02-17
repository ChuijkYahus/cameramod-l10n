package net.mehvahdjukaar.vista.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.moonlight.api.misc.WeakHashSet;
import net.mehvahdjukaar.moonlight.core.client.DummyCamera;
import net.mehvahdjukaar.vista.VistaPlatStuff;
import net.mehvahdjukaar.vista.client.textures.LiveFeedTexture;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static net.minecraft.client.Minecraft.ON_OSX;

public class VistaLevelRenderer {

    private static final Set<SectionOcclusionGraph> MANAGED_GRAPHS = new WeakHashSet<>();
    private static final AtomicReference<SectionOcclusionGraph> MC_OWN_GRAPH = new AtomicReference<>(null);
    private static final DummyCamera DUMMY_CAMERA = new DummyCamera();

    private static ViewFinderBlockEntity renderingLiveFeedVF = null;

    public static boolean isRenderingLiveFeed() {
        return renderingLiveFeedVF != null;
    }

    public static boolean isViewFinderRenderingLiveFeed(ViewFinderBlockEntity vf) {
        return renderingLiveFeedVF == vf;
    }

    public static void clear() {
        DUMMY_CAMERA.entity = null;
        MC_OWN_GRAPH.set(null);
        MANAGED_GRAPHS.clear();
        renderingLiveFeedVF = null;
    }

    // change start: feed-state
    public static void render(LiveFeedTexture text, ViewFinderBlockEntity tile) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget mainTarget = mc.getMainRenderTarget();
        RenderTarget canvas = text.getRenderTarget();
        mc.mainRenderTarget = canvas;

        Camera camera = DUMMY_CAMERA;
        Camera mainCamera = mc.gameRenderer.mainCamera;
        mc.gameRenderer.mainCamera = camera;

        // save old state
        float oldRenderDistance = mc.gameRenderer.renderDistance;
        PostChain oldPostEffect = mc.gameRenderer.postEffect;
        boolean wasEffectActive = mc.gameRenderer.effectActive;

        // switch to feed camera state
        LevelRendererCameraState oldCameraState = LevelRendererCameraState.capture(mc.levelRenderer);
        LevelRendererCameraState feedCameraState = text.getRendererState();
        Matrix4f oldModelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix());

        RenderSystem.backupProjectionMatrix();
        try {
            renderingLiveFeedVF = tile;

            float partialTicks = mc.getTimer().getGameTimeDeltaTicks();
            setupSceneCamera(tile, camera, partialTicks);

            canvas.bindWrite(true);
            RenderSystem.viewport(0, 0, canvas.width, canvas.height);

            text.applyPostChain();

            mc.gameRenderer.renderDistance = 128;// Math.min(oldRenderDistance, ClientConfigs.RENDER_DISTANCE.get());

            // use camera fov
            float fov = tile.getFOV();

            RenderSystem.clear(16640, ON_OSX);
            FogRenderer.setupNoFog();
            RenderSystem.enableCull();

            feedCameraState.apply(mc.levelRenderer);

            MANAGED_GRAPHS.add(feedCameraState.getOcclusionGraph());
            MC_OWN_GRAPH.set(oldCameraState.getOcclusionGraph());

        renderLevel2(mc, canvas, camera, fov);

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

            // swap back
            renderingLiveFeedVF = null;

            mc.mainRenderTarget = mainTarget;
            mc.gameRenderer.mainCamera = mainCamera;

            // restore post process
            mc.gameRenderer.postEffect = oldPostEffect;
            mc.gameRenderer.effectActive = wasEffectActive;
            mc.gameRenderer.renderDistance = oldRenderDistance;

            // restore global render state
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.getModelViewStack().set(oldModelViewMatrix);
            RenderSystem.applyModelViewMatrix();
            mainTarget.bindWrite(true);
            RenderSystem.viewport(0, 0, mainTarget.width, mainTarget.height);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();

            // clear depth only; clearing color here causes visible world/water popping
            RenderSystem.clear(GL11C.GL_DEPTH_BUFFER_BIT, ON_OSX);
        }
    }
    // change end: feed-state


    private static void renderLevel2(Minecraft mc, RenderTarget target, Camera camera, float fov) {

        // --- Capture common GL11 state ---

        boolean blend      = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthTest  = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cullFace   = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean scissor    = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean stencil    = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        boolean alphaTest  = GL11.glIsEnabled(GL11.GL_ALPHA_TEST); // if compatibility profile

        int blendSrc  = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        int blendDst  = GL11.glGetInteger(GL11.GL_BLEND_DST);
        int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int cullMode  = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);

        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);


        float[] clearColor = new float[4];
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);

        try {

            renderLevel(mc, target, camera, fov);

        } finally {

            // --- Restore state deterministically ---

            setState(GL11.GL_BLEND, blend);
            setState(GL11.GL_DEPTH_TEST, depthTest);
            setState(GL11.GL_CULL_FACE, cullFace);
            setState(GL11.GL_SCISSOR_TEST, scissor);
            setState(GL11.GL_STENCIL_TEST, stencil);
            setState(GL11.GL_ALPHA_TEST, alphaTest);

            GL11.glBlendFunc(blendSrc, blendDst);
            GL11.glDepthFunc(depthFunc);
            GL11.glCullFace(cullMode);

            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);


            GL11.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        }
    }

    private static void setState(int cap, boolean desired) {
        boolean current = GL11.glIsEnabled(cap);

            if (current != desired) {
            System.out.println("[GL STATE] " + (cap) +
                    " changed: " + current + " -> " + desired);

            if (desired) {
                GL11.glEnable(cap);
            } else {
                GL11.glDisable(cap);
            }
        }
    }




    //same as game renderer render level but simplified
    private static void renderLevel(Minecraft mc, RenderTarget target, Camera camera, float fov) {
        DeltaTracker deltaTracker = mc.getTimer();
        GameRenderer gr = mc.gameRenderer;
        LevelRenderer lr = mc.levelRenderer;
        Matrix4f oldProjectionMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());

        Matrix4f projMatrix = createProjectionMatrix(gr, target, fov);
        //fix Y inversion
        gr.resetProjectionMatrix(projMatrix);

        PoseStack poseStack = new PoseStack();

        Quaternionf cameraRotation = camera.rotation().conjugate(new Quaternionf());
        Matrix4f cameraMatrix = (new Matrix4f()).rotation(cameraRotation);
        //this below is what actually renders everything
        Vec3 cameraPos = camera.getPosition();
        lr.prepareCullFrustum(cameraPos, cameraMatrix, projMatrix);

        lr.renderLevel(deltaTracker, false, camera, gr,
                gr.lightTexture(), cameraMatrix, projMatrix);

        Matrix4f modelViewMatrix = RenderSystem.getModelViewMatrix();

        VistaPlatStuff.dispatchRenderStageAfterLevel(mc, poseStack, camera, modelViewMatrix, projMatrix);
        gr.resetProjectionMatrix(oldProjectionMatrix);
    }

    @SuppressWarnings("ConstantConditions")
    private static void setupSceneCamera(ViewFinderBlockEntity tile, Camera dummyCamera, float partialTicks) {
        Level level = tile.getLevel();
        float pitch = tile.getPitch(partialTicks);
        float yaw = tile.getYaw(partialTicks);

        if (dummyCamera.entity == null) {
            dummyCamera.entity = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
        }
        Entity dummyCameraEntity = dummyCamera.getEntity();
        Vec3 pos = tile.getBlockPos().getCenter();
        dummyCameraEntity.setPos(pos);
        dummyCameraEntity.setXRot(pitch);
        dummyCameraEntity.setYRot(yaw + 180);

        dummyCamera.setPosition(pos);
        dummyCamera.setRotation(yaw, pitch);
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
                (float) target.width / (float) target.height,
                ViewFinderBlockEntity.NEAR_PLANE, depthFar);
    }


    //mixin called stuff

    public static boolean setupRender(LevelRenderer lr, Camera camera, Frustum frustum, boolean hasCapturedFrustum, boolean isSpectator) {
        if (!isRenderingLiveFeed()) {
            return false;
        }

        if (CompatHandler.SODIUM) return false; //?? todo: give this a custom impl that follows what sodium does

        Vec3 cameraPosition = camera.getPosition();
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;

        // Check if the effective render distance has changed; if so, mark all chunks as needing update
        //TODO: change
        if (minecraft.options.getEffectiveRenderDistance() != lr.lastViewDistance) {
            viewAreaStuffChanged(lr); //never invalidate
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

            lr.viewArea.repositionCamera(playerX, playerZ);
        }

        // Update the section render dispatcher with the camera position
        lr.sectionRenderDispatcher.setCamera(cameraPosition);

        clientLevel.getProfiler().popPush("cull");
        minecraft.getProfiler().popPush("culling");

        // Camera's block position (rounded to nearest block)
        BlockPos cameraBlockPos = camera.getBlockPosition();

        Player player = minecraft.player;
        // Compute camera position in 8-block "units" for occlusion checks
        double cameraUnitX = Math.floor(player.getX() / 8.0);
        double cameraUnitY = Math.floor(player.getY() / 8.0);
        double cameraUnitZ = Math.floor(player.getZ() / 8.0);

        // If the camera has moved to a new 8-block unit, invalidate the occlusion graph
        if (cameraUnitX != lr.prevCamX ||
                cameraUnitY != lr.prevCamY ||
                cameraUnitZ != lr.prevCamZ) {
            //this should never triger for us since the camera never moves
            graph.invalidate(); //needs full update
            //update graph if player itself moved so we discard stale far away sections
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
        for (SectionOcclusionGraph graph : MANAGED_GRAPHS) {
            if (graph != sectionOcclusionGraph) {
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
        for (SectionOcclusionGraph graph : MANAGED_GRAPHS) {
            if (graph != sectionOcclusionGraph) {
                graph.onSectionCompiled(renderSection);
            }
        }
        SectionOcclusionGraph old = MC_OWN_GRAPH.get();
        if (old != null && old != sectionOcclusionGraph) {
            old.onSectionCompiled(renderSection);
        }
    }
}
