package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    // Force the non-fabulous (FANCY) translucency path while rendering our nested level views
    // (mirror reflections / TV feeds). Fabulous's deferred translucency targets + transparency
    // post-chain don't compose correctly into our off-screen canvases, so render-type target
    // binding must fall back to the main (currently bound) target there. Pairs with nulling
    // LevelRenderer.transparencyChain for the same duration (see VistaLevelRenderer#render) —
    // renderLevel branches on that field directly, not on this method.
    @ModifyReturnValue(method = "useShaderTransparency", at = @At("RETURN"))
    private static boolean vista$noFabulousInFeeds(boolean original) {
        return original && !VistaLevelRenderer.isRenderingLiveFeed();
    }
}
