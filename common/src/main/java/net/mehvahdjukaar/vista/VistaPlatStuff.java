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

    private static final Field VANILLA_PIPELINE_FIELD = Arrays.stream(LevelRenderer.class.getDeclaredFields())
            .filter(f -> f.getType().equals(WorldRenderingPipeline.class))
            .findFirst().orElseThrow(() -> new RuntimeException("Failed to find vanilla pipeline field!"));


    private static final ThreadLocal<WorldRenderingPipeline> IRIS_PIPELINE_CACHE = new ThreadLocal<>();

    public static void setVanillaPipeline(LevelRenderer lr) {
        VANILLA_PIPELINE_FIELD.setAccessible(true);
        try {
            IRIS_PIPELINE_CACHE.set((WorldRenderingPipeline) VANILLA_PIPELINE_FIELD.get(lr));
            VANILLA_PIPELINE_FIELD.set(lr, new VanillaRenderingPipeline());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void restoreVanillaPipeline(LevelRenderer lr) {
        VANILLA_PIPELINE_FIELD.setAccessible(true);
        try {
            VANILLA_PIPELINE_FIELD.set(lr, IRIS_PIPELINE_CACHE.get());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
