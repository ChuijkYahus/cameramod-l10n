package net.mehvahdjukaar.vista.client.renderer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
    private SectionOcclusionGraph sectionOcclusionGraph;
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections = new ObjectArrayList<>(10000);

    private LevelRendererCameraState() {
    }

    public static LevelRendererCameraState createNew() {
        var instance = new LevelRendererCameraState();
        instance.sectionOcclusionGraph = new SectionOcclusionGraph();
        Minecraft mc = Minecraft.getInstance();
        LevelRenderer lr = mc.levelRenderer;
        instance.viewArea = new ViewArea(lr.sectionRenderDispatcher, mc.level,
                //TODO: change this
                mc.options.getEffectiveRenderDistance(), lr);
        instance.sectionOcclusionGraph.waitAndReset(instance.viewArea);
        return instance;
    }

    public void copyFrom(LevelRenderer lr) {
        this.viewArea = lr.viewArea;
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
        if(this.viewArea == lr.viewArea){
            this.viewArea = new ViewArea(lr.sectionRenderDispatcher, Minecraft.getInstance().level,
                    Minecraft.getInstance().options.getEffectiveRenderDistance(), lr);

        }
        lr.viewArea = this.viewArea;
        if(this.sectionOcclusionGraph == lr.sectionOcclusionGraph){
            this.sectionOcclusionGraph = new SectionOcclusionGraph();
            this.sectionOcclusionGraph.waitAndReset(this.viewArea);
        }
        lr.sectionOcclusionGraph = this.sectionOcclusionGraph;
        if(this.visibleSections == lr.visibleSections){
            this.visibleSections = new ObjectArrayList<>(10000);
        }
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

}
