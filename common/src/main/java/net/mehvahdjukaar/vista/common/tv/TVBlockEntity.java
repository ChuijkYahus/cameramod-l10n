package net.mehvahdjukaar.vista.common.tv;

import net.mehvahdjukaar.moonlight.api.block.ItemDisplayTile;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.cassette.CassetteTape;
import net.mehvahdjukaar.vista.common.LiveFeedConnectionManager;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.mehvahdjukaar.vista.integration.exposure.ExposureCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class TVBlockEntity extends ItemDisplayTile {

    public static final int SWITCH_ON_ANIMATION_TICKS = 3;
    public static final int SWITCH_OFF_ANIMATION_TICKS = 10;
    private static final int MAX_LOOKED_ENDERMAN = 20;
    private static final float ENDERMAN_PLAYER_DIST_SQ = 20 * 20;

    @Nullable
    private UUID linkedFeedUuid = null;
    @Nullable
    private Holder<CassetteTape> tape = null;

    private int connectedTvHeight = 1;
    private int connectedTvsWidth = 1;

    private int soundLoopTicks = 0;
    private int animationTicks = 0;
    private int switchAnimationTicks = 0;

    private boolean lookingAtEnderman = false;
    private int lookingAtEndermanAnimation = 0;
    private int prevLookingAtEndermanAnimation = 0;

    private boolean wasScreenOn = false;


    public TVBlockEntity(BlockPos pos, BlockState state) {
        super(VistaMod.TV_TILE.get(), pos, state);
    }

    public float getLookingAtEndermanAnimation(float partialTicks) {
        return Mth.lerp(partialTicks, prevLookingAtEndermanAnimation, lookingAtEndermanAnimation)
                / MAX_LOOKED_ENDERMAN;
    }

    @Override
    public void saveAdditional(CompoundTag compound, HolderLookup.Provider registries) {
        super.saveAdditional(compound, registries);
        compound.putInt("ConnectionWidth", connectedTvsWidth);
        compound.putInt("ConnectionHeight", connectedTvHeight);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.connectedTvsWidth = Math.max(1, tag.getInt("ConnectionWidth"));
        this.connectedTvHeight = Math.max(1, tag.getInt("ConnectionHeight"));
        cacheState(); //no called by update client state on first load since level is null..
    }


    public int getConnectedHeight() {
        return connectedTvHeight;
    }

    public  int getConnectedWidth() {
        return connectedTvsWidth;
    }

    public void setConnectionSize(int width, int height) {
        this.connectedTvsWidth = width;
        this.connectedTvHeight = height;
    }

    @Nullable
    public UUID getLinkedFeedUUID() {
        return linkedFeedUuid;
    }

    @Nullable
    public Holder<CassetteTape> getTape() {
        return tape;
    }

    @Override
    public SoundEvent getAddItemSound() {
        return VistaMod.CASSETTE_INSERT_SOUND.get();
    }

    @Override
    protected Component getDefaultName() {
        return Component.literal("tv");
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return stack.is(VistaMod.CASSETTE.get()) || stack.is(VistaMod.HOLLOW_CASSETTE.get()) ||
                (CompatHandler.EXPOSURE && ExposureCompat.isPictureItem(stack));
    }
    //TODO: is this needed? put in renderer?


    private void cacheState() {
        ItemStack displayedItem = this.getDisplayedItem();
        linkedFeedUuid = displayedItem.get(VistaMod.LINKED_FEED_COMPONENT.get());
        tape = displayedItem.get(VistaMod.CASSETTE_TAPE_COMPONENT.get());
    }

    @Override
    public void updateTileOnInventoryChanged() {
        super.updateTileOnInventoryChanged();
        cacheState();
    }

    @Override
    public void updateClientVisualsOnLoad() {
        super.updateClientVisualsOnLoad();
        cacheState();
        this.animationTicks = 0;
    }

    public ItemInteractionResult interactWithPlayerItem(Player player, InteractionHand handIn, ItemStack stack, int slot,
                                                        BlockHitResult hit) {

        ItemStack current = this.getDisplayedItem();
        if (!current.isEmpty()) {
            level.playSound(player, worldPosition, VistaMod.CASSETTE_EJECT_SOUND.get(),
                    SoundSource.BLOCKS, 1, 1);
            //pop current
            Vec3 vec3 = hit.getLocation().add(new Vec3(hit.getDirection().step().mul(0.05f)));

            vec3 = vec3.offsetRandom(this.level.random, 0.7F);

            ItemStack itemStack2 = current.copy();
            ItemEntity itemEntity = new ItemEntity(this.level, vec3.x(), vec3.y(), vec3.z(), itemStack2);
            itemEntity.setDefaultPickUpDelay();
            this.level.addFreshEntity(itemEntity);
            this.clearContent();
            this.setChanged();
            return ItemInteractionResult.sidedSuccess(this.level.isClientSide);
        }

        return super.interactWithPlayerItem(player, handIn, stack, slot);
    }

    public int getAnimationTick() {
        return animationTicks;
    }

    public int getSwitchAnimationTicks() {
        //also detect state change here since this gets called in render tick
        return switchAnimationTicks;
    }

    public static void onTick(Level level, BlockPos pos, BlockState state, TVBlockEntity tile) {
        boolean powered = state.getValue(TVBlock.POWER_STATE).isOn();
        if (level.isClientSide) {
            boolean changedState = false;
            if (powered != tile.wasScreenOn) {
                changedState = true && ClientConfigs.TURN_OFF_EFFECTS.get();
                tile.wasScreenOn = powered;
            }

            if (powered) {
                //we cant switch power anim here as its too late

                if (changedState) {
                    tile.switchAnimationTicks = SWITCH_ON_ANIMATION_TICKS;
                } else if (tile.switchAnimationTicks > 0) {
                    tile.switchAnimationTicks--;
                }
                float duration = tile.getPlayDuration();
                if (++tile.soundLoopTicks >= (duration)) {
                    tile.soundLoopTicks = 0;
                    SoundEvent sound = tile.getPlaySound();
                    level.playLocalSound(pos, sound, SoundSource.BLOCKS, 1, 1.0f, false);
                }
                tile.animationTicks++;
            } else {
                if (changedState) {
                    tile.switchAnimationTicks = -SWITCH_OFF_ANIMATION_TICKS;
                } else if (tile.switchAnimationTicks < 0) {
                    tile.switchAnimationTicks++;
                }
                tile.soundLoopTicks = 0;
                tile.animationTicks = 0;
            }
            tile.prevLookingAtEndermanAnimation = tile.lookingAtEndermanAnimation;
            if (tile.lookingAtEnderman) {
                tile.lookingAtEndermanAnimation = Math.min(MAX_LOOKED_ENDERMAN, tile.lookingAtEndermanAnimation + 1);
            } else {
                tile.lookingAtEndermanAnimation = Math.max(0, tile.lookingAtEndermanAnimation - 1);
            }


        } else {
            if ((level.getGameTime() + pos.asLong()) % 27 == 0) {
                return;
            }
            boolean canSeeEnderman = false;
            if (powered && tile.linkedFeedUuid != null) {
                //server tick logic
                ViewFinderBlockEntity viewFinder = LiveFeedConnectionManager.findLinkedViewFinder(level, tile.linkedFeedUuid);
                if (viewFinder != null) { //stagger updates since this is expensive
                    var doomScrollingPlayers = tile.getPlayersLookingAtFace(level.players());

                    if (viewFinder.angerEndermenBeingLookedAt(doomScrollingPlayers, 32, tile)) {
                        canSeeEnderman = true;
                    }
                }
            }
            boolean couldSeeEnderman = tile.lookingAtEnderman;
            //if changed send block event
            if (canSeeEnderman != couldSeeEnderman) {
                tile.lookingAtEnderman = canSeeEnderman;
                level.blockEvent(pos, state.getBlock(), 1, canSeeEnderman ? 1 : 0);
            }
        }
    }

    private SoundEvent getPlaySound() {
        if (tape != null) {
            var s = tape.value().soundEvent();
            if (s.isPresent()) return s.get().value();
        }
        return VistaMod.TV_STATIC_SOUND.get();
    }

    private int getPlayDuration() {
        if (tape != null) {
            return tape.value().soundDuration().orElse(VistaMod.STATIC_SOUND_DURATION);
        }
        return VistaMod.STATIC_SOUND_DURATION;
    }

    public List<TVSpectatorView> getPlayersLookingAtFace(Collection<? extends Player> players) {
        if (players.isEmpty()) return List.of();
        List<TVSpectatorView> result = new ArrayList<>();

        BlockState state = this.getBlockState();
        Direction facing = state.getValue(TVBlock.FACING);
        float screenW = getScreenPixelWidth() / 16f;
        float screenH = getScreenPixelHeight() / 16f;
        // Screen center: block center offset half a block in facing direction
        Vec2 relativeCenter = this.getScreenBlockCenter();
        Vec3 screenCenterPos = this.worldPosition.getCenter()
                .add(MthUtils.rotateVec3(new Vec3(relativeCenter.x, relativeCenter.y, 0.5), facing.getOpposite()));

        // Screen normal (points outward from screen). facing is horizontal => normal.y == 0
        Vec3 screenNormal = new Vec3(facing.step()).normalize();

        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = screenNormal.cross(up);

        for (Player player : players) {
            TVSpectatorView viewResult = getPlayerHit(player, screenCenterPos, screenNormal, right, up, screenW, screenH);
            if (viewResult != null) result.add(viewResult);
        }

        return result;
    }

    @Nullable
    public TVSpectatorView getPlayerLookingAtFace(Player player) {
        return getPlayersLookingAtFace(List.of(player)).stream().findFirst().orElse(null);
    }

    @Nullable
    private static TVSpectatorView getPlayerHit(Player player, Vec3 screenCenterPos, Vec3 screenNormal,
                                                Vec3 normalRight, Vec3 normalUp, float sideWidth, float sideHeight) {

        final double EPS = 1e-6;
        Vec3 eyePos = player.getEyePosition(1.0F);

        // 1) radius check (cheap, no sqrt)
        Vec3 eyeToCenter = screenCenterPos.subtract(eyePos);
        double distSq = eyeToCenter.lengthSqr();
        if (distSq > ENDERMAN_PLAYER_DIST_SQ) return null;
        eyeToCenter = eyeToCenter.scale(1.0 / Math.sqrt(distSq)); // normalize

        // 2) half-circle in front check (cheap): player must be in front half-space of screen
        double frontDot = eyeToCenter.dot(screenNormal);
        if (frontDot > 0.0) return null;

        // 3) get view vector (normalize for stable intersection math)
        Vec3 playerView = player.getViewVector(1.0F).normalize();

        // 4) ray-plane intersection
        double denom = playerView.dot(screenNormal);
        if (Math.abs(denom) < EPS) return null;

        double t = screenCenterPos.subtract(eyePos).dot(screenNormal) / denom;
        if (t <= 0.0) return null;

        Vec3 hit = eyePos.add(playerView.scale(t));

        // 5) local screen coordinates of hit relative to center
        Vec3 local = hit.subtract(screenCenterPos);
        double x = local.dot(normalRight);
        double y = local.dot(normalUp);

        // 6) bounds check (screen centered at screenCenterPos)
        if (Math.abs(x) <= (sideWidth / 2f) && Math.abs(y) <= (sideHeight / 2f)) {
            // distance = t (distance from eye to hit along ray)
            return new TVSpectatorView(player, new Vec2((float) x / sideWidth, (float) y / sideHeight), t);
        }
        return null;
    }

    public void updateEndermanLookAnimation(int param) {
        this.lookingAtEnderman = param > 0;
    }

    public boolean isScreenOn() {
        return this.wasScreenOn || this.switchAnimationTicks != 0;
    }

    private static final int EDGE_PIXEL_LEN = 4;
    public static final int MIN_SCREEN_PIXEL_SIZE = 16 - EDGE_PIXEL_LEN;

    public Vec2 getScreenBlockCenter() {
        return new Vec2(0.5f * (connectedTvsWidth - 1), 0.5f * (connectedTvHeight - 1));
    }

    public int getScreenPixelWidth() {
        return Math.max(1, connectedTvsWidth) * 16 - EDGE_PIXEL_LEN;
    }

    public int getScreenPixelHeight() {
        return Math.max(1, connectedTvHeight) * 16 - EDGE_PIXEL_LEN;
    }
}
