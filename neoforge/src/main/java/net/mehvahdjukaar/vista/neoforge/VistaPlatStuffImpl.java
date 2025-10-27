package net.mehvahdjukaar.vista.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

public class VistaPlatStuffImpl {
    public static void dispatchRenderStageAfterLevel(Minecraft mc, PoseStack poseStack, Camera camera,
                                                     Matrix4f modelViewMatrix, Matrix4f projMatrix) {
        mc.getProfiler().popPush("neoforge_render_last");

        ClientHooks.dispatchRenderStage(RenderLevelStageEvent.Stage.AFTER_LEVEL, mc.levelRenderer,
                null, modelViewMatrix, projMatrix, mc.levelRenderer.getTicks(),
                camera, mc.levelRenderer.getFrustum());
        mc.getProfiler().pop();

    }
}
