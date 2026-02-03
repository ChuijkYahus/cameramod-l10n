package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.client.ModRenderTypes;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
import net.mehvahdjukaar.vista.common.cassette.CassetteItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface IVideoSource {

    IVideoSource EMPTY = new Empty();

    VertexConsumer getVideoFrameBuilder(
            float partialTick, MultiBufferSource buffer,
            boolean shouldUpdate, int screenSize, int pixelEffectRes,
            int videoAnimationTick, int switchAnim, float staticAnim);

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
        public VertexConsumer getVideoFrameBuilder(float partialTick, MultiBufferSource buffer, boolean shouldUpdate, int screenSize, int pixelEffectRes, int videoAnimationTick, int switchAnim, float staticAnim) {
            return TvScreenVertexConsumers.getBarsVC(buffer, pixelEffectRes, switchAnim);
        }
    }
}
