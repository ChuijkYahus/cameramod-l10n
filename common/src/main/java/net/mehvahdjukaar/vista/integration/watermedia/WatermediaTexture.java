package net.mehvahdjukaar.vista.integration.watermedia;

import net.mehvahdjukaar.vista.client.textures.web.IWebTexture;
import net.mehvahdjukaar.vista.client.web.MediaStatus;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.watermedia.api.media.players.MediaPlayer;

import java.io.IOException;

//a view on the session's player. many screens can share one
public class WatermediaTexture extends AbstractTexture implements IWebTexture {

    private final ResourceLocation textureLocation;
    private final WatermediaSession session;

    public WatermediaTexture(ResourceLocation textureLocation, WatermediaSession session) {
        this.session = session;
        this.textureLocation = textureLocation;
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
        //player is gone with the session, dont hand out a deleted texture name
        if (session.isClosed()) return 0;
        MediaPlayer player = session.getMediaPlayer();
        return player == null ? 0 : (int) player.texture();
    }

    @Override
    public void load(ResourceManager resourceManager) throws IOException {
    }

    @Override
    public void releaseId() {
    }

    @Override
    public void updateAudio(TVBlockEntity tv, boolean playing) {
        if (playing) {
            session.requestSpeaker(tv.getSpeakerSourcePos());
        }
    }

    @Override
    public boolean isPlayingLoudAudio() {
        return session.isSpeakerOn();
    }

    @Override
    public MediaStatus uploadFrameAtTime(int ticks, float deltaTime, boolean paused) {
        session.onFrameDrawn(paused);
        MediaPlayer player = session.getOrCreatePlayer();
        if (player == null) return session.isFailed() ? MediaStatus.FAILED : MediaStatus.LOADING;
        if (player.texture() == MediaPlayer.NO_TEXTURE && !session.isAudioOnly()) return MediaStatus.LOADING;

        return switch (player.status()) {
            case WAITING, LOADING -> MediaStatus.LOADING;
            case BUFFERING -> MediaStatus.BUFFERING;
            case PLAYING, PAUSED -> MediaStatus.READY;
            case STOPPED, ENDED -> MediaStatus.CLOSED;
            case ERROR -> MediaStatus.FAILED;
        };
    }

}
