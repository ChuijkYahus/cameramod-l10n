package net.mehvahdjukaar.vista.client.web;

import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

//pulse code modulation stream
public class PcmAudioStream implements AudioStream {

    private final PcmAudioTrack track;
    private long cursor;

    public PcmAudioStream(PcmAudioTrack track, double startSeconds) {
        this.track = track;
        this.cursor = (long) (startSeconds * PcmAudioTrack.SAMPLE_RATE) * 2;
    }

    @Override
    public AudioFormat getFormat() {
        return PcmAudioTrack.FORMAT;
    }

    @Override
    public ByteBuffer read(int size) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(size);
        cursor = track.fill(cursor, buffer);
        return buffer;
    }

    @Override
    public void close() {
    }
}
