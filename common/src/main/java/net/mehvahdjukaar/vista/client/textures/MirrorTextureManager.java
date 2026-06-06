package net.mehvahdjukaar.vista.client.textures;

import net.mehvahdjukaar.moonlight.api.client.texture_renderer.DynamicTextureRenderer;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the static state for mirror reflections: the texture cache lookups and the RENDER_TICK_END
 * pending queue. The reflection rendering itself lives on {@link MirrorReflectionTexture}.
 */
public class MirrorTextureManager {

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private record Pending(MirrorBlockEntity mirror, Vec2i screenSize, Vec3 eye) {}

    /**
     * Fetches (or allocates) the {@link MirrorReflectionTexture} for the given mirror UUID and
     * screen size, with no side effects on its pending state. Mirrors bypass
     * {@link LiveFeedTexturesManager#SCHEDULER} — they refresh every frame they're in view (cheap
     * off-axis frustum, gated by render distance).
     */
    @Nullable
    public static MirrorReflectionTexture getMirrorTexture(UUID uuid, Vec2i screenSize) {
        ResourceLocation textureId = VistaMod.res(
                "mirror_" + uuid + "_" + screenSize.x() + "x" + screenSize.y());
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
        if (ClientConfigs.MIRROR_UPDATE_MODE.get() != ClientConfigs.MirrorUpdateMode.TEXTURE_REFRESH){
            // Queue into MirrorTextureManager's PENDING list; processPending flushes it from the
            // onRenderTickEnd hook.
            requestUpdate(mirror, screenSize, eye);
        }else {
            // Stashes the BE+eye on the texture and marks it for refresh — the actual reflection
            // render runs from the end-of-frame texture refresh callback, after the outer BE
            // batch is flushed.
            texture.setPending(mirror, eye);
            texture.setUpdateNextTick(true);
        }
        return texture;
    }

    /**
     * Queue a render for the RENDER_TICK_END mode. The BE renderer calls this from
     * {@code render(...)}; {@link #processPending} flushes the queue at the end of the frame.
     */
    public static void requestUpdate(MirrorBlockEntity mirror, Vec2i screenSize, Vec3 eye) {
        PENDING.put(mirror.getId(), new Pending(mirror, screenSize, eye));
    }

    public static void processPending() {
        if (PENDING.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            PENDING.clear();
            return;
        }
        // Snapshot + clear before iterating: each render triggers a nested level render that walks
        // block entities, and any other visible mirror's BE renderer will call requestUpdate(...)
        // again — mutating PENDING mid-iteration would CME. Anything re-queued during iteration
        // lands in a fresh PENDING for next frame.
        List<Pending> snapshot = new ArrayList<>(PENDING.values());
        PENDING.clear();
        for (Pending p : snapshot) {
            MirrorReflectionTexture text = getMirrorTexture(p.mirror.getId(), p.screenSize);
            if (text != null) text.renderReflection(p.mirror, p.eye);
        }
    }

    public static void clear() {
        PENDING.clear();
    }
}
