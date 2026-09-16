package net.mehvahdjukaar.vista.client.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.vista.client.web.PcmSource;
import net.mehvahdjukaar.vista.client.web.TvSpeakerSound;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.concurrent.CompletableFuture;

public class VistaClientPlatStuffImpl {

    public static void dispatchRenderStageAfterLevel(Minecraft mc, PoseStack poseStack, Camera camera,
                                                     Matrix4f modelViewMatrix, Matrix4f projMatrix) {
    }

    public static TvSpeakerSound createTvSpeakerSound(PcmSource source, Vec3 pos, BlockPos tvPos, double startSeconds) {
        return new TvSpeakerSound(source, pos, tvPos, startSeconds) {
            @Override
            public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, ResourceLocation id, boolean repeatInstantly) {
                return CompletableFuture.completedFuture(openStream());
            }
        };
    }
}
