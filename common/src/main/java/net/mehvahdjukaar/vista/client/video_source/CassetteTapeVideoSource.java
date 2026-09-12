package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.CrtOverlay;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
import net.mehvahdjukaar.vista.common.cassette.CassetteTape;
import net.mehvahdjukaar.vista.common.tv.IntAnimationState;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;

public class CassetteTapeVideoSource implements IVideoSource {

    private final @NotNull Holder<CassetteTape> tape;

    private int lastSoundLoop = -1;

    public CassetteTapeVideoSource(Holder<CassetteTape> cassette) {
        this.tape = cassette;

    }

    @Override
    public void updateAudio(TVBlockEntity tv, boolean playing) {
        if (!playing) {
            lastSoundLoop = -1;
            return;
        }
        int duration = Math.max(1, tape.value().soundDuration().orElse(VistaMod.STATIC_SOUND_DURATION));
        int loop = tv.getPlaybackTicks() / duration;
        if (loop == lastSoundLoop) return;
        lastSoundLoop = loop;
        SoundEvent sound = tape.value().soundEvent().map(Holder::value).orElseGet(VistaMod.TV_STATIC_SOUND);
        tv.getLevel().playLocalSound(tv.getBlockPos(), sound, SoundSource.BLOCKS, 1, 1, false);
    }

    @Override
    public @NotNull VertexConsumer getVideoFrameBuilder(
            TVBlockEntity tv, float partialTick, MultiBufferSource buffer, boolean shouldUpdate, Vec2i screenSize, Vec2i pixelEffectRes,
            int animationTick, boolean paused,
            IntAnimationState switchAnim, IntAnimationState staticAnim, boolean showsTime) {
        CrtOverlay overlay = paused ? CrtOverlay.PAUSE : CrtOverlay.NONE;
        return TvScreenVertexConsumers.getTapeVC(buffer, tape, pixelEffectRes, animationTick, overlay, switchAnim);
    }
}
