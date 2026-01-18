package net.mehvahdjukaar.vista.client;

import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

public abstract class GifsManager extends SimplePreparableReloadListener<Map<ResourceLocation,ResourceLocation>> {
    private static GifsManager CURRENT_INSTANCE = null;

    private final Map<ResourceLocation, SimpleAnimatedTexture> knowAnimations = new HashMap<>();

    private final String directory;

    public GifsManager( String directory) {
        this.directory = directory;
        CURRENT_INSTANCE = this;
    }

    @Override
    protected Map<ResourceLocation, ResourceLocation> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, ResourceLocation> map = new HashMap<>();
        FileToIdConverter fileToIdConverter = FileToIdConverter.json(directory);

        for (Map.Entry<ResourceLocation, Resource> entry : fileToIdConverter.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            ResourceLocation resourceLocation2 = fileToIdConverter.fileToId(resourceLocation);
            map.put(resourceLocation2, resourceLocation);
        }
        return map;
    }

    @Override
    protected void apply(Map<ResourceLocation , ResourceLocation> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        for (var entry : object.entrySet()) {
            ResourceLocation id = entry.getKey();
            ResourceLocation resLoc = entry.getValue();
            try {
                SimpleAnimatedTexture texture = new SimpleAnimatedTexture(resLoc);
                manager.register(id, texture);
                knowAnimations.put(id, texture);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load gif animation: " + resLoc, e);
            }
        }
    }
}

