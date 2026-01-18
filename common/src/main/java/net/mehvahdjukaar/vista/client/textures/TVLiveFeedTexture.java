package net.mehvahdjukaar.vista.client.textures;

import net.mehvahdjukaar.moonlight.api.client.texture_renderer.TickableFrameBufferBackedDynamicTexture;
import net.minecraft.client.model.WitherBossModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

public class TVLiveFeedTexture extends TickableFrameBufferBackedDynamicTexture {

    private final UUID associatedUUID;
    @Nullable
    private ResourceLocation postShader;

    public TVLiveFeedTexture(ResourceLocation resourceLocation, int size,
                             @NotNull Consumer<TVLiveFeedTexture> textureDrawingFunction,
                             UUID id) {
        super(resourceLocation, size, (Consumer) textureDrawingFunction);
        this.associatedUUID = id;
        this.postShader = null;
    }

    public UUID getAssociatedUUID() {
        return associatedUUID;
    }

    public @Nullable ResourceLocation getPostShader() {
        return postShader;
    }

    public void setPostShader(@Nullable ResourceLocation postShader) {
        this.postShader = postShader;
    }
}
