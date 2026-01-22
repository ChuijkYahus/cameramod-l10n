package net.mehvahdjukaar.vista.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector4f;

public class LevelRendererCameraState {

    private int lastCameraSectionX = Integer.MIN_VALUE;
    private int lastCameraSectionY = Integer.MIN_VALUE;
    private int lastCameraSectionZ = Integer.MIN_VALUE;
    private double prevCamX = Double.MIN_VALUE;
    private double prevCamY = Double.MIN_VALUE;
    private double prevCamZ = Double.MIN_VALUE;
    private double prevCamRotX = Double.MIN_VALUE;
    private double prevCamRotY = Double.MIN_VALUE;
    @Nullable
    private ViewArea viewArea;
    private int lastViewDistance;
    private Frustum capturedFrustum;
    private Vector4f[] frustumPoints;
    private Vector3d frustumPos;
    private SectionOcclusionGraph sectionOcclusionGraph;

    private LevelRendererCameraState() {

    }

    public static LevelRendererCameraState createNew() {
        var instance = new LevelRendererCameraState();
        instance.frustumPoints = new Vector4f[8];
        instance.frustumPos = new Vector3d(0.0F, 0.0F, 0.0F);
        instance.sectionOcclusionGraph = new SectionOcclusionGraph();
        Minecraft mc = Minecraft.getInstance();
        LevelRenderer lr = mc.levelRenderer;
        instance.viewArea = new ViewArea(lr.sectionRenderDispatcher, mc.level,
                //TODO: change this
                mc.options.getEffectiveRenderDistance(), lr);
        return instance;
    }

    public void copyFrom(LevelRenderer lr) {
        this.viewArea = lr.viewArea;
        this.lastViewDistance = lr.lastViewDistance;
        this.capturedFrustum = lr.capturedFrustum;
        this.frustumPoints = lr.frustumPoints;
        this.frustumPos = lr.frustumPos;
        this.sectionOcclusionGraph = lr.sectionOcclusionGraph;
        this.lastCameraSectionX = lr.lastCameraSectionX;
        this.lastCameraSectionY = lr.lastCameraSectionY;
        this.lastCameraSectionZ = lr.lastCameraSectionZ;
        this.prevCamX = lr.prevCamX;
        this.prevCamY = lr.prevCamY;
        this.prevCamZ = lr.prevCamZ;
        this.prevCamRotX = lr.prevCamRotX;
        this.prevCamRotY = lr.prevCamRotY;
    }

    public static LevelRendererCameraState capture(LevelRenderer lr) {
        var instance = new LevelRendererCameraState();
        instance.copyFrom(lr);
        return instance;
    }

    public void apply(LevelRenderer lr) {
        lr.viewArea = this.viewArea;
        lr.lastViewDistance = this.lastViewDistance;
        lr.capturedFrustum = this.capturedFrustum;
        lr.frustumPoints = this.frustumPoints;
        lr.frustumPos = this.frustumPos;
        lr.sectionOcclusionGraph = this.sectionOcclusionGraph;
        lr.lastCameraSectionX = this.lastCameraSectionX;
        lr.lastCameraSectionY = this.lastCameraSectionY;
        lr.lastCameraSectionZ = this.lastCameraSectionZ;
        lr.prevCamX = this.prevCamX;
        lr.prevCamY = this.prevCamY;
        lr.prevCamZ = this.prevCamZ;
        lr.prevCamRotX = this.prevCamRotX;
        lr.prevCamRotY = this.prevCamRotY;
    }


    public static void setupRender(LevelRenderer lr, Camera camera, Frustum frustum, boolean hasCapturedFrustum, boolean isSpectator) {
        Vec3 cameraPosition = camera.getPosition();
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;

        // Check if the effective render distance has changed; if so, mark all chunks as needing update
        if (minecraft.options.getEffectiveRenderDistance() != lr.lastViewDistance) {
            viewAreaStuffChanged(lr); //never invalidate
        }

        clientLevel.getProfiler().push("camera");

        // Get player's exact coordinates
        Entity player = camera.entity;
        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();

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

        // Compute camera position in 8-block "units" for occlusion checks
        double cameraUnitX = Math.floor(cameraPosition.x / 8.0);
        double cameraUnitY = Math.floor(cameraPosition.y / 8.0);
        double cameraUnitZ = Math.floor(cameraPosition.z / 8.0);

        // If the camera has moved to a new 8-block unit, invalidate the occlusion graph
        if (cameraUnitX != lr.prevCamX ||
                cameraUnitY != lr.prevCamY ||
                cameraUnitZ != lr.prevCamZ) {

            lr.sectionOcclusionGraph.invalidate();
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
                smartCulling = false;
            }

            // Adjust entity view scale based on render distance and scaling option
            double entityViewScale = Mth.clamp(
                    (double) minecraft.options.getEffectiveRenderDistance() / 8.0, 1.0, 2.5
            ) * minecraft.options.entityDistanceScaling().get();
            Entity.setViewScale(entityViewScale);

            minecraft.getProfiler().push("section_occlusion_graph");

            // Update occlusion graph to determine which sections are visible
            lr.sectionOcclusionGraph.update(smartCulling, camera, frustum, lr.visibleSections);
            minecraft.getProfiler().pop();

            // Divide camera rotation by 2 to track significant rotation changes
            double cameraRotXHalf = Math.floor(camera.getXRot() / 2.0);
            double cameraRotYHalf = Math.floor(camera.getYRot() / 2.0);

            // Apply frustum update if the graph changed or camera rotated significantly
            if (lr.sectionOcclusionGraph.consumeFrustumUpdate() ||
                    cameraRotXHalf != lr.prevCamRotX ||
                    cameraRotYHalf != lr.prevCamRotY) {

                lr.applyFrustum(LevelRenderer.offsetFrustum(frustum));
                lr.prevCamRotX = cameraRotXHalf;
                lr.prevCamRotY = cameraRotYHalf;
            }
        }

        minecraft.getProfiler().pop();
    }

    public static void viewAreaStuffChanged(LevelRenderer lr) {
        Level level = Minecraft.getInstance().level;
        Minecraft mc = Minecraft.getInstance();

        lr.lastViewDistance = mc.options.getEffectiveRenderDistance();
        if (lr.viewArea != null) {
            lr.viewArea.releaseAllBuffers();
        }

        //    lr.sectionRenderDispatcher.blockUntilClear();

        lr.viewArea = new ViewArea(lr.sectionRenderDispatcher, level, mc.options.getEffectiveRenderDistance(), lr);
        lr.sectionOcclusionGraph.waitAndReset(lr.viewArea);
        lr.visibleSections.clear();
        Entity entity = mc.getCameraEntity();
        if (entity != null) {
            lr.viewArea.repositionCamera(entity.getX(), entity.getZ());
        }

    }


}
