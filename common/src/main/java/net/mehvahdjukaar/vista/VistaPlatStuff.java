package net.mehvahdjukaar.vista;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

public class VistaPlatStuff {

    @ExpectPlatform
    public static void dispatchRenderStageAfterLevel(Minecraft mc, PoseStack poseStack, Camera camera, Matrix4f modelViewMatrix, Matrix4f projMatrix) {
        throw new AssertionError();
    }

}
