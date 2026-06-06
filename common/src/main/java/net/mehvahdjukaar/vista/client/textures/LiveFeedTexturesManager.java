package net.mehvahdjukaar.vista.client.textures;

import net.mehvahdjukaar.moonlight.api.client.texture_renderer.DynamicTextureRenderer;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class LiveFeedTexturesManager {

    public static class Handle {

        private final ResourceLocation textureId;
        private final UUID uuid;
        private final Vec2i screenSize;

        public Handle(UUID uuid, Vec2i screenSize) {
            this.textureId = VistaMod.res("live_feed/" + uuid + "_" + screenSize.x() + "x" + screenSize.y());
            this.uuid = uuid;
            this.screenSize = screenSize;
        }

        @Nullable
        public LiveFeedTexture getTexture(@Nullable ResourceLocation postShader, boolean requiresUpdate, boolean showsTime) {

            LiveFeedTexture texture = DynamicTextureRenderer.requestTexture(textureId, () ->
                    new LiveFeedTexture(textureId,
                            screenSize.x() * ClientConfigs.LIVE_FEED_RESOLUTION_SCALE.get(),
                            screenSize.y() * ClientConfigs.LIVE_FEED_RESOLUTION_SCALE.get(),
                            uuid));


            if (texture == null) {
                LiveFeedTexture.SCHEDULER.get().forceUpdateNextTick(textureId);
                return null;
            }

            texture.markReferenced(requiresUpdate);

            if (texture.setPostChain(postShader)) {
                requiresUpdate = true;
            }
            if (VistaLevelRenderer.isRenderingLiveFeed()) {
                requiresUpdate = false; //suppress recursive updates
            }
            texture.setShowsTime(showsTime);
            // OR-set: never clear a tick already scheduled by another render pass this frame.
            // setUpdateNextTick(false) would clobber a prior true and starve TVs touched
            // by both a main-view render and a recursive live-feed render in the same frame.
            if (requiresUpdate) {
                texture.setUpdateNextTick(true);
            }
            return texture;
        }
    }

    public static Handle createHandle(UUID uuid, Vec2i screenSize) {
        return new Handle(uuid, screenSize);
    }

    /**
     * Fetches (or allocates) the {@link MirrorReflectionTexture} for the given mirror UUID and
     * screen size, with no side effects on its pending state. Mirrors bypass
     * {@link LiveFeedTexture#SCHEDULER} — they refresh every frame they're in view (cheap
     * off-axis frustum, gated by render distance).
     */
    @Nullable
    public static MirrorReflectionTexture getMirrorTexture(UUID uuid, Vec2i screenSize) {
        ResourceLocation textureId = VistaMod.res(
                "live_feed/mirror_" + uuid + "_" + screenSize.x() + "x" + screenSize.y());
        return DynamicTextureRenderer.requestTexture(textureId, () ->
                new MirrorReflectionTexture(textureId,
                        screenSize.x() * ClientConfigs.MIRROR_RESOLUTION_SCALE.get(),
                        screenSize.y() * ClientConfigs.MIRROR_RESOLUTION_SCALE.get(),
                        uuid));
    }

    /**
     * TEXTURE_REFRESH-mode entry point: fetches the mirror's texture and stashes the BE + camera
     * eye on it so the next end-of-frame refresh tick redraws the reflection from the captured
     * vantage point.
     */
    @Nullable
    public static MirrorReflectionTexture getMirrorTexture(MirrorBlockEntity mirror, Vec2i screenSize, Vec3 eye) {
        MirrorReflectionTexture texture = getMirrorTexture(mirror.getId(), screenSize);
        if (texture == null) return null;
        texture.setPending(mirror, eye);
        texture.setUpdateNextTick(true);
        return texture;
    }

    @SuppressWarnings("ConstantConditions")
    public static void clear() {
        LiveFeedTexture.UPDATE_TIMES.clear();
        //TODO: change just invalidate our own
        DynamicTextureRenderer.clearCache();
    }

    public static void onRenderTickEnd() {
        LiveFeedTexture.onRenderTickEnd();
    }
}
