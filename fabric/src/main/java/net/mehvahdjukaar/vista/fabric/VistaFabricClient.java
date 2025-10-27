package net.mehvahdjukaar.vista.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.GifPathSpriteSource;
import net.mehvahdjukaar.vista.client.TapeTextureManager;
import net.mehvahdjukaar.vista.client.ViewFinderController;
import net.mehvahdjukaar.vista.client.ViewFinderHud;
import net.mehvahdjukaar.vista.mixins.fabric.SpriteSourcesAccessor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;

public class VistaFabricClient {

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(VistaModClient::onClientTick);
        HudRenderCallback.EVENT.register(VistaFabricClient::onRenderHud);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            VistaModClient.onLevelClose();
        });

        ClientPreAttackCallback.EVENT.register((minecraft, localPlayer, i) -> {
            if (ViewFinderController.onPlayerAttack()) {
                return true;
            }
            return false;
        });


        SpriteSourcesAccessor.getTYPES().put(VistaMod.res("directory_gifs"), GifPathSpriteSource.TYPE);


    }

    private static void onRenderHud(GuiGraphics graphics, DeltaTracker partialTicks) {
        ViewFinderHud.INSTANCE.render(graphics, partialTicks);
    }



}
