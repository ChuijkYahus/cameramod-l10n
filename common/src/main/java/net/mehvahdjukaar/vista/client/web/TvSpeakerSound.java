package net.mehvahdjukaar.vista.client.web;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class TvSpeakerSound extends AbstractSoundInstance {

    private final PcmAudioTrack track;
    private final double startSeconds;
    private final double videoClockOffset;

    protected TvSpeakerSound(PcmAudioTrack track, Vec3 pos, double startSeconds, double videoClockOffset) {
        super(VistaMod.TV_SPEAKER_SOUND.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.track = track;
        this.startSeconds = startSeconds;
        this.videoClockOffset = videoClockOffset;
        this.volume = ClientConfigs.AUDIO_VOLUME.get().floatValue();
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    public double getVideoClockOffset() {
        return videoClockOffset;
    }

    public AudioStream openStream() {
        return new PcmAudioStream(track, startSeconds);
    }
}
