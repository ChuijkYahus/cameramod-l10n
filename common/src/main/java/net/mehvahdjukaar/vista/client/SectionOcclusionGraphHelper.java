package net.mehvahdjukaar.vista.client;

import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.Util;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Unique;

import java.lang.reflect.Constructor;

public class SectionOcclusionGraphHelper {

    @Unique
    private static final Constructor<SectionOcclusionGraph.Node> NODE_CONSTRUCTOR = Util.make(() -> {
        try {
            Class<?> nodeClass = Class.forName("net.minecraft.client.renderer.SectionOcclusionGraph$Node");
            Constructor<SectionOcclusionGraph.Node> ctor = (Constructor<SectionOcclusionGraph.Node>)
                    nodeClass.getDeclaredConstructor(
                            SectionRenderDispatcher.RenderSection.class, Direction.class, int.class);
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


}
