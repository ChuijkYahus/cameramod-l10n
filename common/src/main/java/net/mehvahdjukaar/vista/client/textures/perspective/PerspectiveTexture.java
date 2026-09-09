package net.mehvahdjukaar.vista.client.textures.perspective;

import net.mehvahdjukaar.moonlight.api.client.texture_renderer.RenderableDynamicTexture;
import net.mehvahdjukaar.vista.client.renderer.LevelRendererFrustumState;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import java.util.function.Consumer;

public abstract class PerspectiveTexture extends RenderableDynamicTexture {

    private static final Consumer<RenderableDynamicTexture> REFRESH_DISPATCH =
            t -> ((PerspectiveTexture) t).refresh();

    private final UUID associatedUUID;
    private final LevelRendererFrustumState rendererState = new LevelRendererFrustumState();

    protected PerspectiveTexture(ResourceLocation res, int width, int height, UUID id) {
        super(res, width, height, REFRESH_DISPATCH);
        this.associatedUUID = id;
    }

    protected abstract void refresh();

    public void applyPostChain() {
    }

    public LevelRendererFrustumState getRendererState() {
        return rendererState;
    }

    public UUID getAssociatedUUID() {
        return associatedUUID;
    }
}
