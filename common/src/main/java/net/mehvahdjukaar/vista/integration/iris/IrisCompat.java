package net.mehvahdjukaar.vista.integration.iris;

import net.irisshaders.iris.mixin.MixinLevelRenderer;
import net.irisshaders.iris.pipeline.VanillaRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;

import java.lang.reflect.Field;
import java.util.Arrays;

public class IrisCompat {


    private static final Field VANILLA_PIPELINE_FIELD = Arrays.stream(LevelRenderer.class.getDeclaredFields())
            .filter(f -> f.getType().equals(WorldRenderingPipeline.class))
            .findFirst().orElseThrow(() -> new RuntimeException("Failed to find vanilla pipeline field!"));


    private static final ThreadLocal<WorldRenderingPipeline> IRIS_PIPELINE_CACHE = new ThreadLocal<>();
    private static final ThreadLocal<OldRenderState> IRIS_RENDERING_STATE_CACHE = ThreadLocal.withInitial(OldRenderState::new);


    public static WorldRenderingPipeline preparePipeline() {
        //clone rendering state
        LevelRenderer lr = Minecraft.getInstance().levelRenderer;
        OldRenderState oldState = OldRenderState.loadFrom(CapturedRenderingState.INSTANCE);
        IRIS_RENDERING_STATE_CACHE.set(oldState);

        VANILLA_PIPELINE_FIELD.setAccessible(true);
        try {
            IRIS_PIPELINE_CACHE.set((WorldRenderingPipeline) VANILLA_PIPELINE_FIELD.get(lr));
           // VANILLA_PIPELINE_FIELD.set(lr, new VanillaRenderingPipeline());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return new VanillaRenderingPipeline();

    }

    public static void restoreVanillaPipeline(LevelRenderer lr) {
        //restore rendering state
        OldRenderState oldState = IRIS_RENDERING_STATE_CACHE.get();
        if (oldState != null) {
         //   oldState.saveTo(CapturedRenderingState.INSTANCE);
        }
        VANILLA_PIPELINE_FIELD.setAccessible(true);
        //   VANILLA_PIPELINE_FIELD.set(lr, IRIS_PIPELINE_CACHE.get());
    }


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
            return new OldRenderState(state.getGbufferModelView(), state.getGbufferProjection(),
                    state.getFogColor(), state.getFogDensity(), state.getDarknessLightFactor(), state.getTickDelta(),
                    state.getRealTickDelta(), state.getCurrentRenderedBlockEntity(), state.getCurrentRenderedEntity(),
                    state.getCurrentRenderedItem(), state.getCurrentAlphaTest(), state.getCloudTime());
        }
    }
}
