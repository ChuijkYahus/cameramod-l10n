package net.mehvahdjukaar.vista.client.web;

import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

//pulse code modulation stream
public class PcmAudioStream implements AudioStream {

    private final PcmSource source;
    private long cursor;

    public PcmAudioStream(PcmSource source, double startSeconds) {
        this.source = source;
        //whole frames, an odd byte offset swaps every sample's bytes
        this.cursor = (long) (startSeconds * PcmSource.SAMPLE_RATE) * PcmSource.FORMAT.getFrameSize();
    }

    @Override
    public AudioFormat getFormat() {
        return PcmSource.FORMAT;
    }

    @Override
    public ByteBuffer read(int size) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(size);
        cursor = source.readInto(cursor, buffer);
        return buffer;
    }

    @Override
    public void close() {
    }
}
