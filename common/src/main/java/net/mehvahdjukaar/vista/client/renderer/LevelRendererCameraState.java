package net.mehvahdjukaar.vista.client.renderer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class LevelRendererCameraState {

    private int lastCameraSectionX = Integer.MIN_VALUE;
    private int lastCameraSectionY = Integer.MIN_VALUE;
    private int lastCameraSectionZ = Integer.MIN_VALUE;
    private double prevCamX = Double.MIN_VALUE;
    private double prevCamY = Double.MIN_VALUE;
    private double prevCamZ = Double.MIN_VALUE;
    private double prevCamRotX = Double.MIN_VALUE;
    private double prevCamRotY = Double.MIN_VALUE;
    @Nullable
    private ViewArea viewArea;
    private int lastViewDistance;
    @Nullable
    private SectionOcclusionGraph sectionOcclusionGraph;
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections = new ObjectArrayList<>(10000);
    
    private int cameraRenderDistance = 8;

    public LevelRendererCameraState() {
    }

    public void copyFrom(LevelRenderer lr) {
        this.lastViewDistance = lr.lastViewDistance;
        this.sectionOcclusionGraph = lr.sectionOcclusionGraph;
        this.lastCameraSectionX = lr.lastCameraSectionX;
        this.lastCameraSectionY = lr.lastCameraSectionY;
        this.lastCameraSectionZ = lr.lastCameraSectionZ;
        this.prevCamX = lr.prevCamX;
        this.prevCamY = lr.prevCamY;
        this.prevCamZ = lr.prevCamZ;
        this.prevCamRotX = lr.prevCamRotX;
        this.prevCamRotY = lr.prevCamRotY;
        this.visibleSections = lr.visibleSections;
    }

    public static LevelRendererCameraState capture(LevelRenderer lr) {
        var instance = new LevelRendererCameraState();
        instance.copyFrom(lr);
        return instance;
    }

    public void apply(LevelRenderer lr) {
        if (this.sectionOcclusionGraph == null) {
            this.sectionOcclusionGraph = new SectionOcclusionGraph();
        }
        
        if (this.viewArea == null) {
            Minecraft mc = Minecraft.getInstance();
            Level level = mc.level;
            if (level != null) {
                this.viewArea = new ViewArea(
                    lr.sectionRenderDispatcher,
                    level,
                    this.cameraRenderDistance,
                    lr
                );
            }
        }
        
        if (this.viewArea != null) {
            this.sectionOcclusionGraph.waitAndReset(this.viewArea);
            lr.viewArea = this.viewArea;
        }
        
        lr.sectionOcclusionGraph = this.sectionOcclusionGraph;
        lr.visibleSections = this.visibleSections;
        lr.lastViewDistance = this.lastViewDistance;
        lr.lastCameraSectionX = this.lastCameraSectionX;
        lr.lastCameraSectionY = this.lastCameraSectionY;
        lr.lastCameraSectionZ = this.lastCameraSectionZ;
        lr.prevCamX = this.prevCamX;
        lr.prevCamY = this.prevCamY;
        lr.prevCamZ = this.prevCamZ;
        lr.prevCamRotX = this.prevCamRotX;
        lr.prevCamRotY = this.prevCamRotY;
    }

    public SectionOcclusionGraph getOcclusionGraph() {
        return sectionOcclusionGraph;
    }
    
    @Nullable
    public ViewArea getViewArea() {
        return viewArea;
    }
    
    public void setCameraRenderDistance(int distance) {
        this.cameraRenderDistance = distance;
    }
    
    public int getCameraRenderDistance() {
        return cameraRenderDistance;
    }
    
    public void repositionCamera(double x, double z) {
        if (this.viewArea != null) {
            this.viewArea.repositionCamera(x, z);
        }
    }
    
    public void releaseResources() {
        if (this.viewArea != null) {
            this.viewArea.releaseAllBuffers();
            this.viewArea = null;
        }
    }

}
