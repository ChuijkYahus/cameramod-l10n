package net.mehvahdjukaar.vista.client;

import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.Util;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Unique;

import java.lang.reflect.Constructor;

public class ClientChunkStuffHelper {

    @Unique
    private static final Constructor<SectionOcclusionGraph.Node> NODE_CONSTRUCTOR = Util.make(() -> {
        try {
            Class<?> nodeClass = Class.forName("net.minecraft.client.renderer.SectionOcclusionGraph$Node");
            var ctor = (Constructor<SectionOcclusionGraph.Node>)
                    nodeClass.getDeclaredConstructor(
                            SectionRenderDispatcher.RenderSection.class, Direction.class, int.class);
            ctor.setAccessible(true);
            return ctor;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });


    @Unique
    private static final Constructor<SectionRenderDispatcher.RenderSection> RENDER_SECTION_CONSTRUCTOR = Util.make(() -> {
        try {
            Class<?> clazz = Class.forName("net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection");
            var ctor = (Constructor<SectionRenderDispatcher.RenderSection>) clazz.getDeclaredConstructor(
                    SectionRenderDispatcher.class, int.class, int.class, int.class, int.class);
            ctor.setAccessible(true);
            return ctor;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    public static SectionOcclusionGraph.Node createNode(SectionRenderDispatcher.RenderSection section) {
        try {
            return NODE_CONSTRUCTOR.newInstance(section, null, 0);
        } catch (ReflectiveOperationException e) {
            VistaMod.LOGGER.error("Could not create SectionOcclusionGraph.Node for pinned section", e);
            return null;
        }
    }


    public static SectionRenderDispatcher.RenderSection createRenderSection(SectionRenderDispatcher dispatcher, int newIndex, int blockX, int yOrigin, int blockZ) {

        try {
            return RENDER_SECTION_CONSTRUCTOR.newInstance(dispatcher, newIndex,blockX,yOrigin, blockZ);
        } catch (ReflectiveOperationException e) {
            VistaMod.LOGGER.error("Could not create SectionOcclusionGraph.Node for pinned section", e);
            return null;
        }
    }

}
