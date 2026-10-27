package net.mehvahdjukaar.vista.client.web.audio;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class TvSpeakerSoundInstance extends AbstractTickableSoundInstance implements AudioStream {

    //sort of arbitrary
    private static final double LOUD_RMS = 500;
    //engine reads roughly a second at a time so this has to outlast one read
    private static final long LOUD_HOLD_MILLIS = 3000;

    private final FFmpegAudioTrack track;
    private long cursor;
    private volatile long lastLoudMillis = -LOUD_HOLD_MILLIS;

    protected TvSpeakerSoundInstance(FFmpegAudioTrack track, Vec3 pos, double startSeconds) {
        super(VistaMod.TV_SPEAKER_SOUND.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.track = track;
        cursor = track.secondsToBytes(startSeconds);
        this.volume = ClientConfigs.AUDIO_VOLUME.get().floatValue();
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
    }

    public void moveTo(Vec3 pos) {
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
    }

    @Override
    public void tick() {
        this.volume = ClientConfigs.AUDIO_VOLUME.get().floatValue();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    public boolean isLoud() {
        return Util.getMillis() - lastLoudMillis < LOUD_HOLD_MILLIS;
    }

    @Override
    public AudioFormat getFormat() {
        return track.getFormat();
    }

    @Override
    public ByteBuffer read(int size) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(size);
        cursor = track.readInto(cursor, buffer);
        if (rootMeanSquared(buffer) > LOUD_RMS) {
            lastLoudMillis = Util.getMillis();
        }
        return buffer;
    }

    private static double rootMeanSquared(ByteBuffer buffer) {
        ShortBuffer samples = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        int count = samples.remaining();
        if (count == 0) return 0;
        double sum = 0;
        for (int i = 0; i < count; i++) {
            short s = samples.get(i);
            sum += s * s;
        }
        return Math.sqrt(sum / count);
    }

    @Override
    public void close() {
    }
}
