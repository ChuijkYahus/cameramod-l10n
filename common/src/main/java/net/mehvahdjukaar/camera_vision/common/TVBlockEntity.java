package net.mehvahdjukaar.camera_vision.common;

import net.mehvahdjukaar.camera_vision.CameraVision;
import net.mehvahdjukaar.moonlight.api.block.ItemDisplayTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TVBlockEntity extends ItemDisplayTile {

    @Nullable
    private UUID linkedFeedUuid = null;

    public TVBlockEntity(BlockPos pos, BlockState state) {
        super(CameraVision.TV_TILE.get(), pos, state);
    }

    @Nullable
    public UUID getLinkedFeedUUID() {
        return linkedFeedUuid;
    }

    public int getScreenPixelSize() {
        return 12;
    }

    @Override
    protected Component getDefaultName() {
        return Component.literal("tv");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ItemStack stack = this.getDisplayedItem();
        linkedFeedUuid = stack.get(CameraVision.LINKED_FEED_COMPONENT.get());
    }

    @Override
    public ItemInteractionResult interactWithPlayerItem(Player player, InteractionHand handIn, ItemStack stack, int slot) {

        ItemStack current = this.getDisplayedItem();
        if (current.isEmpty()) {
            //pop pop current
            Vec3 vec3 = Vec3.atLowerCornerWithOffset(this.worldPosition, 0.5, 1.01, 0.5)
                    .offsetRandom(this.level.random, 0.7F);
            ItemStack itemStack2 = current.copy();
            ItemEntity itemEntity = new ItemEntity(this.level, vec3.x(), vec3.y(), vec3.z(), itemStack2);
            itemEntity.setDefaultPickUpDelay();
            this.level.addFreshEntity(itemEntity);
            this.clearContent();
        }

        return super.interactWithPlayerItem(player, handIn, stack, slot);
    }
}
