package net.mehvahdjukaar.vista.client.textures;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.TickableFrameBufferBackedDynamicTexture;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.renderer.LevelRendererCameraState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import static net.minecraft.client.Minecraft.ON_OSX;

//todo: improve moonlight class really.... merge tickable, add backbuffer and such
public class LiveFeedTexture extends TickableFrameBufferBackedDynamicTexture implements Dumpable {

    private static final ResourceLocation EMPTY_PIPELINE = VistaMod.res("shaders/post/empty.json");

    private final UUID associatedUUID;

    private final LevelRendererCameraState rendererState = LevelRendererCameraState.createNew();
    private final TextureTarget backBuffer;
    @Nullable
    private final ResourceLocation postFragment;
    @Nullable
    private ResourceLocation postChainID;
    private PostChain postChain;


    public LiveFeedTexture(ResourceLocation resourceLocation, int size,
                           @NotNull Consumer<LiveFeedTexture> textureDrawingFunction,
                           UUID id, @Nullable ResourceLocation postFragment) {
        super(resourceLocation, size, (Consumer) textureDrawingFunction);
        this.associatedUUID = id;
        this.postChainID = null;
        this.postFragment = postFragment;
        //can cause flicker?
        this.backBuffer = new TextureTarget(size, size, true, ON_OSX);
//this too?
        this.recomputePostChain();
    }

    public LevelRendererCameraState getRendererState() {
        return rendererState;
    }

    public UUID getAssociatedUUID() {
        return associatedUUID;
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

    public RenderTarget getBackBuffer() {
        return backBuffer;
    }

    private void recomputePostChain() {
        if (postChain != null) {
            postChain.close();
            postChain = null;
        }
        Minecraft mc = Minecraft.getInstance();
        GameRenderer gameRenderer = mc.gameRenderer;
        try {
            RenderTarget myTarget = this.getFrameBuffer();
            RenderTarget canvasTarget = this.getBackBuffer();
            ResourceLocation chainId = postChainID == null ? EMPTY_PIPELINE : postChainID;
            postChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), canvasTarget, chainId);
            //add extra pass
            if (postFragment != null) {
                RenderTarget swapTarget = postChain.getTempTarget("swap");
                if (swapTarget == null) {
                    postChain.addTempTarget("swap", getWidth(), getHeight());
                }
                postChain.addPass(postFragment.toString(), canvasTarget, swapTarget, false);
                //swap back
                postChain.addPass("vista:blit_flip_y", swapTarget, myTarget, false);
            }else{
                postChain.addPass("vista:blit_flip_y", canvasTarget, myTarget, false);
            }
            for (PostPass postPass : postChain.passes) {
                postPass.setOrthoMatrix(postChain.shaderOrthoMatrix);
            }
        } catch (IOException ioexception) {
            VistaMod.LOGGER.warn("Failed to load shader: {}", postChainID, ioexception);
            gameRenderer.effectActive = false;
        } catch (JsonSyntaxException jsonsyntaxexception) {
            VistaMod.LOGGER.warn("Failed to parse shader: {}", postChainID, jsonsyntaxexception);
            gameRenderer.effectActive = false;
        }
    }

    public boolean setPostChain(@Nullable ResourceLocation newPostChainId) {
        ResourceLocation currentShader = this.postChainID;
        if (Objects.equals(currentShader, newPostChainId)) {
            return false;
        }
        this.postChainID = newPostChainId;

        RenderSystem.recordRenderCall(this::recomputePostChain); //TODO: fix this making entity stuff not render for a split second
        return true;
    }

    @Override
    public void close() {
        super.close();
        if (postChain != null) {
            postChain.close();
            postChain = null;
        }
        if (RenderSystem.isOnRenderThread()) {
            backBuffer.destroyBuffers();
        } else {
            RenderSystem.recordRenderCall(backBuffer::destroyBuffers);
        }
    }

    @Override
    public void dumpContents(ResourceLocation resourceLocation, Path path) throws IOException {
        String string = resourceLocation.toDebugFileName() + ".png";
        Path path2 = path.resolve(string);
        this.download();
        this.getPixels().writeToFile(path2);
    }
}
