package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
import net.mehvahdjukaar.vista.common.BroadcastManager;
import net.mehvahdjukaar.vista.common.cassette.IBroadcastProvider;
import net.mehvahdjukaar.vista.common.tv.IntAnimationState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record BroadcastVideoSource(UUID uuid) implements IVideoSource {

    @Override
    public @NotNull VertexConsumer getVideoFrameBuilder(float partialTick, MultiBufferSource buffer,
                                                        boolean shouldUpdate, int screenSize, int pixelEffectRes,
                                                        int videoAnimationTick, boolean paused,
                                                        IntAnimationState switchAnim, IntAnimationState staticAnim) {
        Level level = Minecraft.getInstance().level;
        BroadcastManager manager = BroadcastManager.getInstance(level);
        IBroadcastProvider broadcast = manager.getBroadcast(uuid, true);

        if (broadcast != null) {

            IVideoSource vfContent = broadcast.getBroadcastVideoSource();
            if (vfContent != null) {
                return vfContent.getVideoFrameBuilder(partialTick, buffer, shouldUpdate, screenSize, pixelEffectRes,
                        videoAnimationTick, paused, switchAnim, staticAnim);
            }
        }
        return TvScreenVertexConsumers.getNoiseVC(buffer, pixelEffectRes, switchAnim);
    }

}
