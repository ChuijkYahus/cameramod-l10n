package net.mehvahdjukaar.vista.client;

import net.mehvahdjukaar.moonlight.api.client.PostShadersHelper;
import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.supplementaries.client.cannon.CannonController;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

//TODO: merge events with supp cannon
public class ViewFinderController {

    private static final PostShadersHelper.Group LENSES_GROUP = new PostShadersHelper.Group(
            VistaMod.res("lenses"), 20
    );

    protected static ViewFinderBlockEntity viewFinder;

    private static CameraType lastCameraType;

    // values controlled by player mouse movement. Not actually what camera uses
    private static float accumulatedYaw;
    private static float accumulatedPitch;

    private static boolean needsToUpdateServer;

    //truth for client view finder zoom. essentially ignore what the sever has so we have full authority and not jerkyness
    private static int viewFinderZoom;

    // lerp camera
    private static float lastCameraYaw = 0;
    private static float lastCameraPitch = 0;

    public static void startControlling(ViewFinderAccess cannonAccess) {
        Minecraft mc = Minecraft.getInstance();
        if (viewFinder == null) {
            viewFinder = cannonAccess;
            lastCameraType = mc.options.getCameraType();
        } //if not it means we entered from manouver mode gui
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        MutableComponent message = Component.translatable("message.vista.viewfinder.control",
                mc.options.keyShift.getTranslatedKeyMessage(),
                mc.options.keyAttack.getTranslatedKeyMessage());
        mc.gui.setOverlayMessage(message, false);
        mc.getNarrator().sayNow(message);

        Camera camera = mc.gameRenderer.getMainCamera();
        ViewFinderBlockEntity tile = viewFinder.getInternalTile();
        viewFinderZoom = tile.getZoomLevel();
        camera.setRotation(Mth.wrapDegrees(tile.getYaw()), Mth.wrapDegrees(tile.getPitch()));
    }

    // only works if we are already controlling
    private static void stopControllingAndSync() {
        if (viewFinder == null) return;
        viewFinder.syncToServer(true);
        stopControlling();
    }

    public static void stopControlling() {
        if (viewFinder == null) return;
        viewFinder = null;
        lastCameraYaw = 0;
        lastCameraPitch = 0;
        if (lastCameraType != null) {
            Minecraft.getInstance().options.setCameraType(lastCameraType);
        }
    }

    public static ViewFinderAccess getViewFinder() {
        return viewFinder;
    }

    public static boolean isActive() {
        return viewFinder != null;
    }

    public static boolean isActiveFor(BlockEntity tile) {
        return viewFinder != null && viewFinder.getInternalTile() == tile;
    }

    public static boolean isActiveAt(BlockPos pos) {
        return viewFinder != null && viewFinder.getInternalTile().getBlockPos() == pos;
    }

    public static boolean isLocked() {
        return viewFinder.getInternalTile().isLocked();
    }

    public static boolean setupCamera(Camera camera, BlockGetter level, Entity entity,
                                      boolean detached, boolean thirdPersonReverse, float partialTick) {

        //TODO: improve and simplify
        if (!isActive()) return false;
        Vec3 centerCannonPos = viewFinder.getGlobalPosition(partialTick);


        // lerp camera
        Vec3 targetCameraPos = centerCannonPos.add(0, 0.5, 0);
        float targetYRot = camera.getYRot() + accumulatedYaw;
        float targetXRot = Mth.clamp(camera.getXRot() + accumulatedPitch, -90, 90);

        camera.setPosition(targetCameraPos);
        camera.setRotation(targetYRot, targetXRot);

        lastCameraYaw = camera.getYRot();
        lastCameraPitch = camera.getXRot();

        accumulatedYaw = 0;
        accumulatedPitch = 0;

        float followSpeed = 1;
        ViewFinderBlockEntity tile = viewFinder.getInternalTile();

        tile.setPitch(viewFinder, Mth.rotLerp(followSpeed, tile.getPitch(), lastCameraPitch));
        // targetYawDeg = Mth.rotLerp(followSpeed, cannon.getYaw(0), targetYawDeg);
        tile.setRenderYaw(viewFinder, lastCameraYaw + viewFinder.getViewFinderGlobalYawOffset(partialTick));

        return true;
    }
    // true cancels the thing

    @EventCalled
    public static boolean onPlayerRotated(double yawAdd, double pitchAdd) {
        if (isActive()) {
            if (isLocked()) return true;
            float scale = 0.2f * (1 - viewFinder.getInternalTile().getNormalizedZoomFactor() + 0.01f);
            accumulatedYaw += (float) (yawAdd * scale);
            accumulatedPitch += (float) (pitchAdd * scale);
            if (yawAdd != 0 || pitchAdd != 0) needsToUpdateServer = true;

            if (viewFinder.shouldRotatePlayerFaceWhenManeuvering()) {
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

    @EventCalled
    public static boolean onMouseScrolled(double scrollDelta) {
        if (!isActive()) return false;
        if (isLocked()) return true;

        if (scrollDelta != 0) {
            ViewFinderBlockEntity tile = viewFinder.getInternalTile();
            int newZoom = (Math.clamp((int) (tile.getZoomLevel() + scrollDelta), 1, ViewFinderBlockEntity.MAX_ZOOM));
            int oldZoom = tile.getZoomLevel();
            if (newZoom != oldZoom) {
                viewFinderZoom = newZoom;
                needsToUpdateServer = true;
                if (newZoom % 4 == 0)
                    //TODO: proper sound here
                    Minecraft.getInstance().getSoundManager()
                            .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 3));
            }
        }
        return true;
    }

    private static void toggleLock() {
        ViewFinderBlockEntity tile = viewFinder.getInternalTile();
        tile.setLocked(!tile.isLocked());

        needsToUpdateServer = true;

    }

    @EventCalled
    public static boolean onPlayerAttack() {
        if (!isActive()) return false;
        toggleLock();
        return true;
    }

    @EventCalled
    public static boolean onPlayerUse() {
        if (!isActive()) return false;
        toggleLock();
        return true;
    }

    @EventCalled
    public static void onInputUpdate(Input input) {
        // resets input
        if (viewFinder.impedePlayerMovementWhenManeuvering()) {
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

    @EventCalled
    public static void onClientTick(Minecraft mc) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        setupPostShaders();
        if (!isActive()) return;
        if (viewFinder.stillValid(player)) {
            //keep setting zoom so we avoid jitter
            viewFinder.getInternalTile().setZoomLevel(viewFinderZoom);
            if (needsToUpdateServer) {
                needsToUpdateServer = false;
                viewFinder.syncToServer(false);
            }
        } else {
            stopControllingAndSync();
        }
    }

    private static void setupPostShaders() {
        if (isActive()) {
            ResourceLocation lensShader = viewFinder.getInternalTile().getLensShader();
            PostShadersHelper.toggleEffect(lensShader, LENSES_GROUP);
        } else {
            PostShadersHelper.toggleEffect(null, LENSES_GROUP);
        }
    }

    //called by mixin. its cancellable. maybe switch all to this
    @EventCalled
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
            ViewFinderBlockEntity tile = viewFinder.getInternalTile();
            return tile.getZoomLevel() > 1;
        }
        return false;
    }


    @EventCalled
    public static float modifyFOV(float start, float modified, Player player) {
        if (isActive()) {
            return viewFinder.getInternalTile().getFOVModifier();
        }
        return modified;
    }
}

