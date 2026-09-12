package net.mehvahdjukaar.vista.client.web;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

public interface PcmSource {

    //probably enough
    int SAMPLE_RATE = 24000;
    int BYTES_PER_SECOND = SAMPLE_RATE * 2;
    AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    boolean hasSamples();

    long readInto(long cursor, ByteBuffer dst);
}
