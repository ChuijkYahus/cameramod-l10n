package net.mehvahdjukaar.vista.client.textures;

import net.mehvahdjukaar.moonlight.api.client.texture_renderer.DynamicTextureRenderer;
import net.mehvahdjukaar.moonlight.api.client.util.LOD;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Owns the static state for mirror reflections: the texture cache lookups and the RENDER_TICK_END
 * pending queue. The reflection rendering itself lives on {@link MirrorReflectionTexture}.
 *
 * <p>Each pending entry is keyed by chain-string (parent UUIDs joined + this mirror's UUID), so
 * the same physical mirror visible through different parent chains queues independently and gets
 * its own off-axis render in RECURSIVE mode.
 */
public class MirrorTextureManager {

    private static final Map<String, Pending> PENDING = new HashMap<>();

    private record Pending(MirrorBlockEntity mirror, Vec2i screenSize, Vec3 eye,
                           int depth, List<UUID> parentChain, int lod) {
    }

    private static int scaledResolution(int baseSize, int depth, int lod) {
        int scaled = baseSize * ClientConfigs.MIRROR_RESOLUTION_SCALE.get();
        if (depth > 0) {
            double divider = Math.pow(ClientConfigs.MIRROR_RECURSION_RES_DIVIDER.get(), depth);
            scaled = (int) (scaled / divider);
        }
        // Distance LOD: halve resolution per level (0 = full, 1 = half, 2 = quarter) so a mirror the
        // player has walked away from streams a cheaper texture without touching the render distance cap.
        scaled >>= lod;
        return Math.max(1, scaled);
    }

    /**
     * Direct-view distance LOD level (0 = full res, 1 = half, 2 = quarter), derived from the fixed
     * {@link LOD} distance bands. Two halving steps spread across the near/medium range; the mirror
     * render-distance cap ({@code MIRROR_RENDER_DISTANCE}) is unchanged, this only lowers the texture
     * resolution for mirrors that are still in range but far away.
     */
    public static int distanceLod(LOD lod) {
        if (lod.within(24)) return 0;     // <= 24 blocks: full resolution
        if (lod.within(40)) return 1;     // <= 40 blocks: half resolution
        return 2;                         //  > 40 blocks: quarter resolution
    }

    /**
     * Same as {@link #distanceLod(LOD)} but always measured from the main camera, for use from
     * inside a nested reflection render where the block-entity dispatcher camera is the reflected one.
     */
    public static int distanceLod(MirrorBlockEntity mirror) {
        return distanceLod(LOD.at(Minecraft.getInstance().gameRenderer.mainCamera, mirror.getBlockPos()));
    }

    private static String chainKey(UUID self, List<UUID> parentChain) {
        if (parentChain.isEmpty()) return self.toString();
        StringBuilder sb = new StringBuilder(parentChain.size() * 37 + 36);
        for (UUID u : parentChain) sb.append(u).append('_');
        sb.append(self);
        return sb.toString();
    }

    /**
     * Direct-view (depth 0) texture, keyed by mirror UUID + size. Returns the same instance for
     * a given (UUID, screenSize) regardless of recursion mode — depth 0 is always the "real"
     * view the player sees.
     */
    @Nullable
    public static MirrorReflectionTexture getMirrorTexture(UUID uuid, Vec2i screenSize, int lod) {
        int w = scaledResolution(screenSize.x(), 0, lod);
        int h = scaledResolution(screenSize.y(), 0, lod);
        // w/h shrink per LOD level, so each distance tier resolves to its own cached texture id —
        // moving between bands transparently switches the mirror to the lower/higher-res texture.
        ResourceLocation textureId = VistaMod.res(
                "mirror_" + uuid + "_" + w + "x" + h);
        return DynamicTextureRenderer.requestTexture(textureId, () ->
                new MirrorReflectionTexture(textureId, w, h, uuid, 0, List.of()));
    }

    /**
     * Chain-keyed texture used by RECURSIVE mode. Each (mirror, parent-chain) combination gets
     * its own off-axis render with attenuated resolution. Resolution at depth D = base /
     * res_divider^D, capped at 1.
     */
    @Nullable
    public static MirrorReflectionTexture getMirrorTextureForChain(UUID uuid, Vec2i screenSize,
                                                                    int depth, List<UUID> parentChain) {
        // Nested chains attenuate by recursion depth only; distance LOD (lod=0) doesn't apply here.
        int w = scaledResolution(screenSize.x(), depth, 0);
        int h = scaledResolution(screenSize.y(), depth, 0);
        String name = "mirror_chain_" + chainKey(uuid, parentChain) + "_" + w + "x" + h + "_d" + depth;
        ResourceLocation textureId = VistaMod.res(name);
        // List.copyOf inside the constructor freezes the chain identity at construction —
        // important because DynamicTextureRenderer caches by id, so subsequent lookups for
        // this same chain must return a texture whose parentChain matches what
        // VistaLevelRenderer expects to push onto the stack.
        final List<UUID> capturedChain = List.copyOf(parentChain);
        return DynamicTextureRenderer.requestTexture(textureId, () ->
                new MirrorReflectionTexture(textureId, w, h, uuid, depth, capturedChain));
    }

    /**
     * Direct-view scheduling entry point (depth 0). Picks between TEXTURE_REFRESH (per-texture
     * end-of-frame refresh callback) and RENDER_TICK_END (PENDING queue flushed from a top-level
     * frame hook). Returns the texture only after its first successful draw — the first frame
     * after allocation is uninitialised, so the BE renderer skips drawing to avoid a white flash.
     */
    @Nullable
    public static MirrorReflectionTexture getMirrorTexture(MirrorBlockEntity mirror, Vec2i screenSize, Vec3 eye, int lod) {
        MirrorReflectionTexture texture = getMirrorTexture(mirror.getId(), screenSize, lod);
        if (texture == null) return null;
        if (ClientConfigs.MIRROR_UPDATE_MODE.get() != ClientConfigs.MirrorUpdateMode.TEXTURE_REFRESH) {
            requestUpdate(mirror, screenSize, eye, 0, List.of(), lod);
        } else {
            texture.setPending(mirror, eye);
            texture.setUpdateNextTick(true);
        }
        return texture.hasRendered() ? texture : null;
    }

    /**
     * RECURSIVE-mode scheduling entry point. Called from the BE renderer when it's running inside
     * another mirror's reflection. Allocates / fetches the chain-keyed texture and queues a
     * render against it for next frame. Skips the TEXTURE_REFRESH fast path on purpose —
     * VistaLevelRenderer is now re-entrant but synchronous nested rendering from inside the BE
     * pass would corrupt vanilla's in-flight bufferSource, so we defer.
     */
    @Nullable
    public static MirrorReflectionTexture getMirrorTextureForChain(MirrorBlockEntity mirror, Vec2i screenSize,
                                                                    Vec3 eye, int depth, List<UUID> parentChain) {
        MirrorReflectionTexture texture = getMirrorTextureForChain(mirror.getId(), screenSize, depth, parentChain);
        if (texture == null) return null;
        // Chain textures attenuate by depth, not distance LOD, so lod is irrelevant (0) here.
        requestUpdate(mirror, screenSize, eye, depth, parentChain, 0);
        return texture.hasRendered() ? texture : null;
    }

    /**
     * Internal: enqueue a render for next-frame flush. {@link #processPending} snapshots and
     * clears before iterating, so re-queues that happen during iteration (from nested mirrors
     * the iteration triggers) land in next frame's PENDING.
     */
    private static void requestUpdate(MirrorBlockEntity mirror, Vec2i screenSize, Vec3 eye,
                                      int depth, List<UUID> parentChain, int lod) {
        String key = "d" + depth + "/" + chainKey(mirror.getId(), parentChain);
        PENDING.put(key, new Pending(mirror, screenSize, eye, depth, parentChain, lod));
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
            MirrorReflectionTexture text = p.depth == 0
                    ? getMirrorTexture(p.mirror.getId(), p.screenSize, p.lod)
                    : getMirrorTextureForChain(p.mirror.getId(), p.screenSize, p.depth, p.parentChain);
            if (text != null) text.renderReflection(p.mirror, p.eye);
        }
    }

    public static void clear() {
        PENDING.clear();
    }
}
