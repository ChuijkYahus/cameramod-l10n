package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
import net.mehvahdjukaar.vista.common.broadcast.BroadcastManager;
import net.mehvahdjukaar.vista.common.cassette.IBroadcastSource;
import net.mehvahdjukaar.vista.common.tv.IntAnimationState;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record BroadcastVideoSource(UUID uuid) implements IVideoSource {

    @Override
    public @NotNull VertexConsumer getVideoFrameBuilder(float partialTick, MultiBufferSource buffer,
                                                        boolean shouldUpdate, Vec2i screenSize, Vec2i pixelEffectRes,
                                                        int videoAnimationTick, boolean paused,
                                                        IntAnimationState switchAnim, IntAnimationState staticAnim,
                                                        boolean showsTime) {
        IVideoSource vfContent = getBroadcastContent();
        if (vfContent != null) {
            return vfContent.getVideoFrameBuilder(partialTick, buffer, shouldUpdate, screenSize, pixelEffectRes,
                    videoAnimationTick, paused, switchAnim, staticAnim, showsTime);
        }
        return TvScreenVertexConsumers.getNoiseVC(buffer, pixelEffectRes, switchAnim);
    }

    @Override
    public void updateAudio(TVBlockEntity tv, boolean playing) {
        IVideoSource vfContent = getBroadcastContent();
        if (vfContent != null) vfContent.updateAudio(tv, playing);
    }

    @Nullable
    private IVideoSource getBroadcastContent() {
        Level level = Minecraft.getInstance().level;
        BroadcastManager manager = BroadcastManager.getInstance(level);
        IBroadcastSource broadcast = manager.getBroadcast(uuid, true);
        if (broadcast == null) return null;
        return broadcast.getBroadcastVideo();
    }
}
