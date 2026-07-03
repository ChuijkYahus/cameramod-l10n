package net.mehvahdjukaar.vista.common.view_finder;

import com.google.common.base.Preconditions;
import net.mehvahdjukaar.moonlight.api.misc.IContainerProvider;
import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Container menu for the view finder. Mirrors the cannon menu but exposes a single
 * slot: the lens the view finder holds.
 */
public class ViewFinderMenu extends AbstractContainerMenu implements IContainerProvider {

    public final ViewFinderBlockEntity viewFinder;

    public ViewFinderMenu(int id, Inventory playerInventory, ViewFinderBlockEntity tile) {
        this(VistaMod.VIEWFINDER_MENU.get(), id, playerInventory, tile);
    }

    public <T extends ViewFinderMenu> ViewFinderMenu(MenuType<T> type, int id, Inventory playerInventory, ViewFinderBlockEntity tile) {
        super(type, id);
        this.viewFinder = Preconditions.checkNotNull(tile);

        checkContainerSize(tile, 1);
        if (this.viewFinder.isInWorld()) tile.startOpen(playerInventory.player);

        //single lens slot, centered in the lens aperture of the background
        this.addSlot(new LensSlot(tile, 0, 68, 34, this));

        for (int si = 0; si < 3; ++si)
            for (int sj = 0; sj < 9; ++sj)
                this.addSlot(new Slot(playerInventory, sj + (si + 1) * 9, 8 + sj * 18, 84 + si * 18));
        for (int si = 0; si < 9; ++si)
            this.addSlot(new Slot(playerInventory, si, 8 + si * 18, 142));
    }

    //client container factory
    public static ViewFinderMenu create(Integer id, Inventory playerInventory, FriendlyByteBuf buf) {
        TileOrEntityTarget target = TileOrEntityTarget.read(buf);
        Level level = playerInventory.player.level();
        BlockEntity tile = target.findTileOrContainedTile(level);
        if (tile instanceof ViewFinderBlockEntity vf) {
            return new ViewFinderMenu(VistaMod.VIEWFINDER_MENU.get(), id, playerInventory, vf);
        }
        throw new IllegalStateException("Could not find view finder tile for target " + target);
    }

    @Override
    public ViewFinderBlockEntity getContainer() {
        return viewFinder;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.viewFinder.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemCopy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack item = slot.getItem();
            itemCopy = item.copy();
            int containerSize = this.viewFinder.getContainerSize();
            if (index < containerSize) {
                if (!this.moveItemStackTo(item, containerSize, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.moveItemStackTo(item, 0, containerSize, false)) {
                return ItemStack.EMPTY;
            }

            if (item.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (item.getCount() == itemCopy.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, item);
        }
        return itemCopy;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (this.viewFinder.isInWorld()) this.viewFinder.stopOpen(player);
    }

    // slot that only accepts what the view finder allows (glass panes / heads) and keeps the menu in sync
    private static class LensSlot extends Slot {
        private final Runnable onChange;

        public LensSlot(Container inventory, int index, int x, int y, AbstractContainerMenu menu) {
            super(inventory, index, x, y);
            this.onChange = () -> menu.slotsChanged(inventory);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            onChange.run();
        }
    }
}
