package net.mehvahdjukaar.vista.common.tv;

import net.mehvahdjukaar.moonlight.api.block.ItemDisplayTile;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.video_source.IVideoSource;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.mehvahdjukaar.vista.integration.exposure.ExposureCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class TVBlockEntity extends ItemDisplayTile {

    public static final int SWITCH_ON_ANIMATION_TICKS = 3;
    public static final int SWITCH_OFF_ANIMATION_TICKS = 10;
    private static final int MAX_LOOKED_ENDERMAN = 20;

    @Nullable
    private TVEndermanObservationController observationController = null;

    //client, I think
    private IVideoSource videoSource = IVideoSource.EMPTY;

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

    public IVideoSource getVideoSource() {
        return videoSource;
    }

    public int getConnectedHeight() {
        return connectedTvHeight;
    }

    public int getConnectedWidth() {
        return connectedTvsWidth;
    }

    public void setConnectionSize(int width, int height) {
        this.connectedTvsWidth = width;
        this.connectedTvHeight = height;
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

        var uuid = displayedItem.get(VistaMod.LINKED_FEED_COMPONENT.get());
        this.observationController = new TVEndermanObservationController(uuid, this);
        this.videoSource = IVideoSource.create(displayedItem);
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

    public static void onTick(Level world, BlockPos pos, BlockState state, TVBlockEntity tv) {
        boolean powered = state.getValue(TVBlock.POWER_STATE).isOn();
        if (world.isClientSide) {
            boolean changedState = false;
            if (powered != tv.wasScreenOn) {
                changedState = true && ClientConfigs.TURN_OFF_EFFECTS.get();
                tv.wasScreenOn = powered;
            }

            if (powered) {
                //we cant switch power anim here as its too late

                if (changedState) {
                    tv.switchAnimationTicks = SWITCH_ON_ANIMATION_TICKS;
                } else if (tv.switchAnimationTicks > 0) {
                    tv.switchAnimationTicks--;
                }
                float duration = tv.videoSource.getVideoDuration();
                if (++tv.soundLoopTicks >= (duration)) {
                    tv.soundLoopTicks = 0;
                    SoundEvent sound = tv.videoSource.getVideoSound();
                    world.playLocalSound(pos, sound, SoundSource.BLOCKS, 1, 1.0f, false);
                }
                tv.animationTicks++;
            } else {
                if (changedState) {
                    tv.switchAnimationTicks = -SWITCH_OFF_ANIMATION_TICKS;
                } else if (tv.switchAnimationTicks < 0) {
                    tv.switchAnimationTicks++;
                }
                tv.soundLoopTicks = 0;
                tv.animationTicks = 0;
            }
            tv.prevLookingAtEndermanAnimation = tv.lookingAtEndermanAnimation;
            if (tv.lookingAtEnderman) {
                tv.lookingAtEndermanAnimation = Math.min(MAX_LOOKED_ENDERMAN, tv.lookingAtEndermanAnimation + 1);
            } else {
                tv.lookingAtEndermanAnimation = Math.max(0, tv.lookingAtEndermanAnimation - 1);
            }


        } else {
            //stagger updates since this is expensive
            if ((world.getGameTime() + pos.asLong()) % 27 == 0) {
                return;
            }
            boolean hasAngeredEntity = false;
            if (powered && tv.observationController != null) {
                //server tick logic
                hasAngeredEntity = tv.observationController.tick();
            }
            boolean couldSeeEnderman = tv.lookingAtEnderman;
            //if changed send block event
            if (hasAngeredEntity != couldSeeEnderman) {
                tv.lookingAtEnderman = hasAngeredEntity;
                world.blockEvent(pos, state.getBlock(), 1, hasAngeredEntity ? 1 : 0);
            }
        }
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
