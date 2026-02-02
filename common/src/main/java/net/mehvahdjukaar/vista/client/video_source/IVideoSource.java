package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.common.cassette.CassetteItem;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface IVideoSource {

    IVideoSource EMPTY = new Empty();

    @Nullable VertexConsumer getVideoFrameBuilder(TVBlockEntity targetScreen, float partialTick, MultiBufferSource buffer,
                                                  boolean shouldUpdate, int screenSize, int pixelEffectRes);

    @Nullable
    default SoundEvent getVideoSound() {
        return null;
    }

    default int getVideoDuration() {
        return 0;
    }

    static IVideoSource create(ItemStack stack) {
        //we could have also implemented in the item but its better separation like thus

        if (stack.getItem() instanceof CassetteItem) {

            return new CassetteTapeVideoSource(stack);
        }
        return EMPTY;
    }

    class Empty implements IVideoSource {

        @Override
        public @Nullable VertexConsumer getVideoFrameBuilder(TVBlockEntity targetScreen, float partialTick,
                                                             MultiBufferSource buffer, boolean shouldUpdate, int screenSize, int pixelEffectRes) {
            return null;
        }
    }
}
