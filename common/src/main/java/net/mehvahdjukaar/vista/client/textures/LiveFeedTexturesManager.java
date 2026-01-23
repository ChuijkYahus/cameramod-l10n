package net.mehvahdjukaar.vista.client.textures;

import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.RenderedTexturesManager;
import net.mehvahdjukaar.moonlight.api.misc.RollingBuffer;
import net.mehvahdjukaar.moonlight.core.client.DummyCamera;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.AdaptiveUpdateScheduler;
import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import net.mehvahdjukaar.vista.common.LiveFeedConnectionManager;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.mehvahdjukaar.vista.integration.distant_horizons.DistantHorizonsCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
    private static final DummyCamera DUMMY_CAMERA = new DummyCamera();

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
    public static ResourceLocation requestLiveFeedTexture(Level level, UUID location, int screenSize,
                                                          boolean requiresUpdate, @Nullable ResourceLocation postShader) {
        if (VistaLevelRenderer.getLifeFeedBeingRendered() != null) {
            requiresUpdate = false; //suppress recursive updates
        }
        ViewFinderBlockEntity tile = LiveFeedConnectionManager.findLinkedViewFinder(level, location);
        if (tile != null) {
            //  postShader = ResourceLocation.parse("shaders/post/spider.json");
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


            VistaLevelRenderer.render(text, tile, DUMMY_CAMERA);


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

}
