package net.mehvahdjukaar.vista.integration.watermedia;

import net.mehvahdjukaar.vista.client.textures.IWebTexture;
import net.mehvahdjukaar.vista.client.web.MediaStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.watermedia.api.image.ImageCache;
import org.watermedia.api.player.videolan.VideoPlayer;
import org.watermedia.videolan4j.factory.MediaPlayerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executor;

public class WatermediaVideoTexture extends AbstractTexture implements IWebTexture {

    private final ResourceLocation textureLocation;
    private final WatermediaSession session;
    private final VideoPlayer videoPlayer;
    private boolean wasPaused = false;

    public WatermediaVideoTexture(ResourceLocation textureLocation, WatermediaSession session, URI uri,
                                  int width, int height, Executor executor) {
        //TODO: figure out width and height
        this.videoPlayer = new VideoPlayer(new MediaPlayerFactory(), Minecraft.getInstance());
        this.session = session;
        this.textureLocation = textureLocation;
        this.videoPlayer.start(uri);
        this.videoPlayer.mute();
        this.videoPlayer.setRepeatMode(true);
        this.videoPlayer.setVolume(0);
    }

    @Override
    public WatermediaSession getSession() {
        return session;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return textureLocation;
    }

    @Override
    public int getId() {
        return videoPlayer.texture();
    }

    @Override
    public void load(ResourceManager resourceManager) throws IOException {

    }

    @Override
    public void close() {
        super.close();
        videoPlayer.release();
    }

    @Override
    public MediaStatus uploadFrameAtTime(int ticks, float deltaTime, boolean paused) {
        videoPlayer.mute();
        if (paused != this.wasPaused) {
            if (paused) videoPlayer.pause();
            else videoPlayer.resume();
        }
        this.wasPaused = paused;

        if (videoPlayer.isBroken() || !videoPlayer.isValid()) {
            return MediaStatus.FAILED;
        }
        if (videoPlayer.isEnded()) {
            return MediaStatus.CLOSED;
        }
        if (videoPlayer.isBuffering() || videoPlayer.isWaiting()) {
            return MediaStatus.BUFFERING;
        }

        if (videoPlayer.isReady()) return MediaStatus.READY;

        if (videoPlayer.isLoading()) {
            return MediaStatus.LOADING;
        }

        return MediaStatus.READY;
    }

}
