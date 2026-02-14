package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
import net.mehvahdjukaar.vista.common.cassette.CassetteTape;
import net.mehvahdjukaar.vista.common.tv.IntAnimationState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CassetteTapeVideoSource implements IVideoSource {

    private final @NotNull Holder<CassetteTape> tape;

    public CassetteTapeVideoSource(Holder<CassetteTape> cassette) {
        this.tape = cassette;

    }

    @Override
    public SoundEvent getVideoSound() {
        var s = tape.value().soundEvent();
        return s.map(Holder::value).orElseGet(VistaMod.TV_STATIC_SOUND);
    }

    @Override
    public int getVideoDuration() {
        return tape.value().soundDuration().orElse(VistaMod.STATIC_SOUND_DURATION);
    }

    @Override
    public @NotNull VertexConsumer getVideoFrameBuilder(
            float partialTick, MultiBufferSource buffer, boolean shouldUpdate, int screenSize, int pixelEffectRes,
            int animationTick, boolean paused,
            IntAnimationState switchAnim, IntAnimationState staticAnim) {

        return TvScreenVertexConsumers.getTapeVC(buffer, tape, pixelEffectRes, animationTick, paused, switchAnim);
    }
}
