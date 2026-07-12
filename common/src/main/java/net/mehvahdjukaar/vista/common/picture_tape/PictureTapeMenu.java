package net.mehvahdjukaar.vista.common.picture_tape;

import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu backing the picture tape. Slots 0..{@link #MAX_MAPS}-1 hold the stored maps; the screen
 * keeps them off-screen and renders/handles them itself. The remaining slots are the standard
 * player inventory. The map slots are always kept compact (no gaps) and mirrored back onto the
 * tape item's content component.
 */
public class PictureTapeMenu extends AbstractContainerMenu {

    public static final int MAX_MAPS = PictureTapeItem.MAX_MAPS;

    // slideshow speed range, in ticks per picture
    public static final int MIN_SPEED = 2;
    public static final int MAX_SPEED = 100;

    // player inventory geometry, shared with the screen so the two agree
    public static final int INV_X = 8;
    public static final int INV_TOP = 99;
    public static final int HOTBAR_Y = INV_TOP + 58;

    private final int tapeSlot;
    private final ItemStack tapeStack;
    private final Player owner;
    private final SimpleContainer tapeContent = new SimpleContainer(MAX_MAPS);
    // ticks each picture is shown when played in a TV
    private int playSpeed;
    // guards against re-entrant container updates while we rewrite the map slots
    private boolean mutating;

    public static PictureTapeMenu fromBuffer(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        return new PictureTapeMenu(containerId, playerInventory, buffer.readVarInt());
    }

    public PictureTapeMenu(int containerId, Inventory playerInventory, int tapeSlot) {
        this(VistaMod.PICTURE_TAPE_MENU.get(), containerId, playerInventory, tapeSlot);
    }

    protected PictureTapeMenu(MenuType<?> type, int containerId, Inventory playerInventory, int tapeSlot) {
        super(type, containerId);
        this.tapeSlot = tapeSlot;
        this.owner = playerInventory.player;
        this.tapeStack = playerInventory.getItem(tapeSlot);
        if (!(tapeStack.getItem() instanceof PictureTapeItem)) {
            throw new IllegalStateException("Expected a picture tape in slot " + tapeSlot + " but got " + tapeStack);
        }

        // load stored maps into the working container
        this.playSpeed = PictureTapeItem.getContent(tapeStack).playbackSpeed();
        this.mutating = true;
        List<ItemStack> stored = PictureTapeItem.getContent(tapeStack).pictures().toList();
        for (int i = 0; i < MAX_MAPS && i < stored.size(); i++) {
            tapeContent.setItem(i, stored.get(i).copy());
        }
        this.mutating = false;

        // map slots (0..MAX_MAPS-1), parked off-screen; the screen draws them as maps and routes clicks
        for (int i = 0; i < MAX_MAPS; i++) {
            this.addSlot(new MapSlot(tapeContent, i));
        }

        // player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new TapeAwareSlot(playerInventory, col + row * 9 + 9, INV_X + col * 18, INV_TOP + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new TapeAwareSlot(playerInventory, col, INV_X + col * 18, HOTBAR_Y));
        }

        // make sure the viewing player has the pixel data for every stored map
        pushMapData();
    }

    public SimpleContainer getTapeContent() {
        return tapeContent;
    }

    public int getFilledCount() {
        int n = 0;
        for (int i = 0; i < MAX_MAPS; i++) {
            if (!tapeContent.getItem(i).isEmpty()) n++;
        }
        return n;
    }

    /**
     * Number of cells the strip shows: every filled map plus one trailing "add" cell (unless full).
     */
    public int getVisibleCells() {
        int filled = getFilledCount();
        return filled < MAX_MAPS ? filled + 1 : MAX_MAPS;
    }

    public int getPlaySpeed() {
        return playSpeed;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // dropping a picture onto an existing one inserts it there and pushes the rest right;
        // fall back to vanilla's swap only when the tape is already full
        if (clickType == ClickType.PICKUP && button == 0 && slotId >= 0 && slotId < MAX_MAPS) {
            ItemStack carried = getCarried();
            int filled = getFilledCount();
            if (slotId < filled && filled < MAX_MAPS
                    && !carried.isEmpty() && PictureTapeItem.isValidEntry(carried)) {
                insertShifted(slotId, carried);
                compact();
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
        compact();
    }

    // Insert the carried item at index, shifting everything from index onward one slot to the right.
    private void insertShifted(int index, ItemStack carried) {
        this.mutating = true;
        for (int i = MAX_MAPS - 1; i > index; i--) {
            tapeContent.setItem(i, tapeContent.getItem(i - 1));
        }
        tapeContent.setItem(index, carried.split(1));
        this.mutating = false;
        setCarried(carried);
    }

    // The speed slider sends the desired ticks-per-picture as the button id.
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= MIN_SPEED && id <= MAX_SPEED) {
            this.playSpeed = id;
            compact();
            return true;
        }
        return false;
    }

    // Remove gaps, keep filled maps contiguous at the front, and persist to the tape item.
    private void compact() {
        if (mutating) return;
        List<ItemStack> filled = new ArrayList<>();
        for (int i = 0; i < MAX_MAPS; i++) {
            ItemStack s = tapeContent.getItem(i);
            if (!s.isEmpty()) filled.add(s);
        }
        boolean hasGap = false;
        for (int i = 0; i < MAX_MAPS; i++) {
            boolean shouldBeFilled = i < filled.size();
            if (tapeContent.getItem(i).isEmpty() == shouldBeFilled) {
                hasGap = true;
                break;
            }
        }
        if (hasGap) {
            this.mutating = true;
            for (int i = 0; i < MAX_MAPS; i++) {
                tapeContent.setItem(i, i < filled.size() ? filled.get(i) : ItemStack.EMPTY);
            }
            this.mutating = false;
        }
        PictureTapeItem.setContent(tapeStack, new PictureTapeContent(new ArrayList<>(filled), playSpeed));
        pushMapData();
    }

    // Server-side: send the map image data for every stored map to the viewing player, since maps
    // sitting inside the tape aren't ticked and would otherwise render blank on the client.
    private void pushMapData() {
        if (!(owner instanceof ServerPlayer serverPlayer)) return;
        for (int i = 0; i < MAX_MAPS; i++) {
            PictureTapeMaps.sendMapData(serverPlayer, tapeContent.getItem(i));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MAX_MAPS) {
                // map -> player inventory
                if (!this.moveItemStackTo(stack, MAX_MAPS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // player inventory -> tape, only filled maps
                if (!PictureTapeItem.isValidEntry(stack) || !this.moveItemStackTo(stack, 0, MAX_MAPS, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getInventory().getItem(tapeSlot).getItem() instanceof PictureTapeItem;
    }

    // Accepts only filled maps, one per slot. Kept off-screen: the screen renders these as maps.
    private static class MapSlot extends Slot {
        public MapSlot(SimpleContainer container, int index) {
            super(container, index, -9000, -9000);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return PictureTapeItem.isValidEntry(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    // The tape being edited can't be moved out of the inventory while its own menu is open.
    private class TapeAwareSlot extends Slot {
        public TapeAwareSlot(Inventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return this.getContainerSlot() != tapeSlot && super.mayPickup(player);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.getContainerSlot() != tapeSlot && super.mayPlace(stack);
        }
    }
}
