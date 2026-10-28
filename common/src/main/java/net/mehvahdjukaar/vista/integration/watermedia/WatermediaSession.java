package net.mehvahdjukaar.vista.integration.watermedia;

import net.mehvahdjukaar.vista.client.textures.web.IWebTexture;
import net.mehvahdjukaar.vista.client.web.IMediaSession;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.watermedia.api.media.MRL;
import org.watermedia.api.media.MediaAPI;
import org.watermedia.api.media.players.MediaPlayer;

import java.net.URI;

public class WatermediaSession implements IMediaSession {

    private final MRL mrl;
    private final int targetWidth;
    private final int targetHeight;
    private volatile boolean closed;

    public WatermediaSession(URI uri, int targetWidth, int targetHeight) {
        this.mrl = MediaAPI.mrl(uri);
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
    }

    @Override
    public IWebTexture createTextureView(ResourceLocation resourceLocation) {
        return new WatermediaTexture(resourceLocation, this);
    }

    public boolean isFetching() {
        return mrl.status() == MRL.Status.FETCHING;
    }

    //render thread only
    @Nullable
    public MediaPlayer createPlayer() {
        MediaPlayer player = MediaAPI.createPlayer(mrl, () -> MediaAPI.glEngine(Thread.currentThread(), Minecraft.getInstance()), MediaAPI::alEngine);
        if (player == null) return null;
        player.maxSize(targetWidth, targetHeight);
        player.repeat(true);
        player.start();
        return player;
    }

    @Override
    public boolean shouldRefreshTexture(IWebTexture tt) {
        return false;
    }

    @Override
    public boolean isFailed() {
        return mrl.status().failed();
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
    }
}
