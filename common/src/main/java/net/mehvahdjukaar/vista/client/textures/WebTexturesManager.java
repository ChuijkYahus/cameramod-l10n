package net.mehvahdjukaar.vista.client.textures;

import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class WebTexturesManager {

    //loading cache?
    private static final Map<String, WebTexture> webTextures = new HashMap<>();

    private static int texturesCounter = 0;

    public static WebTexture requestWebTexture(String url) {
        return webTextures.computeIfAbsent(url, u
                -> new WebTexture(null, u, createNewFreeLocation())
        );
    }

    private static ResourceLocation createNewFreeLocation() {
        return VistaMod.res("web_feed_" + texturesCounter++);
    }
}
