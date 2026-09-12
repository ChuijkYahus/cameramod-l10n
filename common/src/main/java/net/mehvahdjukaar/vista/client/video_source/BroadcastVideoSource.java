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
    //remembered so a feed that went away or got swapped still gets its audio stopped
    @Nullable
    private IVideoSource lastSource;

    public BroadcastVideoSource(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    @Override
    public @NotNull VertexConsumer getVideoFrameBuilder(TVBlockEntity tv, float partialTick, MultiBufferSource buffer,
                                                        boolean shouldUpdate, Vec2i screenSize, Vec2i pixelEffectRes,
                                                        int videoAnimationTick, boolean paused,
                                                        IntAnimationState switchAnim, IntAnimationState staticAnim,
                                                        boolean showsTime) {
        IVideoSource source = getBroadcastContent();
        if (source != null) {
            return source.getVideoFrameBuilder(tv, partialTick, buffer, shouldUpdate, screenSize, pixelEffectRes,
                    videoAnimationTick, paused, switchAnim, staticAnim, showsTime);
        }
        return TvScreenVertexConsumers.getNoiseVC(buffer, pixelEffectRes, switchAnim);
    }

    @Override
    public void updateAudio(TVBlockEntity tv, boolean playing) {
        IVideoSource source = getBroadcastContent();
        if (lastSource != null && lastSource != source) lastSource.updateAudio(tv, false);
        this.lastSource = source;
        if (source != null) source.updateAudio(tv, playing);
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
