package net.mehvahdjukaar.camera_vision.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.mehvahdjukaar.camera_vision.CameraState;
import net.mehvahdjukaar.camera_vision.CameraVision;
import net.mehvahdjukaar.camera_vision.common.TVBlockEntity;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.FrameBufferBackedDynamicTexture;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.RenderedTexturesManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static net.minecraft.client.Minecraft.ON_OSX;

public class TvBlockEntityRenderer implements BlockEntityRenderer<TVBlockEntity> {

    public TvBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static final ResourceLocation ID = CameraVision.res("test");

    @Override
    public void render(TVBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int light, int packedOverlay) {

        var tex = RenderedTexturesManager.requestTexture(CameraVision.res("test3"), 256,
                this::update, true);


        if (tex.isInitialized()) {

            boolean hasText = true;

            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entitySolid(tex.getTextureLocation()));

            PoseStack.Pose pose = poseStack.last();
            int overlay = OverlayTexture.NO_OVERLAY;

            float z = 15.8f / 16f;

            float s = 0.5f;
            poseStack.translate(0.5, hasText ? 0.575 : 0.5, z);

            poseStack.translate(1, 1, 0);

            poseStack.pushPose();

            vertexConsumer.addVertex(pose, -s, -s, 0).setColor(1f, 1f, 1f, 1f).setUv(1f, 0f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);
            vertexConsumer.addVertex(pose, -s, s, 0).setColor(1f, 1f, 1f, 1f).setUv(1f, 1f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);

            vertexConsumer.addVertex(pose, s, s, 0).setColor(1f, 1f, 1f, 1f).setUv(0f, 1f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);
            vertexConsumer.addVertex(pose, s, -s, 0).setColor(1f, 1f, 1f, 1f).setUv(0f, 0f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);

            poseStack.popPose();
        }
    }

    private void update(FrameBufferBackedDynamicTexture text) {
        Minecraft mc = Minecraft.getInstance();

        RenderTarget renderTarget = text.getFrameBuffer();
        renderTarget.bindWrite(true);

        RenderSystem.clear(16640, ON_OSX);
        FogRenderer.setupNoFog();
        RenderSystem.enableCull();
        CameraState.NO_OUTLINE = true;

        if (mc.isGameLoadFinished() && mc.level != null) {
            //render level
            renderLevel(mc.getTimer(), mc, mc.gameRenderer);
        }

        //reset
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        CameraState.TARGET = null;
        CameraState.NO_OUTLINE = false;
    }

    //same as game renderer render level
    public void renderLevel(DeltaTracker deltaTracker,  Minecraft mc, GameRenderer gr) {
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);

        boolean blockOutlines = false;
        Camera camera = gr.getMainCamera();
        double fovNumber = 70;
        Matrix4f projMatrix = gr.getProjectionMatrix(fovNumber);

        Quaternionf cameraRotation = camera.rotation().conjugate(new Quaternionf());
        Matrix4f cameraMatrix = (new Matrix4f()).rotation(cameraRotation);
        mc.levelRenderer.prepareCullFrustum(camera.getPosition(), cameraMatrix, projMatrix);
        mc.levelRenderer.renderLevel(deltaTracker, true, camera, gr,
                       gr.lightTexture(), cameraMatrix, projMatrix);

        mc.getProfiler().popPush("neoforge_render_last");
//        ClientHooks.dispatchRenderStage(Stage.AFTER_LEVEL, mc.levelRenderer, (PoseStack)null, matrix4f1, matrix4f, mc.levelRenderer.getTicks(), camera, mc.levelRenderer.getFrustum());

        mc.getProfiler().pop();
    }

    //level renderer render level

