package net.mehvahdjukaar.vista.common;

import net.mehvahdjukaar.moonlight.api.block.ItemDisplayTile;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.integration.exposure.ExposureCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TVBlockEntity extends ItemDisplayTile {

    public boolean canSeeEnderman;
    @Nullable
    private UUID linkedFeedUuid = null;
    @Nullable
    private Holder<CassetteTape> tape = null;

    private int connectedTvsSize = 1;

    private int soundLoopTicks = 0;
    private int animationTicks = 0;

    public TVBlockEntity(BlockPos pos, BlockState state) {
        super(VistaMod.TV_TILE.get(), pos, state);
    }

    @Override
    public void saveAdditional(CompoundTag compound, HolderLookup.Provider registries) {
        super.saveAdditional(compound, registries);
        compound.putInt("ConnectedTVsSize", connectedTvsSize);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.connectedTvsSize = tag.getInt("ConnectedTVsSize");
    }

    @Nullable
    public UUID getLinkedFeedUUID() {
        return linkedFeedUuid;
    }

    @Nullable
    public Holder<CassetteTape> getTape() {
        return tape;
    }

    public int getScreenPixelSize() {
        return 12;
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
                (VistaMod.EXPOSURE_ON && ExposureCompat.isPictureItem(stack));
    }
    //TODO: is this needed? put in renderer?

    private void cacheClientState() {
        ItemStack displayedItem = this.getDisplayedItem();
        linkedFeedUuid = displayedItem.get(VistaMod.LINKED_FEED_COMPONENT.get());
        tape = displayedItem.get(VistaMod.CASSETTE_TAPE_COMPONENT.get());
    }

    @Override
    public void updateClientVisualsOnLoad() {
        super.updateClientVisualsOnLoad();
        cacheClientState();
    }

    @Override
    public ItemInteractionResult interactWithPlayerItem(Player player, InteractionHand handIn, ItemStack stack, int slot) {

        ItemStack current = this.getDisplayedItem();
        if (!current.isEmpty()) {
            level.playSound(player, worldPosition, VistaMod.CASSETTE_EJECT_SOUND.get(),
                    SoundSource.BLOCKS, 1, 1);
            //pop pop current
            Vec3 vec3 = level.getBlockState(this.worldPosition.above()).isSolid() ?
                    this.worldPosition.relative(getBlockState().getValue(TVBlock.FACING)).getCenter() :
                    Vec3.atLowerCornerWithOffset(this.worldPosition, 0.5, 1.05, 0.5);

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

    public boolean hasVideo() {
        return linkedFeedUuid != null;
    }

    public static void onTick(Level level, BlockPos pos, BlockState state, TVBlockEntity tile) {
        boolean powered = state.getValue(TVBlock.POWERED);
        if (level.isClientSide) {
            if (powered) {
                float duration = tile.getPlayDuration();
                if (++tile.soundLoopTicks >= (duration)) {
                    tile.soundLoopTicks = 0;
                    SoundEvent sound = tile.getPlaySound();
                    level.playLocalSound(pos, sound, SoundSource.BLOCKS, 1, 1.0f, false);
                }
                tile.animationTicks++;
            } else {
                tile.soundLoopTicks = 0;
                tile.animationTicks = 0;
            }
        }
        if (!level.isClientSide) return;
        if (powered && tile.linkedFeedUuid != null) {
            ViewFinderBlockEntity viewFinder = LiveFeedConnectionManager.findLinkedViewFinder(level, tile.linkedFeedUuid);
            if (viewFinder != null && (level.getGameTime() + pos.asLong()) % 1 == 0) { //stagger updates since this is expensive
                tile.canSeeEnderman = false;
                Direction facing = state.getValue(TVBlock.FACING);
                float screenL = 12;
                var doomScrollingPlayers = getPlayersFacingLookingAtFace(level, pos, facing,
                        screenL / 16f, 20); //TODO: adjust for big tvs

                if (viewFinder.angerEnderMen(doomScrollingPlayers, 32,
                        screenL/16f, screenL/16f)) {
                    tile.canSeeEnderman = true;
                    //static
                }

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

    public int getAnimationTick() {
        return animationTicks;
    }


    private static List<TvViewHitResult> getPlayersFacingLookingAtFace(Level level,
                                                                       BlockPos pos,
                                                                       Direction facing,
                                                                       float screenSideLength,
                                                                       float radiusFromCenter) {
        List<? extends Player> allPlayers = level.players();
        if (allPlayers.isEmpty()) return List.of();
        List<TvViewHitResult> result = new ArrayList<>();

        // Screen center: block center offset half a block in facing direction
        Vec3 screenCenterPos = Vec3.atCenterOf(pos).relative(facing, 0.5);
        // Screen normal (points outward from screen). facing is horizontal => normal.y == 0
        Vec3 screenNormal = new Vec3(facing.step()).normalize();

        double radiusSq = (double) radiusFromCenter * (double) radiusFromCenter;
        double halfSide = (double) screenSideLength * 0.5;

        Vec3 up = new Vec3(0, 1, 0);

        // right = normal cross up -> points along screen's local +X
        Vec3 rawRight = screenNormal.cross(up);
        final double EPS = 1e-6;
        Vec3 right;
        double rawRightLen = rawRight.length();
        if (rawRightLen < EPS) {
            // extremely unlikely because facing is horizontal, but keep safe fallback
            right = new Vec3(1, 0, 0);
        } else {
            right = rawRight.scale(1.0 / rawRightLen); // normalize
        }

        // up axis should be orthonormal with right and normal
        Vec3 localUp = right.cross(screenNormal).normalize();

        for (Player p : allPlayers) {
            Vec3 eyePos = p.getEyePosition(1.0F);

            // 1) radius check (cheap, no sqrt)
            Vec3 eyeToCenter = screenCenterPos.subtract(eyePos);
            double distSq = eyeToCenter.lengthSqr();
            if (distSq > radiusSq) continue;
            eyeToCenter = eyeToCenter.scale(1.0 / Math.sqrt(distSq)); // normalize

            // 2) half-circle in front check (cheap): player must be in front half-space of screen
            double frontDot = eyeToCenter.dot(screenNormal);
            if (frontDot > 0.0) continue;

            // 3) get view vector (normalize for stable intersection math)
            Vec3 playerView = p.getViewVector(1.0F).normalize();

            // 4) ray-plane intersection
            double denom = playerView.dot(screenNormal);
            if (Math.abs(denom) < EPS) continue; // nearly parallel -> won't hit reliably

            double t = screenCenterPos.subtract(eyePos).dot(screenNormal) / denom;
            if (t <= 0.0) continue; // intersection is behind the eye

            Vec3 hit = eyePos.add(playerView.scale(t));

            // 5) local screen coordinates of hit relative to center
            Vec3 local = hit.subtract(screenCenterPos);
            double x = local.dot(right);
            double y = local.dot(localUp);

            // 6) bounds check (screen centered at screenCenterPos)
            if (Math.abs(x) <= halfSide && Math.abs(y) <= halfSide) {
                // distance = t (distance from eye to hit along ray)
                result.add(new TvViewHitResult(p, new Vec2((float) x, (float) y), t));
            }
        }

        return result;
    }

}
