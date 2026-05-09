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
import java.util.concurrent.Executor;

public class WatermediaVideoTexture extends AbstractTexture implements IWebTexture {

    private final ResourceLocation textureLocation;
    private final ImageCache imageCache;
    private final VideoPlayer videoPlayer;
    private boolean wasPaused = false;

    public WatermediaVideoTexture(ResourceLocation textureLocation, ImageCache imageCache,
                                  int width, int height, Executor executor) {
        //TODO: figure out width and height
        this.videoPlayer = new VideoPlayer(new MediaPlayerFactory(), Minecraft.getInstance());
        this.imageCache = imageCache;
        this.textureLocation = textureLocation;
        this.videoPlayer.start(imageCache.uri);
        this.videoPlayer.setMuteMode(true);
        this.videoPlayer.setRepeatMode(true);
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
        if (paused != wasPaused) {
            if (paused) videoPlayer.pause();
            else videoPlayer.resume();
        }
        wasPaused = paused;

        ImageCache.Status imageStatus = imageCache.getStatus();
        return switch (imageStatus) {
            case READY -> MediaStatus.READY;
            case LOADING -> MediaStatus.LOADING;
            case WAITING -> MediaStatus.BUFFERING;
            case FAILED, FORGOTTEN -> MediaStatus.FAILED;
        };
    }

}
