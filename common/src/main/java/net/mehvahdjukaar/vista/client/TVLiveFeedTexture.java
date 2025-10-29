package net.mehvahdjukaar.vista.client;

import net.mehvahdjukaar.moonlight.api.client.texture_renderer.FrameBufferBackedDynamicTexture;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.TickableFrameBufferBackedDynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;

public class TVLiveFeedTexture extends TickableFrameBufferBackedDynamicTexture {

    public final UUID associatedUUID;

    public TVLiveFeedTexture(ResourceLocation resourceLocation, int size,
                             @NotNull Consumer<TVLiveFeedTexture> textureDrawingFunction,
                             UUID id) {
        super(resourceLocation, size, (Consumer) textureDrawingFunction);

        this.associatedUUID = id;
    }


}
