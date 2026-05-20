package net.mehvahdjukaar.vista.integration.veil;

import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import net.mehvahdjukaar.moonlight.api.misc.TField;

public class VeilCompat {

    private static final TField<VeilLevelPerspectiveRenderer, Boolean> PERSPECTIVE_RENDERER_MAGIC_GLOBAL_STATE =
            TField.of(VeilLevelPerspectiveRenderer.class, "renderingPerspective");

    public static Runnable decorateWithSameDarnHacksVeilUses(Runnable renderTask) {
        return () -> {

            //why is veil not rendering their own lights in their prespective renderer??
            //maybe becasue that code nukes the entire render target somehow
            boolean magicPerspectiveGlobalState = PERSPECTIVE_RENDERER_MAGIC_GLOBAL_STATE.get(null);
            try {
             //   PERSPECTIVE_RENDERER_MAGIC_GLOBAL_STATE.set(null, true);
                renderTask.run();
            } finally {
                PERSPECTIVE_RENDERER_MAGIC_GLOBAL_STATE.set(null, magicPerspectiveGlobalState);
            }
        };
    }
}
