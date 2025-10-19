package net.mehvahdjukaar.camera_vision.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.mehvahdjukaar.camera_vision.CameraModClient;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.FrameBufferBackedDynamicTexture;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

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

    public static final Function<FrameBufferBackedDynamicTexture, RenderType> CAMERA_VIEW = Util.memoize((resourceLocation) -> {
        RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
                .setShaderState(CAMERA_SHADER_STATE)
                .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation.getTextureLocation(),
                        false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setTexturingState(new TexturingStateShard("set_uniforms",
                        () -> {
                            CameraModClient.CAMERA_VIEW_SHADER.get()
                                    .safeGetUniform("TexelSize")
                                    .set((float) 1 / resourceLocation.getWidth(),
                                            (float) 1 / resourceLocation.getHeight());
                        },
                        () -> {}

                ))
                .createCompositeState(false);
        return create("camera_view", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                1536, true, false, compositeState);
    });


}
