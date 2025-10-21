package net.mehvahdjukaar.vista.mixins.fabric;

import com.google.common.collect.BiMap;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteSources.class)
public class SpriteSourcesAccessor {

    @Accessor("TYPES")
    public static BiMap<ResourceLocation, SpriteSourceType> getTYPES() {
        throw new AssertionError();
    }
}
