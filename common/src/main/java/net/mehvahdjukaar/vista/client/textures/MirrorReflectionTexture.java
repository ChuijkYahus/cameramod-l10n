package net.mehvahdjukaar.vista.client.textures;

import net.mehvahdjukaar.vista.client.renderer.MirrorTextureRenderer;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Texture backing a mirror's reflection. The BE renderer stamps the current frame's mirror and
 * camera-eye position on the texture via {@link #setPending}; the end-of-frame texture refresh
 * consumes both and draws the off-axis frustum render via
 * {@link MirrorTextureRenderer#renderMirror}.
 *
 * <p>Eye position is captured at BE-render time (not at refresh time) so the camera state matches
 * the frame this update was requested for.
 */
public class MirrorReflectionTexture extends PerspectiveTexture {

    @Nullable
    private MirrorBlockEntity pendingMirror;
    @Nullable
    private Vec3 pendingEye;

    public MirrorReflectionTexture(ResourceLocation resourceLocation, int width, int height, UUID id) {
        super(resourceLocation, width, height, id);
    }

    public void setPending(MirrorBlockEntity mirror, Vec3 eye) {
        this.pendingMirror = mirror;
        this.pendingEye = eye;
    }

    @Override
    protected void refresh() {
        MirrorBlockEntity mirror = pendingMirror;
        Vec3 eye = pendingEye;
        pendingMirror = null;
        pendingEye = null;
        if (mirror == null || eye == null) return;
        MirrorTextureRenderer.renderMirror(this, mirror, eye);
    }
}
