package net.mehvahdjukaar.vista.client.textures;

import com.google.common.base.Suppliers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CassetteTexturesManager extends SimplePreparableReloadListener<Map<ResourceLocation, ResourceLocation>> {
    public static final CassetteTexturesManager INSTANCE = new CassetteTexturesManager("textures/cassette_tape");

    private final String directory;
    private final Map<ResourceLocation, Supplier<SimpleAnimatedStripTexture>> knowAnimations = new HashMap<>();

    protected CassetteTexturesManager(String directory) {
        this.directory = directory;
    }

    @Override
    protected Map<ResourceLocation, ResourceLocation> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        //close all
        for (var texture : knowAnimations.entrySet()) {
            Minecraft.getInstance().getTextureManager().release(makeManagerKey(texture.getKey()));
        }

        Map<ResourceLocation, ResourceLocation> map = new HashMap<>();
        FileToIdConverter fileToIdConverter = new FileToIdConverter(directory, ".gif");

        for (Map.Entry<ResourceLocation, Resource> entry : fileToIdConverter.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            ResourceLocation resourceLocation2 = fileToIdConverter.fileToId(resourceLocation);
            map.put(resourceLocation2, resourceLocation);
        }

        fileToIdConverter = new FileToIdConverter(directory, ".png");
        for (Map.Entry<ResourceLocation, Resource> entry : fileToIdConverter.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            ResourceLocation resourceLocation2 = fileToIdConverter.fileToId(resourceLocation);
            map.put(resourceLocation2, resourceLocation);
        }
        return map;
    }

    private @NotNull ResourceLocation makeManagerKey(ResourceLocation id) {
        return id.withPrefix(this.directory + "/");
    }

    @Override
    protected void apply(Map<ResourceLocation, ResourceLocation> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        for (var entry : object.entrySet()) {
            ResourceLocation id = entry.getKey();
            ResourceLocation resLoc = entry.getValue();
            try {
                knowAnimations.put(
                        id, Suppliers.memoize(() -> {
                            SimpleAnimatedStripTexture texture = new SimpleAnimatedStripTexture(resLoc);
                            manager.register(makeManagerKey(id), texture);
                            return texture;
                        })
                );

            } catch (Exception e) {
                throw new RuntimeException("Failed to load gif animation: " + resLoc, e);
            }
        }
    }

    @Nullable
    public SimpleAnimatedStripTexture getAnimatedTexture(ResourceLocation id) {
        return knowAnimations.getOrDefault(id, () -> null).get();
    }
}

