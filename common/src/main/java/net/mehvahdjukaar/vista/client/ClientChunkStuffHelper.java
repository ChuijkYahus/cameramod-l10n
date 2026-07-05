package net.mehvahdjukaar.vista.client;

import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

public class ClientChunkStuffHelper {

    public static SectionOcclusionGraph.Node createNode(SectionRenderDispatcher.RenderSection section) {
        // Constructor widened via vista.accesswidener.
        return new SectionOcclusionGraph.Node(section, null, 0);
    }

    public static SectionRenderDispatcher.RenderSection createRenderSection(SectionRenderDispatcher dispatcher, int newIndex, int blockX, int yOrigin, int blockZ) {
        // RenderSection is a non-static inner class with a public constructor, so it needs
        // the outer-instance qualified `new` syntax rather than reflection.
        return dispatcher.new RenderSection(newIndex, blockX, yOrigin, blockZ);
    }

}
