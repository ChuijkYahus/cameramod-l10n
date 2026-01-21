package net.mehvahdjukaar.vista.client.textures;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.List;
import java.util.SortedSet;

public class LevelRendererTest {


    public static void renderLevel(LevelRenderer lr,
                                   DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix) {

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        TickRateManager tickRateManager = mc.level.tickRateManager();
        float f = deltaTracker.getGameTimeDeltaPartialTick(false);
        RenderSystem.setShaderGameTime(level.getGameTime(), f);
        lr.blockEntityRenderDispatcher.prepare(level, camera, mc.hitResult);
        lr.entityRenderDispatcher.prepare(level, camera, mc.crosshairPickEntity);
        ProfilerFiller profilerFiller = level.getProfiler();
        profilerFiller.popPush("light_update_queue");
        level.pollLightUpdates();
        profilerFiller.popPush("light_updates");
        level.getChunkSource().getLightEngine().runLightUpdates();
        Vec3 vec3 = camera.getPosition();
        double d = vec3.x();
        double e = vec3.y();
        double g = vec3.z();
        profilerFiller.popPush("culling");
        boolean bl = lr.capturedFrustum != null;
        Frustum frustum;
        if (bl) {
            frustum = lr.capturedFrustum;
            frustum.prepare(lr.frustumPos.x, lr.frustumPos.y, lr.frustumPos.z);
        } else {
            frustum = lr.cullingFrustum;
        }

        mc.getProfiler().popPush("captureFrustum");
        if (lr.captureFrustum) {
            lr.captureFrustum(frustumMatrix, projectionMatrix, vec3.x, vec3.y, vec3.z, bl ? new Frustum(frustumMatrix, projectionMatrix) : frustum);
            lr.captureFrustum = false;
        }

        profilerFiller.popPush("clear");
        FogRenderer.setupColor(camera, f, mc.level, mc.options.getEffectiveRenderDistance(), gameRenderer.getDarkenWorldAmount(f));
        FogRenderer.levelFogColor();
        RenderSystem.clear(16640, Minecraft.ON_OSX);
        float h = gameRenderer.getRenderDistance();
        boolean bl2 = mc.level.effects().isFoggyAt(Mth.floor(d), Mth.floor(e)) || mc.gui.getBossOverlay().shouldCreateWorldFog();
        profilerFiller.popPush("sky");
        RenderSystem.setShader(GameRenderer::getPositionShader);
        lr.renderSky(frustumMatrix, projectionMatrix, f, camera, bl2, () -> FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_SKY, h, bl2, f));
        profilerFiller.popPush("fog");
        FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_TERRAIN, Math.max(h, 32.0F), bl2, f);


        profilerFiller.popPush("terrain_setup");
        setupRender(lr, camera, frustum, bl, mc.player.isSpectator());


        profilerFiller.popPush("compile_sections");
        lr.compileSections(camera);


        profilerFiller.popPush("terrain");
        lr.renderSectionLayer(RenderType.solid(), d, e, g, frustumMatrix, projectionMatrix);
        lr.renderSectionLayer(RenderType.cutoutMipped(), d, e, g, frustumMatrix, projectionMatrix);
        lr.renderSectionLayer(RenderType.cutout(), d, e, g, frustumMatrix, projectionMatrix);
        if (level.effects().constantAmbientLight()) {
            Lighting.setupNetherLevel();
        } else {
            Lighting.setupLevel();
        }




        profilerFiller.popPush("entities");
        lr.renderedEntities = 0;
        lr.culledEntities = 0;

        if (lr.itemEntityTarget != null) {
            lr.itemEntityTarget.clear(Minecraft.ON_OSX);
            lr.itemEntityTarget.copyDepthFrom(mc.getMainRenderTarget());
            mc.getMainRenderTarget().bindWrite(false);
        }

        if (lr.weatherTarget != null) {
            lr.weatherTarget.clear(Minecraft.ON_OSX);
        }

        if (lr.shouldShowEntityOutlines()) {
            lr.entityTarget.clear(Minecraft.ON_OSX);
            mc.getMainRenderTarget().bindWrite(false);
        }

        Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
        matrix4fStack.pushMatrix();
        matrix4fStack.mul(frustumMatrix);
        RenderSystem.applyModelViewMatrix();
        boolean bl3 = false;
        PoseStack poseStack = new PoseStack();
        MultiBufferSource.BufferSource bufferSource = lr.renderBuffers.bufferSource();



        for (Entity entity : level.entitiesForRendering()) {
            if (lr.entityRenderDispatcher.shouldRender(entity, frustum, d, e, g) || entity.hasIndirectPassenger(mc.player)) {
                BlockPos blockPos = entity.blockPosition();
                if ((level.isOutsideBuildHeight(blockPos.getY()) || lr.isSectionCompiled(blockPos)) && (entity != camera.getEntity() || camera.isDetached() || camera.getEntity() instanceof LivingEntity && ((LivingEntity) camera.getEntity()).isSleeping()) && (!(entity instanceof LocalPlayer) || camera.getEntity() == entity)) {
                    ++lr.renderedEntities;
                    if (entity.tickCount == 0) {
                        entity.xOld = entity.getX();
                        entity.yOld = entity.getY();
                        entity.zOld = entity.getZ();
                    }

                    MultiBufferSource multiBufferSource;
                    if (lr.shouldShowEntityOutlines() && mc.shouldEntityAppearGlowing(entity)) {
                        bl3 = true;
                        OutlineBufferSource outlineBufferSource = lr.renderBuffers.outlineBufferSource();
                        multiBufferSource = outlineBufferSource;
                        int i = entity.getTeamColor();
                        outlineBufferSource.setColor(FastColor.ARGB32.red(i), FastColor.ARGB32.green(i), FastColor.ARGB32.blue(i), 255);
                    } else {
                        multiBufferSource = bufferSource;
                    }

                    float j = deltaTracker.getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(entity));
                    lr.renderEntity(entity, d, e, g, j, poseStack, multiBufferSource);
                }
            }
        }


        bufferSource.endLastBatch();
        lr.checkPoseStack(poseStack);
        bufferSource.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
        bufferSource.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        bufferSource.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        bufferSource.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));
        profilerFiller.popPush("blockentities");


        for (SectionRenderDispatcher.RenderSection renderSection : lr.visibleSections) {
            List<BlockEntity> list = renderSection.getCompiled().getRenderableBlockEntities();
            if (!list.isEmpty()) {
                for (BlockEntity blockEntity : list) {
                    BlockPos blockPos2 = blockEntity.getBlockPos();
                    MultiBufferSource multiBufferSource2 = bufferSource;
                    poseStack.pushPose();
                    poseStack.translate((double) blockPos2.getX() - d, (double) blockPos2.getY() - e, (double) blockPos2.getZ() - g);
                    SortedSet<BlockDestructionProgress> sortedSet = lr.destructionProgress.get(blockPos2.asLong());
                    if (sortedSet != null && !sortedSet.isEmpty()) {
                        int k = sortedSet.last().getProgress();
                        if (k >= 0) {
                            PoseStack.Pose pose = poseStack.last();
                            VertexConsumer vertexConsumer = new SheetedDecalTextureGenerator(lr.renderBuffers.crumblingBufferSource().getBuffer((RenderType) ModelBakery.DESTROY_TYPES.get(k)), pose, 1.0F);
                            multiBufferSource2 = (renderType) -> {
                                VertexConsumer vertexConsumer2 = bufferSource.getBuffer(renderType);
                                return renderType.affectsCrumbling() ? VertexMultiConsumer.create(vertexConsumer, vertexConsumer2) : vertexConsumer2;
                            };
                        }
                    }

                    lr.blockEntityRenderDispatcher.render(blockEntity, f, poseStack, multiBufferSource2);
                    poseStack.popPose();
                }
            }
        }

        synchronized (lr.globalBlockEntities) {
            for (BlockEntity blockEntity2 : lr.globalBlockEntities) {
                BlockPos blockPos3 = blockEntity2.getBlockPos();
                poseStack.pushPose();
                poseStack.translate((double) blockPos3.getX() - d, (double) blockPos3.getY() - e, (double) blockPos3.getZ() - g);
                lr.blockEntityRenderDispatcher.render(blockEntity2, f, poseStack, bufferSource);
                poseStack.popPose();
            }
        }

        lr.checkPoseStack(poseStack);

        bufferSource.endBatch(RenderType.solid());
        bufferSource.endBatch(RenderType.endPortal());
        bufferSource.endBatch(RenderType.endGateway());
        bufferSource.endBatch(Sheets.solidBlockSheet());
        bufferSource.endBatch(Sheets.cutoutBlockSheet());
        bufferSource.endBatch(Sheets.bedSheet());
        bufferSource.endBatch(Sheets.shulkerBoxSheet());
        bufferSource.endBatch(Sheets.signSheet());
        bufferSource.endBatch(Sheets.hangingSignSheet());
        bufferSource.endBatch(Sheets.chestSheet());


         lr.renderBuffers.outlineBufferSource().endOutlineBatch();
        if (bl3) {
          lr.entityEffect.process(deltaTracker.getGameTimeDeltaTicks());
          mc.getMainRenderTarget().bindWrite(false);
        }

        profilerFiller.popPush("destroyProgress");


        for (var entry : lr.destructionProgress.long2ObjectEntrySet()) {
            BlockPos blockPos = BlockPos.of(entry.getLongKey());
            double l = (double) blockPos.getX() - d;
            double m = (double) blockPos.getY() - e;
            double n = (double) blockPos.getZ() - g;
            if (!(l * l + m * m + n * n > (double) 1024.0F)) {
                SortedSet<BlockDestructionProgress> sortedSet2 = entry.getValue();
                if (sortedSet2 != null && !sortedSet2.isEmpty()) {
                    int o = sortedSet2.last().getProgress();
                    poseStack.pushPose();
                    poseStack.translate((double) blockPos.getX() - d, (double) blockPos.getY() - e, (double) blockPos.getZ() - g);
                    PoseStack.Pose pose2 = poseStack.last();
                    VertexConsumer vertexConsumer2 = new SheetedDecalTextureGenerator(lr.renderBuffers.crumblingBufferSource().getBuffer((RenderType) ModelBakery.DESTROY_TYPES.get(o)), pose2, 1.0F);
                    mc.getBlockRenderer().renderBreakingTexture(level.getBlockState(blockPos), blockPos, level, poseStack, vertexConsumer2);
                    poseStack.popPose();
                }
            }
        }

        lr.checkPoseStack(poseStack);
        HitResult hitResult = mc.hitResult;

        if (renderBlockOutline && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            profilerFiller.popPush("outline");
            BlockPos blockPos4 = ((BlockHitResult) hitResult).getBlockPos();
            BlockState blockState = level.getBlockState(blockPos4);
            if (!blockState.isAir() && level.getWorldBorder().isWithinBounds(blockPos4)) {
                VertexConsumer vertexConsumer3 = bufferSource.getBuffer(RenderType.lines());
                lr.renderHitOutline(poseStack, vertexConsumer3, camera.getEntity(), d, e, g, blockPos4, blockState);
            }
        }

        mc.debugRenderer.render(poseStack, bufferSource, d, e, g);

        bufferSource.endLastBatch();
        bufferSource.endBatch(Sheets.translucentCullBlockSheet());
        bufferSource.endBatch(Sheets.bannerSheet());
        bufferSource.endBatch(Sheets.shieldSheet());
        bufferSource.endBatch(RenderType.armorEntityGlint());
        bufferSource.endBatch(RenderType.glint());
        bufferSource.endBatch(RenderType.glintTranslucent());
        bufferSource.endBatch(RenderType.entityGlint());
        bufferSource.endBatch(RenderType.entityGlintDirect());
        bufferSource.endBatch(RenderType.waterMask());

        lr.renderBuffers.crumblingBufferSource().endBatch();
        if (lr.transparencyChain != null) {
            /*
            bufferSource.endBatch(RenderType.lines());
            bufferSource.endBatch();
            lr.translucentTarget.clear(Minecraft.ON_OSX);
            lr.translucentTarget.copyDepthFrom(mc.getMainRenderTarget());
            profilerFiller.popPush("translucent");
            lr.renderSectionLayer(RenderType.translucent(), d, e, g, frustumMatrix, projectionMatrix);
            profilerFiller.popPush("string");
            lr.renderSectionLayer(RenderType.tripwire(), d, e, g, frustumMatrix, projectionMatrix);
            lr.particlesTarget.clear(Minecraft.ON_OSX);
            lr.particlesTarget.copyDepthFrom(mc.getMainRenderTarget());
            RenderStateShard.PARTICLES_TARGET.setupRenderState();
            profilerFiller.popPush("particles");
            mc.particleEngine.render(lightTexture, camera, f);
            RenderStateShard.PARTICLES_TARGET.clearRenderState();

             */
        } else {

            profilerFiller.popPush("translucent");
            if (lr.translucentTarget != null) {
                lr.translucentTarget.clear(Minecraft.ON_OSX);
            }

            lr.renderSectionLayer(RenderType.translucent(), d, e, g, frustumMatrix, projectionMatrix);
            bufferSource.endBatch(RenderType.lines());
            bufferSource.endBatch();
            profilerFiller.popPush("string");
            lr.renderSectionLayer(RenderType.tripwire(), d, e, g, frustumMatrix, projectionMatrix);
            profilerFiller.popPush("particles");
            mc.particleEngine.render(lightTexture, camera, f);

        }

        if (mc.options.getCloudsType() != CloudStatus.OFF) {

            if (lr.transparencyChain != null) {
                lr.cloudsTarget.clear(Minecraft.ON_OSX);
            }

            profilerFiller.popPush("clouds");
            lr.renderClouds(poseStack, frustumMatrix, projectionMatrix, f, d, e, g);


        }

        if (lr.transparencyChain != null) {
            /*
            RenderStateShard.WEATHER_TARGET.setupRenderState();
            profilerFiller.popPush("weather");
            lr.renderSnowAndRain(lightTexture, f, d, e, g);
            lr.renderWorldBorder(camera);
            RenderStateShard.WEATHER_TARGET.clearRenderState();
            lr.transparencyChain.process(deltaTracker.getGameTimeDeltaTicks());
            mc.getMainRenderTarget().bindWrite(false);

             */
        } else {

            RenderSystem.depthMask(false);
            profilerFiller.popPush("weather");
            lr.renderSnowAndRain(lightTexture, f, d, e, g);
            lr.renderWorldBorder(camera);
            RenderSystem.depthMask(true);


        }

        lr.renderDebug(poseStack, bufferSource, camera);
        bufferSource.endLastBatch();
        matrix4fStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        FogRenderer.setupNoFog();
    }



    public static void setupRender(LevelRenderer levelRenderer, Camera camera, Frustum frustum, boolean hasCapturedFrustum, boolean isSpectator) {
        Vec3 cameraPosition = camera.getPosition();
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;

        // Check if the effective render distance has changed; if so, mark all chunks as needing update
        if (minecraft.options.getEffectiveRenderDistance() != levelRenderer.lastViewDistance) {
            levelRenderer.allChanged();
        }

        clientLevel.getProfiler().push("camera");

        // Get player's exact coordinates
        double playerX = minecraft.player.getX();
        double playerY = minecraft.player.getY();
        double playerZ = minecraft.player.getZ();

        // Convert world coordinates to section (chunk) coordinates
        int cameraSectionX = SectionPos.posToSectionCoord(playerX);
        int cameraSectionY = SectionPos.posToSectionCoord(playerY);
        int cameraSectionZ = SectionPos.posToSectionCoord(playerZ);

        // If the camera has moved to a new section, update the renderer's tracking and reposition the view area
        if (levelRenderer.lastCameraSectionX != cameraSectionX ||
                levelRenderer.lastCameraSectionY != cameraSectionY ||
                levelRenderer.lastCameraSectionZ != cameraSectionZ) {

            levelRenderer.lastCameraSectionX = cameraSectionX;
            levelRenderer.lastCameraSectionY = cameraSectionY;
            levelRenderer.lastCameraSectionZ = cameraSectionZ;

            levelRenderer.viewArea.repositionCamera(playerX, playerZ);
        }

        // Update the section render dispatcher with the camera position
        levelRenderer.sectionRenderDispatcher.setCamera(cameraPosition);

        clientLevel.getProfiler().popPush("cull");
        minecraft.getProfiler().popPush("culling");

        // Camera's block position (rounded to nearest block)
        BlockPos cameraBlockPos = camera.getBlockPosition();

        // Compute camera position in 8-block "units" for occlusion checks
        double cameraUnitX = Math.floor(cameraPosition.x / 8.0);
        double cameraUnitY = Math.floor(cameraPosition.y / 8.0);
        double cameraUnitZ = Math.floor(cameraPosition.z / 8.0);

        // If the camera has moved to a new 8-block unit, invalidate the occlusion graph
        if (cameraUnitX != levelRenderer.prevCamX ||
                cameraUnitY != levelRenderer.prevCamY ||
                cameraUnitZ != levelRenderer.prevCamZ) {

            levelRenderer.sectionOcclusionGraph.invalidate();
        }

        // Store current 8-block unit for future comparisons
        levelRenderer.prevCamX = cameraUnitX;
        levelRenderer.prevCamY = cameraUnitY;
        levelRenderer.prevCamZ = cameraUnitZ;

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
            levelRenderer.sectionOcclusionGraph.update(smartCulling, camera, frustum, levelRenderer.visibleSections);
            minecraft.getProfiler().pop();

            // Divide camera rotation by 2 to track significant rotation changes
            double cameraRotXHalf = Math.floor(camera.getXRot() / 2.0);
            double cameraRotYHalf = Math.floor(camera.getYRot() / 2.0);

            // Apply frustum update if the graph changed or camera rotated significantly
            if (levelRenderer.sectionOcclusionGraph.consumeFrustumUpdate() ||
                    cameraRotXHalf != levelRenderer.prevCamRotX ||
                    cameraRotYHalf != levelRenderer.prevCamRotY) {

                levelRenderer.applyFrustum(LevelRenderer.offsetFrustum(frustum));
                levelRenderer.prevCamRotX = cameraRotXHalf;
                levelRenderer.prevCamRotY = cameraRotYHalf;
            }
        }

        minecraft.getProfiler().pop();
    }


}
