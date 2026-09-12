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

public class BroadcastVideoSource implements IVideoSource {

    private final UUID uuid;
    //what the broadcaster hands out, only compared by identity to notice url or power changes
    @Nullable
    private IVideoSource broadcastSource;
    //our own instance so each screen has its own texture, clock and speaker
    @Nullable
    private IVideoSource screenSource;

    public BroadcastVideoSource(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    @Override
    public @NotNull VertexConsumer getVideoFrameBuilder(float partialTick, MultiBufferSource buffer,
                                                        boolean shouldUpdate, Vec2i screenSize, Vec2i pixelEffectRes,
                                                        int videoAnimationTick, boolean paused,
                                                        IntAnimationState switchAnim, IntAnimationState staticAnim,
                                                        boolean showsTime) {
        if (screenSource != null) {
            return screenSource.getVideoFrameBuilder(partialTick, buffer, shouldUpdate, screenSize, pixelEffectRes,
                    videoAnimationTick, paused, switchAnim, staticAnim, showsTime);
        }
        return TvScreenVertexConsumers.getNoiseVC(buffer, pixelEffectRes, switchAnim);
    }

    @Override
    public void updateAudio(TVBlockEntity tv, boolean playing) {
        IVideoSource broadcast = getBroadcastContent();
        if (broadcast != broadcastSource) {
            if (screenSource != null) screenSource.updateAudio(tv, false);
            this.broadcastSource = broadcast;
            this.screenSource = broadcast == null ? null : broadcast.newInstance();
        }
        if (screenSource != null) screenSource.updateAudio(tv, playing);
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
