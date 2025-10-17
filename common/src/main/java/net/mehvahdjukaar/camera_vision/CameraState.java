package net.mehvahdjukaar.camera_vision;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.jetbrains.annotations.Nullable;

public class CameraState {
    public static  boolean SKIP_WORLD_RENDERING = false;
    public static  boolean SKIP_SKY = false;
    @Nullable
    public static RenderTarget TARGET = null;
    public static boolean NO_OUTLINE = false;


}
