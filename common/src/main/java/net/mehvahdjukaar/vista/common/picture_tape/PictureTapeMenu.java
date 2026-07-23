package net.mehvahdjukaar.vista.common.picture_tape;

import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PictureTapeMenu extends AbstractContainerMenu {

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
    private final SimpleContainer tapeContent;
    private final int maxEntries;
    // ticks each picture is shown when played in a TV. synced so the client keeps it across screen re-inits
    private final DataSlot playSpeed = DataSlot.standalone();
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
        this.maxEntries = PictureTapeItem.getMaxEntries();
        this.tapeContent = new SimpleContainer(maxEntries);
        this.tapeSlot = tapeSlot;
        this.owner = playerInventory.player;
        this.tapeStack = playerInventory.getItem(tapeSlot);
        if (!(tapeStack.getItem() instanceof PictureTapeItem)) {
            throw new IllegalStateException("Expected a picture tape in slot " + tapeSlot + " but got " + tapeStack);
        }

        // load stored maps into the working container
        this.playSpeed.set(PictureTapeItem.getContent(tapeStack).playbackSpeed());
        this.addDataSlot(playSpeed);
        this.mutating = true;
        List<ItemStack> stored = PictureTapeItem.getContent(tapeStack).pictures().toList();
        for (int i = 0; i < this.maxEntries && i < stored.size(); i++) {
            tapeContent.setItem(i, stored.get(i).copy());
        }
        this.mutating = false;

        // map slots (0..this.maxEntries-1), parked off-screen; the screen draws them as maps and routes clicks
        for (int i = 0; i < this.maxEntries; i++) {
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
        for (int i = 0; i < this.maxEntries; i++) {
            if (!tapeContent.getItem(i).isEmpty()) n++;
        }
        return n;
    }

    /**
     * Number of cells the strip shows: every filled map plus one trailing "add" cell (unless full).
     */
    public int getVisibleCells() {
        int filled = getFilledCount();
        return filled < this.maxEntries ? filled + 1 : this.maxEntries;
    }

    public int getPlaySpeed() {
        return playSpeed.get();
    }

    // client-side echo: the button click only reaches the server, so mirror it locally
    // to bridge the round trip in case the screen re-inits in the meantime
    public void setPlaySpeedClientSide(int speed) {
        this.playSpeed.set(speed);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.PICKUP && button == 0 && slotId >= 0 && slotId < this.maxEntries
                && tryInsertCarried(slotId)) {
            compact();
            return;
        }
        super.clicked(slotId, button, clickType, player);
        compact();
    }

    // Drops the carried item into the clicked cell. False when vanilla should handle the click instead,
    // either because there's nothing to add or because the tape is already full.
    private boolean tryInsertCarried(int index) {
        ItemStack carried = getCarried();
        int filled = getFilledCount();
        int freeSpace = this.maxEntries - filled;
        if (carried.isEmpty() || freeSpace <= 0) return false;
        if (PictureTapeItem.isValidEntry(carried)) {
            // only inserting before an existing picture needs us: vanilla places it just fine on the trailing cell
            if (index >= filled) return false;
            insertShifted(index, List.of(carried.split(1)));
            setCarried(carried);
            return true;
        }
        // a pile of pictures goes in as the pictures it holds
        PictureTapeEntries.Unpacked unpacked = PictureTapeEntries.unpack(carried, freeSpace);
        if (unpacked == null) return false;
        insertShifted(Math.min(index, filled), unpacked.pictures());
        setCarried(unpacked.remainder());
        return true;
    }

    // Insert pictures at index, pushing everything from index onward to the right.
    private void insertShifted(int index, List<ItemStack> pictures) {
        List<ItemStack> entries = new ArrayList<>();
        for (int i = 0; i < this.maxEntries; i++) {
            ItemStack s = tapeContent.getItem(i);
            if (!s.isEmpty()) entries.add(s);
        }
        entries.addAll(Math.min(index, entries.size()), pictures);
        this.mutating = true;
        for (int i = 0; i < this.maxEntries; i++) {
            tapeContent.setItem(i, i < entries.size() ? entries.get(i) : ItemStack.EMPTY);
        }
        this.mutating = false;
    }

    // The speed slider sends the desired ticks-per-picture as the button id.
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= MIN_SPEED && id <= MAX_SPEED) {
            this.playSpeed.set(id);
            compact();
            return true;
        }
        return false;
    }

    // Remove gaps, keep filled maps contiguous at the front, and persist to the tape item.
    private void compact() {
        if (mutating) return;
        List<ItemStack> filled = new ArrayList<>();
        for (int i = 0; i < this.maxEntries; i++) {
            ItemStack s = tapeContent.getItem(i);
            if (!s.isEmpty()) filled.add(s);
        }
        boolean hasGap = false;
        for (int i = 0; i < this.maxEntries; i++) {
            boolean shouldBeFilled = i < filled.size();
            if (tapeContent.getItem(i).isEmpty() == shouldBeFilled) {
                hasGap = true;
                break;
            }
        }
        if (hasGap) {
            this.mutating = true;
            for (int i = 0; i < this.maxEntries; i++) {
                tapeContent.setItem(i, i < filled.size() ? filled.get(i) : ItemStack.EMPTY);
            }
            this.mutating = false;
        }
        PictureTapeItem.setContent(tapeStack, new PictureTapeContent(new ArrayList<>(filled), getPlaySpeed()));
        pushMapData();
    }

    // Server-side: send the map image data for every stored map to the viewing player, since maps
    // sitting inside the tape aren't ticked and would otherwise render blank on the client.
    private void pushMapData() {
        if (!(owner instanceof ServerPlayer serverPlayer)) return;
        for (int i = 0; i < this.maxEntries; i++) {
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
            if (index < this.maxEntries) {
                // map -> player inventory
                if (!this.moveItemStackTo(stack, this.maxEntries, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!PictureTapeItem.isValidEntry(stack)) {
                // a pile of pictures empties onto the end of the tape, as far as it fits
                int filled = getFilledCount();
                PictureTapeEntries.Unpacked unpacked = PictureTapeEntries.unpack(stack, this.maxEntries - filled);
                if (unpacked == null) return ItemStack.EMPTY;
                insertShifted(filled, unpacked.pictures());
                slot.set(unpacked.remainder());
                compact();
                return ItemStack.EMPTY;
            } else {
                // player inventory -> tape
                if (!this.moveItemStackTo(stack, 0, this.maxEntries, false)) {
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
