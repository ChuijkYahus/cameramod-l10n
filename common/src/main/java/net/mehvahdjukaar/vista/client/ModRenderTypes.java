package net.mehvahdjukaar.vista.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.moonlight.api.misc.TriFunction;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.textures.AnimationStripData;
import net.mehvahdjukaar.vista.client.textures.SimpleAnimatedStripTexture;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.function.Function;

public class ModRenderTypes extends RenderType {


    public ModRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    private static final ResourceLocation BACKGROUND_TEXTURE = VistaMod.res("textures/block/tv/front/default.png");
    private static final ShaderStateShard CAMERA_SHADER_STATE = new ShaderStateShard(VistaModClient.CAMERA_VIEW_SHADER);
    private static final ShaderStateShard STATIC_SHADER_STATE = new ShaderStateShard(VistaModClient.STATIC_SHADER);
    private static final ShaderStateShard POSTERIZE_SHADER_STATE = new ShaderStateShard(VistaModClient.POSTERIZE_SHADER);

    private static final TriFunction<ResourceLocation, Integer, Integer, RenderType> CAMERA_DRAW_RENDER_TYPE = Utils.memoize(
            (t, s, power) -> ModRenderTypes.createCameraDraw(t, 0, s, power));


    public static RenderType getCameraDraw(ResourceLocation texture, float enderman, int scale, int powerAnim) {
        if (enderman > 0f) {
            return createCameraDraw(texture, enderman, scale, powerAnim);
        } else return CAMERA_DRAW_RENDER_TYPE.apply(texture, scale, powerAnim);
    }

    private static RenderType createCameraDraw(ResourceLocation text, float enderman, int scale, int powerAnim) {
        CompositeState compositeState = CompositeState.builder()
                .setShaderState(CAMERA_SHADER_STATE)
                .setTextureState(new TextureStateShard(text,
                        //TODO: mipmap
                        false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setTexturingState(new TexturingStateShard("set_texel_size",
                        () -> {
                            ShaderInstance shader = VistaModClient.CAMERA_VIEW_SHADER.get();
                            shader.safeGetUniform("SpriteDimensions")
                                    .set(new Vector4f(0, 0, 1, 1f));
                            setCameraDrawUniforms(shader, enderman, scale, powerAnim);
                        },
                        () -> {
                        }))
                .createCompositeState(false);

        return create("camera_view", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                1536, true, false, compositeState);
    }

    public static final TriFunction<SimpleAnimatedStripTexture, Integer, Integer, RenderType> ANIMATED_STRIP_RENDER_TYPE = Utils.memoize((text, scale, powerAnim) -> {
        RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
                .setShaderState(CAMERA_SHADER_STATE)
                //TODO: mipmap
                .setTextureState(RenderStateShard.MultiTextureStateShard.builder()
                        .add(text.location(), false, false)
                        .add(BACKGROUND_TEXTURE, false, false)
                        .build())
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setTexturingState(new TexturingStateShard("set_texel_size",
                        () -> {
                            ShaderInstance shader = VistaModClient.CAMERA_VIEW_SHADER.get();
                            AnimationStripData sprite = text.getStripData();
                            setSpriteDimensions(shader, sprite);
                            setCameraDrawUniforms(shader, 0, scale, powerAnim);
                        },
                        () -> {
                        }))
                .createCompositeState(false);

        return create("camera_view", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                1536, true, false, compositeState);
    });

    private static void setSpriteDimensions(ShaderInstance shader, AnimationStripData sprite) {
        shader.safeGetUniform("SpriteDimensions")
                .set(new Vector4f(
                        0,                     // minU
                        0,                     // minV
                        sprite.frameRelativeW(),    // sizeU
                        sprite.frameRelativeH()     // sizeV
                ));
    }

    private static void setCameraDrawUniforms(ShaderInstance shader, float noise, int screenSize, int powerAnim) {
        float scale = screenSize / 12f;
        float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();
        setFloat(shader, "TriadsPerPixel", ClientConfigs.PIXEL_DENSITY.get() * scale);
        setFloat(shader, "Smear", 1f);
        setFloat(shader, "EnableEnergyNormalize", 0.0f);

        setFloat(shader, "VignetteIntensity", ClientConfigs.VIGNETTE.get());
        //TODO: fix these 2 noise not looking the same when at 1
        setFloat(shader, "NoiseIntensity", noise);
        float switchAnim = 0;
        if (powerAnim > 0) {
            switchAnim =  (powerAnim - pt) / (float) TVBlockEntity.SWITCH_ON_ANIMATION_TICKS;
        } else if (powerAnim < 0) {
            switchAnim = (1 + (powerAnim + pt) / (float) TVBlockEntity.SWITCH_OFF_ANIMATION_TICKS);
        }

        setFloat(shader, "SwitchAnimation", switchAnim);
    }

    public static final RenderType NOISE =
            create("noise", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                    1536, true, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(STATIC_SHADER_STATE)
                            .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                            .setLightmapState(LIGHTMAP)
                            .setTexturingState(new TexturingStateShard("set_texel_size",
                                    () -> {
                                        ShaderInstance shader = VistaModClient.STATIC_SHADER.get();
                                        setFloat(shader, "NoiseIntensity", 1f);
                                    },
                                    () -> {
                                    }))
                            .createCompositeState(false));

    public static final Function<RenderTarget, RenderType> POSTERIZE = Util.memoize((target) -> {
        RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
                .setShaderState(POSTERIZE_SHADER_STATE)
                // .setTextureState(new RenderStateShard.TextureStateShard(target.getTextureLocation(),
                //       false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setTexturingState(new TexturingStateShard("set_uniforms",
                        () -> {
                            RenderSystem.setShaderTexture(0, target.getColorTextureId());
                            ShaderInstance shader = VistaModClient.POSTERIZE_SHADER.get();

                            shader.safeGetUniform("PostMode")
                                    .set(1.0f);
                            shader.safeGetUniform("PostLevels")
                                    .set(new Vector3f(25.0f, 20.0f, 20.0f)); //LCH

                            shader.safeGetUniform("FxaaEdge")
                                    .set(0.0001f);
                            shader.safeGetUniform("FxaaBlend")
                                    .set(1.5f);
                            shader.safeGetUniform("FxaaDiagonal")
                                    .set(1.3f);
                            shader.safeGetUniform("FxaaSpread")
                                    .set(1.2f);

                            shader.safeGetUniform("DitherScale")
                                    .set(1f);
                            shader.safeGetUniform("DitherStrength")
                                    .set(1f);

                        },
                        () -> {
                        }

                ))
                .createCompositeState(false);
        return create("posterize", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS,
                1536, true, false, compositeState);
    });


    private static void setFloat(ShaderInstance shader, String name, float value) {
        shader.safeGetUniform(name).set(value);
    }

    public static RenderType getColorFilter(DyeColor color) {
        return null;
    }
}
