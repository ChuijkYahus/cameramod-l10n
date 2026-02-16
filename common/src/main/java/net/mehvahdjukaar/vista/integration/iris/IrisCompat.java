package net.mehvahdjukaar.vista.integration.iris;

import net.irisshaders.iris.pipeline.VanillaRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;

import java.lang.reflect.Field;
import java.util.Arrays;

public class IrisCompat {

    public static final VanillaRenderingPipeline VISTA_PIPELINE = new VanillaRenderingPipeline();

    public static WorldRenderingPipeline getVistaPipeline() {
        return VISTA_PIPELINE;
    }

    public static Runnable decorateRendererWithoutIris(Runnable renderTask) {
        return () -> {
            LevelRenderer lr = Minecraft.getInstance().levelRenderer;
            OldRenderState oldState = OldRenderState.loadFrom(CapturedRenderingState.INSTANCE);

            VANILLA_PIPELINE_FIELD.setAccessible(true);
            WorldRenderingPipeline oldPipeline;
            try {
                oldPipeline = (WorldRenderingPipeline) VANILLA_PIPELINE_FIELD.get(lr);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            boolean old = ShadowRenderer.ACTIVE;
            ShadowRenderer.ACTIVE = false;
            renderTask.run();
            ShadowRenderer.ACTIVE = old;

            //restore rendering state
            if (oldState != null) oldState.saveTo(CapturedRenderingState.INSTANCE);

            VANILLA_PIPELINE_FIELD.setAccessible(true);
            try {
                VANILLA_PIPELINE_FIELD.set(lr, oldPipeline);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static final Field VANILLA_PIPELINE_FIELD = Arrays.stream(LevelRenderer.class.getDeclaredFields())
            .filter(f -> f.getType().equals(WorldRenderingPipeline.class))
            .findFirst().orElseThrow(() -> new RuntimeException("Failed to find vanilla pipeline field!"));


    private record OldRenderState(
            Matrix4fc gbufferModelView,
            Matrix4fc gbufferProjection,
            Vector3d fogColor,
            float fogDensity,
            float darknessLightFactor,
            float tickDelta,
            float realTickDelta,
            int currentRenderedBlockEntity,
            int currentRenderedEntity,
            int currentRenderedItem,
            float currentAlphaTest,
            float cloudTime) {

        public OldRenderState() {
            this(null, null, new Vector3d(),
                    0, 0, 0, 0, -1, -1, -1, 0, 0);
        }

        public void saveTo(CapturedRenderingState state) {
            state.setGbufferModelView(gbufferModelView);
            state.setGbufferProjection((Matrix4f) gbufferProjection);
            state.setFogColor((float) fogColor.x, (float) fogColor.y, (float) fogColor.z);
            state.setFogDensity(fogDensity);
            state.setDarknessLightFactor(darknessLightFactor);
            state.setTickDelta(tickDelta);
            state.setRealTickDelta(realTickDelta);
            state.setCurrentBlockEntity(currentRenderedBlockEntity);
            state.setCurrentEntity(currentRenderedEntity);
            state.setCurrentRenderedItem(currentRenderedItem);
            state.setCurrentAlphaTest(currentAlphaTest);
            state.setCloudTime(cloudTime);
        }

        public static OldRenderState loadFrom(CapturedRenderingState state) {
            return new OldRenderState(
                    new Matrix4f(state.getGbufferModelView()),
                    new Matrix4f(state.getGbufferProjection()),
                    new Vector3d(state.getFogColor()),
                    state.getFogDensity(), state.getDarknessLightFactor(), state.getTickDelta(),
                    state.getRealTickDelta(), state.getCurrentRenderedBlockEntity(), state.getCurrentRenderedEntity(),
                    state.getCurrentRenderedItem(), state.getCurrentAlphaTest(), state.getCloudTime());
        }
    }
}
