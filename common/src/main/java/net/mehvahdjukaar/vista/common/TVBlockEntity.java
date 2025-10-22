package net.mehvahdjukaar.vista.common;

import net.mehvahdjukaar.moonlight.api.block.ItemDisplayTile;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TVBlockEntity extends ItemDisplayTile {

    @Nullable
    private UUID linkedFeedUuid = null;
    @Nullable
    private Holder<CassetteTape> tape = null;

    private int staticTicks = 0;

    public TVBlockEntity(BlockPos pos, BlockState state) {
        super(VistaMod.TV_TILE.get(), pos, state);
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
        return VistaMod.CASSETTE_INSERT.get();
    }

    @Override
    protected Component getDefaultName() {
        return Component.literal("tv");
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return stack.is(VistaMod.CASSETTE.get()) || stack.is(VistaMod.HOLLOW_CASSETTE.get());
    }

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
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    @Override
    public ItemInteractionResult interactWithPlayerItem(Player player, InteractionHand handIn, ItemStack stack, int slot) {

        ItemStack current = this.getDisplayedItem();
        if (!current.isEmpty()) {
            level.playSound(player, worldPosition, VistaMod.CASSETTE_EJECT.get(),
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

    public static void clientTick(Level level, BlockPos pos, BlockState state, TVBlockEntity tile) {
        if (++tile.staticTicks >= (4 * 20) && state.getValue(TVBlock.POWERED)) {
            tile.staticTicks = 0;
            level.playLocalSound(pos,
                    VistaMod.TV_STATIC.get(), SoundSource.BLOCKS,
                    0.1f, 1.0f, false);
        }
    }
}
