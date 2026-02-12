package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.VistaRenderTypes;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
import net.mehvahdjukaar.vista.common.cassette.CassetteItem;
import net.mehvahdjukaar.vista.common.tv.IntAnimationState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IVideoSource {

    IVideoSource EMPTY = new Empty();

    @NotNull
    VertexConsumer getVideoFrameBuilder(
            float partialTick, MultiBufferSource buffer,
            boolean shouldUpdate, int screenSize, int pixelEffectRes,
            int videoAnimationTick, boolean paused,
            IntAnimationState switchAnim, IntAnimationState staticAnim);

    @Nullable
    default SoundEvent getVideoSound() {
        return null;
    }

    default int getVideoDuration() {
        return 0;
    }

    static IVideoSource create(ItemStack stack) {
        //we could have also implemented in the item but its better separation like this
        if (stack.getItem() instanceof CassetteItem) {
            return new CassetteTapeVideoSource(stack);
        } else if (stack.has(VistaMod.LINKED_FEED_COMPONENT.get())) {
            return new BroadcastVideoSource(stack.get(VistaMod.LINKED_FEED_COMPONENT.get()));
        }
        return EMPTY;
    }

    class Empty implements IVideoSource {

        @Override
        public @NotNull VertexConsumer getVideoFrameBuilder(
                float partialTick, MultiBufferSource buffer,
                boolean shouldUpdate, int screenSize, int pixelEffectRes,
                int videoAnimationTick, boolean paused,
                IntAnimationState switchAnim, IntAnimationState staticAnim) {
            return TvScreenVertexConsumers.getBarsVC(buffer, pixelEffectRes, paused, switchAnim);
        }
    }
}
