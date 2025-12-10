package net.mehvahdjukaar.vista.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.vista.VistaModClient;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ModRenderTypes extends RenderType {


    public ModRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    private static final AtomicReference<Float> ENDERMAN_NOISE = new AtomicReference<>(0f);


    private static final ShaderStateShard CAMERA_SHADER_STATE = new ShaderStateShard(VistaModClient.CAMERA_VIEW_SHADER);
    private static final ShaderStateShard STATIC_SHADER_STATE = new ShaderStateShard(VistaModClient.STATIC_SHADER);
    private static final ShaderStateShard POSTERIZE_SHADER_STATE = new ShaderStateShard(VistaModClient.POSTERIZE_SHADER);

    private static final Function<ResourceLocation, RenderType> CAMERA_DRAW = Util.memoize(
            (t) -> ModRenderTypes.createCameraDraw(t, 0));


    public static RenderType getCameraDraw(ResourceLocation texture, float enderman) {
        if (enderman > 0f) {
            return createCameraDraw(texture, enderman);
        } else return CAMERA_DRAW.apply(texture);
    }

    private static RenderType createCameraDraw(ResourceLocation text, float enderman) {
        CompositeState compositeState = CompositeState.builder()
                .setShaderState(CAMERA_SHADER_STATE)
                .setTextureState(new TextureStateShard(text,
                        //TODO: mipmap
                        false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setTexturingState(new TexturingStateShard("set_texel_size",
                        () -> {
                            ShaderInstance shader = VistaModClient.CAMERA_VIEW_SHADER.get();
                            shader.safeGetUniform("SpriteDimensions")
                                    .set(new Vector4f(0, 0, 1, 1f));
                            setCameraDrawUniforms(shader, enderman);
                        },
                        () -> {
                        }))
                .createCompositeState(false);

        return create("camera_view", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                1536, true, false, compositeState);
    }

    public static final BiFunction<ResourceLocation, Material, RenderType> CAMERA_DRAW_SPRITE = Util.memoize((text, mat) -> {
        RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
                .setShaderState(CAMERA_SHADER_STATE)
                .setTextureState(new RenderStateShard.TextureStateShard(text,
                        //TODO: mipmap
                        false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setTexturingState(new TexturingStateShard("set_texel_size",
                        () -> {
                            ShaderInstance shader = VistaModClient.CAMERA_VIEW_SHADER.get();
                            TextureAtlasSprite sprite = mat.sprite();
                            setSpriteDimensions(shader, sprite);
                            setCameraDrawUniforms(shader, 0);
                        },
                        () -> {
                        }))
                .createCompositeState(false);

        return create("camera_view", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                1536, true, false, compositeState);
    });

    private static void setSpriteDimensions(ShaderInstance shader, TextureAtlasSprite sprite) {
        shader.safeGetUniform("SpriteDimensions")
                .set(new Vector4f(
                        sprite.getU0(),                     // minU
                        sprite.getV0(),                     // minV
                        sprite.getU1() - sprite.getU0(),    // sizeU
                        sprite.getV1() - sprite.getV0()     // sizeV
                ));
    }

    private static void setCameraDrawUniforms(ShaderInstance shader, float noise) {

        setFloat(shader, "TriadsPerPixel", 1.37f);
        setFloat(shader, "Smear", 1f);
        setFloat(shader, "EnableEnergyNormalize", 0.0f);

        setFloat(shader, "VignetteIntensity", 1f);
//TODO: fix these 2 noise not looking the same when at 1
        setFloat(shader, "NoiseIntensity", noise);
    }

    public static final RenderType NOISE =
            create("noise", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                    1536, true, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(STATIC_SHADER_STATE)
                            .setTransparencyState(NO_TRANSPARENCY)
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


    public static void setEndermanStatic(float intensity) {
        ENDERMAN_NOISE.set(intensity);
    }

}
