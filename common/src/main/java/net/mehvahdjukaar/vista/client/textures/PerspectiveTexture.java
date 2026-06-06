package net.mehvahdjukaar.vista.client.textures;

import net.mehvahdjukaar.moonlight.api.client.texture_renderer.RenderableDynamicTexture;
import net.mehvahdjukaar.vista.client.renderer.LevelRendererCameraState;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Base for textures that hold a per-frame rendered view of the world: a camera feed displayed
 * on a TV, or the off-axis reflection rendered into a mirror. Subclasses own their own refresh
 * logic via {@link #refresh()}; the static {@link #REFRESH_DISPATCH} avoids the {@code this::}
 * footgun in the super constructor by routing back through a cast on the parameter.
 */
public abstract class PerspectiveTexture extends RenderableDynamicTexture {

    private static final Consumer<RenderableDynamicTexture> REFRESH_DISPATCH =
            t -> ((PerspectiveTexture) t).refresh();

    private final UUID associatedUUID;
    private final LevelRendererCameraState rendererState = new LevelRendererCameraState();

    protected PerspectiveTexture(ResourceLocation res, int width, int height, UUID id) {
        super(res, width, height, REFRESH_DISPATCH);
        this.associatedUUID = id;
    }

    protected abstract void refresh();

    /** Hook for applying a post-process shader chain to this texture. No-op by default. */
    public void applyPostChain() {
    }

    public LevelRendererCameraState getRendererState() {
        return rendererState;
    }

    public UUID getAssociatedUUID() {
        return associatedUUID;
    }
}
