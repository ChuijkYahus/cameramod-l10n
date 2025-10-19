package net.mehvahdjukaar.camera_vision.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.mehvahdjukaar.camera_vision.CameraModClient;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.FrameBufferBackedDynamicTexture;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Vector3f;

import java.util.function.Function;

public class ModRenderTypes extends RenderType {


    public ModRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static final VertexFormat ENTITY_NO_OVERLAY = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .padding(1).build();

    private static final ShaderStateShard CAMERA_SHADER_STATE = new ShaderStateShard(CameraModClient.CAMERA_VIEW_SHADER);
    private static final ShaderStateShard POSTERIZE_SHADER_STATE = new ShaderStateShard(CameraModClient.POSTERIZE_SHADER);

    public static final Function<FrameBufferBackedDynamicTexture, RenderType> CAMERA_DRAW = Util.memoize((text) -> {
        RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
                .setShaderState(CAMERA_SHADER_STATE)
                .setTextureState(new RenderStateShard.TextureStateShard(text.getTextureLocation(),
                        //TODO: mipmap
                        false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setTexturingState(new TexturingStateShard("set_texel_size",
                        () -> {
                            ShaderInstance shader = CameraModClient.CAMERA_VIEW_SHADER.get();
                            shader.safeGetUniform("TriadsPerPixel")
                                    .set(1.37f);
                            shader.safeGetUniform("Smear")
                                    .set(1f);
                            shader.safeGetUniform("EnableEnergyNormalize")
                                    .set(0.0f);
                        },
                        () -> {
                        }))
                        .createCompositeState(false);

        return create("camera_view", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                1536, true, false, compositeState);
    });

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
                            ShaderInstance shader = CameraModClient.POSTERIZE_SHADER.get();

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


}
