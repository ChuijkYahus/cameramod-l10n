package net.mehvahdjukaar.vista;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.irisshaders.iris.pipeline.VanillaRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.util.Arrays;

public class VistaPlatStuff {

    @ExpectPlatform
    public static void dispatchRenderStageAfterLevel(Minecraft mc, PoseStack poseStack, Camera camera, Matrix4f modelViewMatrix, Matrix4f projMatrix) {
        throw new AssertionError();
    }

}
