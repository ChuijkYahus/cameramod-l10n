package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.common.BroadcastManager;
import net.mehvahdjukaar.vista.common.cassette.IBroadcastProvider;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BroadcastVideoSource implements IVideoSource {

    private final UUID uuid;
    private final TVBlockEntity tv;

    public BroadcastVideoSource(UUID uuid, TVBlockEntity tv) {
        this.uuid = uuid;
        this.tv = tv;
    }

    public @Nullable VertexConsumer getVideoFrameBuilder(TVBlockEntity targetScreen, float partialTick, MultiBufferSource buffer, boolean shouldUpdate,
                                                         int screenSize, int pixelEffectRes, int switchAnim) {
        Level level = tv.getLevel();
        BroadcastManager manager = BroadcastManager.getInstance(level);
        IBroadcastProvider broadcast = manager.getBroadcast(uuid, true);

        IVideoSource vfContent = broadcast.getBroadcastVideoSource();
        if (vfContent == null) return null;
        return vfContent.getVideoFrameBuilder(targetScreen, partialTick, buffer, shouldUpdate, screenSize, pixelEffectRes, switchAnim);

    }

}
