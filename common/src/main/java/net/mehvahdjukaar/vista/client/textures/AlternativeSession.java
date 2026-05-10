package net.mehvahdjukaar.vista.client.textures;

import net.mehvahdjukaar.vista.client.web.IMediaSession;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class AlternativeSession implements IMediaSession {

    private final Supplier<IMediaSession> fallbackSupplier;
    @Nullable
    private IMediaSession primary;
    @Nullable
    private IMediaSession fallback;

    public AlternativeSession(Supplier<IMediaSession> primary, Supplier<IMediaSession> fallbackSupplier) {
        this.primary = primary.get();
        this.fallbackSupplier = fallbackSupplier;
    }

    private IMediaSession getActiveSession() {
        if (primary == null || primary.isFailed()) {
            if (fallback == null) {
                fallback = fallbackSupplier.get();
                try {
                    primary.close();
                    primary = null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return fallback;
        }
        return primary;
    }

    @Override
    public IWebTexture createTextureView(ResourceLocation resourceLocation) {
        return getActiveSession().createTextureView(resourceLocation);
    }

    @Override
    public boolean shouldRefreshTexture(IWebTexture tt) {
        IMediaSession activeSession = getActiveSession();
        if (activeSession != tt.getSession()) {
            return true;
        }
        return activeSession.shouldRefreshTexture(tt);
    }

    @Override
    public boolean isFailed() {
        return getActiveSession().isFailed();
    }

    @Override
    public void close() throws Exception {
        if (primary != null) primary.close();
        if (fallback != null) fallback.close();
    }
}