/*
    public void renderLevel(Minecraft mc,
            DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix) {

        LevelRenderer lr = mc.levelRenderer;
        ClientLevel level = mc.level;
        BlockEntityRenderDispatcher beRenderer = mc.getBlockEntityRenderDispatcher();
        EntityRenderDispatcher entityRenderer = mc.getEntityRenderDispatcher();
        ProfilerFiller profilerfiller = level.getProfiler();
        TickRateManager tickratemanager = level.tickRateManager();



        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);

        RenderSystem.setShaderGameTime(level.getGameTime(), partialTicks);

        beRenderer.prepare(level, camera, mc.hitResult);
        entityRenderer.prepare(level, camera, mc.crosshairPickEntity);

        profilerfiller.popPush("light_update_queue");
        level.pollLightUpdates();
        profilerfiller.popPush("light_updates");

        level.getChunkSource().getLightEngine().runLightUpdates();
        Vec3 cameraPos = camera.getPosition();
        double cameraX = cameraPos.x();
        double cameraY = cameraPos.y();
        double cameraZ = cameraPos.z();
        profilerfiller.popPush("culling");

        boolean flag = lr.capturedFrustum != null;

        Frustum frustum;
        if (flag) {
            frustum = this.capturedFrustum;
            frustum.prepare(this.frustumPos.x, this.frustumPos.y, this.frustumPos.z);
        } else {
            frustum = this.cullingFrustum;
        }

        mc.getProfiler().popPush("captureFrustum");
        if (lr.captureFrustum) {
            lr.captureFrustum(frustumMatrix, projectionMatrix, cameraPos.x, cameraPos.y, cameraPos.z, flag ? new Frustum(frustumMatrix, projectionMatrix) : frustum);
            lr.captureFrustum = false;
        }

        profilerfiller.popPush("clear");
        FogRenderer.setupColor(camera, partialTicks, level, mc.options.getEffectiveRenderDistance(), gameRenderer.getDarkenWorldAmount(partialTicks));
        FogRenderer.levelFogColor();
        RenderSystem.clear(16640, Minecraft.ON_OSX);

        float renderDistance = gameRenderer.getRenderDistance();
        boolean isFoggyHere = mc.level.effects().isFoggyAt(Mth.floor(cameraX), Mth.floor(cameraY)) || mc.gui.getBossOverlay().shouldCreateWorldFog();

        FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_SKY, renderDistance, isFoggyHere, partialTicks);

        profilerfiller.popPush("sky");
        RenderSystem.setShader(GameRenderer::getPositionShader);

        lr.renderSky(frustumMatrix, projectionMatrix, partialTicks, camera, isFoggyHere, () -> {
            FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_SKY, renderDistance, isFoggyHere, partialTicks);
        });
        //ClientHooks.dispatchRenderStage(Stage.AFTER_SKY, this, (PoseStack)null, frustumMatrix, projectionMatrix, this.ticks, camera, frustum);
        profilerfiller.popPush("fog");
        FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_TERRAIN, Math.max(renderDistance, 32.0F), isFoggyHere, partialTicks);
        profilerfiller.popPush("terrain_setup");
        lr.setupRender(camera, frustum, flag, mc.player.isSpectator());
        profilerfiller.popPush("compile_sections");
        lr.compileSections(camera);
        profilerfiller.popPush("terrain");
        lr.renderSectionLayer(RenderType.solid(), cameraX, cameraY, cameraZ, frustumMatrix, projectionMatrix);
        mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).setBlurMipmap(false, mc.options.mipmapLevels().get() > 0);
        lr.renderSectionLayer(RenderType.cutoutMipped(), cameraX, cameraY, cameraZ, frustumMatrix, projectionMatrix);
        mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).restoreLastBlurMipmap();
        lr.renderSectionLayer(RenderType.cutout(), cameraX, cameraY, cameraZ, frustumMatrix, projectionMatrix);
        if (level.effects().constantAmbientLight()) {
            Lighting.setupNetherLevel();
        } else {
            Lighting.setupLevel();
        }

        profilerfiller.popPush("entities");
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

        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.mul(frustumMatrix);
        RenderSystem.applyModelViewMatrix();
        boolean flag2 = false;
        PoseStack posestack = new PoseStack();
        MultiBufferSource.BufferSource multibuffersource$buffersource = lr.renderBuffers.bufferSource();
        Iterator<Entity> var26 = level.entitiesForRendering().iterator();

        while(true) {
            Entity entity;
            do {
                BlockPos blockpos2;
                do {
                    do {
                        do {
                            if (!var26.hasNext()) {
                                multibuffersource$buffersource.endLastBatch();
                                lr.checkPoseStack(posestack);
                                multibuffersource$buffersource.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
                                multibuffersource$buffersource.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
                                multibuffersource$buffersource.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
                                multibuffersource$buffersource.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));
                               // ClientHooks.dispatchRenderStage(Stage.AFTER_ENTITIES, this, posestack, frustumMatrix, projectionMatrix, this.ticks, camera, frustum);
                                profilerfiller.popPush("blockentities");
                                ObjectListIterator var41 = this.visibleSections.iterator();

                                while(true) {
                                    List list;
                                    do {
                                        if (!var41.hasNext()) {
                                            synchronized(lr.globalBlockEntities) {

                                                for (BlockEntity blockentity : lr.globalBlockEntities) {
                                                    if (ClientHooks.isBlockEntityRendererVisible(this.blockEntityRenderDispatcher, blockentity, frustum)) {
                                                        BlockPos blockpos3 = blockentity.getBlockPos();
                                                        posestack.pushPose();
                                                        posestack.translate((double) blockpos3.getX() - cameraX, (double) blockpos3.getY() - cameraY, (double) blockpos3.getZ() - cameraZ);
                                                        if (this.shouldShowEntityOutlines() && blockentity.hasCustomOutlineRendering(mc.player)) {
                                                            flag2 = true;
                                                        }

                                                        this.blockEntityRenderDispatcher.render(blockentity, partialTicks, posestack, multibuffersource$buffersource);
                                                        posestack.popPose();
                                                    }
                                                }
                                            }

                                            lr.checkPoseStack(posestack);
                                            multibuffersource$buffersource.endBatch(RenderType.solid());
                                            multibuffersource$buffersource.endBatch(RenderType.endPortal());
                                            multibuffersource$buffersource.endBatch(RenderType.endGateway());
                                            multibuffersource$buffersource.endBatch(Sheets.solidBlockSheet());
                                            multibuffersource$buffersource.endBatch(Sheets.cutoutBlockSheet());
                                            multibuffersource$buffersource.endBatch(Sheets.bedSheet());
                                            multibuffersource$buffersource.endBatch(Sheets.shulkerBoxSheet());
                                            multibuffersource$buffersource.endBatch(Sheets.signSheet());
                                            multibuffersource$buffersource.endBatch(Sheets.hangingSignSheet());
                                            multibuffersource$buffersource.endBatch(Sheets.chestSheet());
                                            lr.renderBuffers.outlineBufferSource().endOutlineBatch();
                                            if (lr.outlineEffectRequested) {
                                                flag2 |= lr.shouldShowEntityOutlines();
                                                lr.outlineEffectRequested = false;
                                            }

                                            if (flag2) {
                                                lr.entityEffect.process(deltaTracker.getGameTimeDeltaTicks());
                                                mc.getMainRenderTarget().bindWrite(false);
                                            }

                                            ClientHooks.dispatchRenderStage(Stage.AFTER_BLOCK_ENTITIES, this, posestack, frustumMatrix, projectionMatrix, this.ticks, camera, frustum);
                                            profilerfiller.popPush("destroyProgress");
                                            ObjectIterator var42 = this.destructionProgress.long2ObjectEntrySet().iterator();

                                            while(var42.hasNext()) {
                                                Long2ObjectMap.Entry<SortedSet<BlockDestructionProgress>> entry = (Long2ObjectMap.Entry)var42.next();
                                                blockpos2 = BlockPos.of(entry.getLongKey());
                                                double d3 = (double)blockpos2.getX() - cameraX;
                                                double d4 = (double)blockpos2.getY() - cameraY;
                                                double d5 = (double)blockpos2.getZ() - cameraZ;
                                                if (!(d3 * d3 + d4 * d4 + d5 * d5 > 1024.0)) {
                                                    SortedSet<BlockDestructionProgress> sortedset1 = (SortedSet)entry.getValue();
                                                    if (sortedset1 != null && !sortedset1.isEmpty()) {
                                                        int k = ((BlockDestructionProgress)sortedset1.last()).getProgress();
                                                        posestack.pushPose();
                                                        posestack.translate((double)blockpos2.getX() - cameraX, (double)blockpos2.getY() - cameraY, (double)blockpos2.getZ() - cameraZ);
                                                        PoseStack.Pose posestack$pose1 = posestack.last();
                                                        VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(this.renderBuffers.crumblingBufferSource().getBuffer((RenderType)ModelBakery.DESTROY_TYPES.get(k)), posestack$pose1, 1.0F);
                                                        ModelData modelData = level.getModelData(blockpos2);
                                                        mc.getBlockRenderer().renderBreakingTexture(level.getBlockState(blockpos2), blockpos2, level, posestack, vertexconsumer1, modelData);
                                                        posestack.popPose();
                                                    }
                                                }
                                            }

                                            this.checkPoseStack(posestack);
                                            HitResult hitresult = mc.hitResult;
                                            if (renderBlockOutline && hitresult != null && hitresult.getType() == HitResult.Type.BLOCK) {
                                                profilerfiller.popPush("outline");
                                                BlockPos blockpos1 = ((BlockHitResult)hitresult).getBlockPos();
                                                BlockState blockstate = level.getBlockState(blockpos1);
                                                if (!ClientHooks.onDrawHighlight(this, camera, hitresult, deltaTracker, posestack, multibuffersource$buffersource) && !blockstate.isAir() && level.getWorldBorder().isWithinBounds(blockpos1)) {
                                                    VertexConsumer vertexconsumer2 = multibuffersource$buffersource.getBuffer(RenderType.lines());
                                                    this.renderHitOutline(posestack, vertexconsumer2, camera.getEntity(), cameraX, cameraY, cameraZ, blockpos1, blockstate);
                                                }
                                            } else if (hitresult != null && hitresult.getType() == HitResult.Type.ENTITY) {
                                                ClientHooks.onDrawHighlight(this, camera, hitresult, deltaTracker, posestack, multibuffersource$buffersource);
                                            }

                                            mc.debugRenderer.render(posestack, multibuffersource$buffersource, cameraX, cameraY, cameraZ);
                                            multibuffersource$buffersource.endLastBatch();
                                            multibuffersource$buffersource.endBatch(Sheets.translucentCullBlockSheet());
                                            multibuffersource$buffersource.endBatch(Sheets.bannerSheet());
                                            multibuffersource$buffersource.endBatch(Sheets.shieldSheet());
                                            multibuffersource$buffersource.endBatch(RenderType.armorEntityGlint());
                                            multibuffersource$buffersource.endBatch(RenderType.glint());
                                            multibuffersource$buffersource.endBatch(RenderType.glintTranslucent());
                                            multibuffersource$buffersource.endBatch(RenderType.entityGlint());
                                            multibuffersource$buffersource.endBatch(RenderType.entityGlintDirect());
                                            multibuffersource$buffersource.endBatch(RenderType.waterMask());
                                            this.renderBuffers.crumblingBufferSource().endBatch();
                                            if (this.transparencyChain != null) {
                                                multibuffersource$buffersource.endBatch(RenderType.lines());
                                                multibuffersource$buffersource.endBatch();
                                                this.translucentTarget.clear(Minecraft.ON_OSX);
                                                this.translucentTarget.copyDepthFrom(mc.getMainRenderTarget());
                                                profilerfiller.popPush("translucent");
                                                this.renderSectionLayer(RenderType.translucent(), cameraX, cameraY, cameraZ, frustumMatrix, projectionMatrix);
                                                profilerfiller.popPush("string");
                                                this.renderSectionLayer(RenderType.tripwire(), cameraX, cameraY, cameraZ, frustumMatrix, projectionMatrix);
                                                this.particlesTarget.clear(Minecraft.ON_OSX);
                                                this.particlesTarget.copyDepthFrom(mc.getMainRenderTarget());
                                                RenderStateShard.PARTICLES_TARGET.setupRenderState();
                                                profilerfiller.popPush("particles");
                                                mc.particleEngine.render(lightTexture, camera, partialTicks, frustum, (type) -> {
                                                    return true;
                                                });
                                                ClientHooks.dispatchRenderStage(Stage.AFTER_PARTICLES, this, posestack, frustumMatrix, projectionMatrix, this.ticks, camera, frustum);
                                                RenderStateShard.PARTICLES_TARGET.clearRenderState();
                                            } else {
                                                profilerfiller.popPush("solid_particles");
                                                mc.particleEngine.render(lightTexture, camera, partialTicks, frustum, (type) -> {
                                                    return !type.isTranslucent();
                                                });
                                                profilerfiller.popPush("translucent");
                                                if (this.translucentTarget != null) {
                                                    this.translucentTarget.clear(Minecraft.ON_OSX);
                                                }

                                                this.renderSectionLayer(RenderType.translucent(), cameraX, cameraY, cameraZ, frustumMatrix, projectionMatrix);
                                                multibuffersource$buffersource.endBatch(RenderType.lines());
                                                multibuffersource$buffersource.endBatch();
                                                profilerfiller.popPush("string");
                                                this.renderSectionLayer(RenderType.tripwire(), cameraX, cameraY, cameraZ, frustumMatrix, projectionMatrix);
                                                profilerfiller.popPush("particles");
                                                mc.particleEngine.render(lightTexture, camera, partialTicks, frustum, (type) -> {
                                                    return type.isTranslucent();
                                                });
                                                ClientHooks.dispatchRenderStage(Stage.AFTER_PARTICLES, this, posestack, frustumMatrix, projectionMatrix, this.ticks, camera, frustum);
                                            }

                                            if (mc.options.getCloudsType() != CloudStatus.OFF) {
                                                if (this.transparencyChain != null) {
                                                    this.cloudsTarget.clear(Minecraft.ON_OSX);
                                                }

                                                profilerfiller.popPush("clouds");
                                                this.renderClouds(posestack, frustumMatrix, projectionMatrix, partialTicks, cameraX, cameraY, cameraZ);
                                            }

                                            if (this.transparencyChain != null) {
                                                RenderStateShard.WEATHER_TARGET.setupRenderState();
                                                profilerfiller.popPush("weather");
                                                this.renderSnowAndRain(lightTexture, partialTicks, cameraX, cameraY, cameraZ);
                                                ClientHooks.dispatchRenderStage(Stage.AFTER_WEATHER, this, posestack, frustumMatrix, projectionMatrix, this.ticks, camera, frustum);
                                                this.renderWorldBorder(camera);
                                                RenderStateShard.WEATHER_TARGET.clearRenderState();
                                                this.transparencyChain.process(deltaTracker.getGameTimeDeltaTicks());
                                                mc.getMainRenderTarget().bindWrite(false);
                                            } else {
                                                RenderSystem.depthMask(false);
                                                profilerfiller.popPush("weather");
                                                this.renderSnowAndRain(lightTexture, partialTicks, cameraX, cameraY, cameraZ);
                                                ClientHooks.dispatchRenderStage(Stage.AFTER_WEATHER, this, posestack, frustumMatrix, projectionMatrix, this.ticks, camera, frustum);
                                                this.renderWorldBorder(camera);
                                                RenderSystem.depthMask(true);
                                            }

                                            this.renderDebug(posestack, multibuffersource$buffersource, camera);
                                            multibuffersource$buffersource.endLastBatch();
                                            matrix4fstack.popMatrix();
                                            RenderSystem.applyModelViewMatrix();
                                            RenderSystem.depthMask(true);
                                            RenderSystem.disableBlend();
                                            FogRenderer.setupNoFog();
                                            return;
                                        }

                                        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = (SectionRenderDispatcher.RenderSection)var41.next();
                                        list = sectionrenderdispatcher$rendersection.getCompiled().getRenderableBlockEntities();
                                    } while(list.isEmpty());

                                    Iterator var51 = list.iterator();

                                    while(var51.hasNext()) {
                                        BlockEntity blockentity1 = (BlockEntity)var51.next();
                                        if (ClientHooks.isBlockEntityRendererVisible(this.blockEntityRenderDispatcher, blockentity1, frustum)) {
                                            BlockPos blockpos4 = blockentity1.getBlockPos();
                                            MultiBufferSource multibuffersource1 = multibuffersource$buffersource;
                                            posestack.pushPose();
                                            posestack.translate((double)blockpos4.getX() - cameraX, (double)blockpos4.getY() - cameraY, (double)blockpos4.getZ() - cameraZ);
                                            SortedSet<BlockDestructionProgress> sortedset = (SortedSet)this.destructionProgress.get(blockpos4.asLong());
                                            if (sortedset != null && !sortedset.isEmpty()) {
                                                int j = ((BlockDestructionProgress)sortedset.last()).getProgress();
                                                if (j >= 0) {
                                                    PoseStack.Pose posestack$pose = posestack.last();
                                                    VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(this.renderBuffers.crumblingBufferSource().getBuffer((RenderType)ModelBakery.DESTROY_TYPES.get(j)), posestack$pose, 1.0F);
                                                    multibuffersource1 = (arg) -> {
                                                        VertexConsumer vertexconsumer3 = multibuffersource$buffersource.getBuffer(arg);
                                                        return arg.affectsCrumbling() ? VertexMultiConsumer.create(vertexconsumer, vertexconsumer3) : vertexconsumer3;
                                                    };
                                                }
                                            }

                                            if (this.shouldShowEntityOutlines() && blockentity1.hasCustomOutlineRendering(mc.player)) {
                                                flag2 = true;
                                            }

                                            this.blockEntityRenderDispatcher.render(blockentity1, partialTicks, posestack, (MultiBufferSource)multibuffersource1);
                                            posestack.popPose();
                                        }
                                    }
                                }
                            }

                            entity = (Entity)var26.next();
                        } while(!this.entityRenderDispatcher.shouldRender(entity, frustum, cameraX, cameraY, cameraZ) && !entity.hasIndirectPassenger(mc.player));

                        blockpos2 = entity.blockPosition();
                    } while(!level.isOutsideBuildHeight(blockpos2.getY()) && !this.isSectionCompiled(blockpos2));
                } while(entity == camera.getEntity() && !camera.isDetached() && (!(camera.getEntity() instanceof LivingEntity) || !((LivingEntity)camera.getEntity()).isSleeping()));
            } while(entity instanceof LocalPlayer && camera.getEntity() != entity && (entity != mc.player || mc.player.isSpectator()));

            ++this.renderedEntities;
            if (entity.tickCount == 0) {
                entity.xOld = entity.getX();
                entity.yOld = entity.getY();
                entity.zOld = entity.getZ();
            }

            Object multibuffersource;
            if (this.shouldShowEntityOutlines() && mc.shouldEntityAppearGlowing(entity)) {
                flag2 = true;
                OutlineBufferSource outlinebuffersource = this.renderBuffers.outlineBufferSource();
                multibuffersource = outlinebuffersource;
                int i = entity.getTeamColor();
                outlinebuffersource.setColor(FastColor.ARGB32.red(i), FastColor.ARGB32.green(i), FastColor.ARGB32.blue(i), 255);
            } else {
                if (this.shouldShowEntityOutlines() && entity.hasCustomOutlineRendering(mc.player)) {
                    flag2 = true;
                }

                multibuffersource = multibuffersource$buffersource;
            }

            float f2 = deltaTracker.getGameTimeDeltaPartialTick(!tickratemanager.isEntityFrozen(entity));
            this.renderEntity(entity, cameraX, cameraY, cameraZ, f2, posestack, (MultiBufferSource)multibuffersource);
        }
    }

*/

}
