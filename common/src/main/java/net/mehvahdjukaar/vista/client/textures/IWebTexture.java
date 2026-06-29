package net.mehvahdjukaar.vista.client.textures;

import net.mehvahdjukaar.vista.client.web.IMediaSession;
import net.mehvahdjukaar.vista.client.web.MediaError;
import net.mehvahdjukaar.vista.client.web.MediaStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public interface IWebTexture extends AutoCloseable {

    ResourceLocation getTextureLocation();

    default void register() {
        Minecraft.getInstance().getTextureManager().register(this.getTextureLocation(), (AbstractTexture) this);
    }

    default void unregister() {
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        AbstractTexture texture = tm.getTexture(this.getTextureLocation());
        if (texture == this) {
            tm.release(this.getTextureLocation());
        }
    }

    IMediaSession getSession();

    MediaStatus uploadFrameAtTime(int ticks, float deltaTime, boolean paused);

    default int getDownloadProgress() {
        return getSession().getDownloadProgress();
    }

    default MediaError getError() {
        return getSession().getError();
    }

    default boolean isRetrying() {
        return getSession().isRetrying();
    }
}
