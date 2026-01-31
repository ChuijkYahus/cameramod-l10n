package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.cassette.CassetteTape;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CassetteTapeVideoSource implements IVideoSource {

    private final Holder<CassetteTape> tape;

    public CassetteTapeVideoSource(ItemStack cassette) {
        this.tape = cassette.get(VistaMod.CASSETTE_TAPE_COMPONENT.get());

    }

    private SoundEvent getVideoSound() {
        if (tape != null) {
            var s = tape.value().soundEvent();
            if (s.isPresent()) return s.get().value();
        }
        return VistaMod.TV_STATIC_SOUND.get();
    }

    private int getVideoDuration() {
        if (tape != null) {
            return tape.value().soundDuration().orElse(VistaMod.STATIC_SOUND_DURATION);
        }
        return VistaMod.STATIC_SOUND_DURATION;
    }

    @Override
    public @Nullable VertexConsumer getVideoFrameBuilder(TVBlockEntity targetScreen, float partialTick, MultiBufferSource buffer, boolean shouldUpdate, int screenSize, int pixelEffectRes, int switchOnAnim) {
        return null;
    }
}
