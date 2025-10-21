package net.mehvahdjukaar.vista.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.vista.common.ViewFinderAccess;
import net.mehvahdjukaar.vista.common.ViewFinderBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

//TODO: merge events with supp cannon
public class ViewFinderController {

    public static final int MAX_ZOOM = 44;

    protected static ViewFinderAccess access;

    private static CameraType lastCameraType;

    protected static boolean isLocked = false;

    // values controlled by player mouse movement. Not actually what camera uses
    private static float yawIncrease;
    private static float pitchIncrease;

    private static boolean needsToUpdateServer;

    // lerp camera
    private static Vec3 lastCameraPos;
    private static float lastZoomOut = 0;
    private static float lastCameraYaw = 0;
    private static float lastCameraPitch = 0;

    public static void startControlling(ViewFinderAccess cannonAccess) {
        Minecraft mc = Minecraft.getInstance();
        if (access == null) {
            access = cannonAccess;
            lastCameraType = mc.options.getCameraType();
        } //if not it means we entered from manouver mode gui
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        MutableComponent message = Component.translatable("message.vista.viewfinder_control",
                mc.options.keyShift.getTranslatedKeyMessage(),
                mc.options.keyAttack.getTranslatedKeyMessage());
        mc.gui.setOverlayMessage(message, false);
        mc.getNarrator().sayNow(message);
    }

    // only works if we are already controlling
    private static void stopControllingAndSync() {
        if (access == null) return;
        access.syncToServer(true);
        stopControlling();
    }

    public static void stopControlling() {
        if (access == null) return;
        access = null;
        lastCameraYaw = 0;
        lastCameraPitch = 0;
        lastZoomOut = 0;
        lastCameraPos = null;
        if (lastCameraType != null) {
            Minecraft.getInstance().options.setCameraType(lastCameraType);
        }
    }

    public static boolean isActive() {
        return access != null;
    }

    public static boolean setupCamera(Camera camera, BlockGetter level, Entity entity,
                                      boolean detached, boolean thirdPersonReverse, float partialTick) {

        if (!isActive()) return false;
        Vec3 centerCannonPos = access.getCannonGlobalPosition(partialTick);

        if (lastCameraPos == null) {
            lastCameraPos = camera.getPosition();
            lastCameraYaw = camera.getYRot();
            lastCameraPitch = camera.getXRot();
        }

        // lerp camera
        Vec3 targetCameraPos = centerCannonPos.add(0, 0.5, 0);
        float targetYRot = camera.getYRot() + yawIncrease;
        float targetXRot = Mth.clamp(camera.getXRot() + pitchIncrease, -90, 90);

        camera.setPosition(targetCameraPos);
        camera.setRotation(targetYRot, targetXRot);

        lastCameraPos = camera.getPosition();
        lastCameraYaw = camera.getYRot();
        lastCameraPitch = camera.getXRot();
        lastZoomOut = camera.getMaxZoom(4);

        yawIncrease = 0;
        pitchIncrease = 0;

        if (!isLocked) {
            updateViewFinderBlockRenderingAngles(partialTick, camera.getYRot(), camera.getXRot());
        }

        return true;
    }

    private static void updateViewFinderBlockRenderingAngles(float partialTick, float cameraYaw, float cameraPitch) {
        float followSpeed = 1;
        ViewFinderBlockEntity cannon = access.getInternalTile();

        cannon.setPitch(access, Mth.rotLerp(followSpeed, cannon.getPitch(), cameraPitch));
        // targetYawDeg = Mth.rotLerp(followSpeed, cannon.getYaw(0), targetYawDeg);
        cannon.setRenderYaw(access, cameraYaw + access.getCannonGlobalYawOffset(partialTick));
    }

    // true cancels the thing
    public static boolean onPlayerRotated(double yawAdd, double pitchAdd) {
        //TODO: lock with restraints here
        if (isActive()) {
            float scale = 0.2f * (1-getNormalizedZoomFactor() + 0.01f);
            yawIncrease += (float) (yawAdd * scale);
            pitchIncrease += (float) (pitchAdd * scale);
            if (yawAdd != 0 || pitchAdd != 0) needsToUpdateServer = true;

            if (access.shouldRotatePlayerFaceWhenManeuvering()) {
                //make player face camera while maneuvering
                LocalPlayer player = Minecraft.getInstance().player;
                player.turn(Mth.wrapDegrees((lastCameraYaw + yawAdd) - player.yHeadRot),
                        Mth.wrapDegrees((lastCameraPitch + pitchAdd) - player.getXRot()));
                player.yHeadRotO = player.yHeadRot;
                player.xRotO = player.getXRot();
            }
            return true;
        }
        return false;
    }


    public static boolean onMouseScrolled(double scrollDelta) {
        if (!isActive()) return false;

        if (scrollDelta != 0) {
            ViewFinderBlockEntity tile = access.getInternalTile();
            int newZoom = (Math.clamp(1,(int) (tile.getZoomLevel() + scrollDelta), MAX_ZOOM));
            tile.setZoomLevel(newZoom);
            needsToUpdateServer = true;
        }
        return true;
    }


    public static void onInputUpdate(Input input) {
        // resets input
        if (access.impedePlayerMovementWhenManeuvering()) {
            input.down = false;
            input.up = false;
            input.left = false;
            input.right = false;
            input.forwardImpulse = 0;
            input.leftImpulse = 0;
        }
        input.shiftKeyDown = false;
        input.jumping = false;
    }

    public static void onClientTick(Minecraft mc) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!isActive()) return;
        if (access.stillValid(player)) {
            if (needsToUpdateServer) {
                needsToUpdateServer = false;
                access.syncToServer(false);
            }
        } else {
            stopControllingAndSync();
        }
    }

    //called by mixin. its cancellable. maybe switch all to this
    public static boolean onEarlyKeyPress(int key, int scanCode, int action, int modifiers) {
        if (!isActive()) return false;
        if (action != GLFW.GLFW_PRESS) return false;
        var options = Minecraft.getInstance().options;
        if (key == 256) {
            stopControllingAndSync();
            return true;
        } else if (options.keyInventory.matches(key, scanCode)) {

            return true;
        }
        if (options.keyJump.matches(key, scanCode)) {

            return true;
        }
        if (options.keyShift.matches(key, scanCode)) {
            stopControllingAndSync();
            return true;
        }
        return false;
    }

    public static boolean isZooming() {
        if (isActive()) {
            ViewFinderBlockEntity tile = access.getInternalTile();
            return tile.getZoomLevel() > 1;
        }
        return false;
    }

    public static float modifyFOV(float startingFov, float modFov, Player player) {
        if (isActive()) {
            float spyglassZoom = 0.1f;
            float maxZoom = spyglassZoom / 5;
            float normalizedZoom = getNormalizedZoomFactor();
            return Mth.lerp(normalizedZoom, 1, maxZoom);
        }
        return modFov;
    }

    private static float getNormalizedZoomFactor() {
        float normalizedZoom = (access.getInternalTile().getZoomLevel() - 1f) / (MAX_ZOOM - 1f);
        normalizedZoom = 1 - ((1 - normalizedZoom) * (1 - normalizedZoom)); //easing
        return normalizedZoom;
    }
}

