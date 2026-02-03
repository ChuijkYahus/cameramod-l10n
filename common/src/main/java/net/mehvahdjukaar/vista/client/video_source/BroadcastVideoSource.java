package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.client.ModRenderTypes;
import net.mehvahdjukaar.vista.common.BroadcastManager;
import net.mehvahdjukaar.vista.common.cassette.IBroadcastProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BroadcastVideoSource implements IVideoSource {

    private final UUID uuid;

    public BroadcastVideoSource(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public @NotNull VertexConsumer getVideoFrameBuilder(float partialTick, MultiBufferSource buffer,
                                                        boolean shouldUpdate, int screenSize, int pixelEffectRes,
                                                        int videoAnimationTick, int switchAnim, float staticAnim) {
        Level level = Minecraft.getInstance().level;
        BroadcastManager manager = BroadcastManager.getInstance(level);
        IBroadcastProvider broadcast = manager.getBroadcast(uuid, true);

        if (broadcast == null) {
            return buffer.getBuffer(ModRenderTypes.NOISE);
        }
        IVideoSource vfContent = broadcast.getBroadcastVideoSource();
        if (vfContent == null) {
            return buffer.getBuffer(ModRenderTypes.NOISE);
        }
        return vfContent.getVideoFrameBuilder(partialTick, buffer, shouldUpdate, screenSize, pixelEffectRes,
                videoAnimationTick, switchAnim, staticAnim);

    }

}
