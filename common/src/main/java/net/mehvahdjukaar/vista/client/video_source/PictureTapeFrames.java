package net.mehvahdjukaar.vista.client.video_source;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side registry resolving a picture-tape entry to the texture used when playing it on a TV.
 * The vanilla map provider is built in; integrations register their own (gated behind their
 * mod-compat check). First match wins.
 */
public class PictureTapeFrames {

    public interface Provider {
        boolean matches(ItemStack stack);

        @Nullable
        ResourceLocation getTexture(ItemStack stack);
    }

    private static final List<Provider> PROVIDERS = new ArrayList<>();

    static {
        register(new MapFrameTextureProvider());
        register(new PaintingFrameTextureProvider());
    }

    public static void register(Provider provider) {
        PROVIDERS.add(provider);
    }

    @Nullable
    public static ResourceLocation getFrameTexture(ItemStack stack) {
        for (Provider provider : PROVIDERS) {
            if (provider.matches(stack)) return provider.getTexture(stack);
        }
        return null;
    }
}
