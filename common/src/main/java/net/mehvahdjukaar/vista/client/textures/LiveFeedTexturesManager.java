package net.mehvahdjukaar.vista.client.textures;

import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.RenderedTexturesManager;
import net.mehvahdjukaar.moonlight.api.misc.RollingBuffer;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.AdaptiveUpdateScheduler;
import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import net.mehvahdjukaar.vista.common.BroadcastManager;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.mehvahdjukaar.vista.integration.distant_horizons.DistantHorizonsCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class LiveFeedTexturesManager {

    private static final ResourceLocation POSTERIZE_FRAGMENT_SHADER = VistaMod.res("posterize");
    private static final BiMap<UUID, ResourceLocation> LIVE_FEED_LOCATIONS = HashBiMap.create();
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
    public static ResourceLocation requestLiveFeedTexture(
            Level level, UUID location, int screenSize,
            boolean requiresUpdate, @Nullable ResourceLocation postShader) {

        ViewFinderBlockEntity tile = BroadcastManager.findLinkedViewFinder(level, location);
        if (tile == null) return null;
        ResourceLocation feedId = getOrCreateFeedId(location);
        LiveFeedTexture texture = RenderedTexturesManager.requestTexture(feedId, () ->
                new LiveFeedTexture(feedId,
                        screenSize * ClientConfigs.RESOLUTION_SCALE.get(),
                        LiveFeedTexturesManager::refreshTexture, location, POSTERIZE_FRAGMENT_SHADER));

        if (texture.setPostChain(postShader)) {
            requiresUpdate = true;
        }
        if (VistaLevelRenderer.isRenderingLiveFeed()) {
            requiresUpdate = false; //suppress recursive updates
        }
        if (!requiresUpdate) {
            texture.unMarkForUpdate();
        }
        if (texture.isInitialized()) {
            return texture.getTextureLocation();
        } else {
            SCHEDULER.get().forceUpdateNextTick(feedId);
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
    }

    public static void onRenderTickEnd() {
        SCHEDULER.get().onEndOfFrame();
    }

    private static void refreshTexture(LiveFeedTexture text) {
        Minecraft mc = Minecraft.getInstance();

        ClientLevel level = mc.level;
        if (!mc.isGameLoadFinished() || level == null) return;
        if (mc.isPaused()) return;

        ResourceLocation textureId = text.getTextureLocation();

        Runnable runTask = () -> {

            setLastUpdatedTime(textureId, level);

            UUID uuid = text.getAssociatedUUID();
            ViewFinderBlockEntity tile = BroadcastManager.findLinkedViewFinder(level, uuid);
            if (tile == null) return; //TODO: do something here


            VistaLevelRenderer.render(text, tile);

            if (VistaMod.funny()) {
                drawOverlay(text.getFrameBuffer(), VistaModClient.LL_LOGO);
            }


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

    private static void drawOverlay(RenderTarget frameBuffer, ResourceLocation overlayTexture) {
        Minecraft mc = Minecraft.getInstance();
        var oldTarget = mc.getMainRenderTarget();
        frameBuffer.bindWrite(true);

        AbstractTexture texture = mc.getTextureManager().getTexture(overlayTexture);

        RenderSystem.assertOnRenderThread();
        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._enableBlend();

        ShaderInstance shaderInstance = Objects.requireNonNull(mc.gameRenderer.blitShader, "Blit shader not loaded");
        shaderInstance.setSampler("DiffuseSampler", texture.getId());
        shaderInstance.apply();
        BufferBuilder bufferBuilder = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLIT_SCREEN);
        bufferBuilder.addVertex(0.0F, 0.0F, 0.0F);
        bufferBuilder.addVertex(1.0F, 0.0F, 0.0F);
        bufferBuilder.addVertex(1.0F, 1.0F, 0.0F);
        bufferBuilder.addVertex(0.0F, 1.0F, 0.0F);
        BufferUploader.draw(bufferBuilder.buildOrThrow());
        shaderInstance.clear();
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);
        oldTarget.bindWrite(true);
    }


}
