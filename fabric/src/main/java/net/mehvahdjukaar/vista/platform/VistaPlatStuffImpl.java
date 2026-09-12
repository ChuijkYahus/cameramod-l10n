package net.mehvahdjukaar.vista.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.vista.client.web.PcmAudioTrack;
import net.mehvahdjukaar.vista.client.web.TvSpeakerSound;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.concurrent.CompletableFuture;

public class VistaPlatStuffImpl {
    public static void dispatchRenderStageAfterLevel(Minecraft mc, PoseStack poseStack, Camera camera,
                                                     Matrix4f modelViewMatrix, Matrix4f projMatrix) {
    }

    public static void tickEnergy(TVBlockEntity tv) {
    }

    public static void invalidateBlockCapabilities(Level level, BlockPos pos) {
    }

    public static TvSpeakerSound createTvSpeakerSound(PcmAudioTrack track, Vec3 pos, double startSeconds, double videoClockOffset) {
        return new TvSpeakerSound(track, pos, startSeconds, videoClockOffset) {
            @Override
            public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, ResourceLocation id, boolean repeatInstantly) {
                return CompletableFuture.completedFuture(openStream());
            }
        };
    }
}
