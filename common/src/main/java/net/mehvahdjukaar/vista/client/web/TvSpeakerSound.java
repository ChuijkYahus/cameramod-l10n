package net.mehvahdjukaar.vista.client.web;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class TvSpeakerSound extends AbstractSoundInstance {

    private final PcmSource source;
    private final double startSeconds;
    @Nullable
    private volatile PcmAudioStream stream;

    protected TvSpeakerSound(PcmSource source, Vec3 pos, double startSeconds) {
        super(VistaMod.TV_SPEAKER_SOUND.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.source = source;
        this.startSeconds = startSeconds;
        this.volume = ClientConfigs.AUDIO_VOLUME.get().floatValue();
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    public AudioStream openStream() {
        PcmAudioStream opened = new PcmAudioStream(source, startSeconds);
        this.stream = opened;
        return opened;
    }

    public boolean isLoud() {
        PcmAudioStream opened = this.stream;
        return opened != null && opened.wasLoudRecently();
    }
}
