package net.mehvahdjukaar.vista.client.textures;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.TickableFrameBufferBackedDynamicTexture;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

public class TVLiveFeedTexture extends TickableFrameBufferBackedDynamicTexture {

    private final UUID associatedUUID;
    @Nullable
    private ResourceLocation postShader;

    private PostChain postChain;

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

    public void applyPostEffectInplace() {
        Minecraft mc = Minecraft.getInstance();
        GameRenderer gameRenderer = mc.gameRenderer;
        if (postShader == null) {
            //remove without closing
            gameRenderer.postEffect = null;
            gameRenderer.effectActive = false;
            return;
        }
        if (postChain == null) {
            try {
                //no resize
                RenderTarget myTarget = this.getFrameBuffer();
                this.postChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), myTarget, postShader);
                this.postChain.resize(getWidth(), getHeight()); //dumb buts needed to initialize stuff
                //cache post chain
                gameRenderer.postEffect = this.postChain;
                gameRenderer.effectActive = true;
            } catch (IOException ioexception) {
                VistaMod.LOGGER.warn("Failed to load shader: {}", postShader, ioexception);
                gameRenderer.effectActive = false;
            } catch (JsonSyntaxException jsonsyntaxexception) {
                VistaMod.LOGGER.warn("Failed to parse shader: {}", postShader, jsonsyntaxexception);
                gameRenderer.effectActive = false;
            }
        } else {
            gameRenderer.effectActive = true;
            gameRenderer.postEffect = this.postChain; //use cached. brr
        }
    }

    public void setPostShader(@Nullable ResourceLocation postShader) {
        this.postShader = postShader;
        if (postChain != null) {
            postChain.close();
            postChain = null;
        }
    }

    @Override
    public void close() {
        super.close();
        if (postChain != null) {
            postChain.close();
            postChain = null;
        }
    }
}
