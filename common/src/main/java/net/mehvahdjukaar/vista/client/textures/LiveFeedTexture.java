package net.mehvahdjukaar.vista.client.textures;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.moonlight.api.client.PostShadersHelper;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.RenderableDynamicTexture;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.CrtOverlay;
import net.mehvahdjukaar.vista.client.SlidingWindowCounter;
import net.mehvahdjukaar.vista.client.renderer.LevelRendererCameraState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class LiveFeedTexture extends RenderableDynamicTexture {

    private static final PostShadersHelper.Group POSTERIZE_GROUP = new PostShadersHelper.Group(
            VistaMod.res("1"), 0
    );

    private static final PostShadersHelper.Group LENS_EFFECTS_GROUP = new PostShadersHelper.Group(
            VistaMod.res("0"), 1
    );

    private static final ResourceLocation CAMERA_POST_PIPELINE = VistaMod.res("shaders/post/posterize_camera.json");

    private final UUID associatedUUID;

    private final LevelRendererCameraState rendererState = new LevelRendererCameraState();
    @Nullable
    private ResourceLocation extraPostChainID;
    private PostChain postChain;
    private boolean disconnected = false;
    private boolean showsTime = false;

    public boolean showsTime() {
        return showsTime;
    }

    public void setShowsTime(boolean showsTime) {
        this.showsTime = showsTime;
    }

    private enum RefType {
        LIVE, PAUSED
    }

    private final SlidingWindowCounter<RefType> references =
            new SlidingWindowCounter<>(Duration.ofSeconds(3), Duration.ofSeconds(1));

    public LiveFeedTexture(ResourceLocation resourceLocation, int width, int height,
                           @NotNull Consumer<LiveFeedTexture> textureDrawingFunction,
                           UUID id) {
        super(resourceLocation, width, height, textureDrawingFunction);
        this.associatedUUID = id;
        this.extraPostChainID = null;
        //can cause flicker?
        //this too?
        this.recomputePostChain();
    }

    public void markReferenced(boolean paused) {
        references.record(paused ? RefType.PAUSED : RefType.LIVE);
    }

    public CrtOverlay getOverlay(boolean wantPaused) {
        if (isDisconnected()) return CrtOverlay.DISCONNECT;
        int paused = references.getCount(RefType.LIVE);
        int live = references.getCount(RefType.PAUSED);
        if (live == 0 && paused != 0) {
            return CrtOverlay.PAUSE;
        }
        if (paused == 0 || !wantPaused) {
            return CrtOverlay.NONE;
        }
        return CrtOverlay.PAUSE_PLAY;
    }

    public LevelRendererCameraState getRendererState() {
        return rendererState;
    }

    public UUID getAssociatedUUID() {
        return associatedUUID;
    }

    public void applyPostChain() {
        if (isClosed()) {
            VistaMod.LOGGER.error("apply post on closed");
            return;
        }
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
        if (isClosed()) {
            VistaMod.LOGGER.error("recompute post on closed");
            return;
        }

        if (postChain != null) {
            postChain.close();
            postChain = null;
        }
        Minecraft mc = Minecraft.getInstance();
        GameRenderer gameRenderer = mc.gameRenderer;
        try {
            RenderTarget canvasTarget = this.getRenderTarget();

            postChain = PostShadersHelper.refreshComposite(postChain, CAMERA_POST_PIPELINE, POSTERIZE_GROUP, canvasTarget);

            if (extraPostChainID != null) {
                postChain = PostShadersHelper.refreshComposite(postChain, extraPostChainID, LENS_EFFECTS_GROUP, canvasTarget);
            }
        } catch (IOException ioexception) {
            VistaMod.LOGGER.warn("Failed to load shader: {}", extraPostChainID, ioexception);
            gameRenderer.effectActive = false;
        } catch (JsonSyntaxException jsonsyntaxexception) {
            VistaMod.LOGGER.warn("Failed to parse shader: {}", extraPostChainID, jsonsyntaxexception);
            gameRenderer.effectActive = false;
        }
    }

    public boolean setPostChain(@Nullable ResourceLocation newPostChainId) {
        ResourceLocation currentShader = this.extraPostChainID;
        if (Objects.equals(currentShader, newPostChainId)) {
            return false;
        }
        this.extraPostChainID = newPostChainId;

        RenderSystem.recordRenderCall(this::recomputePostChain);
        return true;
    }

    @Override
    public void close() {
        super.close();
        this.closed = true;
        if (postChain != null) {
            postChain.close();
            postChain = null;
        }
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public void setDisconnected(boolean inactive) {
        this.disconnected = inactive;
    }
}
