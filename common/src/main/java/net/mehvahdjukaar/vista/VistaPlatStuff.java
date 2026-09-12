package net.mehvahdjukaar.vista;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.mehvahdjukaar.vista.client.web.PcmAudioTrack;
import net.mehvahdjukaar.vista.client.web.TvSpeakerSound;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.joml.Matrix4f;

public class VistaPlatStuff {

    @Contract
    @PlatformImpl
    public static void dispatchRenderStageAfterLevel(Minecraft mc, PoseStack poseStack, Camera camera, Matrix4f modelViewMatrix, Matrix4f projMatrix) {
        throw new AssertionError();
    }

    @Contract
    @PlatformImpl
    public static void tickEnergy(TVBlockEntity tv) {
        throw new AssertionError();
    }

    @Contract
    @PlatformImpl
    public static void invalidateBlockCapabilities(Level level, BlockPos pos) {
        throw new AssertionError();
    }

    @Contract
    @PlatformImpl
    public static TvSpeakerSound createTvSpeakerSound(PcmAudioTrack track, Vec3 pos, double startSeconds, double videoClockOffset) {
        throw new AssertionError();
    }
}
