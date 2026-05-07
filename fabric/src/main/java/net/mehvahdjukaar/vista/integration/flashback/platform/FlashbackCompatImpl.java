package net.mehvahdjukaar.vista.integration.flashback.platform;

import com.moulberry.flashback.editor.ui.ReplayUI;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class FlashbackCompatImpl {
    public static Runnable decorateRenderRestoringMatrices(Runnable task) {
        return () -> {
            Matrix4f lastProjectionMatrix = ReplayUI.lastProjectionMatrix;
            Quaternionf lastViewQuaternion = ReplayUI.lastViewQuaternion;
            try {
                task.run();
            } finally {
                ReplayUI.lastProjectionMatrix.set(lastProjectionMatrix);
                ReplayUI.lastViewQuaternion.set(lastViewQuaternion);
            }
        };

    }
}


