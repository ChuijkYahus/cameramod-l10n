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

    private static final ResourceLocation EMPTY_PIPELINE = VistaMod.res("shaders/post/empty.json");

    private final UUID associatedUUID;

    @Nullable
    private final ResourceLocation postFragment;
    @Nullable
    private ResourceLocation postChainID;
    private PostChain postChain;

    public TVLiveFeedTexture(ResourceLocation resourceLocation, int size,
                             @NotNull Consumer<TVLiveFeedTexture> textureDrawingFunction,
                             UUID id, @Nullable ResourceLocation postFragment) {
        super(resourceLocation, size, (Consumer) textureDrawingFunction);
        this.associatedUUID = id;
        this.postChainID = null;
        this.postFragment = postFragment;
        this.recomputePostChain();
    }

    public UUID getAssociatedUUID() {
        return associatedUUID;
    }

    public @Nullable ResourceLocation getPostShader() {
        return postChainID;
    }

    public void applyPostChain() {
        GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
        if (postChain != null) {
            gameRenderer.effectActive = true;
            gameRenderer.postEffect = this.postChain;
        } else {
            gameRenderer.postEffect = null;
            gameRenderer.effectActive = false;
        }
    }

    private void recomputePostChain() {
        Minecraft mc = Minecraft.getInstance();
        GameRenderer gameRenderer = mc.gameRenderer;
        try {
            RenderTarget myTarget = this.getFrameBuffer();
            ResourceLocation chainId = postChainID == null ? EMPTY_PIPELINE : postChainID;
            postChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), myTarget, chainId);

            //add extra pass
            if (postFragment != null) {
                RenderTarget swapTarget = postChain.getTempTarget("swap");
                if (swapTarget == null) {
                    postChain.addTempTarget("swap", getWidth(), getHeight());
                }
                postChain.addPass("vista:posterize", myTarget, swapTarget, false);
                //swap back
                postChain.addPass("blit", swapTarget, myTarget, false);
            }

            this.postChain.resize(getWidth(), getHeight()); //dumb buts needed to initialize stuff
        } catch (IOException ioexception) {
            VistaMod.LOGGER.warn("Failed to load shader: {}", postChainID, ioexception);
            gameRenderer.effectActive = false;
        } catch (JsonSyntaxException jsonsyntaxexception) {
            VistaMod.LOGGER.warn("Failed to parse shader: {}", postChainID, jsonsyntaxexception);
            gameRenderer.effectActive = false;
        }
    }

    public void setPostChain(@Nullable ResourceLocation postChainId) {
        this.postChainID = postChainId;
        if (postChain != null) {
            postChain.close();
            postChain = null;
        }
        recomputePostChain();
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
