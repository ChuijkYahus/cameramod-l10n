package net.mehvahdjukaar.vista.client.web;

import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class TvSpeakerSound extends AbstractSoundInstance {

    private final PcmAudioTrack track;
    private final double startSeconds;

    protected TvSpeakerSound(PcmAudioTrack track, Vec3 pos, double startSeconds) {
        super(VistaMod.TV_SPEAKER_SOUND.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.track = track;
        this.startSeconds = startSeconds;
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    public AudioStream openStream() {
        return new PcmAudioStream(track, startSeconds);
    }
}
