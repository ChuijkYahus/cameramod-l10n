package net.mehvahdjukaar.vista.client.textures;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CassetteTexturesManager extends SimplePreparableReloadListener<Map<ResourceLocation, ResourceLocation>> {
    public static final CassetteTexturesManager INSTANCE = new CassetteTexturesManager("textures/cassette_tape");

    private final String directory;
    private final Map<ResourceLocation, SimpleAnimatedTexture> knowAnimations = new HashMap<>();

    protected CassetteTexturesManager(String directory) {
        this.directory = directory;
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
    protected void apply(Map<ResourceLocation, ResourceLocation> object, ResourceManager resourceManager, ProfilerFiller profiler) {
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

    @Nullable
    public SimpleAnimatedTexture getAnimatedTexture(ResourceLocation id) {
        return knowAnimations.get(id);
    }
}

