package net.mehvahdjukaar.vista.integration.watermedia;

import net.mehvahdjukaar.vista.client.textures.web.IWebTexture;
import net.mehvahdjukaar.vista.client.web.MediaStatus;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.watermedia.api.player.videolan.VideoPlayer;

import java.io.IOException;

//a view on the session's player. many screens can share one
public class WatermediaVideoTexture extends AbstractTexture implements IWebTexture {

    private final ResourceLocation textureLocation;
    private final WatermediaSession session;
    private final VideoPlayer videoPlayer;

    public WatermediaVideoTexture(ResourceLocation textureLocation, WatermediaSession session, VideoPlayer videoPlayer) {
        this.session = session;
        this.textureLocation = textureLocation;
        this.videoPlayer = videoPlayer;
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
        return videoPlayer.texture();
    }

    @Override
    public void load(ResourceManager resourceManager) throws IOException {
    }

    @Override
    public void close() {
    }

    @Override
    public void releaseId() {
    }

    @Override
    public void updateAudio(TVBlockEntity tv, boolean playing) {
        float volume = 0;
        if (playing) {
            var options = Minecraft.getInstance().options;
            double distance = IWebTexture.distanceToCamera(tv.getScreenRect().center());
            float falloff = Mth.clamp(1 - (float) distance / SPEAKER_RANGE, 0, 1);
            volume = falloff * ClientConfigs.AUDIO_VOLUME.get().floatValue()
                    * options.getSoundSourceVolume(SoundSource.MASTER) * options.getSoundSourceVolume(SoundSource.BLOCKS);
        }
        session.requestAudio(playing, volume);
    }

    @Override
    public MediaStatus uploadFrameAtTime(int ticks, float deltaTime, boolean paused) {
        session.requestPlayback(paused);

        if (videoPlayer.isBroken()) return MediaStatus.FAILED;
        if (videoPlayer.isEnded()) return MediaStatus.CLOSED;
        if (videoPlayer.isBuffering()) return MediaStatus.BUFFERING;
        if (videoPlayer.isReady()) return MediaStatus.READY;
        return MediaStatus.LOADING;
    }

}
