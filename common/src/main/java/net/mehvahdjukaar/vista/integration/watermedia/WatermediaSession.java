package net.mehvahdjukaar.vista.integration.watermedia;

import net.mehvahdjukaar.vista.client.textures.web.IWebTexture;
import net.mehvahdjukaar.vista.client.web.IMediaSession;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.SOFTSourceSpatialize;
import org.watermedia.api.media.MRL;
import org.watermedia.api.media.MediaAPI;
import org.watermedia.api.media.players.MediaPlayer;
import org.watermedia.api.util.MediaType;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WatermediaSession implements IMediaSession {

    private static final Map<URI, WatermediaSession> OPEN = new ConcurrentHashMap<>();
    private static final int UNSEEN_PAUSE_TICKS = 20;

    private final URI uri;
    private final MRL mrl;
    private int targetWidth;
    private int targetHeight;
    private volatile int users;

    //one player for every screen showing this url
    @Nullable
    private MediaPlayer mediaPlayer;
    private boolean isFailed;
    private int framesDrawn;
    private int framesPaused;
    private int ticksUnseen;

    @Nullable
    private Vec3 closestSpeakerPos;
    private double closestSpeakerDistance = Double.MAX_VALUE;
    private boolean speakerOn;

    private WatermediaSession(URI uri) {
        this.uri = uri;
        this.mrl = MediaAPI.mrl(uri);
    }

    public static synchronized WatermediaSession acquire(URI uri, int width, int height) {
        WatermediaSession session = OPEN.computeIfAbsent(uri, WatermediaSession::new);
        session.users++;
        if (width > session.targetWidth || height > session.targetHeight) {
            session.targetWidth = Math.max(width, session.targetWidth);
            session.targetHeight = Math.max(height, session.targetHeight);
            if (session.mediaPlayer != null) session.mediaPlayer.maxSize(session.targetWidth, session.targetHeight);
        }
        return session;
    }

    public static void onClientTick() {
        for (WatermediaSession session : OPEN.values()) session.applyRequests();
    }

    @Override
    public IWebTexture createTextureView(ResourceLocation resourceLocation) {
        return new WatermediaTexture(resourceLocation, this);
    }

    @Nullable
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    //render thread only
    @Nullable
    public MediaPlayer getOrCreatePlayer() {
        if (mediaPlayer != null || isFailed) return mediaPlayer;
        if (mrl.status() == MRL.Status.FETCHING) return null;

        mediaPlayer = MediaAPI.createPlayer(mrl, () -> MediaAPI.glEngine(Thread.currentThread(), Minecraft.getInstance()), MediaAPI::alEngine);
        if (mediaPlayer == null) {
            isFailed = true;
            return null;
        }
        mediaPlayer.maxSize(targetWidth, targetHeight);
        mediaPlayer.repeat(true);
        mediaPlayer.start();
        return mediaPlayer;
    }

    public void requestPlayback(boolean paused) {
        framesDrawn++;
        if (paused) framesPaused++;
    }

    public void requestSpeaker(Vec3 pos) {
        double distance = IWebTexture.distanceToCamera(pos);
        if (distance < closestSpeakerDistance) {
            closestSpeakerPos = pos;
            closestSpeakerDistance = distance;
        }
    }


    public boolean isSpeakerOn() {
        return speakerOn;
    }

    private void applyRequests() {
        ticksUnseen = framesDrawn > 0 ? 0 : ticksUnseen + 1;
        if (mediaPlayer != null) {
            //tick without frames is normal at low fps, only stop once its been gone a while
            boolean unseen = ticksUnseen > UNSEEN_PAUSE_TICKS;
            if (framesDrawn > 0 || unseen) {
                boolean paused = unseen || framesPaused == framesDrawn || Minecraft.getInstance().isPaused();
                if (mediaPlayer.paused() != paused) mediaPlayer.pause(paused);
            }
            updateSpeaker(mediaPlayer);
        }
        framesDrawn = 0;
        framesPaused = 0;
        closestSpeakerPos = null;
        closestSpeakerDistance = Double.MAX_VALUE;
    }

    private void updateSpeaker(MediaPlayer mp) {
        int source = mp.audioSource();
        speakerOn = closestSpeakerPos != null && closestSpeakerDistance <= IWebTexture.SPEAKER_RANGE;

        if (source == MediaPlayer.NO_SOURCE) return;
        if (!speakerOn) {
            mp.volume(0);
            return;
        }
        float blocks = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.BLOCKS);
        mp.volume((int) (blocks * ClientConfigs.AUDIO_VOLUME.get() * 100));

        //more nonsense state machine stuff
        AL10.alSourcefv(source, AL10.AL_POSITION, new float[]{(float) closestSpeakerPos.x, (float) closestSpeakerPos.y, (float) closestSpeakerPos.z});
        AL10.alSourcei(source, AL10.AL_DISTANCE_MODEL, AL11.AL_LINEAR_DISTANCE);
        AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, IWebTexture.SPEAKER_RANGE);
        //set al to be sure
        AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 1.0F);
        AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 0.0F);
        AL10.alSourcei(source, SOFTSourceSpatialize.AL_SOURCE_SPATIALIZE_SOFT, AL10.AL_TRUE);
    }

    @Override
    public boolean shouldRefreshTexture(IWebTexture tt) {
        return false;
    }

    @Override
    public boolean isFailed() {
        return isFailed || mrl.status().failed() || (mediaPlayer != null && mediaPlayer.error());
    }

    @Override
    public boolean isAudioOnly() {
        return mediaPlayer != null && mediaPlayer.source().type() == MediaType.AUDIO;
    }

    public boolean isClosed() {
        return users <= 0;
    }

    // last one to close frees player
    @Override
    public void close() {
        synchronized (WatermediaSession.class) {
            if (--users > 0) return;
            OPEN.remove(uri, this);
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
