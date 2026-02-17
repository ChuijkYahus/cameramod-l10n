package net.mehvahdjukaar.vista.integration.iris;

import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.pipeline.VanillaRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Supplier;

public class IrisCompat {


    // true while Vista is rendering a camera pass (should work)
    private static final WorldRenderingPipeline VISTA_PIPELINE = new VanillaRenderingPipeline();
    private static final ThreadLocal<Boolean> VISTA_RENDERING = ThreadLocal.withInitial(() -> false);
    private static Supplier<Boolean> irisOff;


    public static boolean isVistaRendering() {
        return VISTA_RENDERING.get();
    }
    // change end: vista-flag

    public static void maybeResetTemporalHistoryForBobbing(Minecraft mc) {
    }

    public static void addConfigs(ConfigBuilder builder) {
    }

    // change start: vista-wrap
    public static Runnable decorateRendererWithoutShadows(Runnable renderTask) {
        return () -> {
            boolean oldActive = ShadowRenderer.ACTIVE;

    @Nullable
    public static WorldRenderingPipeline getModifiedPipeline(){
        return  VISTA_RENDERING.get() && irisOff.get() ? VISTA_PIPELINE : null;
    }

    public static Runnable decorateRendererWithoutShaderPacks(Runnable renderTask) {
        return () -> {
            LevelRenderer lr = Minecraft.getInstance().levelRenderer;
            boolean oldShadowActive = ShadowRenderer.ACTIVE;
            boolean oldVistaRendering = VISTA_RENDERING.get();
            OldRenderState oldState = OldRenderState.loadFrom(CapturedRenderingState.INSTANCE);

            WorldRenderingPipeline oldPipeline = getCurrentPipeline(lr);

            try {
                ShadowRenderer.ACTIVE = false;
                VISTA_RENDERING.set(true);
                renderTask.run();
            } finally {
                ShadowRenderer.ACTIVE = oldActive;
                // put state back
                ShadowRenderer.ACTIVE = oldShadowActive;
                VISTA_RENDERING.set(oldVistaRendering);
                oldState.saveTo(CapturedRenderingState.INSTANCE);
                setCurrentPipeline(lr, oldPipeline);

            }
        };
    }

    public static Runnable decorateRendererWithoutIris(Runnable renderTask) {
        return decorateRendererWithoutShadows(renderTask);
    }
    // change end: vista-wrap


    private static void setCurrentPipeline(LevelRenderer lr, WorldRenderingPipeline oldPipeline) {
        try {
            VANILLA_PIPELINE_FIELD.setAccessible(true);
            VANILLA_PIPELINE_FIELD.set(lr, oldPipeline);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ThreadLocal<WorldRenderingPipeline> IRIS_PIPELINE_CACHE = new ThreadLocal<>();
    private static final ThreadLocal<OldRenderState> IRIS_RENDERING_STATE_CACHE = ThreadLocal.withInitial(OldRenderState::new);

    public static WorldRenderingPipeline preparePipeline() {
        LevelRenderer lr = Minecraft.getInstance().levelRenderer;
        OldRenderState oldState = OldRenderState.loadFrom(CapturedRenderingState.INSTANCE);
        IRIS_RENDERING_STATE_CACHE.set(oldState);

        VANILLA_PIPELINE_FIELD.setAccessible(true);
        try {
            IRIS_PIPELINE_CACHE.set((WorldRenderingPipeline) VANILLA_PIPELINE_FIELD.get(lr));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return new VanillaRenderingPipeline();
    }

    public static void restoreVanillaPipeline(LevelRenderer lr) {
        OldRenderState oldState = IRIS_RENDERING_STATE_CACHE.get();
        if (oldState != null) {
        }
        VANILLA_PIPELINE_FIELD.setAccessible(true);
    }
    private static WorldRenderingPipeline getCurrentPipeline(LevelRenderer lr) {
        try {
            VANILLA_PIPELINE_FIELD.setAccessible(true);
            return (WorldRenderingPipeline) VANILLA_PIPELINE_FIELD.get(lr);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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

    public static void addConfigs(ConfigBuilder builder) {
        irisOff = builder
                .comment("Attempts to disable iris shaders in the live feed view")
                .define("iris_off_hack", true);
    }
}
