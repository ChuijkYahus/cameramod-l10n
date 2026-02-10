package net.mehvahdjukaar.vista.client;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.minecraft.resources.ResourceLocation;

public enum CrtOverlay {
    NONE(VistaMod.res("missing")),
    PAUSE(VistaModClient.PAUSE_OVERLAY),
    LOADING(VistaModClient.LOADING_OVERLAY),
    DISCONNECT(VistaModClient.DISCONNECT_OVERLAY);

    public final ResourceLocation texture;

    CrtOverlay(ResourceLocation texture) {
        this.texture = texture;
    }
}
